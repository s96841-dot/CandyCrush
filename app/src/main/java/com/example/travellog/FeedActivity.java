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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travellog.utils.PostsAdapter;
import com.google.firebase.auth.FirebaseAuth;

public class FeedActivity extends AppCompatActivity {
    private static final String TAG = "FeedActivity";
    protected String nickname;
    int age;
    int level;
    private RecyclerView recyclerView;
    private PostsAdapter postsAdapter;


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

        // Setup for the logout button
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

        // --- START OF ADDED CODE ---

        // Setup for the "Add Post" button
        Button addPostButton = findViewById(R.id.new_bottom_button);
        addPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Add Post button clicked. Navigating to AddPostActivity.");
                // Create an Intent to start AddPostActivity
                Intent intent = new Intent(FeedActivity.this, AddPostActivity.class);
                // Start the new activity
                startActivity(intent);
            }
        });

        // --- END OF ADDED CODE ---

        readUserData();

        // Update the welcome text
        TextView welcomeTextView = findViewById(R.id.TextViewactivity_feed);
        String welcomeMessage = "Welcome, " + nickname + " (Level: " + level + ")!";
        welcomeTextView.setText(welcomeMessage);
        Log.d(TAG, "onCreate: Updated welcome text to: '" + welcomeMessage + "'");
        initRecyclerView();
    }

    private void readUserData(){
        Log.d(TAG, "readUserData: start");
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);

        nickname = sharedPreferences.getString("nickname", "N/A");
        Log.d(TAG, "readUserData: nickname: " + nickname);
        age = sharedPreferences.getInt("age", 0);
        Log.d(TAG, "readUserData: age: " + age);
        level = sharedPreferences.getInt("level", 1);
        Log.d(TAG, "readUserData: level: " + level);
    }
    private void initRecyclerView()
    {
        recyclerView = findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsAdapter = new PostsAdapter();
        recyclerView.setAdapter(postsAdapter);
    }

}