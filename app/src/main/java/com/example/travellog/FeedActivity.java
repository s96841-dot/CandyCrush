package com.example.travellog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView; // Import for TextView

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class FeedActivity extends AppCompatActivity {
    private static final String TAG = "FeedActivity";
    protected String nickname;
    int age;
    // --- START OF MODIFIED CODE ---
    int level;
    // --- END OF MODIFIED CODE ---

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

        Button logOutButton = findViewById(R.id.buttonlogout);
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick:start ");
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                Log.d(TAG, "onClick:end ");

            }
        });
        readUserData();

        // --- START OF MODIFIED CODE ---
        TextView welcomeTextView = findViewById(R.id.TextViewactivity_feed);
        // Create a welcome message that includes the user's level
        String welcomeMessage = "Welcome, " + nickname + " (Level: " + level + ")!";
        welcomeTextView.setText(welcomeMessage);
        Log.d(TAG, "onCreate: Updated welcome text to: '" + welcomeMessage + "'");
        // --- END OF MODIFIED CODE ---
    }
    private void readUserData(){
        Log.d(TAG, "readUserData: start");
        //about to read data from userInfo.xml
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);

        // nickname - "N/A" is a default value if nickname is not found in the file
        nickname = sharedPreferences.getString("nickname", "N/A");
        Log.d(TAG, "readUserData: nickname: " + nickname);
        // age - 0 is a default value if age is not found in the file
        age = sharedPreferences.getInt("age", 0);
        Log.d(TAG, "readUserData: age: " + age);

        // --- START OF MODIFIED CODE ---
        // level - 1 is a default value if level is not found in the file
        level = sharedPreferences.getInt("level", 1);
        Log.d(TAG, "readUserData: level: " + level);
        // --- END OF MODIFIED CODE ---
    }

}
