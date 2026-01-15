package com.example.CandyCrush;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private int lastUnlockedLevel = 1;
    private FirebaseAuth mAuth;
    private GridLayout levelsGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mAuth = FirebaseAuth.getInstance();
        levelsGrid = findViewById(R.id.levelsGrid);

        // חיבור כפתור ה-Logout
        Button logoutButton = findViewById(R.id.btn_map_logout);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                mAuth.signOut();
                SharedPreferences prefs = getSharedPreferences("userInfo", MODE_PRIVATE);
                prefs.edit().clear().apply();

                Intent intent = new Intent(MapActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // טעינה ראשונית של הכפתורים
        refreshMap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // קריטי: בכל פעם שחוזרים מהמשחק למפה, בודקים אם רמה חדשה נפתחה
        Log.d(TAG, "onResume: Refreshing map buttons.");
        refreshMap();
    }

    /**
     * פונקציה שטוענת את הנתונים המעודכנים ובונת את הכפתורים מחדש
     */
    private void refreshMap() {
        // 1. קריאת השלב האחרון מהזיכרון המקומי
        SharedPreferences prefs = getSharedPreferences("userInfo", MODE_PRIVATE);
        lastUnlockedLevel = prefs.getInt("level", 1);
        Log.d(TAG, "Current unlocked level: " + lastUnlockedLevel);

        // 2. ניקוי הלוח הקיים לפני בנייה מחדש
        if (levelsGrid != null) {
            levelsGrid.removeAllViews();

            // 3. יצירת כפתורי השלבים (12 שלבים)
            for (int i = 1; i <= 12; i++) {
                addLevelButton(i);
            }
        }
    }

    private void addLevelButton(int levelNum) {
        Button levelButton = new Button(this);
        levelButton.setText("Level " + levelNum);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        int marginInPx = (int) (8 * getResources().getDisplayMetrics().density);
        params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        levelButton.setLayoutParams(params);

        // לוגיקת נעילה: אם מספר השלב גבוה מהשלב הפתוח - הכפתור נעול
        if (levelNum > lastUnlockedLevel) {
            levelButton.setEnabled(false);
            levelButton.setAlpha(0.5f); // מראה של כפתור נעול
        } else {
            levelButton.setEnabled(true);
            levelButton.setAlpha(1.0f); // מראה של כפתור פעיל

            // רק שלבים פתוחים אפשר ללחוץ
            levelButton.setOnClickListener(v -> {
                Intent intent = new Intent(MapActivity.this, FeedActivity.class);
                intent.putExtra("SELECTED_LEVEL", levelNum);
                startActivity(intent);
            });
        }

        levelsGrid.addView(levelButton);
    }
    private void syncLevelFromFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("level")) {
                            int cloudLevel = documentSnapshot.getLong("level").intValue();

                            // אם הרמה בענן גבוהה יותר ממה שיש לנו במכשיר, נעדכן
                            if (cloudLevel > lastUnlockedLevel) {
                                lastUnlockedLevel = cloudLevel;
                                // עדכון הזיכרון המקומי
                                getSharedPreferences("userInfo", MODE_PRIVATE)
                                        .edit().putInt("level", cloudLevel).apply();
                                // רענון הכפתורים
                                refreshMap();
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error syncing level", e));
        }
    }
}