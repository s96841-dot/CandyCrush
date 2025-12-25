package com.example.CandyCrush; // Or your actual package name


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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.CandyCrush.gameui.GameGridView; // Make sure this is the correct path
import com.example.CandyCrush.gamecore.LevelConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

// <<< MODIFIED: Implement the GameStateListener >>>
public class FeedActivity extends AppCompatActivity implements GameGridView.GameStateListener {

    private static final String TAG = "FeedActivity";

    private GameGridView gameGridView;
    private int currentLevel = 1;

    private Spinner levelSpinner;
    private Button logoutButtonTop;
    private TextView welcomeTextView;
    private Button shuffleButton;
    private TextView scoreTextView;    private String nickname;

    private int userOverallLevel;

    private static final String ANONYMOUS_NICKNAME_FALLBACK = "Player";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity starting.");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        gameGridView = findViewById(R.id.gameGridView);
        levelSpinner = findViewById(R.id.levelSpinner);
        logoutButtonTop = findViewById(R.id.logoutButtonTop);
        welcomeTextView = findViewById(R.id.TextViewactivity_feed);
        shuffleButton = findViewById(R.id.shuffleButton);
        scoreTextView = findViewById(R.id.score_text_view);

        if (gameGridView == null) Log.e(TAG, "onCreate: GameGridView not found!");
        if (levelSpinner == null) Log.e(TAG, "onCreate: levelSpinner not found!");
        if (logoutButtonTop == null) Log.e(TAG, "onCreate: logoutButtonTop not found!");
        if (welcomeTextView == null) Log.e(TAG, "onCreate: welcomeTextView not found!");
        if (shuffleButton == null) Log.e(TAG, "onCreate: shuffleButton not found!");

        // <<< NEW: Set the listener for GameGridView >>>
        if (gameGridView != null) {
            gameGridView.setGameStateListener(this);
        }

