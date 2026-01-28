package com.example.CandyCrush.gamecore; // Make sure this package is correct

import android.util.Log; // <<-- CORRECT IMPORT FOR LOGGING

import java.util.HashMap;
import java.util.Map;

public class LevelConfig {
    private int levelNumber;
    private int rows;
    private int cols;
    private int targetScore;
    private int[][] customGridLayout;
    private int maxMoves; // כמות מהלכים מקסימלית
    private int targetCandyType = -1; // סוג סוכריה לאיסוף (-1 אם אין)
    private int targetCandyCount = 0; // כמה צריך לאסוף

    private static final Map<Integer, LevelConfig> levelConfigsMap = new HashMap<>();
    private static int maxDefinedLevel = 0;

    // Constructor for levels with a custom grid layout AND a target score
    public LevelConfig(int levelNumber, int targetScore, int[][] customGridLayout) {
        this.levelNumber = levelNumber;
        this.rows = customGridLayout.length;
        this.cols = customGridLayout.length > 0 ? customGridLayout[0].length : 0;
        this.targetScore = targetScore;
        this.customGridLayout = customGridLayout;
        this.maxMoves = 0;
        this.targetCandyType = -1;
        this.targetCandyCount = 0;
    }

    // Constructor for levels with specified rows/cols (random generation) AND a target score
    public LevelConfig(int levelNumber, int rows, int cols, int targetScore) {
        this.levelNumber = levelNumber;
        this.rows = rows;
        this.cols = cols;
        this.targetScore = targetScore;
        this.customGridLayout = null;
        this.maxMoves = 0;
        this.targetCandyType = -1;
        this.targetCandyCount = 0;
    }

    public LevelConfig(int levelNumber,
                       int rows,
                       int cols,
                       int targetScore,
                       int maxMoves,
                       int targetCandyType,
                       int targetCandyCount,
                       int[][] customGridLayout) {
        this.levelNumber = levelNumber;
        this.rows = rows;
        this.cols = cols;
        this.targetScore = targetScore;
        this.maxMoves = maxMoves;
        this.targetCandyType = targetCandyType;
        this.targetCandyCount = targetCandyCount;
        this.customGridLayout = customGridLayout;
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

    public int getMaxMoves() {
        return maxMoves;
    }

    public int getTargetCandyType() {
        return targetCandyType;
    }

    public int getTargetCandyCount() {
        return targetCandyCount;
    }

    public int[][] getCustomGridLayout() {
        return customGridLayout;
    }

    // Local levels removed; levels are expected to come from Firestore.

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
