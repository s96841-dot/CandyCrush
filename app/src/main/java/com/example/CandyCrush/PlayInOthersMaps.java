package com.example.CandyCrush;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PlayInOthersMaps extends AppCompatActivity {
    private LinearLayout levelsContainer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play_in_others_maps);

        levelsContainer = findViewById(R.id.multiplayer_levels_container);
        Button backButton = findViewById(R.id.btn_back_to_map);
        backButton.setOnClickListener(v -> finish());

        loadMultiplayerLevels();
    }

    private void loadMultiplayerLevels() {
        FirebaseFirestore.getInstance()
                .collection("multiplayerLevels")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    levelsContainer.removeAllViews();
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        docs.add(snapshot);
                    }

                    if (docs.isEmpty()) {
                        TextView emptyText = new TextView(this);
                        emptyText.setText("No multiplayer levels yet. Create one from the map screen.");
                        levelsContainer.addView(emptyText);
                        return;
                    }

                    for (QueryDocumentSnapshot snapshot : docs) {
                        addLevelCard(snapshot);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load multiplayer levels.", Toast.LENGTH_SHORT).show()
                );
    }

    private void addLevelCard(QueryDocumentSnapshot snapshot) {
        final String levelId = snapshot.getId();
        String levelNameStr = snapshot.getString("levelName");
        String creatorNameStr = snapshot.getString("creatorName");
        Long levelNumber = snapshot.getLong("levelNumber");

        if (levelNameStr == null || levelNameStr.trim().isEmpty()) {
            levelNameStr = levelNumber == null ? "Unnamed Level" : "Level " + levelNumber;
        }
        if (creatorNameStr == null || creatorNameStr.trim().isEmpty()) {
            creatorNameStr = "Player";
        }

        final String levelName = levelNameStr;
        final String creatorName = creatorNameStr;

        Button levelButton = new Button(this);
        levelButton.setAllCaps(false);
        levelButton.setText(levelName + "\nby " + creatorName);
        levelButton.setOnClickListener(v -> openMultiplayerLevel(levelId, levelName, creatorName));
        levelsContainer.addView(levelButton);
    }

    private void openMultiplayerLevel(String levelDocId, String levelName, String creatorName) {
        Intent intent = new Intent(this, FeedActivity.class);
        intent.putExtra("LEVEL_SOURCE", "MULTIPLAYER");
        intent.putExtra("MULTIPLAYER_LEVEL_DOC_ID", levelDocId);
        intent.putExtra("MULTIPLAYER_LEVEL_NAME", levelName);
        intent.putExtra("MULTIPLAYER_CREATOR_NAME", creatorName);
        startActivity(intent);
    }
}
