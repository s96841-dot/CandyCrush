package com.example.CandyCrush.utils;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;


public interface SupabaseStorageService {
    @Multipart
    @POST("/storage/v1/object/{bucket}/{filename}")
    Call<ResponseBody> uploadFile(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Path("bucket") String bucket,
            @Path("filename") String filename,
            @Part MultipartBody.Part file
    );
}
