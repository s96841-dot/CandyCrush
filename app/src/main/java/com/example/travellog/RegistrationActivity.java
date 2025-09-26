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
    private StorageReference storageReference;

    private Uri imageUri; // This will hold the URI from gallery or camera after processing
    private Uri cameraImageUri; // Temporary URI for camera output

    private ActivityResultLauncher<Intent> galleryImagePickerLauncher;
    private ActivityResultLauncher<Uri> cameraImageLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure you have a layout file named "activity_registration.xml"
        // in your res/layout directory.
        setContentView(R.layout.activity_registration);

        // Make sure these IDs match the IDs in your activity_registration.xml
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        nicknameEditText = findViewById(R.id.et_nickname);
        ageEditText = findViewById(R.id.et_age);
        registerButton = findViewById(R.id.btn_register);
        choosePictureButton = findViewById(R.id.btn_choose_picture);
        profileImageView = findViewById(R.id.iv_profile_picture);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        // --- Initialize ActivityResultLauncher for GALLERY image picking ---
        galleryImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        imageUri = result.getData().getData(); // Store the selected image URI
                        Log.d(TAG, "Gallery image selected: " + (imageUri != null ? imageUri.toString() : "null"));
                        displayImage(imageUri);
                    } else {
                        Log.d(TAG, "Gallery image picking cancelled or failed.");
                    }
                }
        );

        // --- Initialize ActivityResultLauncher for CAMERA image capture ---
        cameraImageLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        // The image was saved to cameraImageUri specified in launchCamera()
                        // Now use cameraImageUri as the source.
                        imageUri = cameraImageUri; // Update the main imageUri
                        Log.d(TAG, "Camera image captured successfully to: " + (imageUri != null ? imageUri.toString() : "null"));
                        displayImage(imageUri);
                    } else {
                        Log.d(TAG, "Camera image capture cancelled or failed.");
                    }
                }
        );

        // --- Initialize ActivityResultLauncher for CAMERA PERMISSION request ---
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Camera permission granted by user.");
                        launchCamera(); // Permission granted, now launch camera
                    } else {
                        Log.d(TAG, "Camera permission denied by user.");
                        Toast.makeText(this, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show();
                    }
                });


        if (choosePictureButton != null) {
            choosePictureButton.setOnClickListener(v -> showImageSourceDialog());
        } else {
            Log.e(TAG, "Choose picture button (btn_choose_picture) not found! Check your layout file and findViewById call.");
        }

        if (registerButton != null) {
            registerButton.setOnClickListener(v -> {
                Log.d(TAG, "Register button clicked.");
                performRegistration();
            });
        } else {
            Log.e(TAG, "Register button (btn_register) not found! Check your layout file and findViewById call.");
        }
    }

    private void displayImage(Uri uriToDisplay) {
        // --- ADDED LOGGING ---
        Log.d(TAG, "displayImage - URI received: " + (uriToDisplay != null ? uriToDisplay.toString() : "null"));
        // --- END ADDED LOGGING ---
        if (uriToDisplay != null) {
            Glide.with(this)
                    .load(uriToDisplay)
                    .placeholder(android.R.drawable.ic_menu_camera) // default placeholder
                    .error(android.R.drawable.ic_dialog_alert) // error placeholder
                    .into(profileImageView);
        } else {
            Log.w(TAG, "displayImage: URI to display is null.");
            profileImageView.setImageResource(android.R.drawable.ic_menu_camera); // Set a default
        }
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Profile Picture");
        String[] options = {"Select from Gallery", "Take Photo"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) { // Gallery
                launchGallery();
            } else if (which == 1) { // Camera
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
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        Log.d(TAG, "Image file created: " + imageFile.getAbsolutePath());
        return imageFile;
    }


    private void launchCamera() {
        Log.d(TAG, "launchCamera: Attempting to launch camera.");
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e(TAG, "Error occurred while creating the image file for camera", ex);
            Toast.makeText(this, "Could not create image file for camera.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            // Use getPackageName() to construct the authority string dynamically and correctly.
            String authority = getPackageName() + ".provider";
            Log.d(TAG, "FileProvider authority determined as: " + authority);

            // Store the URI for the camera to write to.
            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    authority, // Use the dynamically determined authority
                    photoFile
            );
            Log.d(TAG, "Camera will save image to temporary URI: " + (cameraImageUri != null ? cameraImageUri.toString() : "null"));
            cameraImageLauncher.launch(cameraImageUri); // Pass the output URI to the camera
        } else {
            Log.e(TAG, "launchCamera: photoFile is null after trying to create it.");
        }
    }


    private void performRegistration() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String nickname = nicknameEditText.getText().toString().trim();
        String ageString = ageEditText.getText().toString().trim();

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
        if (imageUri == null) {
            Toast.makeText(this, "Please select or take a profile picture.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "performRegistration: Profile picture not selected/taken.");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageString);
            if (age <= 0 || age > 120) { // Basic age validation
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

        Log.d(TAG, "performRegistration: Attempting to create user with email: " + email);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            uploadImageToFirebaseStorage(firebaseUser, nickname, age);
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

    private void uploadImageToFirebaseStorage(FirebaseUser firebaseUser, String nickname, int age) {
        // --- ADDED LOGGING ---
        Log.d(TAG, "uploadImageToFirebaseStorage - imageUri to upload: " + (imageUri != null ? imageUri.toString() : "null"));
        // --- END ADDED LOGGING ---
        if (imageUri != null) {
            // Create a unique path for the image in Firebase Storage
            final StorageReference profileImageRef = storageReference.child("profile_pictures/" + firebaseUser.getUid() + "/" + UUID.randomUUID().toString() + ".jpg");
            Log.d(TAG, "uploadImageToFirebaseStorage: Uploading from URI: " + imageUri.toString() + " to " + profileImageRef.getPath());

            profileImageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Image uploaded successfully to Firebase Storage.");
                        profileImageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            String imageUrl = downloadUri.toString();
                            Log.d(TAG, "Image download URL from Firebase Storage: " + imageUrl);
                            updateUserProfileAndSaveData(firebaseUser, nickname, age, imageUrl);
                        }).addOnFailureListener(e -> {
                            Log.w(TAG, "Failed to get image download URL from Firebase Storage", e);
                            Toast.makeText(RegistrationActivity.this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            // Still proceed to save user data, but without the image URL
                            updateUserProfileAndSaveData(firebaseUser, nickname, age, null);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Image upload to Firebase Storage failed", e);
                        Toast.makeText(RegistrationActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Still proceed to save user data, but without the image URL
                        updateUserProfileAndSaveData(firebaseUser, nickname, age, null);
                    });
        } else {
            Log.w(TAG, "uploadImageToFirebaseStorage: imageUri is null, proceeding without image upload.");
            updateUserProfileAndSaveData(firebaseUser, nickname, age, null); // No image selected/taken
        }
    }

    private void updateUserProfileAndSaveData(FirebaseUser firebaseUser, String nickname, int age, String imageUrl) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(nickname)
                // If you also want to set the photo URI in the Firebase Auth user profile:
                // .setPhotoUri(imageUrl != null ? Uri.parse(imageUrl) : null)
                .build();

        firebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated in Firebase Auth (nickname).");
                    } else {
                        Log.w(TAG, "Failed to update user profile in Firebase Auth (nickname).", task.getException());
                    }
                });

        // Create a user map to save to Firestore
        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        user.put("age", age);
        user.put("email", firebaseUser.getEmail()); // Save email for reference
        if (imageUrl != null) {
            user.put("profileImageUrl", imageUrl);
        } else {
            user.put("profileImageUrl", ""); // Or handle as null, or a default placeholder URL
        }
        user.put("uid", firebaseUser.getUid()); // Save UID

        // Save user data to Firestore
        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data successfully written to Firestore!");
                    Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    // Navigate to another activity, e.g., LoginActivity or MainActivity
                    // Ensure LoginActivity exists or change to your main activity
                    Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish(); // Finish RegistrationActivity so user can't go back to it
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing user document to Firestore", e);
                    Toast.makeText(RegistrationActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Optionally, you might want to sign out the user here or implement retry logic
                    // For example: FirebaseAuth.getInstance().signOut();
                });
    }
}
