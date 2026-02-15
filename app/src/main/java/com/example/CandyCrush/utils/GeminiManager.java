package com.example.CandyCrush.utils;

import android.text.TextUtils;

import com.example.CandyCrush.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiManager {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String[] MODEL_PATHS = new String[]{
            "v1beta/models/gemini-1.5-flash:generateContent",
            "v1beta/models/gemini-1.5-flash-latest:generateContent",
            "v1/models/gemini-1.5-flash:generateContent",
            "v1beta/models/gemini-1.5-pro:generateContent",
            "v1/models/gemini-1.5-pro:generateContent",
            "v1beta/models/gemini-2.0-flash:generateContent",
            "v1/models/gemini-2.0-flash:generateContent"
    };
    private static final int MAX_RETRIES_PER_MODEL = 2;
    private static final long DEFAULT_RETRY_DELAY_MS = 1500L;
    private static final long MAX_RETRY_DELAY_MS = 30000L;
    private static final int MAX_ERROR_CHARS = 160;
    private static final long DEFAULT_QUOTA_COOLDOWN_MS = 60000L;
    private static final Pattern RETRY_DELAY_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)(ms|s)");

    private static volatile long quotaBackoffUntilMs = 0L;
    private static GeminiManager instance;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build();

    public interface GeminiCallback {
        void onSuccess(String result);

        void onError(Throwable error);
    }

    private GeminiManager() {
    }

    public static synchronized GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }

    public boolean isConfigured() {
        return !TextUtils.isEmpty(BuildConfig.GEMINI_API_KEY);
    }

    public void sendText(String prompt, GeminiCallback callback) {
        if (!isConfigured()) {
            callback.onError(new IllegalStateException("Gemini API key not configured."));
            return;
        }
        if (TextUtils.isEmpty(prompt)) {
            callback.onError(new IllegalArgumentException("Prompt is empty."));
            return;
        }

        long now = System.currentTimeMillis();
        if (now < quotaBackoffUntilMs) {
            callback.onError(new IOException("Gemini temporarily unavailable (quota cooldown). Using local generation fallback."));
            return;
        }

        sendTextWithModel(prompt, callback, 0, 0);
    }

    private void sendTextWithModel(String prompt, GeminiCallback callback, int modelIndex, int retryCount) {
        if (modelIndex >= MODEL_PATHS.length) {
            callback.onError(new IOException("Gemini is unavailable for this API key/project. Using local generation fallback."));
            return;
        }

        String url = "https://generativelanguage.googleapis.com/"
                + MODEL_PATHS[modelIndex]
                + "?key="
                + BuildConfig.GEMINI_API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(buildPayload(prompt), JSON))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (retryCount < MAX_RETRIES_PER_MODEL) {
                    retryAfter(prompt, callback, modelIndex, retryCount + 1, DEFAULT_RETRY_DELAY_MS);
                    return;
                }

                if (modelIndex < MODEL_PATHS.length - 1) {
                    sendTextWithModel(prompt, callback, modelIndex + 1, 0);
                    return;
                }

                callback.onError(new IOException("Gemini network error. Using local generation fallback."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                int code = response.code();
                response.close();

                if (!response.isSuccessful()) {
                    handleErrorResponse(prompt, callback, modelIndex, retryCount, code, responseBody);
                    return;
                }

                String text = extractFirstTextPart(responseBody);
                if (TextUtils.isEmpty(text)) {
                    if (modelIndex < MODEL_PATHS.length - 1) {
                        sendTextWithModel(prompt, callback, modelIndex + 1, 0);
                        return;
                    }
                    callback.onError(new IOException("Gemini returned empty content. Using local generation fallback."));
                    return;
                }

                callback.onSuccess(text);
            }
        });
    }

    private void handleErrorResponse(String prompt,
                                     GeminiCallback callback,
                                     int modelIndex,
                                     int retryCount,
                                     int code,
                                     String errorBody) {
        ApiErrorDetails error = parseApiError(errorBody);

        if (code == 404) {
            if (modelIndex < MODEL_PATHS.length - 1) {
                sendTextWithModel(prompt, callback, modelIndex + 1, 0);
                return;
            }
            callback.onError(new IOException("Gemini endpoint not available for this key/project. Using local generation fallback."));
            return;
        }

        if (code == 429) {
            long retryDelay = error.retryDelayMs >= 0 ? error.retryDelayMs : DEFAULT_QUOTA_COOLDOWN_MS;
            setQuotaBackoff(retryDelay);

            if (modelIndex < MODEL_PATHS.length - 1) {
                sendTextWithModel(prompt, callback, modelIndex + 1, 0);
                return;
            }

            if (retryCount < MAX_RETRIES_PER_MODEL && error.retryDelayMs >= 0) {
                retryAfter(prompt, callback, modelIndex, retryCount + 1, error.retryDelayMs);
                return;
            }

            callback.onError(new IOException("Gemini quota/rate limit reached. Please enable billing or try again later. Using local generation fallback."));
            return;
        }

        if (code >= 500 && code <= 599 && retryCount < MAX_RETRIES_PER_MODEL) {
            retryAfter(prompt, callback, modelIndex, retryCount + 1, DEFAULT_RETRY_DELAY_MS);
            return;
        }

        if (code == 401 || code == 403) {
            callback.onError(new IOException("Gemini API key is invalid or billing is disabled. Using local generation fallback."));
            return;
        }

        String message = !TextUtils.isEmpty(error.message)
                ? shrinkMessage(error.message)
                : "Unexpected Gemini API error.";
        callback.onError(new IOException("Gemini error " + code + ": " + message + " Using local generation fallback."));
    }

    private void setQuotaBackoff(long delayMs) {
        long safeDelayMs = Math.max(DEFAULT_QUOTA_COOLDOWN_MS, clampRetryDelay(delayMs));
        quotaBackoffUntilMs = System.currentTimeMillis() + safeDelayMs;
    }

    private void retryAfter(String prompt,
                            GeminiCallback callback,
                            int modelIndex,
                            int retryCount,
                            long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(clampRetryDelay(delayMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sendTextWithModel(prompt, callback, modelIndex, retryCount);
        }).start();
    }

    private long clampRetryDelay(long delayMs) {
        if (delayMs < 0) {
            return DEFAULT_RETRY_DELAY_MS;
        }
        return Math.min(delayMs, MAX_RETRY_DELAY_MS);
    }

    private String buildPayload(String prompt) {
        try {
            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject root = new JSONObject();
            root.put("contents", contents);
            return root.toString();
        } catch (Exception e) {
            return "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt + "\"}]}]}";
        }
    }

    private String extractFirstTextPart(String responseBody) {
        try {
            JSONObject root = new JSONObject(responseBody);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) {
                return null;
            }
            JSONObject candidate = candidates.optJSONObject(0);
            if (candidate == null) {
                return null;
            }
            JSONObject content = candidate.optJSONObject("content");
            if (content == null) {
                return null;
            }
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null || parts.length() == 0) {
                return null;
            }
            JSONObject firstPart = parts.optJSONObject(0);
            if (firstPart == null || !firstPart.has("text")) {
                return null;
            }
            return firstPart.optString("text", null);
        } catch (Exception e) {
            return null;
        }
    }

    private ApiErrorDetails parseApiError(String body) {
        ApiErrorDetails details = new ApiErrorDetails();
        if (TextUtils.isEmpty(body)) {
            return details;
        }

        try {
            JSONObject root = new JSONObject(body);
            JSONObject error = root.optJSONObject("error");
            if (error == null) {
                return details;
            }

            details.message = error.optString("message", "");
            details.status = error.optString("status", "");

            JSONArray apiDetails = error.optJSONArray("details");
            if (apiDetails != null) {
                for (int i = 0; i < apiDetails.length(); i++) {
                    JSONObject item = apiDetails.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    String retryDelay = item.optString("retryDelay", "");
                    if (!TextUtils.isEmpty(retryDelay)) {
                        long ms = parseDurationToMs(retryDelay);
                        if (ms >= 0) {
                            details.retryDelayMs = ms;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            String lower = body.toLowerCase(Locale.US);
            if (lower.contains("quota") || lower.contains("rate")) {
                details.status = "RESOURCE_EXHAUSTED";
            }
            details.message = body;
        }

        return details;
    }

    private long parseDurationToMs(String text) {
        Matcher matcher = RETRY_DELAY_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            return -1L;
        }

        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2);
        if ("ms".equals(unit)) {
            return (long) value;
        }
        return (long) (value * 1000L);
    }

    private String shrinkMessage(String message) {
        if (TextUtils.isEmpty(message)) {
            return "Unknown error.";
        }
        String oneLine = message.replace('\n', ' ').replace("  ", " ").trim();
        int bullet = oneLine.indexOf("*");
        if (bullet > 0) {
            oneLine = oneLine.substring(0, bullet).trim();
        }
        if (oneLine.length() > MAX_ERROR_CHARS) {
            return oneLine.substring(0, MAX_ERROR_CHARS - 3) + "...";
        }
        return oneLine;
    }

    private static class ApiErrorDetails {
        String message = "";
        String status = "";
        long retryDelayMs = -1L;
    }
}
