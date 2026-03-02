package com.example.CandyCrush;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CandyCrush.gamecore.LevelConfig;
import com.example.CandyCrush.gameui.GameGridView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_LEVEL_NUMBER = "extra_level_number";
    public static final String EXTRA_GRID_SIZE = "extra_grid_size";
    public static final String EXTRA_TARGET_SCORE = "extra_target_score";
    public static final String EXTRA_MOVES_LIMIT = "extra_moves_limit";
    public static final String EXTRA_TARGET_CANDY_TYPE = "extra_target_candy_type";
    public static final String EXTRA_TARGET_CANDY_COUNT = "extra_target_candy_count";
    public static final String EXTRA_LAYOUT = "extra_layout";

    private GameGridView previewGrid;
    private TextView summaryText;
    private Button saveButton;

    private int levelNumber;
    private int gridSize;
    private int targetScore;
    private int movesLimit;
    private int targetCandyType;
    private int targetCandyCount;
    private List<List<Integer>> layout;

    private boolean isSaving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_preview);

        previewGrid = findViewById(R.id.preview_grid);
        summaryText = findViewById(R.id.preview_summary);
        saveButton = findViewById(R.id.btn_preview_save);
        Button backButton = findViewById(R.id.btn_preview_back);

        readExtras();
        renderPreview();

        saveButton.setOnClickListener(v -> saveLevel());
        backButton.setOnClickListener(v -> finish());
    }

    private void readExtras() {
        levelNumber = getIntent().getIntExtra(EXTRA_LEVEL_NUMBER, 1);
        gridSize = getIntent().getIntExtra(EXTRA_GRID_SIZE, 8);
        targetScore = getIntent().getIntExtra(EXTRA_TARGET_SCORE, 2500);
        movesLimit = getIntent().getIntExtra(EXTRA_MOVES_LIMIT, 30);
        targetCandyType = getIntent().getIntExtra(EXTRA_TARGET_CANDY_TYPE, -1);
        targetCandyCount = getIntent().getIntExtra(EXTRA_TARGET_CANDY_COUNT, 0);

        String rawLayout = getIntent().getStringExtra(EXTRA_LAYOUT);
        Type layoutType = new TypeToken<List<List<Integer>>>() {}.getType();
        layout = TextUtils.isEmpty(rawLayout) ? null : new Gson().fromJson(rawLayout, layoutType);
    }

    private void renderPreview() {
        int[][] customGrid = toGrid(layout);
        int rows = customGrid == null ? gridSize : customGrid.length;
        int cols = customGrid == null ? gridSize : customGrid[0].length;

        summaryText.setText(
                "Level " + levelNumber + "\n" +
                        "Grid: " + rows + "x" + cols + "\n" +
                        "Goal: " + targetScore + "\n" +
                        "Moves: " + movesLimit + "\n" +
                        "Target Candy: " + targetCandyType + " x" + targetCandyCount
        );

        LevelConfig config = new LevelConfig(
                levelNumber,
                rows,
                cols,
                targetScore,
                movesLimit,
                targetCandyType,
                targetCandyCount,
                customGrid
        );
        previewGrid.setupGridFromRemote(config);
        previewGrid.setOnTouchListener((v, event) -> true);
    }

    private int[][] toGrid(List<List<Integer>> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        int[][] grid = new int[source.size()][];
        for (int r = 0; r < source.size(); r++) {
            List<Integer> row = source.get(r);
            grid[r] = new int[row.size()];
            for (int c = 0; c < row.size(); c++) {
                grid[r][c] = row.get(c);
            }
        }
        return grid;
    }

    private void saveLevel() {
        if (isSaving) {
            return;
        }

        isSaving = true;
        saveButton.setEnabled(false);

        Map<String, Object> data = new HashMap<>();
        data.put("targetScore", targetScore);
        data.put("movesLimit", movesLimit);
        data.put("gridSize", gridSize);
        data.put("targetCandyType", targetCandyType);
        data.put("targetCandyCount", targetCandyCount);
        if (layout != null) {
            data.put("customGridLayout", CreateLevelActivity.toRowMap(layout));
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            data.put("createdBy", currentUser.getUid());
            data.put("creatorName", currentUser.getDisplayName() == null ? "Player" : currentUser.getDisplayName());
            data.put("isUserGenerated", true);
        }

        FirebaseFirestore.getInstance().collection("levels").document(String.valueOf(levelNumber))
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    isSaving = false;
                    saveButton.setEnabled(true);
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    isSaving = false;
                    saveButton.setEnabled(true);
                    Toast.makeText(this, "Failed to save level: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Level saved")
                .setMessage("Your level was saved successfully.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}