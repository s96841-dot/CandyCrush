package com.example.CandyCrush; // Or your actual package name

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
// Import other necessary classes

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EnteryActivity extends AppCompatActivity {

    private static final String TAG = "EnteryActivity";

    private Button playButton;
    private Button registerButton;
    // Add any other UI elements like TextViews for title, etc.

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity starting.");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entery); // <<--- YOUR LAYOUT FILE FOR ENTERY ACTIVITY

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> { // <<--- YOUR ROOT LAYOUT ID
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        playButton = findViewById(R.id.play_button);       // <<--- YOUR PLAY BUTTON ID
        registerButton = findViewById(R.id.played_before_button); // <<--- YOUR REGISTER BUTTON ID

        if (playButton == null) {
            Log.e(TAG, "onCreate: Play button not found! Check your layout XML and ID.");
        }
        if (registerButton == null) {
            Log.e(TAG, "onCreate: Register button not found! Check your layout XML and ID.");
        }


        // --- Play Button Logic ---
        if (playButton != null) {
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null && !currentUser.isAnonymous()) {
                        // User is authenticated and NOT anonymous
                        Log.d(TAG, "Play button clicked: User is authenticated (UID: " + currentUser.getUid() + "). Navigating to FeedActivity.");
                        Intent intent = new Intent(EnteryActivity.this, FeedActivity.class);
                        startActivity(intent);
                        // Do not finish EnteryActivity, user might want to come back from FeedActivity
                    } else {
                        // No authenticated user, or an anonymous user exists.
                        // Redirect to LoginActivity.
                        if (currentUser != null && currentUser.isAnonymous()) {
                            Log.d(TAG, "Play button clicked: Anonymous user detected (UID: " + currentUser.getUid() + "). Signing out and navigating to LoginActivity.");
                            mAuth.signOut(); // Sign out the anonymous user
                        } else {
                            Log.d(TAG, "Play button clicked: No user / Null user. Navigating to LoginActivity.");
                        }
                        Intent intent = new Intent(EnteryActivity.this, LoginActivity.class);
                        // Optional: You can pass an extra if LoginActivity needs to behave differently
                        // For example, to redirect back to FeedActivity after successful login:
                        // intent.putExtra(LoginActivity.EXTRA_REDIRECT_AFTER_LOGIN, true);
                        startActivity(intent);
                        // Do not finish EnteryActivity, user might want to come back from LoginActivity
                    }
                }
            });
        }

        // --- Register Button Logic ---
        if (registerButton != null) {
            registerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Register button clicked: Navigating to RegistrationActivity.");
                    Intent intent = new Intent(EnteryActivity.this, RegistrationActivity.class);
                    startActivity(intent);
                    // Do not finish EnteryActivity
                }
            });
        }
        Log.d(TAG, "onCreate: Activity setup complete.");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // EnteryActivity should generally NOT have an AuthStateListener that automatically
        // navigates or signs in users. Its purpose is to be a manual branching point.
        // If you had one, ensure it's removed or its logic is compatible with this new flow.
        Log.d(TAG, "onStart: EnteryActivity started/resumed.");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "onStart: Current user state - UID: " + currentUser.getUid() + ", IsAnonymous: " + currentUser.isAnonymous());
        } else {
            Log.d(TAG, "onStart: Current user state - Null");
        }
        // No automatic navigation or sign-in here.
    }

    // No automatic anonymous sign-in methods should be present or called from EnteryActivity.
}
