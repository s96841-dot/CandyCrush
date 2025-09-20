package com.example.travellog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView; // Import for TextView

import androidx.activity.EdgeToEdge;import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class FeedActivity extends AppCompatActivity {
    private static final String TAG = "FeedActivity";
    protected String nickname;
    int age;
    int level;

    // Define a constant for the anonymous nickname
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

        Button logOutButton = findViewById(R.id.buttonlogout);
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Logout button clicked.");
                FirebaseAuth.getInstance().signOut();
                // Clear SharedPreferences when logging out
                clearUserData();
                Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                Log.d(TAG, "onClick: Logout complete, navigated to LoginActivity.");
            }
        });

        readUserData(); // Read data from SharedPreferences

        TextView welcomeTextView = findViewById(R.id.TextViewactivity_feed);
        String welcomeMessage;

        // Check if the user is anonymous based on the nickname
        if (ANONYMOUS_NICKNAME.equals(nickname)) {
            welcomeMessage = "Welcome anonymous";
            Log.d(TAG, "onCreate: User is anonymous. Setting welcome message: '" + welcomeMessage + "'");
        } else {
            // Create a welcome message that includes the user's level for registered users
            welcomeMessage = "Welcome, " + nickname + " (Level: " + level + ")!";
            Log.d(TAG, "onCreate: User is '" + nickname + "'. Setting welcome message: '" + welcomeMessage + "'");
        }
        welcomeTextView.setText(welcomeMessage);
    }

    private void readUserData(){
        Log.d(TAG, "readUserData: Reading data from userInfo.xml");
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);

        // nickname - ANONYMOUS_NICKNAME ("N/A") is a default value if nickname is not found
        nickname = sharedPreferences.getString("nickname", ANONYMOUS_NICKNAME);
        Log.d(TAG, "readUserData: nickname: " + nickname);

        // age - 0 is a default value if age is not found in the file
        age = sharedPreferences.getInt("age", 0); // Age might not be relevant for anonymous
        Log.d(TAG, "readUserData: age: " + age);

        // level - 1 is a default value if level is not found in the file
        level = sharedPreferences.getInt("level", 1); // Level might not be relevant for anonymous
        Log.d(TAG, "readUserData: level: " + level);
    }

    private void clearUserData() {
        Log.d(TAG, "clearUserData: Clearing user data from SharedPreferences.");
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Clears all data from this SharedPreferences file
        editor.apply();
        Log.i(TAG, "clearUserData: User data cleared.");
    }
}
