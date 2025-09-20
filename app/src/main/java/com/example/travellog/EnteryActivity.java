package com.example.travellog; // Make sure this matches your actual package name

import android.content.Intent;import android.os.Bundle;
import android.view.View; // Import View for OnClickListener
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EnteryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entery); // This links to your activity_entery.xml

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find the buttons by their IDs from the XML layout
        Button playButton = findViewById(R.id.play_button);
        Button loginButton = findViewById(R.id.login_button_entry);

        // Set click listener for the Play button
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start FeedActivity
                Intent intent = new Intent(EnteryActivity.this, FeedActivity.class);
                startActivity(intent);
            }
        });

        // Set click listener for the Login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start LoginActivity
                Intent intent = new Intent(EnteryActivity.this, LoginActivity.class);
                startActivity(intent);
                // Optionally, if you don't want the user to return to EnteryActivity
                // after going to LoginActivity, you can call finish():
                // finish();
            }
        });
    }
}
