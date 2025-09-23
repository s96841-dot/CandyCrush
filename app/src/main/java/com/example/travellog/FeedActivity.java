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
import com.example.travellog.gamecore.LevelConfig; // Ensure this import is correct

public class FeedActivity extends AppCompatActivity {

    private static final String TAG = "FeedActivity";

    private GameGridView gameGridView;
    private int currentLevel = 1; // Default starting level

    private Spinner levelSpinner;
    private Button logoutButtonTop;
    private TextView welcomeTextView;

    protected String nickname;
    int age;
    int userOverallLevel;
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

        gameGridView = findViewById(R.id.gameGridView);
        if (gameGridView == null) {
            Log.e(TAG, "GameGridView (R.id.gameGridView) not found! Activity cannot function.");
            // Consider finishing the activity or showing an error message to the user
            return;
        }

        levelSpinner = findViewById(R.id.levelSpinner);
        logoutButtonTop = findViewById(R.id.logoutButtonTop);
        welcomeTextView = findViewById(R.id.TextViewactivity_feed);

        // --- Populate Spinner ---
        int maxLevels = LevelConfig.getMaxLevels(); // This should now be 10
        if (maxLevels <= 0) {
            Log.e(TAG, "Max levels reported by LevelConfig is " + maxLevels + ". Spinner cannot be populated.");
            // Handle this case, maybe disable spinner or show error
        } else {
            Log.d(TAG, "Populating spinner with " + maxLevels + " levels.");
            Integer[] levelNumbers = new Integer[maxLevels];
            for (int i = 0; i < maxLevels; i++) {
                levelNumbers[i] = i + 1; // Levels 1 to maxLevels
            }
            ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, levelNumbers);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            levelSpinner.setAdapter(adapter);
        }

        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLevel = (Integer) parent.getItemAtPosition(position);
                Log.d(TAG, "Spinner selected level: " + currentLevel);
                if (gameGridView != null) {
                    gameGridView.setupGridForLevel(currentLevel);
                }
                updateWelcomeMessage();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        if (logoutButtonTop != null) {
            logoutButtonTop.setOnClickListener(view -> {
                Log.d(TAG, "Logout button clicked.");
                clearUserData();
                // TODO: Replace MainActivity.class with your actual Login/Main activity class
                Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        } else {
            Log.w(TAG, "Logout button (R.id.logoutButtonTop) not found.");
        }

        readUserData();
        updateWelcomeMessage();

        // Set initial spinner selection and load initial grid
        if (maxLevels > 0) {
            if (currentLevel > 0 && currentLevel <= maxLevels) {
                levelSpinner.setSelection(currentLevel - 1); // Spinner is 0-indexed
            } else {
                levelSpinner.setSelection(0); // Default to first item
                currentLevel = 1; // Ensure currentLevel is consistent
            }
            // gameGridView.setupGridForLevel(currentLevel); // This will be triggered by setSelection if listener fires,
            // or can be called explicitly if needed.
            // For safety, an explicit call ensures it.
            if(gameGridView != null && levelSpinner.getCount() > 0) { // Ensure spinner has items
                // The listener should fire when setSelection is called,
                // but an explicit call here after spinner is populated is a safe fallback.
                // gameGridView.setupGridForLevel(currentLevel);
            } else if (gameGridView != null) {
                // If spinner somehow failed to populate but we have a default currentLevel
                gameGridView.setupGridForLevel(currentLevel);
            }
        } else if (gameGridView != null) {
            // If no levels defined (maxLevels <=0), maybe set up a default small grid or show error
            gameGridView.setupGridForLevel(1); // Fallback to level 1 config
        }
    }

    private void updateWelcomeMessage() {
        if (welcomeTextView != null) {
            String message;
            if (ANONYMOUS_NICKNAME.equals(nickname)) {
                message = "Welcome anonymous";
            } else {
                message = "Welcome, " + nickname + " (Progress: " + userOverallLevel + ") - Playing Level: " + currentLevel;
            }
            welcomeTextView.setText(message);
        }
    }

    private void readUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        nickname = sharedPreferences.getString("Nickname", ANONYMOUS_NICKNAME);
        age = sharedPreferences.getInt("Age", 0);
        userOverallLevel = sharedPreferences.getInt("Level", 1);
        // currentLevel = sharedPreferences.getInt("CurrentGameLevel", 1); // Optionally load last played game level
        Log.d(TAG, "Read user data: Nickname=" + nickname + ", Age=" + age + ", OverallLevel=" + userOverallLevel + ", CurrentGameLevel=" + currentLevel);
    }

    private void clearUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "User data cleared.");
        nickname = ANONYMOUS_NICKNAME;
        age = 0;
        userOverallLevel = 1;
        currentLevel = 1;
    }
}
