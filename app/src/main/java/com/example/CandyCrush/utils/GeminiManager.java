package com.example.CandyCrush.utils;

import android.text.TextUtils;

import com.example.CandyCrush.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiManager {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static GeminiManager instance;

    private final OkHttpClient httpClient = new OkHttpClient();

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

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY)
                .post(RequestBody.create(buildPayload(prompt), JSON))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new IOException("Gemini error: " + response.code()));
                    return;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                String text = extractFirstTextPart(responseBody);
                if (TextUtils.isEmpty(text)) {
                    callback.onError(new IOException("Gemini returned empty content."));
                    return;
                }
                callback.onSuccess(text);
            }
        });
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
}
