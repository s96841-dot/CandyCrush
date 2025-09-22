package com.example.travellog.gamecore;

import android.util.Log;

public class LevelConfig {

    private static final LevelConfig[] configs = {
            // Existing levels
            new LevelConfig(1, 3, 4, 100), // Level 1
            new LevelConfig(2, 4, 4, 150), // Level 2
            new LevelConfig(3, 4, 5, 200), // Level 3

            // --- ADD NEW LEVELS HERE ---
            new LevelConfig(4, 5, 5, 250), // Level 4
            new LevelConfig(5, 5, 6, 300), // Level 5
            new LevelConfig(6, 6, 6, 350), // Level 6
            new LevelConfig(7, 6, 7, 400), // Level 7
            new LevelConfig(8, 7, 7, 450), // Level 8
            new LevelConfig(9, 7, 8, 500), // Level 9
            new LevelConfig(10, 8, 8, 550) // Level 10
            // You can continue adding more levels if needed
    };

    // Instance variables (levelNumber, rows, cols, targetScore)
    private int levelNumber;
    private int rows;
    private int cols;
    private int targetScore;

    // Constructor
    public LevelConfig(int levelNumber, int rows, int cols, int targetScore) {
        this.levelNumber = levelNumber;
        this.rows = rows;
        this.cols = cols;
        this.targetScore = targetScore;
    }

    // Getter methods
    public int getLevelNumber() { return levelNumber; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTargetScore() { return targetScore; }

    // Static method to get a specific level's configuration
    public static LevelConfig getConfigForLevel(int levelNum) {
        if (levelNum > 0 && levelNum <= configs.length) {
            return configs[levelNum - 1]; // levelNum is 1-based, array is 0-based
        }
        Log.w("LevelConfig", "Requested level " + levelNum + " is out of bounds. Returning default (level 1) config.");
        if (configs.length > 0) {
            return configs[0]; // Default to level 1 if out of bounds
        }
        Log.e("LevelConfig", "No level configurations defined!");
        return null; // Should not happen if configs is initialized
    }

    // Static method to get the maximum number of defined levels
    public static int getMaxLevels() {
        if (configs == null) {
            return 0;
        }
        return configs.length; // This will now return 10 (or however many you define)
    }
}

