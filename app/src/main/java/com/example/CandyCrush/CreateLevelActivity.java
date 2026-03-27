package com.example.CandyCrush;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.CandyCrush.gamecore.Candy;
import com.example.CandyCrush.utils.GeminiManager;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateLevelActivity extends AppCompatActivity {

    private static final String TAG = "CreateLevelActivity";

    private EditText levelNumberInput;
    private EditText levelNameInput;
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
    private TextView aiPreviewText;
    private boolean isGenerating = false;

    private final GeminiManager geminiManager = GeminiManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_level);


        levelNumberInput = findViewById(R.id.input_level_number);
        levelNameInput = findViewById(R.id.input_level_name);
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
        aiPreviewText = findViewById(R.id.text_ai_preview);

        saveButton.setOnClickListener(view -> openPreview());
        cancelButton.setOnClickListener(view -> finish());
        generateLayoutButton.setOnClickListener(view -> generateLayoutFromPrompt());
    }
    private void openPreview() {
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

        Intent previewIntent = new Intent(this, LevelPreviewActivity.class);
        previewIntent.putExtra(
                LevelPreviewActivity.EXTRA_LEVEL_NAME,
                levelNameInput.getText().toString().trim()
        );
        previewIntent.putExtra(LevelPreviewActivity.EXTRA_LEVEL_NUMBER, levelNumber);
        previewIntent.putExtra(LevelPreviewActivity.EXTRA_GRID_SIZE, gridSize);
        previewIntent.putExtra(LevelPreviewActivity.EXTRA_TARGET_SCORE, targetScore);
        previewIntent.putExtra(LevelPreviewActivity.EXTRA_MOVES_LIMIT, movesLimit);
        previewIntent.putExtra(LevelPreviewActivity.EXTRA_TARGET_CANDY_TYPE, targetCandyType);
        previewIntent.putExtra(LevelPreviewActivity.EXTRA_TARGET_CANDY_COUNT, targetCandyCount);
        previewIntent.putExtra(LevelPreviewActivity.EXTRA_LAYOUT, new Gson().toJson(customLayout));
        startActivity(previewIntent);
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

    public static Map<String, Object> toRowMap(List<List<Integer>> layout) {
        Map<String, Object> rows = new HashMap<>();
        for (int i = 0; i < layout.size(); i++) {
            rows.put("row" + i, layout.get(i));
        }
        return rows;
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
        geminiManager.sendText(buildGeminiPrompt(promptText), this, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                LevelGenerationResult generationResult = parseGeminiLevelConfig(result);
                runOnUiThread(() -> {
                    if (generationResult == null) {
                        applyLocalFallbackGeneration(promptText, "Gemini response was invalid JSON.");
                    } else {
                        LevelGenerationResult normalizedResult = enforceRequestedShape(promptText, generationResult);
                        applyGeneratedLevel(normalizedResult, "Generated full level with Gemini.");
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                String reason = error != null && !TextUtils.isEmpty(error.getMessage())
                        ? error.getMessage()
                        : "Gemini request failed.";

                if (!isExpectedGeminiFallback(reason)) {
                    Log.e(TAG, "Gemini request failed", error);
                }

                runOnUiThread(() -> applyLocalFallbackGeneration(promptText, reason));
            }
        });
    }

    private boolean isExpectedGeminiFallback(String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }
        String normalized = message.toLowerCase(java.util.Locale.US);
        return normalized.contains("using local generation fallback")
                || normalized.contains("quota")
                || normalized.contains("rate limit")
                || normalized.contains("billing")
                || normalized.contains("endpoint not available")
                || normalized.contains("network error")
                || normalized.contains("returned empty content");
    }

    private String buildGeminiPrompt(String promptText) {
        return "You are a strict JSON-only generator for Candy Crush level configurations.\n" +
                        "\n" +
                        "CRITICAL RULES:\n" +
                        "- Output EXACTLY one valid JSON object.\n" +
                        "- Output NO markdown, NO backticks, NO comments, NO explanations, NO extra text.\n" +
                        "- The response must start with { and end with }.\n" +
                        "- The JSON must be parseable.\n" +
                        "\n" +
                        "SCHEMA:\n" +
                        "{\n" +
                        "  \"gridSize\": int,\n" +
                        "  \"targetScore\": int,\n" +
                        "  \"movesLimit\": int,\n" +
                        "  \"targetCandyType\": int,\n" +
                        "  \"targetCandyCount\": int,\n" +
                        "  \"layout\": int[][]\n" +
                        "}\n" +
                        "\n" +
                        "CANDY TYPE MAP:\n" +
                        "0 = red\n" +
                        "1 = orange\n" +
                        "2 = yellow\n" +
                        "3 = green\n" +
                        "4 = blue\n" +
                        "5 = purple\n" +
                        "\n" +
                        "HARD CONSTRAINTS:\n" +
                        "- gridSize must be between 7 and 10.\n" +
                        "- layout must be exactly gridSize x gridSize.\n" +
                        "- Each row must contain exactly gridSize integers.\n" +
                        "- Number of rows must equal gridSize.\n" +
                        "- Allowed layout cell values:\n" +
                        "  - -1 = empty background (outside the requested shape)\n" +
                        "  - 0..5 = valid playable candy cell (inside the requested shape)\n" +
                        "- You must support ANY user-requested shape (heart, moon, arrow, letters, symbols, animals, etc.).\n" +
                        "- The layout MUST visually represent the requested shape as pixel art.\n" +
                        "- Keep the shape centered and clearly recognizable.\n" +
                        "- Preserve the key visual traits of the requested shape (symmetry, corners, hollow areas, orientation) so it is recognizable.\n" +
                        "\n" +
                        "GAME BALANCING RULES:\n" +
                        "- targetScore must be between 1500 and 6000.\n" +
                        "- movesLimit must be between 20 and 45.\n" +
                        "- targetCandyType must be -1 unless a specific candy objective is clearly requested.\n" +
                        "- targetCandyCount must be >= 0.\n" +
                        "\n" +
                        "SELF-CHECK BEFORE OUTPUT:\n" +
                        "1. Ensure layout is square.\n" +
                        "2. Ensure all rows are equal length.\n" +
                        "3. Ensure values are only -1 or 0..5.\n" +
                        "4. Ensure JSON is valid.\n" +
                        "5. Ensure only one JSON object is returned.\n" +
                        "\n" +
                        "User request: " + promptText;

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

    private void applyGeneratedLevel(LevelGenerationResult result, String message) {
        customGridLayoutInput.setText(layoutToString(result.layout));
        gridSizeInput.setText(String.valueOf(result.gridSize));
        targetScoreInput.setText(String.valueOf(result.targetScore));
        movesLimitInput.setText(String.valueOf(result.movesLimit));
        targetCandyTypeInput.setText(String.valueOf(result.targetCandyType));
        targetCandyCountInput.setText(String.valueOf(result.targetCandyCount));
        aiPreviewText.setText(buildPreviewText(result));

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        isGenerating = false;
        generateLayoutButton.setEnabled(true);
    }
    private String buildPreviewText(LevelGenerationResult result) {
        return "AI Preview\n" +
                "Grid: " + result.gridSize + "x" + result.gridSize + " | Moves: " + result.movesLimit + " | Goal: " + result.targetScore +
                "\nTarget candy: " + result.targetCandyType + " (x" + result.targetCandyCount + ")\n\n" +
                layoutToString(result.layout);
    }

    private void applyLocalFallbackGeneration(String promptText, String reason) {
        LevelGenerationResult fallback = buildOfflineLevel(promptText);
        applyGeneratedLevel(fallback, "Generated level locally.");
        if (!TextUtils.isEmpty(reason) && !isExpectedGeminiFallback(reason)) {
            Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
        }
    }

    private LevelGenerationResult buildOfflineLevel(String promptText) {
        String normalizedPrompt = promptText == null ? "" : promptText.toLowerCase(java.util.Locale.US);
        String shape = detectRequestedShape(normalizedPrompt);
        int gridSize = chooseGridSizeForShape(shape);

        int shapeCandyType = Math.abs(promptText == null ? 0 : promptText.hashCode()) % Candy.NUMBER_OF_REGULAR_CANDY_TYPES;
        int accentCandyType = (shapeCandyType + 2) % Candy.NUMBER_OF_REGULAR_CANDY_TYPES;
        List<List<Integer>> layout = "custom".equals(shape)
                ? buildProceduralShapeLayout(gridSize, shapeCandyType, accentCandyType, normalizedPrompt)
                : createShapeLayout(shape, gridSize, shapeCandyType, accentCandyType);

        int targetScore = 3000;
        int movesLimit = 30;
        int targetCandyCount = Math.max(12, gridSize + 6);
        return new LevelGenerationResult(gridSize, targetScore, movesLimit, shapeCandyType, targetCandyCount, layout);
    }

    private String detectRequestedShape(String prompt) {
        if (containsAny(prompt, "flower", "blossom", "petal", "rose", "daisy", "tulip")) {
            return "flower";
        }
        if (containsAny(prompt, "heart", "love", "❤", "❤️", "♥")) {
            return "heart";
        }
        if (containsAny(prompt, "diamond", "rhombus")) {
            return "diamond";
        }
        if (containsAny(prompt, "circle", "round", "orb", "disk")) {
            return "circle";
        }
        if (containsAny(prompt, "ring", "donut", "doughnut", "hollow circle")) {
            return "ring";
        }
        if (containsAny(prompt, "plus", "cross", "+")) {
            return "plus";
        }
        if (containsAny(prompt, "x shape", "letter x", "diagonal cross")) {
            return "x";
        }
        if (containsAny(prompt, "star")) {
            return "star";
        }
        if (containsAny(prompt, "triangle", "pyramid")) {
            return "triangle";
        }
        if (containsAny(prompt, "rectangle", "box", "square")) {
            return "rectangle";
        }
        return "custom";
    }

    private int chooseGridSizeForShape(String shape) {
        switch (shape) {
            case "flower":
            case "star":
                return 9;
            case "heart":
                return 10;
            case "ring":
            case "custom":
                return 9;
            case "circle":
                return 8;
            default:
                return 8;
        }
    }

    private boolean containsAny(String prompt, String... tokens) {
        if (TextUtils.isEmpty(prompt)) {
            return false;
        }
        for (String token : tokens) {
            if (prompt.contains(token)) {
                return true;
            }
        }
        return false;
    }
    private List<List<Integer>> buildHeartLayoutFromMask(int gridSize, int candyType, String[] mask) {
        List<List<Integer>> layout = buildEmptyLayout(gridSize);
        int maskSize = mask.length;
        int rowOffset = Math.max(0, (gridSize - maskSize) / 2);
        int colOffset = Math.max(0, (gridSize - maskSize) / 2);
        for (int r = 0; r < maskSize && (r + rowOffset) < gridSize; r++) {
            String row = mask[r];
            for (int c = 0; c < row.length() && (c + colOffset) < gridSize; c++) {
                if (row.charAt(c) == '1') {
                    layout.get(r + rowOffset).set(c + colOffset, candyType);
                }
            }
        }
        return layout;
    }

    private LevelGenerationResult enforceRequestedShape(String promptText, LevelGenerationResult generated) {
        String normalizedPrompt = promptText == null ? "" : promptText.toLowerCase(java.util.Locale.US);
        if (!hasShapeIntent(normalizedPrompt)) {
            return generated;
        }

        if (hasShapeBackground(generated.layout)) {
            return generated;
        }

        String requestedShape = detectRequestedShape(normalizedPrompt);
        int shapeCandyType = findDominantShapeCandyType(generated.layout);
        int accentCandyType = (shapeCandyType + 2) % Candy.NUMBER_OF_REGULAR_CANDY_TYPES;
        List<List<Integer>> enforcedLayout = "custom".equals(requestedShape)
                ? buildProceduralShapeLayout(generated.gridSize, shapeCandyType, accentCandyType, normalizedPrompt)
                : createShapeLayout(requestedShape, generated.gridSize, shapeCandyType, accentCandyType);

        return new LevelGenerationResult(
                generated.gridSize,
                generated.targetScore,
                generated.movesLimit,
                generated.targetCandyType,
                generated.targetCandyCount,
                enforcedLayout
        );
    }


    private boolean hasShapeIntent(String prompt) {
        if (TextUtils.isEmpty(prompt)) {
            return false;
        }
        return containsAny(prompt, "shape", "form", "silhouette", "pattern", "draw", "look like", "in the form of");
    }

    private List<List<Integer>> buildProceduralShapeLayout(int gridSize,
                                                           int shapeCandyType,
                                                           int accentCandyType,
                                                           String seedText) {
        List<List<Integer>> layout = buildEmptyLayout(gridSize);
        int center = gridSize / 2;
        int hash = Math.abs(seedText == null ? 0 : seedText.hashCode());
        int mode = hash % 4;
        int radius = Math.max(2, gridSize / 3);

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                int dx = c - center;
                int dy = r - center;
                boolean fill;
                switch (mode) {
                    case 0:
                        fill = Math.abs(dx) + Math.abs(dy) <= radius;
                        break;
                    case 1:
                        fill = Math.abs(dx) <= radius - Math.abs(dy) / 2 && Math.abs(dy) <= radius;
                        break;
                    case 2:
                        fill = dx * dx + dy * dy <= radius * radius;
                        break;
                    default:
                        fill = (Math.abs(dx) <= 1 || Math.abs(dy) <= 1 || Math.abs(dx) == Math.abs(dy))
                                && Math.abs(dx) <= radius && Math.abs(dy) <= radius;
                        break;
                }
                if (fill) {
                    layout.get(r).set(c, ((r + c + hash) % 3 == 0) ? accentCandyType : shapeCandyType);
                }
            }
        }
        return layout;
    }

    private boolean hasShapeBackground(List<List<Integer>> layout) {
        for (List<Integer> row : layout) {
            for (Integer value : row) {
                if (value != null && value == -1) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<List<Integer>> createShapeLayout(String shape,
                                                  int gridSize,
                                                  int shapeCandyType,
                                                  int accentCandyType) {
        switch (shape) {
            case "flower":
                return buildFlowerLayout(gridSize, shapeCandyType, accentCandyType);
            case "heart":
                return buildHeartLayout(gridSize, shapeCandyType);
            case "diamond":
                return buildDiamondLayout(gridSize, shapeCandyType);
            case "circle":
                return buildCircleLayout(gridSize, shapeCandyType, false);
            case "ring":
                return buildCircleLayout(gridSize, shapeCandyType, true);
            case "plus":
                return buildPlusLayout(gridSize, shapeCandyType);
            case "x":
                return buildXLayout(gridSize, shapeCandyType);
            case "star":
                return buildStarLayout(gridSize, shapeCandyType, accentCandyType);
            case "triangle":
                return buildTriangleLayout(gridSize, shapeCandyType);
            default:
                return buildRectangleLayout(gridSize, shapeCandyType);
        }
    }

    private List<List<Integer>> buildRectangleLayout(int gridSize, int candyType) {
        List<List<Integer>> layout = buildEmptyLayout(gridSize);
        for (int r = 1; r < gridSize - 1; r++) {
            for (int c = 1; c < gridSize - 1; c++) {
                layout.get(r).set(c, candyType);
            }
        }
        return layout;
    }

    private List<List<Integer>> buildFlowerLayout(int gridSize, int petalCandyType, int centerCandyType) {
        List<List<Integer>> layout = buildEmptyLayout(gridSize);
        int center = gridSize / 2;
        int centerRadiusSquared = 2;
        int petalRadiusSquared = 5;

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                int dx = c - center;
                int dy = r - center;

                int centerDistanceSquared = dx * dx + dy * dy;
                boolean centerDisk = centerDistanceSquared <= centerRadiusSquared;

                boolean topPetal = (dx * dx + (dy + 2) * (dy + 2)) <= petalRadiusSquared;
                boolean bottomPetal = (dx * dx + (dy - 2) * (dy - 2)) <= petalRadiusSquared;
                boolean leftPetal = ((dx + 2) * (dx + 2) + dy * dy) <= petalRadiusSquared;
                boolean rightPetal = ((dx - 2) * (dx - 2) + dy * dy) <= petalRadiusSquared;

                boolean diagonalTopLeft = ((dx + 2) * (dx + 2) + (dy + 2) * (dy + 2)) <= petalRadiusSquared;
                boolean diagonalTopRight = ((dx - 2) * (dx - 2) + (dy + 2) * (dy + 2)) <= petalRadiusSquared;
                boolean diagonalBottomLeft = ((dx + 2) * (dx + 2) + (dy - 2) * (dy - 2)) <= petalRadiusSquared;
                boolean diagonalBottomRight = ((dx - 2) * (dx - 2) + (dy - 2) * (dy - 2)) <= petalRadiusSquared;

                boolean petal = topPetal || bottomPetal || leftPetal || rightPetal
                        || diagonalTopLeft || diagonalTopRight || diagonalBottomLeft || diagonalBottomRight;

                if (centerDisk) {
                    layout.get(r).set(c, centerCandyType);
                } else if (petal) {
                    layout.get(r).set(c, petalCandyType);
                }
            }
        }

        return layout;
    }

    private List<List<Integer>> buildHeartLayout(int gridSize, int candyType) {
        if (gridSize >= 10) {
            return buildHeartLayoutFromMask(gridSize, candyType, new String[]{
                    "0011001100",
                    "0111111110",
                    "1111111111",
                    "1111111111",
                    "0111111110",
                    "0011111100",
                    "0001111000",
                    "0000110000",
                    "0000100000",
                    "0000000000"
            });
        }
        List<List<Integer>> layout = buildEmptyLayout(gridSize);
        double scale = 2.2 / (gridSize - 1);
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                double x = (c - (gridSize - 1) / 2.0) * scale;
                double y = ((gridSize - 1) / 2.0 - r) * scale;
                double eq = Math.pow(x * x + y * y - 1, 3) - x * x * y * y * y;
                if (eq <= 0 && y > -1.3) {
                    layout.get(r).set(c, candyType);
                }
            }
        }
        return layout;
    }

    private List<List<Integer>> buildDiamondLayout(int gridSize, int candyType) {
        List<List<Integer>> layout = buildEmptyLayout(gridSize);
        int center = gridSize / 2;
        int radius = gridSize / 2;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (Math.abs(r - center) + Math.abs(c - center) <= radius - 1) {
                    layout.get(r).set(c, candyType);
                }
            }
        }
        return layout;
    }

    private List<List<Integer>> buildCircleLayout(int gridSize, int candyType, boolean hollow) {
        List<List<Integer>> layout = buildEmptyLayout(gridSize);
        int center = gridSize / 2;
        int outer = (gridSize / 2) - 1;
        int inner = Math.max(1, outer - 1);
        int outerSq = outer * outer;
        int innerSq = inner * inner;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                int dx = c - center;
                int dy = r - center;
                int d = dx * dx + dy * dy;
                if (d <= outerSq && (!hollow || d >= innerSq)) {
                    layout.get(r).set(c, candyType);
                }
            }
        }
        return layout;
    }

    private List<List<Integer>> buildPlusLayout(int gridSize, int candyType) {
        List<List<Integer>> layout = buildEmptyLayout(gridSize);
        int center = gridSize / 2;
        int half = gridSize / 3;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                boolean vertical = c >= center - 1 && c <= center + 1 && r >= center - half && r <= center + half;
                boolean horizontal = r >= center - 1 && r <= center + 1 && c >= center - half && c <= center + half;
                if (vertical || horizontal) {
                    layout.get(r).set(c, candyType);
                }
            }
        }
        return layout;
    }

    private List<List<Integer>> buildXLayout(int gridSize, int candyType) {
        List<List<Integer>> layout = buildEmptyLayout(gridSize);
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (Math.abs(r - c) <= 1 || Math.abs((r + c) - (gridSize - 1)) <= 1) {
                    layout.get(r).set(c, candyType);
                }
            }
        }
        return layout;
    }

    private List<List<Integer>> buildTriangleLayout(int gridSize, int candyType) {
        List<List<Integer>> layout = buildEmptyLayout(gridSize);
        int center = gridSize / 2;
        for (int r = 1; r < gridSize - 1; r++) {
            int span = r;
            for (int c = center - span; c <= center + span; c++) {
                if (c >= 1 && c < gridSize - 1) {
                    layout.get(r).set(c, candyType);
                }
            }
        }
        return layout;
    }

    private List<List<Integer>> buildStarLayout(int gridSize, int candyType, int accentCandyType) {
        List<List<Integer>> layout = buildPlusLayout(gridSize, candyType);
        int center = gridSize / 2;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (Math.abs(r - c) <= 1 || Math.abs((r + c) - (gridSize - 1)) <= 1) {
                    layout.get(r).set(c, accentCandyType);
                }
                if (Math.abs(r - center) <= 1 || Math.abs(c - center) <= 1) {
                    if (layout.get(r).get(c) != -1) {
                        layout.get(r).set(c, candyType);
                    }
                }
            }
        }
        return layout;
    }

    private List<List<Integer>> buildEmptyLayout(int gridSize) {
        List<List<Integer>> layout = new ArrayList<>();
        for (int r = 0; r < gridSize; r++) {
            List<Integer> row = new ArrayList<>();
            for (int c = 0; c < gridSize; c++) {
                row.add(-1);
            }
            layout.add(row);
        }
        return layout;
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
