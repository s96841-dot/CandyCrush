package com.example.CandyCrush; // Make sure this matches your project's package name

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
// Removed image related imports:
// import android.Manifest;
// import android.content.pm.PackageManager;
// import android.net.Uri;
// import android.os.Environment;
// import android.provider.MediaStore;
// import androidx.activity.result.ActivityResultLauncher;
// import androidx.activity.result.contract.ActivityResultContracts;
// import androidx.appcompat.app.AlertDialog;
// import androidx.core.app.ActivityCompat;
// import androidx.core.content.ContextCompat;
// import androidx.core.content.FileProvider;
// import com.bumptech.glide.Glide;
// import com.google.firebase.storage.FirebaseStorage;
// import com.google.firebase.storage.StorageReference;
// import java.io.File;
// import java.io.IOException;
// import java.text.SimpleDateFormat;
// import java.util.Date;
// import java.util.UUID;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";

    private EditText emailEditText, passwordEditText, nicknameEditText, ageEditText;
    private Button registerButton;
    // Removed: private Button choosePictureButton;
    // Removed: private ImageView profileImageView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    // Removed: private StorageReference storageReference;

    // Removed: private Uri imageUri;
    // Removed: private Uri cameraImageUri;

    // Removed: private ActivityResultLauncher<Intent> galleryImagePickerLauncher;
    // Removed: private ActivityResultLauncher<Uri> cameraImageLauncher;
    // Removed: private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        nicknameEditText = findViewById(R.id.et_nickname);
        ageEditText = findViewById(R.id.et_age);
        registerButton = findViewById(R.id.btn_register);
        // Removed: choosePictureButton = findViewById(R.id.btn_choose_picture);
        // Removed: profileImageView = findViewById(R.id.iv_profile_picture);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Removed: storageReference = FirebaseStorage.getInstance().getReference();

        // Removed all ActivityResultLauncher initializations for image handling

        // Removed: choosePictureButton click listener
        // if (choosePictureButton != null) {
        // choosePictureButton.setOnClickListener(v -> showImageSourceDialog());
        // } else {
        // Log.e(TAG, "Choose picture button (btn_choose_picture) not found!");
        // }

        if (registerButton != null) {
            registerButton.setOnClickListener(v -> {
                Log.d(TAG, "Register button clicked.");
                performRegistration();
            });
        } else {
            Log.e(TAG, "Register button (btn_register) not found!");
        }
    }

    // Removed: displayImage method
    // Removed: showImageSourceDialog method
    // Removed: launchGallery method
    // Removed: checkCameraPermissionAndLaunch method
    // Removed: createImageFile method
    // Removed: launchCamera method

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
        // Removed: Crucial check for imageUri
        // if (imageUri == null) {
        // Toast.makeText(this, "Please select or take a profile picture.", Toast.LENGTH_LONG).show();
        // Log.w(TAG, "performRegistration: Profile picture (imageUri) is null.");
        // return;
        // }
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
        // TODO: Show a loading indicator here (if you have one)

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // TODO: Hide loading indicator here (if you have one)
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Directly call updateUserProfileAndSaveData without image information
                            updateUserProfileAndSaveData(firebaseUser, nickname, age);
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

    // Removed: uploadImageToFirebaseStorage method

    // Modified to not accept or handle imageUrl
    private void updateUserProfileAndSaveData(FirebaseUser firebaseUser, String nickname, int age) {
        Log.d(TAG, "updateUserProfileAndSaveData - Saving user data without profile image.");

        UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest.Builder()
                .setDisplayName(nickname);

        // Removed: Optionally update Firebase Auth user photo URI
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
        user.put("uid", firebaseUser.getUid());
        user.put("email", firebaseUser.getEmail());
        user.put("nickname", nickname);
        user.put("age", age);
        user.put("level", 1); // Default starting level, if you have one
        // Removed: user.put("profileImageUrl", imageUrl);
        Log.d(TAG, "Firestore: Saving user data without profileImageUrl.");


        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "User data successfully written to Firestore!");
                    Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error writing user document to Firestore", e);
                    Toast.makeText(RegistrationActivity.this, "Failed to save all user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
