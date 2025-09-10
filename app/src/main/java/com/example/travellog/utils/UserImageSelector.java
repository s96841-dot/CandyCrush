package com.example.travellog.utils;
import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class UserImageSelector {

    private AppCompatActivity activity;

    private ImageView imageView;

    private Uri imageUri;
    private Bitmap imageBitmap;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private static final String TAG = "UserImageSelector";

    public UserImageSelector(AppCompatActivity activity, ImageView imageView){
        this.activity = activity;
        this.imageView = imageView;
        this.imageUri = null;
        this.imageBitmap = null;
        initResultLaunchers();
    }
    public void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(activity)
                .setTitle("Select Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Log.d(TAG, "User chose to take a photo");
                        openCamera();
                    } else {
                        Log.d(TAG, "User chose to pick from gallery");
                        openImagePicker();
                    }
                })
                .show();
    }
    private void openCamera() {
        Log.d(TAG, "openCamera: start");

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
        Log.d(TAG, "openCamera: end");
    }

    private void openImagePicker() {
        Log.d(TAG, "openImagePicker: start");


        // Launch the photo picker and let the user choose images and videos.
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly
                        .INSTANCE)
                .build());


        Log.d(TAG, "openImagePicker: end");
    }

    private void initResultLaunchers()
    {
        Log.d(TAG, "InitResultLaunchers: start");
        // Registers a photo picker activity launcher in single-select mode.
        pickMedia =
                activity.registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    // Callback is invoked after the user selects a media item or closes the
                    // photo picker.
                    if (uri != null) {
                        Log.d(TAG, "PhotoPicker: Selected URI: " + uri);
                        this.imageUri = uri;
                        imageView.setImageURI(uri);
                    } else {
                        Log.d(TAG, "PhotoPicker: No media selected");
                    }
                });

        cameraLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Got a camera result");
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Camera result code is ok");
                        // Handle successful photo capture
                        Intent data = result.getData();
                        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                        if (bitmap != null) {
                            Log.d(TAG, "setting bitmap");
                            imageView.setImageBitmap(bitmap);
                            this.imageBitmap = bitmap;
                        } else {
                            Log.e(TAG, "Error retrieving image from camera intent");
                        }
                    } else {
                        Log.d(TAG, "Invalid code returned from camera intent");
                    }
                }
        );

        Log.d(TAG, "InitResultLaunchers: done");
    }

    public File createImageFile()
    {
        if(imageUri != null)
        {
            Log.d(TAG, "createImageFile: creating image from uri");
            return ImageFileCreator.createTempFileFromUri(imageUri, activity);
        }
        else if(imageBitmap != null)
        {
            Log.d(TAG, "createImageFile: creating image from bitmap");
            return ImageFileCreator.createTempFileFromBitmap(imageBitmap, activity);
        }

        Log.w(TAG, "createImageFile: source is not defined");
        return null;
    }

}
