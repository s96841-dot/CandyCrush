package com.example.travellog; // Or your actual package name


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView; // Added
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast; // Added

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull; // Added
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide; // Added
import com.example.travellog.gameui.GameGridView;
import com.example.travellog.gamecore.LevelConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference; // Added
import com.google.firebase.firestore.DocumentSnapshot; // Added
import com.google.firebase.firestore.FirebaseFirestore; // Added

public class FeedActivity extends AppCompatActivity {

    private static final String TAG = "FeedActivity";

    private GameGridView gameGridView;
    private int currentLevel = 1;

    private Spinner levelSpinner;
    private Button logoutButtonTop;
    private TextView welcomeTextView;
    private ImageView profileImageViewFeed; // Added

    // User data fields (primarily from SharedPreferences for quick access)
    private String nickname;
    // private int age; // Not used in current welcome message logic
    private int userOverallLevel;
    private String profileImageUrlFromFirestore; // To store URL fetched from Firestore

    private static final String ANONYMOUS_NICKNAME_FALLBACK = "Player";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Added for Firestore access

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
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        gameGridView = findViewById(R.id.gameGridView);
        levelSpinner = findViewById(R.id.levelSpinner);
        logoutButtonTop = findViewById(R.id.logoutButtonTop);
        welcomeTextView = findViewById(R.id.TextViewactivity_feed);
        profileImageViewFeed = findViewById(R.id.profileImageViewFeed); // Initialize ImageView

        if (gameGridView == null) Log.e(TAG, "onCreate: GameGridView not found!");
        if (levelSpinner == null) Log.e(TAG, "onCreate: levelSpinner not found!");
        if (logoutButtonTop == null) Log.e(TAG, "onCreate: logoutButtonTop not found!");
        if (welcomeTextView == null) Log.e(TAG, "onCreate: welcomeTextView not found!");
        if (profileImageViewFeed == null) Log.e(TAG, "onCreate: profileImageViewFeed not found!");
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
        // Load data from SharedPreferences first for quick display
        readUserDataFromPrefs();
        // Then load potentially more up-to-date data from Firestore (especially profile image URL)
        loadUserDataFromFirestore(currentUser); // Pass currentUser
        // UI setup that depends on this data will be called within/after Firestore load
    }

    // Renamed to avoid confusion with the new Firestore loading method
    private void readUserDataFromPrefs() {
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        nickname = sharedPreferences.getString("nickname", ANONYMOUS_NICKNAME_FALLBACK);
        userOverallLevel = sharedPreferences.getInt("level", 1);
        Log.d(TAG, "Read user data from Prefs: Nickname=" + nickname + ", OverallLevel=" + userOverallLevel);
        // Initial update of welcome message with data from SharedPreferences
        updateWelcomeMessage();
        // Initially set a default profile image, it will be updated by Firestore load if available
        if (profileImageViewFeed != null) {
            profileImageViewFeed.setImageResource(R.drawable.ic_default_profile);
        }
    }

    private void loadUserDataFromFirestore(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            Log.e(TAG, "Cannot load user data from Firestore, firebaseUser is null");
            // Setup UI with whatever we got from SharedPreferences or defaults
            setupRemainingUI();
            return;
        }
        String userId = firebaseUser.getUid();
        DocumentReference userDocRef = db.collection("users").document(userId); // Assuming "users" collection

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    // Update nickname and level from Firestore if they exist,
                    // potentially overriding SharedPreferences if Firestore is more current.
                    String firestoreNickname = document.getString("nickname");
                    if (firestoreNickname != null && !firestoreNickname.isEmpty()) {
                        nickname = firestoreNickname; // Update local nickname
                    }
                    // You might also have "level" in Firestore, update userOverallLevel if so
                    // Long firestoreLevel = document.getLong("level");
                    // if (firestoreLevel != null) {
                    //    userOverallLevel = firestoreLevel.intValue();
                    // }

                    profileImageUrlFromFirestore = document.getString("profileImageUrl");
                    Log.d(TAG, "Firestore data: Nickname=" + nickname + ", ProfileImgURL=" + profileImageUrlFromFirestore);

                    // Update UI with data from Firestore
                    updateWelcomeMessage(); // Re-update with potentially new nickname
                    updateProfileImage();

                } else {
                    Log.d(TAG, "No such user document in Firestore for ID: " + userId);
                    // Use SharedPreferences data or defaults if Firestore doc doesn't exist
                    updateProfileImage(); // Will use null URL, so default image
                }
            } else {
                Log.e(TAG, "Error getting user document from Firestore: ", task.getException());
                Toast.makeText(FeedActivity.this, "Failed to load latest profile details.", Toast.LENGTH_SHORT).show();
                // Use SharedPreferences data or defaults on error
                updateProfileImage(); // Will use null URL, so default image
            }
            // Setup spinner and logout button after attempting to load user data
            setupRemainingUI();
        });
    }

    private void updateProfileImage() {
        if (profileImageViewFeed == null) return;

        if (profileImageUrlFromFirestore != null && !profileImageUrlFromFirestore.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrlFromFirestore)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .circleCrop() // Optional: if you want circular images
                    .into(profileImageViewFeed);
            Log.d(TAG, "Profile image updated from Firestore URL.");
        } else {
            profileImageViewFeed.setImageResource(R.drawable.ic_default_profile);
            Log.d(TAG, "No profile image URL from Firestore, using default.");
        }
    }

    // Call this method AFTER user data (especially from Firestore) has been fetched
    private void setupRemainingUI() {
        // --- Populate Spinner ---
        setupLevelSpinner();

        // --- Logout Button ---
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
        // ... (your existing setupLevelSpinner logic remains unchanged)
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

        if (currentLevel < 1 || currentLevel > maxLevels) {
            currentLevel = 1;
        }
        levelSpinner.setSelection(currentLevel - 1);
    }


    private void updateWelcomeMessage() {
        if (welcomeTextView != null) {
            // Use the 'nickname' field which is updated by both SharedPreferences and Firestore
            String displayedNickname = (nickname == null || nickname.isEmpty()) ? ANONYMOUS_NICKNAME_FALLBACK : nickname;
            // The 'userOverallLevel' is currently only from SharedPreferences in this setup
            // 'currentLevel' is from the spinner selection
            String message = "Welcome, " + displayedNickname + " (Progress: " + userOverallLevel + ") - Level: " + currentLevel;
            welcomeTextView.setText(message);
            Log.d(TAG, "Welcome message updated: " + message);
        }
    }

    // This was previously readUserData(), renamed to be more specific
    // private void readUserDataFromPrefs() { ... } // Defined above

    private void clearLocalUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Local SharedPreferences (userInfo) cleared.");
        nickname = ANONYMOUS_NICKNAME_FALLBACK;
        // age = 0;
        userOverallLevel = 1;
        currentLevel = 1;
        profileImageUrlFromFirestore = null; // Clear this as well
    }
}
