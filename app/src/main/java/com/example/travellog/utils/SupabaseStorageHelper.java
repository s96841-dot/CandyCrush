package com.example.travellog.utils;
import android.util.Log;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseStorageHelper {
    private static final String supabaseUrl = "https://jipzbfudpaddedjqdwix.supabase.co";

    private static final String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImppcHpiZnVkcGFkZGVkanFkd2l4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc0OTEyNjEsImV4cCI6MjA3MzA2NzI2MX0.TqsncFShZULyk4p15CKdlUNQopzy8OonH3j8OCN-lhY";

    private static final String SUPABASE_BUCKET = "Travel Log";

    private static final String TAG = "SupabaseStorageHelper";

    public static void uploadPicture(final File file, final String filePath, OnResultCallback callback) {
        try {

            Log.i(TAG, "uploadPicture: Uploading file to Supabase: " + filePath);

            OkHttpClient client = new OkHttpClient.Builder().build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(supabaseUrl + "/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            SupabaseStorageService service = retrofit.create(SupabaseStorageService.class);

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", filePath, requestFile);

            String bearer = "Bearer " + supabaseKey;
            Call<ResponseBody> call = service.uploadFile(
                    supabaseKey,
                    bearer,
                    SUPABASE_BUCKET,
                    filePath,
                    body
            );
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String publicUrl =  getFileSupabaseUrl(filePath);
                        Log.i(TAG, "uploadPicture: Profile picture uploaded successfully to Supabase. Public URL: " + publicUrl);
                        callback.onResult(true, publicUrl, null);
                    } else {
                        Log.e(TAG, "uploadPicture: Supabase upload failed: " + response.message());
                        callback.onResult(false, null, response.message());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "Supabase upload failed", t);
                    callback.onResult(false, null, t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception during Supabase upload", e);
            callback.onResult(false, null, e.getMessage());
        }
    }

    public static String getFileSupabaseUrl(String filePath) {
        return supabaseUrl + "/storage/v1/object/public/" + SUPABASE_BUCKET + "/" + filePath;
    }

    public interface OnResultCallback {
        void onResult(boolean success, String url, String error);
    }
}
