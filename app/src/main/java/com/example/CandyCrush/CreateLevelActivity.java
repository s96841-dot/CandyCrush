package com.example.CandyCrush;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateLevelActivity extends AppCompatActivity {

    private static final String TAG = "CreateLevelActivity";

    private EditText levelNumberInput;
    private EditText gridSizeInput;
    private EditText targetScoreInput;
    private EditText movesLimitInput;
    private EditText targetCandyTypeInput;
    private EditText targetCandyCountInput;
    private EditText customGridLayoutInput;
    private Button saveButton;
    private Button cancelButton;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_level);

        db = FirebaseFirestore.getInstance();

        levelNumberInput = findViewById(R.id.input_level_number);
        gridSizeInput = findViewById(R.id.input_grid_size);
        targetScoreInput = findViewById(R.id.input_target_score);
        movesLimitInput = findViewById(R.id.input_moves_limit);
        targetCandyTypeInput = findViewById(R.id.input_target_candy_type);
        targetCandyCountInput = findViewById(R.id.input_target_candy_count);
        customGridLayoutInput = findViewById(R.id.input_custom_grid);
        saveButton = findViewById(R.id.btn_save_level);
        cancelButton = findViewById(R.id.btn_cancel_level);

        saveButton.setOnClickListener(view -> saveLevel());
        cancelButton.setOnClickListener(view -> finish());
    }

    private void saveLevel() {
        Integer levelNumber = parseRequiredInt(levelNumberInput.getText().toString().trim(), "Level number");
        if (levelNumber == null) {
            return;
        }

        Integer targetScore = parseOptionalInt(targetScoreInput.getText().toString().trim(), 0);
        Integer movesLimit = parseOptionalInt(movesLimitInput.getText().toString().trim(), 0);
        Integer targetCandyType = parseOptionalInt(targetCandyTypeInput.getText().toString().trim(), -1);
        Integer targetCandyCount = parseOptionalInt(targetCandyCountInput.getText().toString().trim(), 0);
        if (targetScore == null || movesLimit == null || targetCandyType == null || targetCandyCount == null) {
            return;
        }

        String gridSizeText = gridSizeInput.getText().toString().trim();
        Integer gridSize = TextUtils.isEmpty(gridSizeText) ? null : parseOptionalInt(gridSizeText, 0);
        if (!TextUtils.isEmpty(gridSizeText) && gridSize == null) {
            return;
        }

        String layoutText = customGridLayoutInput.getText().toString().trim();
        List<List<Integer>> customLayout = null;
        if (!layoutText.isEmpty()) {
            customLayout = parseCustomLayout(layoutText);
            if (customLayout == null) {
                return;
            }
            if (gridSize == null || gridSize == 0) {
                gridSize = customLayout.size();
            }
            int layoutRows = customLayout.size();
            int layoutCols = customLayout.get(0).size();
            if (layoutRows != gridSize || layoutCols != gridSize) {
                Toast.makeText(this, "Custom layout must match grid size.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (gridSize == null || gridSize <= 0) {
            Toast.makeText(this, "Grid size is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("targetScore", targetScore);
        data.put("movesLimit", movesLimit);
        data.put("gridSize", gridSize);
        data.put("targetCandyType", targetCandyType);
        data.put("targetCandyCount", targetCandyCount);
        if (customLayout != null) {
            data.put("customGridLayout", customLayout);
        }

        db.collection("levels").document(String.valueOf(levelNumber))
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Level saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save level", e);
                    Toast.makeText(this, "Failed to save level.", Toast.LENGTH_SHORT).show();
                });
    }

    private Integer parseRequiredInt(String value, String label) {
        if (TextUtils.isEmpty(value)) {
            Toast.makeText(this, label + " is required.", Toast.LENGTH_SHORT).show();
            return null;
        }
        return parseOptionalInt(value, 0);
    }

    private Integer parseOptionalInt(String value, int fallback) {
        if (TextUtils.isEmpty(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number: " + value, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private List<List<Integer>> parseCustomLayout(String text) {
        String[] lines = text.split("\\r?\\n");
        List<List<Integer>> layout = new ArrayList<>();
        int expectedCols = -1;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split("[,\\s]+");
            List<Integer> row = new ArrayList<>();
            for (String part : parts) {
                try {
                    row.add(Integer.parseInt(part));
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid layout value: " + part, Toast.LENGTH_SHORT).show();
                    return null;
                }
            }
            if (expectedCols == -1) {
                expectedCols = row.size();
            } else if (row.size() != expectedCols) {
                Toast.makeText(this, "All layout rows must have the same length.", Toast.LENGTH_SHORT).show();
                return null;
            }
            layout.add(row);
        }
        if (layout.isEmpty()) {
            Toast.makeText(this, "Custom layout is empty.", Toast.LENGTH_SHORT).show();
            return null;
        }
        return layout;
    }
}