package com.example.travellog; // Make sure this matches your project's package name

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable; // Added for @Nullable
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";

    private EditText emailEditText, passwordEditText, nicknameEditText, ageEditText;
    private Button registerButton, choosePictureButton;
    private ImageView profileImageView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference; // Top-level storage reference

    private Uri imageUri; // This will hold the URI from gallery or camera after processing
    private Uri cameraImageUri; // Temporary URI for camera output

    private ActivityResultLauncher<Intent> galleryImagePickerLauncher;
    private ActivityResultLauncher<Uri> cameraImageLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        nicknameEditText = findViewById(R.id.et_nickname);
        ageEditText = findViewById(R.id.et_age);
        registerButton = findViewById(R.id.btn_register);
        choosePictureButton = findViewById(R.id.btn_choose_picture);
        profileImageView = findViewById(R.id.iv_profile_picture);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference(); // Initialize storage reference

        galleryImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        imageUri = result.getData().getData();
                        Log.d(TAG, "Gallery image selected: " + imageUri);
                        displayImage(imageUri);
                    } else {
                        Log.d(TAG, "Gallery image picking cancelled or failed.");
                    }
                }
        );

        cameraImageLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        imageUri = cameraImageUri; // Use the URI where the camera saved the full-size image
                        Log.d(TAG, "Camera image captured successfully to: " + imageUri);
                        displayImage(imageUri);
                    } else {
                        Log.d(TAG, "Camera image capture cancelled or failed.");
                    }
                }
        );

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Camera permission granted by user.");
                        launchCamera();
                    } else {
                        Log.d(TAG, "Camera permission denied by user.");
                        Toast.makeText(this, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show();
                    }
                });

        if (choosePictureButton != null) {
            choosePictureButton.setOnClickListener(v -> showImageSourceDialog());
        } else {
            Log.e(TAG, "Choose picture button (btn_choose_picture) not found!");
        }

        if (registerButton != null) {
            registerButton.setOnClickListener(v -> {
                Log.d(TAG, "Register button clicked.");
                performRegistration();
            });
        } else {
            Log.e(TAG, "Register button (btn_register) not found!");
        }
    }

    private void displayImage(Uri uriToDisplay) {
        Log.d(TAG, "displayImage - URI received: " + (uriToDisplay != null ? uriToDisplay.toString() : "null"));
        if (uriToDisplay != null) {
            Glide.with(this)
                    .load(uriToDisplay)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(profileImageView);
        } else {
            Log.w(TAG, "displayImage: URI to display is null.");
            profileImageView.setImageResource(android.R.drawable.ic_menu_camera);
        }
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Profile Picture");
        String[] options = {"Select from Gallery", "Take Photo"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                launchGallery();
            } else if (which == 1) {
                checkCameraPermissionAndLaunch();
            }
        });
        builder.show();
    }

    private void launchGallery() {
        Log.d(TAG, "launchGallery: Opening gallery.");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryImagePickerLauncher.launch(intent);
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission already granted. Launching camera.");
            launchCamera();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Log.d(TAG, "Showing camera permission rationale.");
            new AlertDialog.Builder(this)
                    .setTitle("Camera Permission Needed")
                    .setMessage("This app needs camera access to allow you to take a profile picture.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        Log.d(TAG, "User agreed to rationale. Requesting permission again.");
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        Log.d(TAG, "User cancelled rationale dialog.");
                        dialog.dismiss();
                    })
                    .create().show();
        } else {
            Log.d(TAG, "Requesting camera permission (no rationale needed).");
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory for images: " + storageDir.getAbsolutePath());
                throw new IOException("Failed to create directory " + storageDir.getAbsolutePath());
            }
        }
        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        Log.d(TAG, "Image file created: " + imageFile.getAbsolutePath());
        return imageFile;
    }

    private void launchCamera() {
        Log.d(TAG, "launchCamera: Attempting to launch camera.");
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e(TAG, "Error occurred while creating the image file for camera", ex);
            Toast.makeText(this, "Could not create image file for camera.", Toast.LENGTH_SHORT).show();
            return;
        }

        // cameraImageUri will store the URI for the file where the camera should save the image
        cameraImageUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider", // Authority must match AndroidManifest.xml
                photoFile
        );
        Log.d(TAG, "Camera will save image to temporary URI: " + cameraImageUri);
        cameraImageLauncher.launch(cameraImageUri); // Pass this URI to the camera
    }

    private void performRegistration() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String nickname = nicknameEditText.getText().toString().trim();
        String ageString = ageEditText.getText().toString().trim();

        // --- Start Input Validations ---
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            emailEditText.requestFocus();
            Log.w(TAG, "performRegistration: Invalid email.");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            Log.w(TAG, "performRegistration: Invalid password.");
            return;
        }
        if (TextUtils.isEmpty(nickname)) {
            nicknameEditText.setError("Nickname is required");
            nicknameEditText.requestFocus();
            Log.w(TAG, "performRegistration: Nickname empty.");
            return;
        }
        if (TextUtils.isEmpty(ageString)) {
            ageEditText.setError("Age is required");
            ageEditText.requestFocus();
            Log.w(TAG, "performRegistration: Age empty.");
            return;
        }
        // Crucial check for imageUri before proceeding with registration
        if (imageUri == null) {
            Toast.makeText(this, "Please select or take a profile picture.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "performRegistration: Profile picture (imageUri) is null.");
            return; // Stop registration if no image is selected/taken
        }
        int age;
        try {
            age = Integer.parseInt(ageString);
            if (age <= 0 || age > 120) {
                ageEditText.setError("Enter a valid age");
                ageEditText.requestFocus();
                Log.w(TAG, "performRegistration: Invalid age number.");
                return;
            }
        } catch (NumberFormatException e) {
            ageEditText.setError("Enter a valid number for age");
            ageEditText.requestFocus();
            Log.w(TAG, "performRegistration: Age not a number.");
            return;
        }
        // --- End Input Validations ---

        Log.d(TAG, "performRegistration: All validations passed. Attempting to create user with email: " + email);
        // TODO: Show a loading indicator here

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // TODO: Hide loading indicator here
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // ImageUri is already checked for null before this point
                            uploadImageToFirebaseStorage(firebaseUser, nickname, age, imageUri);
                        } else {
                            Log.e(TAG, "createUserWithEmail:success but firebaseUser is null!");
                            Toast.makeText(RegistrationActivity.this, "Registration succeeded but failed to get user details.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegistrationActivity.this, "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Changed to accept imageUri as a parameter to be explicit
    private void uploadImageToFirebaseStorage(FirebaseUser firebaseUser, String nickname, int age, Uri imageUriToUpload) {
        Log.d(TAG, "uploadImageToFirebaseStorage - imageUri to upload: " + imageUriToUpload);

        // Define the path and filename in Firebase Storage
        // Using UID for the main folder is good. UUID for filename ensures uniqueness if user re-uploads.
        final StorageReference profileImageRef = storageReference
                .child("profile_pictures/" + firebaseUser.getUid() + "/" + UUID.randomUUID().toString() + ".jpg");

        Log.d(TAG, "uploadImageToFirebaseStorage: Uploading from URI: " + imageUriToUpload + " to " + profileImageRef.getPath());
        // TODO: Show a loading indicator for image upload

        profileImageRef.putFile(imageUriToUpload)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully to Firebase Storage. Getting download URL...");
                    profileImageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String imageUrl = downloadUri.toString();
                        Log.i(TAG, "Image download URL from Firebase Storage: " + imageUrl); // Changed to Info for emphasis
                        updateUserProfileAndSaveData(firebaseUser, nickname, age, imageUrl);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get image download URL from Firebase Storage", e);
                        Toast.makeText(RegistrationActivity.this, "Image uploaded but failed to get URL. Profile saved without image.", Toast.LENGTH_LONG).show();
                        updateUserProfileAndSaveData(firebaseUser, nickname, age, null); // Pass null if URL retrieval fails
                    });
                })
                .addOnFailureListener(e -> {
                    // TODO: Hide loading indicator for image upload
                    Log.e(TAG, "Image upload to Firebase Storage failed", e);
                    Toast.makeText(RegistrationActivity.this, "Image upload failed. Profile saved without image.", Toast.LENGTH_LONG).show();
                    updateUserProfileAndSaveData(firebaseUser, nickname, age, null); // Pass null if upload fails
                })
                .addOnCompleteListener(task -> {
                    // This will be called after success or failure of putFile,
                    // but before getDownloadUrl finishes if putFile was successful.
                    // Might be a good place to hide a general image upload progress.
                    Log.d(TAG, "putFile task completed (success or failure).");
                });
    }

    // Added @Nullable for imageUrl to indicate it can be null
    private void updateUserProfileAndSaveData(FirebaseUser firebaseUser, String nickname, int age, @Nullable String imageUrl) {
        Log.d(TAG, "updateUserProfileAndSaveData - Received imageUrl: " + (imageUrl != null ? imageUrl : "null"));

        UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest.Builder()
                .setDisplayName(nickname);

        // Optionally update Firebase Auth user photo URI
        // if (imageUrl != null) {
        // profileUpdatesBuilder.setPhotoUri(Uri.parse(imageUrl));
        // }
        UserProfileChangeRequest profileUpdates = profileUpdatesBuilder.build();

        firebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated in Firebase Auth (displayName).");
                    } else {
                        Log.w(TAG, "Failed to update user profile in Firebase Auth (displayName).", task.getException());
                    }
                });

        Map<String, Object> user = new HashMap<>();
        user.put("uid", firebaseUser.getUid()); // Good to store UID explicitly
        user.put("email", firebaseUser.getEmail());
        user.put("nickname", nickname);
        user.put("age", age);
        user.put("level", 1); // Default starting level, if you have one

        // --- THIS IS THE KEY FIX ---
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            user.put("profileImageUrl", imageUrl);
            Log.d(TAG, "Firestore: Saving with profileImageUrl: " + imageUrl);
        } else {
            user.put("profileImageUrl", null); // Store null if no valid image URL
            Log.d(TAG, "Firestore: Saving with profileImageUrl: null");
        }
        // --- END KEY FIX ---

        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "User data successfully written to Firestore!"); // Changed to Info
                    Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    // Navigate to LoginActivity or your main app screen
                    Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finishAffinity(); // Finishes this activity and all parent activities
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error writing user document to Firestore", e);
                    Toast.makeText(RegistrationActivity.this, "Failed to save all user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Consider what to do here. If Firestore save fails, user is created in Auth but not in DB.
                    // Maybe sign out the user? Or provide a retry mechanism?
                    // mAuth.getCurrentUser().delete(); // Drastic: deletes the auth user too. Use with caution.
                });
    }
}
