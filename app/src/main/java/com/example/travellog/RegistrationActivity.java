package com.example.travellog; // Or your actual package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
// Import other necessary classes

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText nicknameEditText; // Example: Add nickname field
    private EditText ageEditText;      // Example: Add age field
    private Button registerButton;
    // Add any other UI elements like a "Back to Login" TextView/Button

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity starting.");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration); // <<--- YOUR LAYOUT FILE

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> { // <<--- YOUR ROOT LAYOUT ID
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        emailEditText = findViewById(R.id.emailEditText);         // <<--- YOUR EMAIL EDITTEXT ID
        passwordEditText = findViewById(R.id.passwordEditText);   // <<--- YOUR PASSWORD EDITTEXT ID
        nicknameEditText = findViewById(R.id.et_nickname);   // <<--- YOUR NICKNAME EDITTEXT ID (if you have one)
        ageEditText = findViewById(R.id.et_age);             // <<--- YOUR AGE EDITTEXT ID (if you have one)
        registerButton = findViewById(R.id.btn_register); // <<--- YOUR REGISTER BUTTON ID

        if (registerButton == null) {
            Log.e(TAG, "onCreate: Register button not found!");
        }
        // Add null checks for other EditTexts if they are critical

        if (registerButton != null) {
            registerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Register button clicked.");
                    performRegistration();
                }
            });
        }
    }

    private void performRegistration() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String nickname = nicknameEditText.getText().toString().trim(); // Get nickname
        String ageString = ageEditText.getText().toString().trim();       // Get age as string

        // --- Basic Validation ---
        if (email.isEmpty() || password.isEmpty() || nickname.isEmpty() || ageString.isEmpty()) {
            Toast.makeText(RegistrationActivity.this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "performRegistration: One or more fields are empty.");
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(RegistrationActivity.this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "performRegistration: Password too short.");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageString);
            if (age <= 0) {
                Toast.makeText(RegistrationActivity.this, "Please enter a valid age.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "performRegistration: Invalid age entered.");
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(RegistrationActivity.this, "Please enter a valid number for age.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "performRegistration: Age is not a valid number.");
            return;
        }
        // --- End Validation ---


        Log.d(TAG, "performRegistration: Attempting to create user with email: " + email);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Optionally set display name for the FirebaseUser object
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(nickname)
                                        .build();
                                firebaseUser.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileTask -> {
                                            if (profileTask.isSuccessful()) {
                                                Log.d(TAG, "User profile updated with display name.");
                                            }
                                        });

                                // Save additional user data to Firestore
                                saveAdditionalUserData(firebaseUser, nickname, age);
                            } else {
                                Log.e(TAG, "createUserWithEmail:success but firebaseUser is null!");
                                Toast.makeText(RegistrationActivity.this, "Registration succeeded but failed to get user details.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegistrationActivity.this, "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveAdditionalUserData(FirebaseUser firebaseUser, String nickname, int age) {
        String userId = firebaseUser.getUid();
        Map<String, Object> userData = new HashMap<>();
        userData.put("nickname", nickname);
        userData.put("age", age);
        userData.put("level", 1); // Default starting level
        // Add any other default fields you want to store

        Log.d(TAG, "saveAdditionalUserData: Saving data for UID: " + userId);
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data successfully written to Firestore for UID: " + userId);
                    Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

                    // Save to SharedPreferences as well so FeedActivity can pick it up immediately
                    saveUserDataLocally(nickname, age, 1);

                    // Navigate to FeedActivity
                    Intent intent = new Intent(RegistrationActivity.this, FeedActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
                    startActivity(intent);
                    finish(); // Finish RegistrationActivity
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing user data to Firestore for UID: " + userId, e);
                    Toast.makeText(RegistrationActivity.this, "Registration succeeded but failed to save profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    // Optional: You might want to sign the user out here if saving profile data is critical
                    // mAuth.signOut();
                    // Or allow them to proceed and try saving profile later
                });
    }

    // Copied from LoginActivity for consistency - ensure SharedPreferences key is the same
    private void saveUserDataLocally(String nickname, int age, int level){
        Log.d(TAG, "saveUserDataLocally: Saving user data to SharedPreferences. Nickname: " + nickname);
        // Ensure this SharedPreferences name is consistent with LoginActivity and FeedActivity
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nickname", nickname);
        editor.putInt("age", age);
        editor.putInt("level", level);
        editor.apply();
        Log.i(TAG, "saveUserDataLocally: User data saved successfully.");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in (and not anonymous)
        // If so, they shouldn't be on the registration screen.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && !currentUser.isAnonymous()) {
            Log.d(TAG, "onStart: User already authenticated and not anonymous (UID: " + currentUser.getUid() + "). Redirecting to FeedActivity.");
            Intent intent = new Intent(RegistrationActivity.this, FeedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (currentUser != null && currentUser.isAnonymous()) {
            Log.d(TAG, "onStart: Anonymous user (UID: " + currentUser.getUid() + ") found. Signing out and staying on registration.");
            mAuth.signOut(); // Ensure we don't proceed with an anonymous user for registration
        }
    }
}
