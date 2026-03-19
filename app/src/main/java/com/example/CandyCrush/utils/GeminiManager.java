package com.example.CandyCrush.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executor;

public class GeminiManager {
    private static GeminiManager instance;

    private static final String MODEL_VERSION = "gemini-2.5-flash";
    private static final String TAG = "GeminiManager";

    private GeminiManager() {
    }

    public static GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }

    public void sendText(String promptStr, Context context, GeminiCallback callback) {
        send(promptStr, null, null, null, context, callback);
    }

    public void sendImageAndText(Bitmap bitmap, String promptStr, Context context, GeminiCallback callback) {
        send(promptStr, bitmap, null, null, context, callback);
    }

    public void sendFileAndText(Uri fileUri, String mimeType, String promptStr, Context context, GeminiCallback callback) {
        byte[] bytes;
        try (InputStream in = context.getContentResolver().openInputStream(fileUri)) {
            if (in == null) {
                throw new IllegalStateException("Unable to open file: " + fileUri);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            int n;
            while ((n = in.read(tmp)) != -1) {
                buffer.write(tmp, 0, n);
            }
            bytes = buffer.toByteArray();
        } catch (Exception e) {
            callback.onError(e);
            return;
        }

        send(promptStr, null, bytes, mimeType, context, callback);
    }

    private void send(String promptStr, Bitmap bitmap, byte[] bytes, String mimeType, Context context, GeminiCallback callback) {
        if (TextUtils.isEmpty(promptStr)) {
            callback.onError(new IllegalArgumentException("Prompt cannot be empty."));
            return;
        }

        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(MODEL_VERSION);
        GenerativeModelFutures model = GenerativeModelFutures.from(ai);

        Content.Builder builder = new Content.Builder();
        if (bitmap != null) {
            builder.addImage(bitmap);
        }
        if (bytes != null) {
            String resolvedMimeType = TextUtils.isEmpty(mimeType) ? "application/octet-stream" : mimeType;
            builder.addInlineData(bytes, resolvedMimeType);
        }
        Content prompt = builder.addText(promptStr).build();

        Executor executor = ContextCompat.getMainExecutor(context);
        ListenableFuture<GenerateContentResponse> response = model.generateContent(prompt);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result != null ? result.getText() : null;
                if (TextUtils.isEmpty(text)) {
                    callback.onError(new IllegalStateException("Gemini returned empty content."));
                    return;
                }
                callback.onSuccess(text);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Gemini request failed", t);
                callback.onError(t);
            }
        }, executor);
    }

    public interface GeminiCallback {
        void onSuccess(String result);

        void onError(Throwable error);
    }
}