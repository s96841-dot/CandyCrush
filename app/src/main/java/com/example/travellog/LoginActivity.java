package com.example.travellog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    FirebaseAuth auth;
    EditText emailEditText;
    EditText passwordEditText;
    Button btn_login;
    TextView registerLinkTextView; // Moved declaration here for clarity
    Button goToMainPageButton;    // Declare the button

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Activity starting.");
        auth = FirebaseAuth.getInstance();

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login); // Ensure this uses R.layout.activity_login

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        emailEditText = findViewById(R.id.et_email);
        passwordEditText = findViewById(R.id.et_passward); // Still has "passward", recommend changing to et_password
        btn_login = findViewById(R.id.btn_login);
        registerLinkTextView = findViewById(R.id.link_register);
        goToMainPageButton = findViewById(R.id.go_to_main_page_button); // Initialize the new button

        // Check if user is already signed in (moved after UI initialization for clarity)
        if (auth.getCurrentUser() != null) {
            Log.i(TAG, "onCreate: User already signed in. Navigating to FeedActivity.");
            getUserDataFromFirestore();
        }

        // Set OnClickListener for the Register link
        if (registerLinkTextView != null) {
            registerLinkTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick: Register link clicked. Navigating to RegistrationActivity.");
                    Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                    startActivity(intent);
                    finish(); // Optional: finish LoginActivity if you don't want users to go back
                }
            });
        } else {
            Log.e(TAG, "onCreate: registerLinkTextView is null. Check ID in XML.");
        }

        // Set OnClickListener for the Login button
        if (btn_login != null) {
            btn_login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick: Login button clicked.");
                    performLogin();
                }
            });
        } else {
            Log.e(TAG, "onCreate: btn_login is null. Check ID in XML.");
        }

        // Set OnClickListener for the "Go to Main Page" button
        if (goToMainPageButton != null) {
            goToMainPageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: Go to Main Page button clicked. Navigating to EnteryActivity.");
                    Intent intent = new Intent(LoginActivity.this, EnteryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish(); // Close LoginActivity
                }
            });
        } else {
            Log.e(TAG, "onCreate: goToMainPageButton is null. Check ID in XML.");
        }

        Log.d(TAG, "onCreate: Activity setup complete.");
    }

    private void performLogin() {
        Log.d(TAG, "performLogin: Attempting to log in user.");
        // Ensure UI elements are not null before accessing them
        if (emailEditText == null || passwordEditText == null) {
            Log.e(TAG, "performLogin: Email or Password EditText is null.");
            Toast.makeText(LoginActivity.this, "Error initializing login form.", Toast.LENGTH_LONG).show();
            return;
        }

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "performLogin: Email or password field is empty.");
            Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_LONG).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.i(TAG, "performLogin: signInWithEmail:success.");
                        getUserDataFromFirestore();
                    } else {
                        Log.w(TAG, "performLogin: signInWithEmail:failure", task.getException());
                        String errorMessage = "Authentication failed. ";
                        if (task.getException() != null) {
                            errorMessage += task.getException().getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startFeedActivity() {
        Log.d(TAG, "startFeedActivity: Navigating to FeedActivity.");
        Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
        startActivity(intent);
        finish();
    }

    private void getUserDataFromFirestore() {
        Log.d(TAG, "getUserDataFromFirestore: Fetching user data from Firestore.");
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "getUserDataFromFirestore: Cannot get user data, user is not authenticated.");
            // Don't show a toast here if this is called when user is already signed in but data fetch fails later
            // The performLogin method handles auth failure toasts.
            // Consider what should happen if already logged in but data is missing.
            // Maybe navigate to a profile setup screen or show a specific message.
            // For now, let's prevent a crash and log it.
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String nickname = document.getString("nickname");
                                Long ageLong = document.getLong("age");
                                int age = (ageLong != null) ? ageLong.intValue() : 0;
                                Long levelLong = document.getLong("level");
                                int level = (levelLong != null) ? levelLong.intValue() : 1;

                                Log.i(TAG, "getUserDataFromFirestore: Success. Nickname: " + nickname + ", Age: " + age + ", Level: " + level);
                                saveUserDataLocally(nickname, age, level);
                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                startFeedActivity();
                            } else {
                                Log.w(TAG, "getUserDataFromFirestore: User data does not exist for UID: " + userId);
                                Toast.makeText(LoginActivity.this, "User profile not found. Please complete registration.", Toast.LENGTH_LONG).show();
                                // Optional: Navigate to RegistrationActivity or a profile setup screen
                                // Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                                // startActivity(intent);
                                // Or sign out if profile is mandatory
                                // auth.signOut();
                            }
                        } else {
                            Log.e(TAG, "getUserDataFromFirestore: Error getting user data.", task.getException());
                            Toast.makeText(LoginActivity.this, "Error getting user profile.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserDataLocally(String nickname, int age, int level){
        Log.d(TAG, "saveUserDataLocally: Saving user data to SharedPreferences. Nickname: " + nickname);
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nickname", nickname);
        editor.putInt("age", age);
        editor.putInt("level", level);
        editor.apply();
        Log.i(TAG, "saveUserDataLocally: User data saved successfully.");
    }
}
