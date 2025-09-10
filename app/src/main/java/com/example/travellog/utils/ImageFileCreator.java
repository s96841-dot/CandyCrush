package com.example.travellog.utils;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageFileCreator {
    private static final String TAG = "ImageFileCreator";

    public static File createTempFileFromUri(Uri uri, Context context) {
        Log.d(TAG, "Creating temp file from URI: " + uri);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File tempFile = File.createTempFile("upload", ".jpg", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
            Log.d(TAG, "Temp file created: " + tempFile.getAbsolutePath());
            return tempFile;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "createTempFileFromUri: failed: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, "createTempFileFromUri: failed: " + e.getMessage());
            return null;
        }
    }

    public static File createTempFileFromBitmap(Bitmap bitmap, Context context) {
        try {
            File tempFile = File.createTempFile("upload", ".jpg", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
            Log.d(TAG, "Temp file created from bitmap: " + tempFile.getAbsolutePath());
            return tempFile;
        } catch (IOException e) {
            Log.d(TAG, "Temp file creation failed: " + e.getMessage());
            return null;
        }
    }
}
