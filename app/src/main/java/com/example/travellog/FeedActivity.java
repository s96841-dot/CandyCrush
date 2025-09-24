package com.example.travellog; // Or your actual package name


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
// Import other necessary classes

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.travellog.gameui.GameGridView; // <<--- CORRECTED IMPORT
import com.example.travellog.gamecore.LevelConfig;   // Assuming this is the correct package
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FeedActivity extends AppCompatActivity {

    private static final String TAG = "FeedActivity";

    private GameGridView gameGridView;
    private int currentLevel = 1; // Default starting level

    private Spinner levelSpinner;
    private Button logoutButtonTop;
    private TextView welcomeTextView;

    // User data fields
    private String nickname;
    private int age; // Although not used in welcome message in this version, kept for consistency
    private int userOverallLevel;

    private static final String ANONYMOUS_NICKNAME_FALLBACK = "Player"; // Fallback if nickname is somehow null/empty

    private FirebaseAuth mAuth;
    // No AuthStateListener needed here if onStart handles the user check robustly.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity starting.");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed); // <<--- YOUR LAYOUT FILE

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> { // <<--- YOUR ROOT LAYOUT ID
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // Critical user check happens in onStart() before UI setup that depends on user data

        // Initialize UI elements (can be done here or after user check in onStart)
        gameGridView = findViewById(R.id.gameGridView);
        levelSpinner = findViewById(R.id.levelSpinner);
        logoutButtonTop = findViewById(R.id.logoutButtonTop);
        welcomeTextView = findViewById(R.id.TextViewactivity_feed); // Ensure this ID is correct

        if (gameGridView == null) {
            Log.e(TAG, "onCreate: GameGridView (R.id.gameGridView) not found! Activity cannot function.");
            // Consider finishing or showing critical error
            return;
        }
        if (levelSpinner == null) Log.e(TAG, "onCreate: levelSpinner not found!");
        if (logoutButtonTop == null) Log.e(TAG, "onCreate: logoutButtonTop not found!");
        if (welcomeTextView == null) Log.e(TAG, "onCreate: welcomeTextView not found!");

        // Setup that doesn't strictly depend on immediate user data can go here
        // Spinner population and logout button listener will be set up after user validation in onStart
        // or here if the data they use is loaded conditionally.
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Activity starting/resuming.");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.isAnonymous()) {
            // CRITICAL: No valid user, or an anonymous user somehow reached here.
            // This should not happen if LoginActivity/RegistrationActivity/EnteryActivity are correct.
            Log.e(TAG, "onStart: User is NULL or ANONYMOUS. Redirecting to LoginActivity. UID: " + (currentUser != null ? currentUser.getUid() : "null"));
            if (currentUser != null && currentUser.isAnonymous()) {
                mAuth.signOut(); // Ensure anonymous user is signed out
            }
            Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // Stop further execution of this onStart
        }

        // --- User is authenticated and not anonymous, proceed with setup ---
        Log.d(TAG, "onStart: User is authenticated (UID: " + currentUser.getUid() + "). Proceeding with FeedActivity setup.");
        loadUserDataAndSetupUI();
    }


    private void loadUserDataAndSetupUI() {
        readUserData(); // Load from SharedPreferences
        updateWelcomeMessage(); // Update welcome message with nickname and level

        // --- Populate Spinner ---
        setupLevelSpinner();

        // --- Logout Button ---
        if (logoutButtonTop != null) {
            logoutButtonTop.setOnClickListener(view -> {
                Log.d(TAG, "Logout button clicked.");
                mAuth.signOut(); // Sign out from Firebase
                clearLocalUserData(); // Clear SharedPreferences
                Log.d(TAG, "Navigating to LoginActivity after logout.");
                Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Set initial spinner selection and load initial grid
        // This is done after spinner is populated in setupLevelSpinner()
        // and gameGridView.setupGridForLevel is called by the spinner's listener
    }

    private void setupLevelSpinner() {
        if (levelSpinner == null) return;

        int maxLevels = LevelConfig.getMaxLevels();
        if (maxLevels <= 0) {
            Log.e(TAG, "Max levels reported by LevelConfig is " + maxLevels + ". Spinner cannot be populated.");
            levelSpinner.setEnabled(false); // Disable spinner if no levels
            if(gameGridView != null) gameGridView.setupGridForLevel(1); // Load a default level 1
            return;
        }

        Log.d(TAG, "Populating spinner with " + maxLevels + " levels.");
        Integer[] levelNumbers = new Integer[maxLevels];
        for (int i = 0; i < maxLevels; i++) {
            levelNumbers[i] = i + 1; // Levels 1 to maxLevels
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, levelNumbers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(adapter);
        levelSpinner.setEnabled(true);

        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLevel = (Integer) parent.getItemAtPosition(position);
                Log.d(TAG, "Spinner selected level: " + currentLevel);
                if (gameGridView != null) {
                    gameGridView.setupGridForLevel(currentLevel);
                }
                updateWelcomeMessage(); // Update welcome message to include current playing level
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Set initial spinner selection based on potentially stored or default currentLevel
        // Ensure currentLevel is valid before trying to set selection
        if (currentLevel < 1 || currentLevel > maxLevels) {
            currentLevel = 1; // Default to level 1 if invalid
        }
        levelSpinner.setSelection(currentLevel - 1); // Spinner is 0-indexed
        // The onItemSelected listener should fire here and call gameGridView.setupGridForLevel()
    }


    private void updateWelcomeMessage() {
        if (welcomeTextView != null) {
            String displayedNickname = (nickname == null || nickname.isEmpty()) ? ANONYMOUS_NICKNAME_FALLBACK : nickname;
            String message = "Welcome, " + displayedNickname + " (Progress: " + userOverallLevel + ") - Playing Level: " + currentLevel;
            welcomeTextView.setText(message);
            Log.d(TAG, "Welcome message updated: " + message);
        }
    }

    private void readUserData() {
        // Ensure this SharedPreferences name is consistent with LoginActivity and RegistrationActivity
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        nickname = sharedPreferences.getString("nickname", ANONYMOUS_NICKNAME_FALLBACK);
        age = sharedPreferences.getInt("age", 0); // Age not displayed but loaded
        userOverallLevel = sharedPreferences.getInt("level", 1); // Overall user level
        // currentLevel (for the game) is managed by the spinner, could also be persisted if desired
        Log.d(TAG, "Read user data: Nickname=" + nickname + ", Age=" + age + ", OverallLevel=" + userOverallLevel);
    }

    private void clearLocalUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Local SharedPreferences (userInfo) cleared.");
        // Reset local fields to avoid stale data if activity somehow reused without full restart
        nickname = ANONYMOUS_NICKNAME_FALLBACK;
        age = 0;
        userOverallLevel = 1;
        currentLevel = 1;
    }

    // No AuthStateListener generally needed if onStart handles the initial user check.
    // If you need to react to auth changes *while* FeedActivity is active (e.g., token revoked),
    // then an AuthStateListener might be added, but its logic would also need to redirect to Login.
}

