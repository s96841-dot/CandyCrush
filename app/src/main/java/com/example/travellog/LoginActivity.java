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
    private static final String TAG = "LoginActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Activity starting.");
        auth = FirebaseAuth.getInstance();

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check if user is already signed in
        if (auth.getCurrentUser() != null) {
            Log.i(TAG, "onCreate: User already signed in. Navigating to FeedActivity.");
            // If user is already logged in, we should also fetch their data before proceeding
            getUserDataFromFirestore();
        }

        TextView registerLinkTextView = findViewById(R.id.link_register);
        emailEditText = findViewById(R.id.et_email);
        passwordEditText = findViewById(R.id.et_passward);
        btn_login = findViewById(R.id.btn_login);

        registerLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Register link clicked. Navigating to RegistrationActivity.");
                Intent intent=new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Login button clicked.");
                performLogin();
            }
        });
        Log.d(TAG, "onCreate: Activity setup complete.");
    }

    private void performLogin() {
        Log.d(TAG, "performLogin: Attempting to log in user.");
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "performLogin: Email or password field is empty.");
            Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_LONG).show();
            return;
        }

        // Perform Firebase authentication
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, now get user data from Firestore
                        Log.i(TAG, "performLogin: signInWithEmail:success.");
                        getUserDataFromFirestore();
                    } else {
                        // If sign in fails, display a message to the user.
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
        finish(); // Finish LoginActivity so user can't navigate back to it
    }

    private void getUserDataFromFirestore() {
        Log.d(TAG, "getUserDataFromFirestore: Fetching user data from Firestore.");
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "getUserDataFromFirestore: Cannot get user data, user is not authenticated.");
            Toast.makeText(LoginActivity.this, "Authentication error.", Toast.LENGTH_LONG).show();
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
                                // User data exists
                                String nickname = document.getString("nickname");
                                Long ageLong = document.getLong("age");
                                int age = (ageLong != null) ? ageLong.intValue() : 0;

                                // --- START OF MODIFIED CODE ---
                                Long levelLong = document.getLong("level");
                                int level = (levelLong != null) ? levelLong.intValue() : 1; // Default to 1 if not set

                                Log.i(TAG, "getUserDataFromFirestore: Success. Nickname: " + nickname + ", Age: " + age + ", Level: " + level);

                                saveUserDataLocally(nickname, age, level);
                                // --- END OF MODIFIED CODE ---

                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                                // Navigate to FeedActivity after getting and saving data
                                startFeedActivity();

                            } else {
                                // User is authenticated but has no data in Firestore
                                Log.w(TAG, "getUserDataFromFirestore: User data does not exist for UID: " + userId);
                                Toast.makeText(LoginActivity.this, "User profile not found. Please complete registration.", Toast.LENGTH_LONG).show();
                                // Optional: Navigate to a profile creation screen or log out
                                // auth.signOut();
                            }
                        } else {
                            // Handle errors in fetching data
                            Log.e(TAG, "getUserDataFromFirestore: Error getting user data.", task.getException());
                            Toast.makeText(LoginActivity.this, "Error getting user profile.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // --- START OF MODIFIED CODE ---
    private void saveUserDataLocally(String nickname, int age, int level){
        Log.d(TAG, "saveUserDataLocally: Saving user data to SharedPreferences. Nickname: " + nickname);
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nickname", nickname);
        editor.putInt("age", age);
        editor.putInt("level", level); // Save the level
        editor.apply();
        Log.i(TAG, "saveUserDataLocally: User data saved successfully.");
    }
    // --- END OF MODIFIED CODE ---
}