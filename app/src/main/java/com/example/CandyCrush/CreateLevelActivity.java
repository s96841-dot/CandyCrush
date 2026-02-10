package com.example.CandyCrush;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.CandyCrush.gamecore.Candy;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.example.CandyCrush.utils.GeminiManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private EditText promptInput;
    private Button saveButton;
    private Button cancelButton;
    private Button generateLayoutButton;
    private boolean isSaving = false;
    private boolean isGenerating = false;

    private FirebaseFirestore db;
    private final GeminiManager geminiManager = GeminiManager.getInstance();

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
        promptInput = findViewById(R.id.input_prompt);
        saveButton = findViewById(R.id.btn_save_level);
        cancelButton = findViewById(R.id.btn_cancel_level);
        generateLayoutButton = findViewById(R.id.btn_generate_layout);

        saveButton.setOnClickListener(view -> saveLevel());
        cancelButton.setOnClickListener(view -> finish());
        generateLayoutButton.setOnClickListener(view -> generateLayoutFromPrompt());
    }

    private void saveLevel() {
        if (isSaving) {
            return;
        }
        Integer levelNumber = parseRequiredInt(levelNumberInput.getText().toString().trim(), "Level number");
        if (levelNumber == null) {
            return;
        }
        if (levelNumber <= 0) {
            Toast.makeText(this, "Level number must be greater than 0.", Toast.LENGTH_SHORT).show();
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
            data.put("customGridLayout", toRowMap(customLayout));
        }

        isSaving = true;
        saveButton.setEnabled(false);
        Toast.makeText(this, "Saving level...", Toast.LENGTH_SHORT).show();

        db.collection("levels").document(String.valueOf(levelNumber))
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    isSaving = false;
                    saveButton.setEnabled(true);
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    isSaving = false;
                    saveButton.setEnabled(true);
                    Log.e(TAG, "Failed to save level", e);
                    Toast.makeText(this, "Failed to save level: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private Map<String, Object> toRowMap(List<List<Integer>> layout) {
        Map<String, Object> rows = new HashMap<>();
        for (int i = 0; i < layout.size(); i++) {
            rows.put("row" + i, layout.get(i));
        }
        return rows;
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Level saved")
                .setMessage("Your level was saved successfully.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void generateLayoutFromPrompt() {
        if (isGenerating) {
            return;
        }
        String promptText = promptInput.getText().toString();
        if (TextUtils.isEmpty(promptText.trim())) {
            Toast.makeText(this, "Describe the level you want to generate.", Toast.LENGTH_SHORT).show();
            return;
        }

        isGenerating = true;
        generateLayoutButton.setEnabled(false);
        if (!geminiManager.isConfigured()) {
            Log.w(TAG, "Gemini API key not set.");
            applyGenerationError("Gemini API key not configured.");
            return;
        }

        requestGeminiLayout(promptText);
    }

    private String layoutToString(List<List<Integer>> layout) {
        StringBuilder builder = new StringBuilder();
        for (int r = 0; r < layout.size(); r++) {
            List<Integer> row = layout.get(r);
            for (int c = 0; c < row.size(); c++) {
                if (c > 0) {
                    builder.append(' ');
                }
                builder.append(row.get(c));
            }
            if (r < layout.size() - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private void requestGeminiLayout(String promptText) {
        geminiManager.sendText(buildGeminiPrompt(promptText), new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                LevelGenerationResult generationResult = parseGeminiLevelConfig(result);
                runOnUiThread(() -> {
                    if (generationResult == null) {
                        applyGenerationError("Gemini response invalid.");
                    } else {
                        applyGeneratedLevel(generationResult);
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "Gemini request failed", error);
                runOnUiThread(() -> applyGenerationError(error.getMessage() == null ? "Gemini request failed." : error.getMessage()));
            }
        });
    }

    private String buildGeminiPrompt(String promptText) {
        return "You generate complete Candy Crush level configs. "
                + "Return ONLY one JSON object with keys: gridSize,targetScore,movesLimit,targetCandyType,targetCandyCount,layout. "
                + "Rules: gridSize 7..10, layout is square gridSize x gridSize. "
                + "Cell values: -1 for free space, 0..5 for regular candy types. "
                + "Create the requested shape inside layout. "
                + "Use -1 for non-shape space so the app can auto-fill safe candies there. "
                + "targetScore > 0, movesLimit between 15 and 60, targetCandyType -1 or 0..5, targetCandyCount >= 0. "
                + "No markdown, no backticks, no explanations. "
                + "User request: " + promptText;
    }

    private LevelGenerationResult parseGeminiLevelConfig(String geminiText) {
        try {
            String jsonObjectText = extractJsonObject(geminiText);
            if (TextUtils.isEmpty(jsonObjectText)) {
                return null;
            }

            JSONObject root = new JSONObject(jsonObjectText);
            if (!root.has("layout")) {
                return null;
            }

            List<List<Integer>> layout = parseLayoutArray(root.optJSONArray("layout"));
            if (!isValidLayout(layout)) {
                return null;
            }

            int shapeCandyType = findDominantShapeCandyType(layout);
            fillFreeSpacesWithSafeCandies(layout, shapeCandyType);

            int gridSize = getIntOrDefault(root, "gridSize", layout.size());
            if (gridSize != layout.size()) {
                gridSize = layout.size();
            }

            int targetScore = Math.max(500, getIntOrDefault(root, "targetScore", 2500));
            int movesLimit = clamp(getIntOrDefault(root, "movesLimit", 30), 15, 60);
            int targetCandyType = normalizeTargetCandyType(getIntOrDefault(root, "targetCandyType", shapeCandyType));
            int targetCandyCount = Math.max(0, getIntOrDefault(root, "targetCandyCount", Math.max(10, gridSize + 4)));

            return new LevelGenerationResult(gridSize, targetScore, movesLimit, targetCandyType, targetCandyCount, layout);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse Gemini level config", e);
            return null;
        }
    }

    private List<List<Integer>> parseLayoutArray(JSONArray rows) {
        List<List<Integer>> layout = new ArrayList<>();
        if (rows == null) {
            return layout;
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONArray rowArray = rows.optJSONArray(i);
            if (rowArray == null) {
                return new ArrayList<>();
            }
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < rowArray.length(); j++) {
                row.add(rowArray.optInt(j, Integer.MIN_VALUE));
            }
            layout.add(row);
        }
        return layout;
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) {
            return "";
        }
        return text.substring(start, end + 1).trim();
    }

    private int getIntOrDefault(JSONObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            return obj.getInt(key);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int findDominantShapeCandyType(List<List<Integer>> layout) {
        int[] counts = new int[Candy.NUMBER_OF_REGULAR_CANDY_TYPES];
        for (List<Integer> row : layout) {
            for (Integer value : row) {
                if (value != null && value >= 0 && value < Candy.NUMBER_OF_REGULAR_CANDY_TYPES) {
                    counts[value]++;
                }
            }
        }
        int bestType = 0;
        int bestCount = -1;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > bestCount) {
                bestCount = counts[i];
                bestType = i;
            }
        }
        return bestType;
    }

    private void fillFreeSpacesWithSafeCandies(List<List<Integer>> layout, int shapeCandyType) {
        int size = layout.size();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (layout.get(r).get(c) != -1) {
                    continue;
                }

                int chosen = chooseSafeCandyForCell(layout, r, c, shapeCandyType);
                layout.get(r).set(c, chosen);
            }
        }
    }

    private int chooseSafeCandyForCell(List<List<Integer>> layout, int r, int c, int shapeCandyType) {
        for (int type = 0; type < Candy.NUMBER_OF_REGULAR_CANDY_TYPES; type++) {
            if (type == shapeCandyType) {
                continue;
            }
            if (!createsImmediateMatch(layout, r, c, type)) {
                return type;
            }
        }

        for (int type = 0; type < Candy.NUMBER_OF_REGULAR_CANDY_TYPES; type++) {
            if (!createsImmediateMatch(layout, r, c, type)) {
                return type;
            }
        }

        return (shapeCandyType + 1) % Candy.NUMBER_OF_REGULAR_CANDY_TYPES;
    }

    private boolean createsImmediateMatch(List<List<Integer>> layout, int r, int c, int type) {
        int left1 = getCell(layout, r, c - 1);
        int left2 = getCell(layout, r, c - 2);
        if (left1 == type && left2 == type) {
            return true;
        }

        int up1 = getCell(layout, r - 1, c);
        int up2 = getCell(layout, r - 2, c);
        if (up1 == type && up2 == type) {
            return true;
        }

        int right1 = getCell(layout, r, c + 1);
        int right2 = getCell(layout, r, c + 2);
        if (right1 == type && right2 == type) {
            return true;
        }

        int down1 = getCell(layout, r + 1, c);
        int down2 = getCell(layout, r + 2, c);
        if (down1 == type && down2 == type) {
            return true;
        }

        if (left1 == type && right1 == type) {
            return true;
        }
        return up1 == type && down1 == type;
    }

    private int getCell(List<List<Integer>> layout, int r, int c) {
        if (r < 0 || c < 0 || r >= layout.size() || c >= layout.get(r).size()) {
            return Integer.MIN_VALUE;
        }
        Integer value = layout.get(r).get(c);
        return value == null ? Integer.MIN_VALUE : value;
    }

    private int normalizeTargetCandyType(int targetCandyType) {
        if (targetCandyType == -1) {
            return -1;
        }
        return clamp(targetCandyType, 0, Candy.NUMBER_OF_REGULAR_CANDY_TYPES - 1);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isValidLayout(List<List<Integer>> layout) {
        if (layout == null || layout.isEmpty()) {
            return false;
        }
        int size = layout.size();
        if (size < 7 || size > 10) {
            return false;
        }
        for (List<Integer> row : layout) {
            if (row.size() != size) {
                return false;
            }
            for (Integer value : row) {
                if (value == null) {
                    return false;
                }
                if (value != -1 && (value < 0 || value >= Candy.NUMBER_OF_REGULAR_CANDY_TYPES)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void applyGeneratedLevel(LevelGenerationResult result) {
        customGridLayoutInput.setText(layoutToString(result.layout));
        gridSizeInput.setText(String.valueOf(result.gridSize));
        targetScoreInput.setText(String.valueOf(result.targetScore));
        movesLimitInput.setText(String.valueOf(result.movesLimit));
        targetCandyTypeInput.setText(String.valueOf(result.targetCandyType));
        targetCandyCountInput.setText(String.valueOf(result.targetCandyCount));

        Toast.makeText(this, "Generated full level with Gemini.", Toast.LENGTH_SHORT).show();
        isGenerating = false;
        generateLayoutButton.setEnabled(true);
    }

    private static class LevelGenerationResult {
        final int gridSize;
        final int targetScore;
        final int movesLimit;
        final int targetCandyType;
        final int targetCandyCount;
        final List<List<Integer>> layout;

        LevelGenerationResult(int gridSize,
                              int targetScore,
                              int movesLimit,
                              int targetCandyType,
                              int targetCandyCount,
                              List<List<Integer>> layout) {
            this.gridSize = gridSize;
            this.targetScore = targetScore;
            this.movesLimit = movesLimit;
            this.targetCandyType = targetCandyType;
            this.targetCandyCount = targetCandyCount;
            this.layout = layout;
        }
    }

    private void applyGenerationError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        isGenerating = false;
        generateLayoutButton.setEnabled(true);
    }
}