package com.example.CandyCrush;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    FirebaseAuth auth;
    EditText emailEditText;
    EditText passwordEditText;
    Button btn_login;
    TextView registerLinkTextView;
    Button goToMainPageButton; // This button's behavior will be handled by EnteryActivity now

    private static final String TAG = "LoginActivity";
    public static final String EXTRA_REDIRECT_AFTER_LOGIN = "redirect_after_login_to_feed";


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

        emailEditText = findViewById(R.id.et_email);
        passwordEditText = findViewById(R.id.et_passward);
        btn_login = findViewById(R.id.btn_login);
        registerLinkTextView = findViewById(R.id.link_register);
        // goToMainPageButton might not be needed on LoginActivity anymore if EnteryActivity is the entry point
        // If it's still here, its functionality needs to be re-evaluated.
        // For now, I'm assuming EnteryActivity will be the main entry point.
        goToMainPageButton = findViewById(R.id.go_to_main_page_button);
        if(goToMainPageButton != null) {
            goToMainPageButton.setVisibility(View.GONE); // Example: Hide if not relevant from here
        }


        // --- REVISED: Check user state ---
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isAnonymous()) {
                Log.i(TAG, "onCreate: Anonymous user detected (UID: " + currentUser.getUid() + "). Signing out anonymous user and showing login form.");
                auth.signOut(); // Sign out the anonymous user
                // After signOut, currentUser will effectively be null for the purpose of this screen.
                // The UI will just show the login form.
            } else {
                // User is NOT anonymous, properly authenticated.
                Log.i(TAG, "onCreate: User is already authenticated (UID: " + currentUser.getUid() + "). Fetching data and navigating.");
                getUserDataFromFirestore(false); // Pass flag indicating not from explicit login button
            }
        } else {
            // No user is signed in (currentUser is null)
            Log.i(TAG, "onCreate: No user currently signed in. Displaying login form.");
        }
        // --- END OF REVISED CHECK ---


        if (registerLinkTextView != null) {
            registerLinkTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick: Register link clicked. Navigating to RegistrationActivity.");
                    Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                    startActivity(intent);
                    // Do not finish LoginActivity, user might want to come back
                }
            });
        } else {
            Log.e(TAG, "onCreate: registerLinkTextView is null. Check ID in XML.");
        }

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
        Log.d(TAG, "onCreate: Activity setup complete.");
    }

    private void performLogin() {
        Log.d(TAG, "performLogin: Attempting to log in user.");
        if (emailEditText == null || passwordEditText == null) { /* ... */ return; }
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) { /* ... */ return; }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.i(TAG, "performLogin: signInWithEmail:success.");
                        // Check if we need to redirect specifically because user came from "Play"
                        boolean redirectToFeed = getIntent().getBooleanExtra(EXTRA_REDIRECT_AFTER_LOGIN, true);
                        getUserDataFromFirestore(redirectToFeed);
                    } else {
                        Log.w(TAG, "performLogin: signInWithEmail:failure", task.getException());
                        // ... your error handling ...
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startFeedActivity() {
        Log.d(TAG, "startFeedActivity: Navigating to FeedActivity.");
        Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Modified to accept a flag
    private void getUserDataFromFirestore(boolean navigateToFeedOnSuccess) {
        Log.d(TAG, "getUserDataFromFirestore: Fetching user data.");
        FirebaseUser userForFirestore = auth.getCurrentUser();

        if (userForFirestore == null || userForFirestore.isAnonymous()) {
            // This should ideally not be hit if called after a successful non-anonymous login,
            // or if onCreate signed out an anonymous user.
            Log.e(TAG, "getUserDataFromFirestore: User is null or anonymous. Cannot proceed to get Firestore data for FeedActivity. User UID: " + (userForFirestore != null ? userForFirestore.getUid() : "null"));
            // Stay on LoginActivity or show appropriate message.
            // If this is hit after a login attempt, it's an error in the login flow.
            Toast.makeText(LoginActivity.this, "Login state error. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }

        String userId = userForFirestore.getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String nickname = document.getString("nickname");
                            int age = document.getLong("age") != null ? document.getLong("age").intValue() : 0;
                            int level = document.getLong("level") != null ? document.getLong("level").intValue() : 1;

                            Log.i(TAG, "getUserDataFromFirestore: Success. Nickname: " + nickname);
                            saveUserDataLocally(nickname, age, level);
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            if (navigateToFeedOnSuccess) {
                                startFeedActivity();
                            } else {
                                Log.d(TAG, "User data fetched, but not navigating to FeedActivity based on flag (e.g. initial app start, already logged in).");
                                // If this was called from onCreate for an already authenticated user, FeedActivity was already started.
                                // If LoginActivity is still visible, it means it was the top activity.
                                // If another activity like EnteryActivity wants to redirect here after login,
                                // we might need a different mechanism like startActivityForResult or a broadcast.
                                // For now, the primary case is a direct login.
                                startFeedActivity(); // Default to starting feed activity if logic gets here.
                            }
                        } else {
                            Log.w(TAG, "getUserDataFromFirestore: User data does not exist in Firestore for UID: " + userId);
                            Toast.makeText(LoginActivity.this, "User profile not found. Please complete registration.", Toast.LENGTH_LONG).show();
                            auth.signOut(); // Sign out user if their profile is mandatory and missing
                        }
                    } else {
                        Log.e(TAG, "getUserDataFromFirestore: Error getting user data.", task.getException());
                        Toast.makeText(LoginActivity.this, "Error retrieving user profile.", Toast.LENGTH_LONG).show();
                        auth.signOut(); // Sign out user on error fetching profile
                    }
                });
    }

    private void saveUserDataLocally(String nickname, int age, int level){
        // ... your existing saveUserDataLocally ...
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nickname", nickname);
        editor.putInt("age", age);
        editor.putInt("level", level);
        editor.apply();
    }
}
