package com.example.travellog.gameui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.travellog.R;
import com.example.travellog.gamecore.Candy;
import com.example.travellog.gamecore.LevelConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameGridView extends View {
    private static final String TAG = "GameGridView";

    private Paint paint;
    private Paint backgroundPaint;

    private int gridRows = 0;
    private int gridCols = 0;
    private List<List<Candy>> candies;
    private Random random = new Random();

    private int cellSize = 0;
    private int gridOffsetX = 0;
    private int gridOffsetY = 0;

    private Bitmap[] candyBitmaps;
    // --- CRITICAL ---
    // This value MUST match the number of candy image files you have.
    // The files in res/drawable should be named like:
    // candy_type_0.jpeg (or .png, .webp, etc.)
    // candy_type_1.jpeg
    // ...
    // The loop in initBitmaps will try to load 'candy_type_0', 'candy_type_1', etc.,
    // and the system will append the correct extension.
    private static final int NUMBER_OF_CANDY_TYPES = 6; // <<< ENSURE THIS IS CORRECT!

    private LevelConfig currentLevelConfig;
    private boolean allBitmapsLoadedSuccessfully = false;

    public GameGridView(Context context) {
        super(context);
        init(context, null);
    }

    public GameGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GameGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#E0E0E0"));

        candies = new ArrayList<>();
        Log.i(TAG, "Initializing GameGridView. Expecting NUMBER_OF_CANDY_TYPES: " + NUMBER_OF_CANDY_TYPES);

        if (NUMBER_OF_CANDY_TYPES <= 0) {
            Log.e(TAG, "CRITICAL ERROR: NUMBER_OF_CANDY_TYPES is " + NUMBER_OF_CANDY_TYPES + ". No bitmaps will be loaded. Grid will be PINK.");
            allBitmapsLoadedSuccessfully = false;
        } else {
            initBitmaps(context);
        }
    }

    private void initBitmaps(Context context) {
        Log.i(TAG, "initBitmaps: Starting to load " + NUMBER_OF_CANDY_TYPES + " candy bitmaps.");
        candyBitmaps = new Bitmap[NUMBER_OF_CANDY_TYPES];
        int successfullyLoadedCount = 0;
        String resourceNameBase = "candy_type_"; // System looks for 'candy_type_0', 'candy_type_1', etc. (extension is handled by system)

        for (int i = 0; i < NUMBER_OF_CANDY_TYPES; i++) {
            String targetResourceNameWithoutExtension = resourceNameBase + i;
            Log.d(TAG, "initBitmaps: Attempting to find drawable resource named '" + targetResourceNameWithoutExtension + "'.");

            int resourceId = 0;
            try {
                resourceId = context.getResources().getIdentifier(targetResourceNameWithoutExtension, "drawable", context.getPackageName());
            } catch (Exception e) {
                Log.e(TAG, "initBitmaps: EXCEPTION while getting resource ID for '" + targetResourceNameWithoutExtension + "'. Package name: " + context.getPackageName(), e);
                continue;
            }

            if (resourceId != 0) {
                Log.d(TAG, "initBitmaps: Found resource ID " + resourceId + " for '" + targetResourceNameWithoutExtension + "'. Attempting to decode.");
                try {
                    Bitmap decodedBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
                    if (decodedBitmap != null) {
                        candyBitmaps[i] = decodedBitmap;
                        successfullyLoadedCount++;
                        Log.i(TAG, "initBitmaps: Successfully loaded and decoded resource for '" + targetResourceNameWithoutExtension + "' (ID: " + resourceId + ").");
                    } else {
                        Log.e(TAG, "initBitmaps: FAILED TO DECODE resource for '" + targetResourceNameWithoutExtension + "' (ID: " + resourceId + "). " +
                                "Ensure the image file (e.g., .jpeg, .png) is valid and not corrupted.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "initBitmaps: EXCEPTION while decoding resource ID " + resourceId + " ('" + targetResourceNameWithoutExtension + "').", e);
                }
            } else {
                Log.e(TAG, "initBitmaps: DRAWABLE RESOURCE NAMED '" + targetResourceNameWithoutExtension + "' NOT FOUND. " +
                        "Ensure a file like '" + targetResourceNameWithoutExtension + ".jpeg' or '" + targetResourceNameWithoutExtension + ".png' " +
                        "(exact lowercase name) exists in your 'res/drawable' folder and is a valid image type.");
            }
        }

        Log.i(TAG, "initBitmaps: Finished. Successfully loaded " + successfullyLoadedCount + " out of " + NUMBER_OF_CANDY_TYPES + " expected candy bitmaps.");
        if (successfullyLoadedCount == NUMBER_OF_CANDY_TYPES) {
            allBitmapsLoadedSuccessfully = true;
            Log.i(TAG, "initBitmaps: All candy bitmaps loaded successfully! Game should display correctly.");
        } else {
            allBitmapsLoadedSuccessfully = false;
            Log.e(TAG, "initBitmaps: CRITICAL FAILURE - NOT ALL BITMAPS LOADED. " +
                    (NUMBER_OF_CANDY_TYPES - successfullyLoadedCount) + " bitmap(s) are missing or failed to load. " +
                    "The grid will likely show pink placeholders. Review logs above for specific file errors.");
        }
    }

    public void setupGridForLevel(int levelNumber) {
        Log.d(TAG, "setupGridForLevel called for level: " + levelNumber);
        currentLevelConfig = LevelConfig.getConfigForLevel(levelNumber);

        if (currentLevelConfig == null) {
            Log.e(TAG, "Failed to get LevelConfig for level: " + levelNumber + ". Using fallback 3x3 random grid.");
            this.gridRows = 3;
            this.gridCols = 3;
            this.currentLevelConfig = new LevelConfig(levelNumber, 3, 3, 0); // Create a dummy config
        } else {
            this.gridRows = currentLevelConfig.getRows();
            this.gridCols = currentLevelConfig.getCols();
            Log.d(TAG, "Level " + levelNumber + " configured. Rows: " + gridRows + ", Cols: " + gridCols +
                    ", HasCustomLayout: " + (currentLevelConfig.getCustomGridLayout() != null));
        }

        if (gridRows <= 0 || gridCols <= 0) {
            Log.w(TAG, "Invalid grid dimensions from LevelConfig: " + gridRows + "x" + gridCols + ". Defaulting to 1x1.");
            this.gridRows = 1;
            this.gridCols = 1;
        }

        initCandies();
        requestLayout();
        invalidate();
    }

    private void initCandies() {
        if (!allBitmapsLoadedSuccessfully) {
            Log.e(TAG, "initCandies: ABORTING candy initialization because not all bitmaps were loaded. Grid will be pink or empty.");
            candies = new ArrayList<>(); // Ensure candies list is empty to prevent drawing attempts later
            return;
        }

        if (currentLevelConfig == null || gridRows <= 0 || gridCols <= 0) {
            candies = new ArrayList<>();
            Log.w(TAG, "initCandies: currentLevelConfig is null or invalid grid dimensions (" + gridRows + "x" + gridCols + "). Candies list cleared.");
            return;
        }

        candies = new ArrayList<>();
        int[][] layout = currentLevelConfig.getCustomGridLayout();

        if (layout != null) {
            Log.d(TAG, "Initializing candies using custom layout for level " + currentLevelConfig.getLevelNumber());
            if (layout.length != gridRows || (layout.length > 0 && layout[0].length != gridCols)) {
                Log.w(TAG, "Custom layout dimensions mismatch. Layout: " + layout.length + "x" + (layout.length > 0 ? layout[0].length : 0) +
                        ", Config: " + gridRows + "x" + gridCols + ". Using layout dimensions for safety.");
                // Best effort: adjust gridRows/gridCols to match layout, but this indicates a config error.
                gridRows = layout.length;
                if (gridRows > 0) {
                    gridCols = layout[0].length; // Assuming all rows in custom layout have same length as first
                    // More robust: find max column length or ensure all rows have same length
                    if (gridCols == 0) { // If first row was empty, try to find a valid column count from other rows
                        for(int[] row : layout) { if (row != null && row.length > 0) { gridCols = row.length; break;} }
                    }
                } else {
                    gridCols = 0;
                }
            }

            for (int i = 0; i < gridRows; i++) {
                List<Candy> rowList = new ArrayList<>();
                if (i >= layout.length || layout[i] == null || layout[i].length != gridCols ) {
                    Log.e(TAG, "Custom layout row " + i + " is problematic (null, or wrong column count: " +
                            (layout.length > i && layout[i]!=null ? layout[i].length : "N/A") + " vs expected " + gridCols +
                            "). Filling with default type 0 for this row.");
                    for (int k = 0; k < gridCols; k++) { // Use the (potentially adjusted) gridCols
                        rowList.add(new Candy(0)); // Fill with default
                    }
                } else {
                    for (int j = 0; j < gridCols; j++) { // Use the (potentially adjusted) gridCols
                        int candyType = layout[i][j];
                        if (candyType < 0 || candyType >= NUMBER_OF_CANDY_TYPES) {
                            Log.w(TAG, "Invalid candy type (" + candyType + ") in custom layout at [" + i + "][" + j + "]. Max valid type is " + (NUMBER_OF_CANDY_TYPES-1) + ". Using type 0.");
                            candyType = 0;
                        }
                        rowList.add(new Candy(candyType));
                    }
                }
                candies.add(rowList);
            }
        } else {
            Log.d(TAG, "Initializing candies randomly for level " + currentLevelConfig.getLevelNumber());
            if (NUMBER_OF_CANDY_TYPES == 0) { // Should have been caught by allBitmapsLoadedSuccessfully check
                Log.e(TAG, "Cannot generate random candies, NUMBER_OF_CANDY_TYPES is 0 (this shouldn't happen if bitmaps loaded).");
                return;
            }
            for (int i = 0; i < gridRows; i++) {
                List<Candy> rowList = new ArrayList<>();
                for (int j = 0; j < gridCols; j++) {
                    int randomType = random.nextInt(NUMBER_OF_CANDY_TYPES);
                    rowList.add(new Candy(randomType));
                }
                candies.add(rowList);
            }
        }

        if (candies.isEmpty() && gridRows > 0 && gridCols > 0) {
            Log.w(TAG, "Candies list is empty after initialization, but grid dimensions are valid. This might indicate an issue in custom layout processing.");
        } else if (!candies.isEmpty() && candies.get(0) != null && !candies.get(0).isEmpty() && candies.get(0).get(0) != null) { // Added null check for candies.get(0)
            Log.d(TAG, "Candies initialized. Grid: " + gridRows + "x" + gridCols + ". First candy type (example): " + candies.get(0).get(0).getType());
        } else {
            Log.d(TAG, "Candies list might be empty or first element is null/empty after init. Grid: " + gridRows + "x" + gridCols);
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (gridCols <= 0 || gridRows <= 0) { // Check for non-positive dimensions
            setMeasuredDimension(parentWidth, parentHeight); // Or some default minimum
            Log.w(TAG, "onMeasure: gridCols or gridRows is zero or negative. Using parent dimensions.");
            return;
        }

        int proposedCellSizeByWidth = parentWidth / gridCols;
        int proposedCellSizeByHeight = parentHeight / gridRows;
        cellSize = Math.min(proposedCellSizeByWidth, proposedCellSizeByHeight);
        if (cellSize <= 0) {
            cellSize = 1; // Ensure cellSize is positive
            Log.w(TAG, "onMeasure: Calculated cellSize was <= 0. Clamped to 1.");
        }

        int measuredWidth = cellSize * gridCols;
        int measuredHeight = cellSize * gridRows;
        setMeasuredDimension(measuredWidth, measuredHeight);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (gridCols <= 0 || gridRows <= 0 || w <= 0 || h <= 0) { // Check for non-positive dimensions
            cellSize = 0;
            gridOffsetX = 0;
            gridOffsetY = 0;
            Log.w(TAG, "onSizeChanged: Grid dimensions or view size is zero/negative. Resetting cell calculations.");
            return;
        }

        int csw = w / gridCols;
        int csh = h / gridRows;
        cellSize = Math.min(csw, csh);

        if (cellSize <= 0) {
            cellSize = 1; // Ensure cellSize is positive
            Log.w(TAG, "onSizeChanged: Calculated cellSize was <= 0. Clamped to 1.");
        }

        int totalGridWidth = cellSize * gridCols;
        int totalGridHeight = cellSize * gridRows;
        gridOffsetX = (w - totalGridWidth) / 2;
        gridOffsetY = (h - totalGridHeight) / 2;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (canvas == null) {
            Log.e(TAG, "onDraw: Canvas is null! Aborting draw.");
            return;
        }

        // Draw background for the grid area
        if (gridRows > 0 && gridCols > 0 && cellSize > 0) {
            canvas.drawRect(
                    gridOffsetX,
                    gridOffsetY,
                    gridOffsetX + gridCols * cellSize,
                    gridOffsetY + gridRows * cellSize,
                    backgroundPaint
            );
        }

        if (!allBitmapsLoadedSuccessfully) {
            Log.e(TAG, "onDraw: Drawing PINK placeholder because 'allBitmapsLoadedSuccessfully' is false.");
            paint.setColor(Color.MAGENTA);
            // Draw over the entire potential grid area if dimensions are somewhat valid
            if (gridRows > 0 && gridCols > 0 && cellSize > 0) {
                canvas.drawRect(gridOffsetX, gridOffsetY, gridOffsetX + gridCols * cellSize, gridOffsetY + gridRows * cellSize, paint);
            } else { // Fallback if grid dimensions are also bad, draw over whole view
                canvas.drawColor(Color.MAGENTA);
            }
            return; // Stop further drawing if basic bitmaps aren't loaded
        }

        if (candies == null || candies.isEmpty() || cellSize <= 0) {
            Log.w(TAG, "onDraw: Not drawing candies. Candies list is " + (candies == null ? "null" : "empty") + " or cellSize is " + cellSize);
            return;
        }

        for (int i = 0; i < gridRows; i++) {
            if (i >= candies.size()) { Log.w(TAG, "onDraw: Row index " + i + " out of bounds for candies list."); continue; }
            List<Candy> rowList = candies.get(i);
            if (rowList == null) { Log.w(TAG, "onDraw: Row " + i + " is null."); continue; }
            // Ensure rowList has the expected number of columns for safety before accessing elements
            if (rowList.size() != gridCols) {
                Log.w(TAG, "onDraw: Row " + i + " has incorrect column count. Expected " + gridCols + ", got " + rowList.size() + ". Drawing placeholders for this row.");
                // Optionally draw placeholders for this malformed row if you want to see it
                for(int k=0; k < gridCols; k++) { // Iterate up to expected gridCols to draw placeholders
                    drawPlaceholder(canvas, i, k);
                }
                continue;
            }

            for (int j = 0; j < gridCols; j++) { // Iterate up to gridCols as rowList.size() is now confirmed to match
                Candy candy = rowList.get(j); // Safe to get
                if (candy == null) {
                    Log.w(TAG, "onDraw: Candy object at (" + i + "," + j + ") is null. Drawing placeholder.");
                    drawPlaceholder(canvas, i, j);
                    continue;
                }

                int candyType = candy.getType();

                if (candyType < 0 || candyBitmaps == null || candyType >= candyBitmaps.length || candyBitmaps[candyType] == null) {
                    Log.w(TAG, "onDraw: Problem with candyType (" + candyType + ") or its bitmap at (" + i + "," + j + "). " +
                            "candyBitmaps.length: " + (candyBitmaps != null ? candyBitmaps.length : "null") +
                            ". Drawing placeholder.");
                    drawPlaceholder(canvas, i, j);
                    continue;
                }

                Bitmap candyBitmap = candyBitmaps[candyType];
                int left = gridOffsetX + j * cellSize;
                int top = gridOffsetY + i * cellSize;
                Rect destRect = new Rect(left, top, left + cellSize, top + cellSize);
                canvas.drawBitmap(candyBitmap, null, destRect, paint);
            }
        }
    }

    private void drawPlaceholder(Canvas canvas, int row, int col) {
        if (cellSize <= 0 || canvas == null) return;
        paint.setColor(Color.MAGENTA);
        canvas.drawRect(gridOffsetX + col * cellSize, gridOffsetY + row * cellSize,
                gridOffsetX + (col + 1) * cellSize, gridOffsetY + (row + 1) * cellSize, paint);
    }
}
