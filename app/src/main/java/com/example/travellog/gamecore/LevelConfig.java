package com.example.travellog.gamecore;

import android.util.Log;

public class LevelConfig {

    private int levelNumber;
    private int rows;
    private int cols;
    private int targetScore;
    private int[][] customGridLayout; // Stores the predefined layout of candy types for custom levels

    /**
     * Constructor for levels WITH a custom, predefined candy layout.
     * Rows and cols are derived from the layout dimensions.
     * @param levelNumber The number of the level.
     * @param targetScore The score required to pass the level.
     * @param customGridLayout A 2D array defining the type of candy for each cell.
     */
    public LevelConfig(int levelNumber, int targetScore, int[][] customGridLayout) {
        this.levelNumber = levelNumber;
        this.targetScore = targetScore;
        this.customGridLayout = customGridLayout;

        if (customGridLayout != null && customGridLayout.length > 0 && customGridLayout[0].length > 0) {
            this.rows = customGridLayout.length;
            this.cols = customGridLayout[0].length;
        } else {
            Log.e("LevelConfig", "Custom grid layout for level " + levelNumber + " is null or empty. Defaulting to 3x3.");
            // This is an error in level definition if this constructor is used.
            this.rows = 3; // Fallback
            this.cols = 3; // Fallback
            // Consider throwing an IllegalArgumentException here if a custom layout is expected but invalid.
        }
    }

    /**
     * Constructor for levels that will be RANDOMLY generated.
     * For these levels, customGridLayout will be null.
     * @param levelNumber The number of the level.
     * @param rows The number of rows for the random grid.
     * @param cols The number of columns for the random grid.
     * @param targetScore The score required to pass the level.
     */
    public LevelConfig(int levelNumber, int rows, int cols, int targetScore) {
        this.levelNumber = levelNumber;
        this.rows = rows;
        this.cols = cols;
        this.targetScore = targetScore;
        this.customGridLayout = null; // No custom layout, indicates random generation needed
    }

    // --- Static array defining all level configurations ---
    // Make sure the candy type integers used in customGridLayouts (e.g., 0, 1, 2, 3, 4, 5)
    // are valid and correspond to loaded bitmaps in GameGridView (i.e., less than NUMBER_OF_CANDY_TYPES).
    private static final LevelConfig[] configs = {
            // Level 1: Custom 4x4 layout
            new LevelConfig(1, 100, new int[][]{
                    {0, 1, 2}, // Row 0
                    {1, 2, 0}, // Row 1
                    {0, 1, 2}, // Row 2
            }),

            // Level 2: Random 5x4 grid (uses the other constructor)
            new LevelConfig(2, 5, 4, 150),

            // Level 3: Custom 3x3 layout
            new LevelConfig(3, 200, new int[][]{
                    {5, 4, 0},
                    {1, 2, 3},
                    {3, 0, 5}
            }),

            // Level 4: Random 6x5 grid
            new LevelConfig(4, 6, 5, 1000),

            // Level 5: Custom 5x5 layout (example)
            new LevelConfig(5, 300, new int[][]{
                    {0,0,1,1,0},
                    {0,2,3,2,0},
                    {1,3,4,3,1},
                    {0,2,3,2,0},
                    {0,0,1,1,0},
                    {0,0,1,1,0},
                    {0,0,1,1,0}

            }),

            // Level 6: Random 7x6 grid
            new LevelConfig(6, 7, 6, 4000),

            // Level 7: Random 7x7 grid
            new LevelConfig(7, 7, 7, 7000),

            // Level 8: Random 8x7 grid
            new LevelConfig(8, 8, 7, 8000),

            // Level 9: Random 8x8 grid
            new LevelConfig(9, 9, 9, 9000),

            // Level 10: Custom 6x6 layout (example)
            // You can make this larger or smaller, just an example.
            // Using types 0-5 assuming NUMBER_OF_CANDY_TYPES in GameGridView is at least 6.
            new LevelConfig(10, 550, new int[][]{
                    {0,1,2,3,4,5},
                    {5,0,1,2,3,4},
                    {4,5,0,1,2,3},
                    {3,4,5,0,1,2},
                    {2,3,4,5,0,1},
                    {1,2,3,4,5,0}
            })
    };

    // --- Getter methods ---
    public int getLevelNumber() { return levelNumber; }
    public int getRows() { return rows; } // This will be from customGridLayout if provided, else from constructor
    public int getCols() { return cols; } // This will be from customGridLayout if provided, else from constructor
    public int getTargetScore() { return targetScore; }

    /**
     * Returns the custom grid layout for this level.
     * @return A 2D int array representing the candy types for each cell,
     *         or null if this level is intended for random generation.
     */
    public int[][] getCustomGridLayout() { return customGridLayout; }

    // --- Static methods ---
    public static LevelConfig getConfigForLevel(int levelNum) {
        if (levelNum > 0 && levelNum <= configs.length) {
            return configs[levelNum - 1]; // levelNum is 1-based, array is 0-based
        }
        Log.w("LevelConfig", "Requested level " + levelNum + " is out of bounds. Returning default (level 1) config or null if no configs exist.");
        if (configs != null && configs.length > 0) {
            return configs[0]; // Default to level 1 if out of bounds and configs exist
        }
        Log.e("LevelConfig", "No level configurations defined at all! Returning null.");
        return null;
    }

    public static int getMaxLevels() {
        return (configs == null) ? 0 : configs.length;
    }
}
