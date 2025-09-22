package com.example.travellog; // Or your actual main package

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.travellog.gameui.GameGridView;
import com.example.travellog.gamecore.LevelConfig; // Assuming LevelConfig has MAX_LEVELS or similar

public class FeedActivity extends AppCompatActivity {

    private static final String TAG = "FeedActivity";

    private GameGridView gameGridView;
    private int currentLevel = 1; // Default starting level

    // New UI Elements
    private Spinner levelSpinner;
    private Button logoutButtonTop;

    // Existing UI Elements (if still needed)
    private TextView welcomeTextView;    // User data fields (if you're using SharedPreferences)
    protected String nickname;
    int age; // User's age
    int userOverallLevel; // User's overall progress level, distinct from grid level
    private static final String ANONYMOUS_NICKNAME = "N/A";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize GameGridView
        gameGridView = findViewById(R.id.gameGridView);
        if (gameGridView == null) {
            Log.e(TAG, "GameGridView (R.id.gameGridView) not found!");
            return;
        }

        // Initialize new UI elements
        levelSpinner = findViewById(R.id.levelSpinner);
        logoutButtonTop = findViewById(R.id.logoutButtonTop);
        welcomeTextView = findViewById(R.id.TextViewactivity_feed); // Your welcome message TextView

        // --- Populate Spinner ---
        // Assuming LevelConfig has a way to know the max number of levels
        // If not, you might need to hardcode it or get it from another source
        int maxLevels = LevelConfig.getMaxLevels(); // You'll need to implement getMaxLevels() in LevelConfig
        Integer[] levelNumbers = new Integer[maxLevels];
        for (int i = 0; i < maxLevels; i++) {
            levelNumbers[i] = i + 1;
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, levelNumbers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(adapter);

        // --- Spinner Item Selection Listener ---
        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLevel = (Integer) parent.getItemAtPosition(position);
                Log.d(TAG, "Spinner selected level: " + currentLevel);
                gameGridView.setupGridForLevel(currentLevel);
                // Optionally update the welcome message or other UI if it depends on the grid level
                updateWelcomeMessage(); // If welcome message needs to reflect current game level
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // --- Logout Button Listener ---
        if (logoutButtonTop != null) {
            logoutButtonTop.setOnClickListener(view -> {
                Log.d(TAG, "Logout button clicked.");
                clearUserData(); // Your existing method to clear user data
                // Navigate back to LoginActivity or WelcomeActivity
                Intent intent = new Intent(FeedActivity.this, LoginActivity.class); // Assuming MainActivity is your login screen
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Close FeedActivity
            });
        } else {
            Log.w(TAG, "Logout button (R.id.logoutButtonTop) not found.");
        }


        // Load user data and set initial state
        readUserData(); // Your existing method
        updateWelcomeMessage();

        // Set initial spinner selection (if currentLevel was loaded from SharedPreferences, for example)
        // Or just default to the first level (index 0)
        if(currentLevel > 0 && currentLevel <= maxLevels) {
            levelSpinner.setSelection(currentLevel - 1); // Spinner is 0-indexed
        } else {
            levelSpinner.setSelection(0); // Default to first item
            currentLevel = 1; // Ensure currentLevel is consistent
        }
        // Initial grid setup will be triggered by the spinner's onItemSelected listener
        // OR you can call it explicitly here if the spinner's listener isn't guaranteed to fire on init
        // gameGridView.setupGridForLevel(currentLevel); // Might be redundant if spinner listener fires

        // Remove listeners for old buttons
        // nextLevelButton, prevLevelButton related logic is now handled by the Spinner
    }

    // --- Helper Method to get Max Levels from LevelConfig ---
    // You need to add this static method to your LevelConfig.java
    // Example in LevelConfig.java:
    // public static int getMaxLevels() { return configs.length; // if configs is your array of level configurations }

    private void updateWelcomeMessage() {
        if (welcomeTextView != null) {
            String message;
            if (ANONYMOUS_NICKNAME.equals(nickname)) {
                message = "Welcome anonymous";
            } else {
                // You might want to display the user's overall level, or the current game grid level
                message = "Welcome, " + nickname + " (Progress: " + userOverallLevel + ") - Playing Level: " + currentLevel;
            }
            welcomeTextView.setText(message);
            Log.d(TAG, "Welcome message updated: " + message);
        }
    }


    // --- Your Existing User Data Methods ---
    private void readUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        nickname = sharedPreferences.getString("Nickname", ANONYMOUS_NICKNAME);
        age = sharedPreferences.getInt("Age", 0);
        userOverallLevel = sharedPreferences.getInt("Level", 1); // User's overall progress
        // Optionally, load the last played grid level if you want to persist it
        // currentLevel = sharedPreferences.getInt("CurrentGameLevel", 1);
        Log.d(TAG, "Read user data: Nickname=" + nickname + ", Age=" + age + ", OverallLevel=" + userOverallLevel);
    }

    private void clearUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "User data cleared.");
        // Reset local variables if needed
        nickname = ANONYMOUS_NICKNAME;
        age = 0;
        userOverallLevel = 1;
        currentLevel = 1; // Reset game level to default
    }

    // You might not need updateActionButton anymore if the logout button is always visible
    // and its text doesn't change. If it does, keep/modify it.
    /*
    private void updateActionButton(boolean isAnonymous) {
        if (logoutButtonTop != null) { // Update to new button ID
            if (isAnonymous) {
                logoutButtonTop.setText("Sign In"); // Or whatever your anonymous action is
                 logoutButtonTop.setOnClickListener(v -> {
                    // Intent to MainActivity or LoginActivity
                    startActivity(new Intent(FeedActivity.this, MainActivity.class));
                    finish();
                });
            } else {
                logoutButtonTop.setText("Logout");
                logoutButtonTop.setOnClickListener(v -> {
                    clearUserData();
                    startActivity(new Intent(FeedActivity.this, MainActivity.class));
                    finish();
                });
            }
        }
    }
    */
}