        // Click Listener for the Shuffle Button
        if (shuffleButton != null) {
            // shuffleButton.setVisibility(View.VISIBLE); // Keep GONE from XML, listener will manage it

            shuffleButton.setOnClickListener(view -> {
                Log.d(TAG, "Shuffle button clicked.");
                if (gameGridView != null) {
                    gameGridView.shuffleBoard(); // <<< MODIFIED: Call the actual shuffle method
                }
                // The button should ideally be hidden by onMovesAvailable() callback
                // or after a shuffle timeout if no moves are still available.
                // For now, we can hide it immediately, assuming shuffle creates moves.
                // shuffleButton.setVisibility(View.GONE);
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Activity starting/resuming.");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.isAnonymous()) {
            Log.e(TAG, "onStart: User is NULL or ANONYMOUS. Redirecting to LoginActivity. UID: " + (currentUser != null ? currentUser.getUid() : "null"));
            if (currentUser != null && currentUser.isAnonymous()) {
                mAuth.signOut();
            }
            Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        Log.d(TAG, "onStart: User is authenticated (UID: " + currentUser.getUid() + "). Proceeding with FeedActivity setup.");
        readUserDataFromPrefs();
        loadUserDataFromFirestore(currentUser);
    }

    private void readUserDataFromPrefs() {
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        nickname = sharedPreferences.getString("nickname", ANONYMOUS_NICKNAME_FALLBACK);
        userOverallLevel = sharedPreferences.getInt("level", 1);
        Log.d(TAG, "Read user data from Prefs: Nickname=" + nickname + ", OverallLevel=" + userOverallLevel);
        updateWelcomeMessage();
    }

    private void loadUserDataFromFirestore(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            Log.e(TAG, "Cannot load user data from Firestore, firebaseUser is null");
            setupRemainingUI();
            return;
        }
        String userId = firebaseUser.getUid();
        DocumentReference userDocRef = db.collection("users").document(userId);

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String firestoreNickname = document.getString("nickname");
                    if (firestoreNickname != null && !firestoreNickname.isEmpty()) {
                        nickname = firestoreNickname;
                    }
                    Long firestoreOverallLevel = document.getLong("level");
                    if (firestoreOverallLevel != null) {
                        userOverallLevel = firestoreOverallLevel.intValue();
                    }
                    Log.d(TAG, "Firestore data loaded: Nickname=" + nickname + ", OverallLevel=" + userOverallLevel);
                    updateWelcomeMessage();
                } else {
                    Log.d(TAG, "No such user document in Firestore for ID: " + userId);
                }
            } else {
                Log.e(TAG, "Error getting user document from Firestore: ", task.getException());
                Toast.makeText(FeedActivity.this, "Failed to load latest profile details.", Toast.LENGTH_SHORT).show();
            }
            setupRemainingUI();
        });
    }


    private void setupRemainingUI() {
        setupLevelSpinner();
        if (logoutButtonTop != null) {
            logoutButtonTop.setOnClickListener(view -> {
                Log.d(TAG, "Logout button clicked.");
                mAuth.signOut();
                clearLocalUserData();
                Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupLevelSpinner() {
        if (levelSpinner == null) return;
        int maxLevels = LevelConfig.getMaxLevels();
        if (maxLevels <= 0) {
            Log.e(TAG, "Max levels reported by LevelConfig is " + maxLevels + ". Spinner cannot be populated.");
            levelSpinner.setEnabled(false);
            if(gameGridView != null) gameGridView.setupGridForLevel(1);
            return;
        }

        Log.d(TAG, "Populating spinner with " + maxLevels + " levels.");
        Integer[] levelNumbers = new Integer[maxLevels];
        for (int i = 0; i < maxLevels; i++) {
            levelNumbers[i] = i + 1;
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, levelNumbers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(adapter);
        levelSpinner.setEnabled(true);

        int initialSpinnerPosition = 0;
        if (currentLevel >= 1 && currentLevel <= maxLevels) {
            initialSpinnerPosition = currentLevel - 1;
        } else {
            currentLevel = 1;
        }
        levelSpinner.setSelection(initialSpinnerPosition);
        if (gameGridView != null) {
            gameGridView.setupGridForLevel(currentLevel); // This will trigger listener calls if implemented in GameGridView
        }
        updateWelcomeMessage();


        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLevel = (Integer) parent.getItemAtPosition(position);
                Log.d(TAG, "Spinner selected level: " + currentLevel);
                if (gameGridView != null) {
                    // This setup will eventually trigger checks in GameGridView that call the listener
                    gameGridView.setupGridForLevel(currentLevel);
                }
                updateWelcomeMessage();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void updateWelcomeMessage() {
        if (welcomeTextView != null) {
            String displayedNickname = (nickname == null || nickname.isEmpty()) ? ANONYMOUS_NICKNAME_FALLBACK : nickname;
            String message = "Welcome, " + displayedNickname + " - Level: " + currentLevel;
            welcomeTextView.setText(message);
            Log.d(TAG, "Welcome message updated: " + message);
        }
    }

    private void clearLocalUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Local SharedPreferences (userInfo) cleared.");
        nickname = ANONYMOUS_NICKNAME_FALLBACK;
        userOverallLevel = 1;
        currentLevel = 1;
    }

    // --- GameStateListener Implementation ---
    @Override
    public void onNoMovesAvailable() {
        runOnUiThread(() -> { // Ensure UI updates are on the main thread
            Log.d(TAG, "GameStateListener: No moves available. Showing shuffle button.");
            if (shuffleButton != null) {
                shuffleButton.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onMovesAvailable() {
        runOnUiThread(() -> { // Ensure UI updates are on the main thread
            Log.d(TAG, "GameStateListener: Moves are available. Hiding shuffle button.");
            if (shuffleButton != null) {
                shuffleButton.setVisibility(View.GONE);
            }
        });
    }
    @Override
    public void onScoreChanged(int newScore) {
        // This is called from GameGridView every time the score changes.
        // We must update the UI on the main thread.
        runOnUiThread(() -> {
            if (scoreTextView != null) {
                scoreTextView.setText("XP: " + newScore);
            }
        });
    }

}
