package com.example.travellog.gamecore; // Make sure this package is correct

import android.util.Log; // <<-- CORRECT IMPORT FOR LOGGING

import java.util.HashMap;
import java.util.Map;

public class LevelConfig {    private int levelNumber;
    private int rows;
    private int cols;
    private int targetScore;
    private int[][] customGridLayout;

    private static final Map<Integer, LevelConfig> levelConfigsMap = new HashMap<>();
    private static int maxDefinedLevel = 0;

    // Constructor for levels with a custom grid layout AND a target score
    public LevelConfig(int levelNumber, int targetScore, int[][] customGridLayout) {
        this.levelNumber = levelNumber;
        this.rows = customGridLayout.length;
        this.cols = customGridLayout.length > 0 ? customGridLayout[0].length : 0;
        this.targetScore = targetScore;
        this.customGridLayout = customGridLayout;
    }

    // Constructor for levels with specified rows/cols (random generation) AND a target score
    public LevelConfig(int levelNumber, int rows, int cols, int targetScore) {
        this.levelNumber = levelNumber;
        this.rows = rows;
        this.cols = cols;
        this.targetScore = targetScore;
        this.customGridLayout = null;
    }

    // Getters
    public int getLevelNumber() {
        return levelNumber;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public int[][] getCustomGridLayout() {
        return customGridLayout;
    }

    // Static initializer block to define and add levels
    static {
        // --- DEFINE YOUR LEVELS HERE and ADD THEM to the map ---

        // Level 1: Custom Layout, Target Score 100
        addLevelConfig(new LevelConfig(1, 100, new int[][]{
                {0, 1, 2}, // Row 0
                {1, 2, 0}, // Row 1
                {0, 1, 2}  // Row 2
        })); // <<-- ADDED addLevelConfig(...) and semicolon

        // Level 2: Random 5x4 grid (uses the other constructor), Target Score 150
        addLevelConfig(new LevelConfig(2, 5, 4, 150)); // <<-- ADDED addLevelConfig(...) and semicolon

        // Level 3: Custom 3x3 layout, Target Score 200
        addLevelConfig(new LevelConfig(3, 200, new int[][]{
                {5, 4, 0},
                {1, 2, 3},
                {3, 0, 5}
        })); // <<-- ADDED addLevelConfig(...) and semicolon

        // Level 4: Random 6x5 grid, Target Score 1000
        addLevelConfig(new LevelConfig(4, 6, 5, 1000)); // <<-- ADDED addLevelConfig(...) and semicolon

        // Level 5: Custom 5x5 layout (example), Target Score 300
        // NOTE: Your original layout for level 5 had 7 rows defined for a 5x5 concept.
        // I've adjusted it to be truly 5 rows. Adjust as needed.
        addLevelConfig(new LevelConfig(5, 300, new int[][]{
                {0,0,1,1,0},
                {0,2,3,2,0},
                {1,3,4,3,1},
                {0,2,3,2,0},
                {0,0,1,1,0}
                // Removed extra rows:
                // {0,0,1,1,0},
                // {0,0,1,1,0}
        })); // <<-- ADDED addLevelConfig(...) and semicolon

        // Level 6: Random 7x6 grid, Target Score 4000
        addLevelConfig(new LevelConfig(6, 7, 6, 4000)); // <<-- ADDED addLevelConfig(...) and semicolon

        // Level 7: Random 7x7 grid, Target Score 7000
        addLevelConfig(new LevelConfig(7, 7, 7, 7000)); // <<-- ADDED addLevelConfig(...) and semicolon

        // Level 8: Random 8x7 grid, Target Score 8000
        addLevelConfig(new LevelConfig(8, 8, 7, 8000)); // <<-- ADDED addLevelConfig(...) and semicolon

        // Level 9: Random 8x8 grid, Target Score 9000
        addLevelConfig(new LevelConfig(9, 8, 8, 9000)); // Corrected rows/cols to 8x8 as per description // <<-- ADDED addLevelConfig(...) and semicolon

        // Level 10: Custom 6x6 layout, Target Score 550
        addLevelConfig(new LevelConfig(10, 550, new int[][]{
                {0,1,2,3,4,5},
                {5,0,1,2,3,4},
                {4,5,0,1,2,3},
                {3,4,5,0,1,2},
                {2,3,4,5,0,1},
                {1,2,3,4,5,0}
        })); // <<-- ADDED addLevelConfig(...) and semicolon
    }

    private static void addLevelConfig(LevelConfig config) {
        if (config == null) {
            Log.e("LevelConfig", "Attempted to add a null config.");
            return;
        }
        if (levelConfigsMap.containsKey(config.getLevelNumber())) {
            Log.w("LevelConfig", "Overwriting configuration for level: " + config.getLevelNumber());
        }
        levelConfigsMap.put(config.getLevelNumber(), config);
        if (config.getLevelNumber() > maxDefinedLevel) {
            maxDefinedLevel = config.getLevelNumber();
        }
    }

    public static LevelConfig getConfigForLevel(int levelNumber) {
        if (levelConfigsMap.containsKey(levelNumber)) {
            return levelConfigsMap.get(levelNumber);
        }
        // Use standard Android Log
        Log.e("LevelConfig", "Configuration for level " + levelNumber + " not found!");
        return null;
    }

    public static int getMaxLevels() {
        return maxDefinedLevel;
    }
}
