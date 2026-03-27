package com.example.CandyCrush; // Or your actual package name


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.CandyCrush.gamecore.LevelConfig;
import com.example.CandyCrush.gameui.GameGridView;
import com.example.CandyCrush.utils.BackgroundMusicManager;
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
    private boolean isMultiplayerLevel = false;
    private String multiplayerLevelDocId;
    private String multiplayerLevelName;
    private String multiplayerCreatorName;

    private Button logoutButtonTop;
    private Button shuffleButton;
    private TextView scoreTextView;
    private TextView missionTextView;
    private TextView movesTextView;
    private String nickname;
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


        // קישור רכיבי ה-UI
        gameGridView = findViewById(R.id.gameGridView);
        logoutButtonTop = findViewById(R.id.btn_back_to_map);
        shuffleButton = findViewById(R.id.shuffleButton);
        scoreTextView = findViewById(R.id.score_text_view);
        missionTextView = findViewById(R.id.mission_text_view);
        movesTextView = findViewById(R.id.moves_text_view);

        // --- תיקון 1: חיבור כפתור המפה ---
        if (logoutButtonTop != null) {
            logoutButtonTop.setOnClickListener(view -> {
                Log.d(TAG, "Map button clicked. Returning to map.");
                returnToMap();
            });
        }

        // הגדרת ה-Listener ללוח
        if (gameGridView != null) {
            gameGridView.setGameStateListener(this);
        }

        // --- תיקון 2: טעינת השלב שנבחר מהמפה באופן מיידי ---
        resolveLevelSourceFromIntent();

        if (gameGridView != null) {
            if (isMultiplayerLevel) {
                loadMultiplayerLevelFromFirestore(multiplayerLevelDocId);
            } else {
                loadLevelConfigFromFirestore(currentLevel);
            }
        }
        // כפתור Shuffle
        if (shuffleButton != null) {
            shuffleButton.setOnClickListener(view -> {
                if (gameGridView != null) gameGridView.shuffleBoard();
            });
        }
    }


    // ADD THIS METHOD TO FIX THE COMPILATION ERROR
    @Override
    public void onMissionUpdate(int currentScore, int targetScore, int movesLeft) {
        runOnUiThread(() -> {
            if (missionTextView != null) {
                // מציג למשל: Goal: 500 / 1200
                missionTextView.setText("Goal: " + currentScore + " / " + targetScore);
            }
            if (scoreTextView != null) {
                scoreTextView.setText("XP: " + currentScore);
            }
            if (movesTextView != null) {
                movesTextView.setText("Moves: " + movesLeft);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Activity starting/resuming.");

        // 1. קודם כל קוראים מהזיכרון המקומי (SharedPreferences)
        // זה מבטיח שהרמה (userOverallLevel) תהיה מעודכנת לפני כל פעולה אחרת
        readUserDataFromPrefs();

        // 2. בדיקה אם המשתמש מחובר ל-Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.isAnonymous()) {
            Log.e(TAG, "onStart: User not authenticated. Redirecting to Login.");
            if (currentUser != null && currentUser.isAnonymous()) {
                mAuth.signOut();
            }
            Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        Log.d(TAG, "onStart: User authenticated (UID: " + currentUser.getUid() + "). Checking Firestore for updates.");

        // 3. עדכון הנתונים מהענן (Firestore) ברקע
        // זה יעדכן את ה-userOverallLevel אם שיחקת ממכשיר אחר
        loadUserDataFromFirestore(currentUser);
    }

    private void readUserDataFromPrefs() {
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        nickname = sharedPreferences.getString("nickname", ANONYMOUS_NICKNAME_FALLBACK);
        userOverallLevel = sharedPreferences.getInt("level", 1);
        Log.d(TAG, "Read user data from Prefs: Nickname=" + nickname + ", OverallLevel=" + userOverallLevel);
        // No welcome banner in this screen; nickname is kept for future use.
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
                    // No welcome banner in this screen; nickname is kept for future use.                } else {
                    Log.d(TAG, "No such user document in Firestore for ID: " + userId);
                }
            } else {
                Log.e(TAG, "Error getting user document from Firestore: ", task.getException());
                Toast.makeText(FeedActivity.this, "Failed to load latest profile details.", Toast.LENGTH_SHORT).show();
            }
            setupRemainingUI();
        });
    }
    private void resolveLevelSourceFromIntent() {
        String levelSource = getIntent().getStringExtra("LEVEL_SOURCE");
        isMultiplayerLevel = "MULTIPLAYER".equals(levelSource);

        if (isMultiplayerLevel) {
            multiplayerLevelDocId = getIntent().getStringExtra("MULTIPLAYER_LEVEL_DOC_ID");
            multiplayerLevelName = getIntent().getStringExtra("MULTIPLAYER_LEVEL_NAME");
            multiplayerCreatorName = getIntent().getStringExtra("MULTIPLAYER_CREATOR_NAME");
            Log.d(TAG, "Loading multiplayer level. DocId=" + multiplayerLevelDocId);
        } else {
            int selectedLevel = getIntent().getIntExtra("SELECTED_LEVEL", 1);
            this.currentLevel = selectedLevel;
            Log.d(TAG, "Loading single player level: " + currentLevel);
        }
    }


    private void setupRemainingUI() {
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
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        returnToMap();
    }
    /**
     * חוזר למפת השלבים ומנקה את היסטוריית המסכים של המשחק
     */
    private void returnToMap() {
        Log.d(TAG, "returnToMap: Navigating back to MapActivity.");
        Intent intent = new Intent(FeedActivity.this, MapActivity.class);
        // FLAG_ACTIVITY_CLEAR_TOP דואג שאם המפה כבר פתוחה ברקע, הוא פשוט יחזור אליה ולא יפתח אחת חדשה
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // סוגר את FeedActivity הנוכחי
    }
    public void unlockNextLevel(int completedLevel) {
        //1. עדכון מקומי (בשביל המפה שתעבוד מהר)
        if (completedLevel >= userOverallLevel) {
            userOverallLevel = completedLevel + 1;

            SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
            sharedPreferences.edit().putInt("level", userOverallLevel).apply();
            Log.d(TAG, "Local level updated to: " + userOverallLevel);

            // 2. עדכון ב-Firebase (בשביל לשמור את הנתונים בענן)
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                db.collection("users").document(user.getUid())
                        .update("level", userOverallLevel)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Cloud sync successful!"))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Cloud sync failed, trying set().", e);
                            // אם התיעוד לא קיים, ניצור אותו
                            java.util.Map<String, Object> data = new java.util.HashMap<>();
                            data.put("level", userOverallLevel);
                            db.collection("users").document(user.getUid()).set(data, com.google.firebase.firestore.SetOptions.merge());
                        });
            }
        }
    }

    // This method will be called when the user taps "Continue" after winning
    public void handleLevelCompleteNavigation() {
        if (!isMultiplayerLevel) {
            unlockNextLevel(currentLevel); // Save progress only in single player map
        }        returnToMap(); // Go back to Map
    }
    private void loadMultiplayerLevelFromFirestore(String levelDocId) {
        if (levelDocId == null || levelDocId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid multiplayer level.", Toast.LENGTH_SHORT).show();
            returnToMap();
            return;
        }

        db.collection("multiplayerLevels")
                .document(levelDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String levelName = documentSnapshot.getString("levelName");
                        String creatorName = documentSnapshot.getString("creatorName");
                        if (!TextUtils.isEmpty(levelName)) {
                            multiplayerLevelName = levelName;
                        }
                        if (!TextUtils.isEmpty(creatorName)) {
                            multiplayerCreatorName = creatorName;
                        }
                        applyLevelFromDocument(documentSnapshot, documentSnapshot.getLong("levelNumber"));
                    } else {
                        Toast.makeText(this, "Multiplayer level not found.", Toast.LENGTH_SHORT).show();
                        returnToMap();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading multiplayer level", e);
                    Toast.makeText(this, "Check internet connection", Toast.LENGTH_SHORT).show();
                });
    }
    private void loadLevelConfigFromFirestore(int levelNumber) {
        Log.d(TAG, "Fetching config for level: " + levelNumber);

        db.collection("levels").document(String.valueOf(levelNumber))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        applyLevelFromDocument(documentSnapshot, (long) levelNumber);
                    } else {
                        Log.e(TAG, "Level " + levelNumber + " config not found in Firestore.");
                        Toast.makeText(this, "Level data not found in Firestore.", Toast.LENGTH_SHORT).show();
                        returnToMap();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading level config", e);
                    Toast.makeText(this, "Check internet connection", Toast.LENGTH_SHORT).show();
                });
    }

    private void applyLevelFromDocument(DocumentSnapshot documentSnapshot, Long defaultLevelNumber) {
        int targetScore = getLongField(documentSnapshot, "targetScore", 0);
        int movesLimit = getLongField(documentSnapshot, "movesLimit", 0);
        int gridSize = getLongField(documentSnapshot, "gridSize", 8);
        int targetCandyType = getLongField(documentSnapshot, "targetCandyType", -1);
        int targetCandyCount = getLongField(documentSnapshot, "targetCandyCount", 0);
        int[][] customGridLayout = parseCustomGridLayout(documentSnapshot.get("customGridLayout"));

        int rows = gridSize;
        int cols = gridSize;
        if (customGridLayout != null) {
            rows = customGridLayout.length;
            cols = customGridLayout.length > 0 ? customGridLayout[0].length : gridSize;
        }

        int levelNumber = defaultLevelNumber == null ? 1 : defaultLevelNumber.intValue();
        LevelConfig config = new LevelConfig(
                levelNumber,
                rows,
                cols,
                targetScore,
                movesLimit,
                targetCandyType,
                targetCandyCount,
                customGridLayout
        );
        gameGridView.setupGridFromRemote(config);

        if (isMultiplayerLevel && missionTextView != null) {
            String nameToShow = TextUtils.isEmpty(multiplayerLevelName) ? "Multiplayer Level" : multiplayerLevelName;
            String creatorToShow = TextUtils.isEmpty(multiplayerCreatorName) ? "Player" : multiplayerCreatorName;
            missionTextView.setText(nameToShow + " • by " + creatorToShow);
        }
    }
    private int getLongField(DocumentSnapshot snapshot, String fieldName, int defaultValue) {
        Long value = snapshot.getLong(fieldName);
        if (value == null) {
            return defaultValue;
        }
        return value.intValue();
    }

    private int[][] parseCustomGridLayout(Object rawLayout) {
        if (rawLayout == null) {
            return null;
        }
        if (rawLayout instanceof java.util.List) {
            java.util.List<?> rows = (java.util.List<?>) rawLayout;
            if (rows.isEmpty()) {
                return null;
            }
            int[][] grid = new int[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                Object rowObj = rows.get(i);
                if (!(rowObj instanceof java.util.List)) {
                    return null;
                }
                java.util.List<?> row = (java.util.List<?>) rowObj;
                grid[i] = new int[row.size()];
                for (int j = 0; j < row.size(); j++) {
                    Object value = row.get(j);
                    if (!(value instanceof Number)) {
                        return null;
                    }
                    grid[i][j] = ((Number) value).intValue();
                }
            }
            return grid;
        }

        if (rawLayout instanceof java.util.Map) {
            java.util.Map<?, ?> rows = (java.util.Map<?, ?>) rawLayout;
            if (rows.isEmpty()) {
                return null;
            }
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (Object key : rows.keySet()) {
                keys.add(String.valueOf(key));
            }
            keys.sort(java.util.Comparator.comparingInt(this::extractRowIndex));

            int[][] grid = new int[keys.size()][];
            for (int i = 0; i < keys.size(); i++) {
                Object rowObj = rows.get(keys.get(i));
                if (!(rowObj instanceof java.util.List)) {
                    return null;
                }
                java.util.List<?> row = (java.util.List<?>) rowObj;
                grid[i] = new int[row.size()];
                for (int j = 0; j < row.size(); j++) {
                    Object value = row.get(j);
                    if (!(value instanceof Number)) {
                        return null;
                    }
                    grid[i][j] = ((Number) value).intValue();
                }
            }
            return grid;
        }

        return null;
    }

    private int extractRowIndex(String key) {
        if (key == null) {
            return 0;
        }
        String digits = key.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        BackgroundMusicManager.onScreenStart(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BackgroundMusicManager.onScreenStop();
    }

}
