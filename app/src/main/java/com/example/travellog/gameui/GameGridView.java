package com.example.travellog.gameui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.travellog.gamecore.Candy;
import com.example.travellog.gamecore.LevelConfig;

import java.util.ArrayList;
import java.util.Collections; // <<< NEW: Added for Collections.shuffle
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameGridView extends View {
    private static final String TAG = "GameGridView";

    // Paints

    private Paint paint;
    private Paint backgroundPaint;
    private Paint highlightPaint;
    private Paint dialogBackgroundPaint;
    private Paint dialogTextPaint;
    private Paint dialogScoreTextPaint;

    // Grid Properties
    private int gridRows = 0;
    private int gridCols = 0;
    private int cellSize = 0;
    private int gridOffsetX = 0;
    private int gridOffsetY = 0;

    // Candy Data and Bitmaps
    private List<List<Candy>> candies;
    private Random random = new Random();
    private Bitmap[] candyBitmaps;
    private static final int NUMBER_OF_CANDY_TYPES = 6; // Ensure this matches your assets & LevelConfig
    private boolean allBitmapsLoadedSuccessfully = false;

    // Level and Game State
    private LevelConfig currentLevelConfig;
    private Candy selectedCandyObject = null;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int gameScore = 0;
    private long startTimeMillis = 0;
    private boolean levelTimerRunning = false;
    private boolean isLevelComplete = false;
    private int currentScoreForDialog = 0;
    private long timeTakenMillisForDialog = 0;
    private boolean isBoardSettling = false; // Flag to prevent interaction while board is auto-processing
    private Handler gameLoopHandler = new Handler(Looper.getMainLooper());

    // <<< NEW: GameStateListener interface and variable >>>
    private GameStateListener gameStateListener;
    private Point lastInteractedPoint = null;

    public interface GameStateListener {
        void onNoMovesAvailable();
        void onMovesAvailable(); // To hide shuffle button if moves become available
        // You might add other callbacks: onScoreChanged(int newScore), onLevelCompleted(), etc.
    }

    public void setGameStateListener(GameStateListener listener) {
        this.gameStateListener = listener;
    }
    // <<< END NEW >>>


    // Point helper class
    private static class Point {
        int r, c;
        Point(int r, int c) { this.r = r; this.c = c; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return r == point.r && c == point.c;
        }
        @Override
        public int hashCode() { return 31 * r + c; }
        @Override
        public String toString() { return "[" + r + "," + c + "]"; }
    }

    // Constructors
    public GameGridView(Context context) { super(context); init(context); }
    public GameGridView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(context); }
    public GameGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(context); }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#E0E0E0")); // Light gray background
        highlightPaint = new Paint();
        highlightPaint.setColor(Color.YELLOW); // Keep yellow for selection
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(8);
        highlightPaint.setAntiAlias(true);

        dialogBackgroundPaint = new Paint();
        dialogBackgroundPaint.setColor(Color.argb(220, 0, 0, 0)); // More opaque
        dialogBackgroundPaint.setStyle(Paint.Style.FILL);

        dialogTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialogTextPaint.setColor(Color.WHITE);
        dialogTextPaint.setTextAlign(Paint.Align.CENTER);

        dialogScoreTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialogScoreTextPaint.setColor(Color.rgb(127, 255, 0));
        dialogScoreTextPaint.setTextAlign(Paint.Align.CENTER);

        candies = new ArrayList<>();
        initBitmaps(context);
    }

    // MODIFIED: initBitmaps to load all sprites including special ones
    private void initBitmaps(Context context) {
        // Use the constant from Candy.java for the total number of unique sprites
        if (Candy.TOTAL_NUMBER_OF_SPRITES <= 0) {
            Log.e(TAG, "CRITICAL ERROR: Candy.TOTAL_NUMBER_OF_SPRITES is not positive.");
            allBitmapsLoadedSuccessfully = false;
            return;
        }
        Log.i(TAG, "initBitmaps: Attempting to load " + Candy.TOTAL_NUMBER_OF_SPRITES + " candy bitmaps.");
        // Size the array to hold all possible sprites
        candyBitmaps = new Bitmap[Candy.TOTAL_NUMBER_OF_SPRITES];
        int successfullyLoadedCount = 0;

        for (int i = 0; i < Candy.TOTAL_NUMBER_OF_SPRITES; i++) {
            String targetResourceName;
            // Determine the drawable resource name based on the sprite index 'i'
            // which corresponds to the TYPE constants in Candy.java
            if (i >= Candy.TYPE_NORMAL_0 && i < Candy.NUMBER_OF_REGULAR_CANDY_TYPES) {
                // Regular candies are typically named like "candy_type_0", "candy_type_1", etc.
                targetResourceName = "candy_type_" + i;
            } else if (i == Candy.TYPE_BOMB) {targetResourceName = "bomb"; // Expects bomb.png
            } else if (i == Candy.TYPE_EXPLODING_BOMB) {
                targetResourceName = "exploding_bomb"; // Expects exploding_bomb.png
            } else if (i == Candy.TYPE_MEGA_BOMB) {
                targetResourceName = "mega_bomb"; // Expects mega_bomb.png
            } else {
                // This case should ideally not be reached if TOTAL_NUMBER_OF_SPRITES is correct
                Log.w(TAG, "initBitmaps: No defined resource name for sprite index " + i);
                continue; // Skip this unknown sprite index
            }

            int resourceId = context.getResources().getIdentifier(targetResourceName, "drawable", context.getPackageName());

            if (resourceId != 0) {
                try {
                    candyBitmaps[i] = BitmapFactory.decodeResource(context.getResources(), resourceId);
                    if (candyBitmaps[i] != null) {
                        successfullyLoadedCount++;
                        Log.d(TAG, "initBitmaps: Successfully loaded '" + targetResourceName + "' for index " + i);
                    } else {
                        // This means the resource was found but couldn't be decoded (e.g., corrupted file, wrong format)
                        Log.e(TAG, "initBitmaps: Failed to decode resource for '" + targetResourceName + "'. Check file integrity and name.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "initBitmaps: Exception decoding '" + targetResourceName + "'", e);
                }
            } else {
                // This means the file (e.g., bomb.png) was not found in res/drawable
                Log.e(TAG, "initBitmaps: Drawable resource '" + targetResourceName + "' (for index " + i + ") NOT FOUND. Ensure it exists in res/drawable.");
            }
        }

        // Check if all *expected* sprites were loaded
        allBitmapsLoadedSuccessfully = successfullyLoadedCount == Candy.TOTAL_NUMBER_OF_SPRITES;

        if (!allBitmapsLoadedSuccessfully) {
            Log.e(TAG, "initBitmaps: CRITICAL FAILURE - NOT ALL EXPECTED BITMAPS LOADED. Loaded " + successfullyLoadedCount + " out of " + Candy.TOTAL_NUMBER_OF_SPRITES);
            Log.e(TAG, "Review Logcat for specific missing drawable resources (e.g., bomb.png, candy_type_X.png, etc.).");
        } else {
            Log.i(TAG, "initBitmaps: All " + Candy.TOTAL_NUMBER_OF_SPRITES + " expected candy bitmaps appear to be loaded successfully!");
        }
    }



    public void setupGridForLevel(int levelNumber) {
        Log.d(TAG, "setupGridForLevel: Setting up for level " + levelNumber);
        isLevelComplete = false;
        gameScore = 0;
        startTimeMillis = System.currentTimeMillis();
        levelTimerRunning = true;
        isBoardSettling = true; // Prevent interaction until board is initially stable

        currentLevelConfig = LevelConfig.getConfigForLevel(levelNumber);
        if (currentLevelConfig == null) {
            Log.e(TAG, "Failed to get LevelConfig for level: " + levelNumber + ". Using fallback.");
            this.gridRows = 5; this.gridCols = 5;
            this.currentLevelConfig = new LevelConfig(levelNumber, this.gridRows, this.gridCols, 500);
        } else {
            this.gridRows = currentLevelConfig.getRows();
            this.gridCols = currentLevelConfig.getCols();
        }
        if (gridRows <= 0) this.gridRows = 1;
        if (gridCols <= 0) this.gridCols = 1;

        initCandiesAndStabilizeBoard(); // This calls stabilizationLoop which calls findAllMatchesOnBoard

        selectedRow = -1; selectedCol = -1; selectedCandyObject = null;
        requestLayout(); // Recalculate dimensions
        invalidate();    // Redraw

        // <<< MODIFIED: After setting up a new grid, check for available moves >>>
        // We'll add the actual call to the listener later, once hasAvailableMoves() is implemented
        // For now, this is a conceptual placement.
        // if (gameStateListener != null) {
        //     if (!hasAvailableMoves()) { // hasAvailableMoves() is not yet implemented
        //         gameStateListener.onNoMovesAvailable();
        //     } else {
        //         gameStateListener.onMovesAvailable();
        //     }
        // }
    }

    // MODIFIED: To use correct constants for candy generation and validation
    private void initCandiesAndStabilizeBoard() {
        if (!allBitmapsLoadedSuccessfully) {
            Log.e(TAG, "Cannot initialize candies - bitmaps not loaded.");
            isBoardSettling = false; return;
        }
        if (currentLevelConfig == null) {
            Log.e(TAG, "Cannot initialize candies - currentLevelConfig is null.");
            isBoardSettling = false; return;
        }
        if (gridRows <= 0 || gridCols <= 0) {
            Log.e(TAG, "Cannot initialize candies - grid dimensions invalid.");
            isBoardSettling = false; return;
        }
        // Ensure we have regular candy types to generate
        if (Candy.NUMBER_OF_REGULAR_CANDY_TYPES <= 0) {
            Log.e(TAG, "Cannot initialize candies - Candy.NUMBER_OF_REGULAR_CANDY_TYPES is not positive.");
            isBoardSettling = false; return;
        }

        candies.clear();
        int[][] layout = currentLevelConfig.getCustomGridLayout();
        boolean useCustomLayout = layout != null && layout.length == gridRows &&
                (gridRows > 0 && layout[0].length == gridCols);

        for (int i = 0; i < gridRows; i++) {
            List<Candy> rowList = new ArrayList<>(gridCols);
            for (int j = 0; j < gridCols; j++) {
                int candyType;
                if (useCustomLayout) {
                    candyType = layout[i][j];
                    // Validate custom layout types against *all* loaded sprites
                    if (candyType < 0 || candyType >= Candy.TOTAL_NUMBER_OF_SPRITES) {
                        Log.w(TAG, "Custom layout type " + candyType + " out of bounds for loaded sprites. Defaulting to regular candy.");
                        candyType = Candy.TYPE_NORMAL_0; // Fallback to a default regular candy
                    }
                } else {
                    // For random fill, generate ONLY regular candy types
                    candyType = random.nextInt(Candy.NUMBER_OF_REGULAR_CANDY_TYPES);
                }
                rowList.add(new Candy(candyType));
            }
            candies.add(rowList);
        }
        Log.i(TAG, "Candies initialized. Size: " + candies.size() + "x" + (candies.isEmpty() ? 0 : candies.get(0).size()));

        isBoardSettling = true; // Board needs to stabilize after initialization
        gameLoopHandler.post(this::stabilizationLoop);
    }


    // MODIFIED: To use findAllMatchGroupsOnBoard and handle its output for stabilization
    private void stabilizationLoop() {
        isBoardSettling = true; // Ensure settling flag is true during stabilization
        lastInteractedPoint = null; // No user interaction is involved in stabilization matches

        // Call the new method that returns List<MatchGroup>
        List<MatchGroup> initialMatchGroups = findAllMatchGroupsOnBoard();

        if (initialMatchGroups != null && !initialMatchGroups.isEmpty()) {
            Log.d(TAG, "Stabilization: Found " + initialMatchGroups.size() + " initial match groups. Processing...");
            Set<Point> pointsToClear = new HashSet<>();
            for (MatchGroup group : initialMatchGroups) {
                // During initial stabilization, we typically just clear matches
                // without creating special candies from them.
                pointsToClear.addAll(group.points);
            }

            if (!pointsToClear.isEmpty()) {
                removeCandies(pointsToClear);
                // gameScore += pointsToClear.size() * 10; // Optional: Score stabilization matches
                applyGravity();
                refillBoard(); // Refill with regular candies
                invalidate();
                gameLoopHandler.postDelayed(this::stabilizationLoop, 150); // Loop until stable
            } else {
                // This case might happen if findAllMatchGroupsOnBoard returned groups
                // but after filtering or processing, no points actually needed clearing.
                // Or if logic changes to sometimes not clear from groups.
                Log.i(TAG, "Stabilization: No points to clear from initial groups. Board considered stable.");
                finishStabilization();
            }
        } else {
            Log.i(TAG, "Stabilization: No initial match groups found. Board is stable.");
            finishStabilization();
        }
    }

    // NEW: Helper method to finalize stabilization and check for moves
    private void finishStabilization() {
        Log.i(TAG, "Board is stable. Initial stabilization complete.");
        isBoardSettling = false; // Board is now stable
        lastInteractedPoint = null; // Clear any lingering interaction point
        invalidate();
        if (gameStateListener != null) {
            if (!hasAvailableMoves()) {
                Log.d(TAG, "finishStabilization: No moves detected, notifying listener.");
                gameStateListener.onNoMovesAvailable();
            } else {
                Log.d(TAG, "finishStabilization: Moves available, notifying listener.");
                gameStateListener.onMovesAvailable();
            }
        }
    }



    private Candy getCandyAt(int r, int c) {
        if (candies != null && r >= 0 && r < gridRows && c >= 0 && c < gridCols &&
                candies.get(r) != null && c < candies.get(r).size()) {
            return candies.get(r).get(c);
        }
        return null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (gridCols <= 0 || gridRows <= 0) {
            setMeasuredDimension(parentWidth, parentHeight); return;
        }
        cellSize = Math.min(parentWidth / gridCols, parentHeight / gridRows);
        if (cellSize <= 0) cellSize = 1;
        setMeasuredDimension(cellSize * gridCols, cellSize * gridRows);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (gridCols <= 0 || gridRows <= 0 || w <= 0 || h <= 0) {
            cellSize = 0; gridOffsetX = 0; gridOffsetY = 0; return;
        }
        cellSize = Math.min(w / gridCols, h / gridRows);
        if (cellSize <= 0) cellSize = 1;
        gridOffsetX = (w - (cellSize * gridCols)) / 2;
        gridOffsetY = (h - (cellSize * gridRows)) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas == null) return;
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        if (cellSize <= 0 || candies == null || candies.isEmpty() || !allBitmapsLoadedSuccessfully) {
            paint.setColor(Color.RED); paint.setTextSize(50);
            String message = "Grid not ready";
            if (!allBitmapsLoadedSuccessfully) {
                message = "Bitmaps loading error!";
            } else if (candies == null || candies.isEmpty()) {
                message = "Candies not initialized!";
            } else if (cellSize <= 0) {
                message = "Cell size invalid!";
            }
            // Center the error message
            float textWidth = paint.measureText(message);
            canvas.drawText(message, (getWidth() - textWidth) / 2f, getHeight() / 2f, paint);
            return;
        }

        for (int r = 0; r < gridRows; r++) {
            if (candies.get(r) == null) {
                Log.w(TAG, "onDraw: Row " + r + " in candies list is null.");
                continue; // Should not happen if initialized correctly
            }
            for (int c = 0; c < gridCols; c++) {
                Candy currentCandy = getCandyAt(r, c);
                if (currentCandy != null) {
                    // Get the candy's type, which directly corresponds to its sprite index
                    int candySpriteIndex = currentCandy.getType();

                    // Check if the type is a valid index for our loaded bitmaps
                    if (candySpriteIndex >= 0 && candySpriteIndex < Candy.TOTAL_NUMBER_OF_SPRITES &&
                            candyBitmaps[candySpriteIndex] != null) {
                        Bitmap candyBitmap = candyBitmaps[candySpriteIndex];
                        int destLeft = gridOffsetX + c * cellSize;
                        int destTop = gridOffsetY + r * cellSize;
                        Rect destRect = new Rect(destLeft, destTop, destLeft + cellSize, destTop + cellSize);
                        canvas.drawBitmap(candyBitmap, null, destRect, null);
                    } else {
                        // Fallback drawing if bitmap is missing or type is out of bounds
                        paint.setColor(Color.MAGENTA); // Use a distinct color for errors/unknowns
                        canvas.drawRect(gridOffsetX + c * cellSize, gridOffsetY + r * cellSize,
                                gridOffsetX + (c + 1) * cellSize, gridOffsetY + (r + 1) * cellSize, paint);

                        // Log verbose info only once per missing type to avoid spamming Logcat
                        if (candySpriteIndex < 0 || candySpriteIndex >= Candy.TOTAL_NUMBER_OF_SPRITES) {
                            Log.v(TAG, "onDraw: Invalid candy type " + candySpriteIndex + " at (" + r + "," + c + "). Out of sprite bounds.");
                        } else if (candyBitmaps[candySpriteIndex] == null) {
                            Log.v(TAG, "onDraw: Missing bitmap for type/spriteIndex " + candySpriteIndex + " (e.g., bomb.png). Check initBitmaps logs.");
                        }
                    }
                }
                // Draw grid lines over each cell space
                paint.setStyle(Paint.Style.STROKE); paint.setColor(Color.GRAY); paint.setStrokeWidth(1);
                canvas.drawRect(gridOffsetX + c * cellSize, gridOffsetY + r * cellSize,
                        gridOffsetX + (c + 1) * cellSize, gridOffsetY + (r + 1) * cellSize, paint);
                paint.setStyle(Paint.Style.FILL); // Reset paint style for next iteration
            }
        }

        // Draw selection highlight if a candy is selected
        if (selectedRow != -1 && selectedCol != -1) {
            canvas.drawRect(gridOffsetX + selectedCol * cellSize, gridOffsetY + selectedRow * cellSize,
                    gridOffsetX + (selectedCol + 1) * cellSize, gridOffsetY + (selectedRow + 1) * cellSize,
                    highlightPaint);
        }

        // Level complete dialog drawing is currently disabled
        // if (isLevelComplete) {
        //    drawLevelCompleteDialog(canvas);
        // }
    }



    private void drawLevelCompleteDialog(Canvas canvas) {
        float dialogLeft = getWidth() * 0.1f, dialogTop = getHeight() * 0.25f;
        float dialogRight = getWidth() * 0.9f, dialogBottom = getHeight() * 0.75f;
        RectF dialogRect = new RectF(dialogLeft, dialogTop, dialogRight, dialogBottom);
        canvas.drawRoundRect(dialogRect, 30, 30, dialogBackgroundPaint);

        dialogTextPaint.setTextSize(getHeight() * 0.05f);
        dialogScoreTextPaint.setTextSize(getHeight() * 0.06f);
        float textX = getWidth() / 2f, currentY = dialogTop + getHeight() * 0.12f;

        canvas.drawText("Level Complete!", textX, currentY, dialogTextPaint);
        currentY += getHeight() * 0.12f;
        canvas.drawText("Score: " + currentScoreForDialog, textX, currentY, dialogScoreTextPaint);
        currentY += getHeight() * 0.1f;
        String timeStr = String.format("%02d:%02d", (timeTakenMillisForDialog / (1000 * 60)) % 60, (timeTakenMillisForDialog / 1000) % 60);
        canvas.drawText("Time: " + timeStr, textX, currentY, dialogTextPaint);
        currentY += getHeight() * 0.12f;
        dialogTextPaint.setTextSize(getHeight() * 0.04f);
        canvas.drawText("Tap to Continue", textX, currentY, dialogTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isBoardSettling) return true;

        if (isLevelComplete) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isLevelComplete = false; levelTimerRunning = false;
                if (currentLevelConfig != null) {
                    int nextLevel = currentLevelConfig.getLevelNumber() + 1;
                    if (nextLevel <= LevelConfig.getMaxLevels()) setupGridForLevel(nextLevel);
                    else Log.i(TAG, "All levels completed!");
                }
                invalidate(); return true;
            }
            return true;
        }

        if (!allBitmapsLoadedSuccessfully || candies == null || candies.isEmpty() || cellSize == 0) {
            return super.onTouchEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int col = (int) ((event.getX() - gridOffsetX) / cellSize);
            int row = (int) ((event.getY() - gridOffsetY) / cellSize);
            if (row >= 0 && row < gridRows && col >= 0 && col < gridCols) handleCellTouch(row, col);
            else if (selectedRow != -1) {
                selectedRow = -1; selectedCol = -1; selectedCandyObject = null; invalidate();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    // MODIFIED: To correctly set lastInteractedPoint
    private void handleCellTouch(int row, int col) {
        Log.d(TAG, "handleCellTouch: Touched cell (" + row + "," + col + ")");        Candy touchedCandy = getCandyAt(row, col);

        if (touchedCandy == null) {
            Log.w(TAG, "handleCellTouch: Touched null candy at (" + row + "," + col + "), possibly an empty space.");
            if (selectedRow != -1) { // If a candy was selected, deselect it
                Log.d(TAG, "handleCellTouch: Deselecting due to touch on empty space.");
                selectedRow = -1; selectedCol = -1; selectedCandyObject = null;
                lastInteractedPoint = null; // Clear interaction on deselect
                invalidate();
            }
            return;
        }

        // --- Future: Special Candy Tap Logic ---
        // if (touchedCandy.isSpecialCandy() && selectedRow == -1) {
        // // If a special candy (like a Bomb) is tapped directly (not part of a swap)
        // // handleSpecialCandyTap(row, col, touchedCandy);
        // // return;
        // }
        // --- End Future ---

        if (selectedRow == -1) {
            // No candy currently selected, so select this one
            selectedRow = row;
            selectedCol = col;
            selectedCandyObject = touchedCandy;
            lastInteractedPoint = new Point(row, col); // Store this as the first point of interaction
            Log.d(TAG, "handleCellTouch: Selected candy at (" + row + "," + col + ") type: " + touchedCandy.getType());
        } else {
            // A candy is already selected (at selectedRow, selectedCol)
            if (selectedRow == row && selectedCol == col) {
                // Tapped the same selected candy again: Deselect
                Log.d(TAG, "handleCellTouch: Deselected candy at (" + row + "," + col + ")");
                selectedRow = -1; selectedCol = -1; selectedCandyObject = null;
                lastInteractedPoint = null; // Clear interaction on deselect
            } else if (isAdjacent(row, col, selectedRow, selectedCol)) {
                // Tapped an adjacent candy: Attempt swap
                Log.d(TAG, "handleCellTouch: Attempting swap between (" + selectedRow + "," + selectedCol + ") and (" + row + "," + col + ")");
                int firstCandyR = selectedRow;
                int firstCandyC = selectedCol;

                // The `lastInteractedPoint` for creating a special candy is the one the user
                // effectively "moved" or the second one they tapped to complete the pair for a swap.
                // This is (row, col) in this context.
                lastInteractedPoint = new Point(row, col);
                Log.d(TAG, "handleCellTouch: lastInteractedPoint set to (" + row + "," + col + ") for potential special creation.");

                // Deselect before processing swap to clear visual selection highlight
                selectedRow = -1; selectedCol = -1; selectedCandyObject = null;

                swapCandies(firstCandyR, firstCandyC, row, col); // Perform the visual swap first
                invalidate(); // Show the swapped state immediately

                // Now, check if this swap results in a match
                if (checkAndProcessMatchesAfterSwap(firstCandyR, firstCandyC, row, col)) {
                    // Match found and processMatchesAndContinueLoop will handle further logic.
                    // isBoardSettling will be true.
                    Log.d(TAG, "handleCellTouch: Swap resulted in a match. Board is settling.");
                } else {
                    // No match from the swap, so swap back.
                    Log.d(TAG, "handleCellTouch: No match from swap. Swapping back.");
                    swapCandies(row, col, firstCandyR, firstCandyC); // Swap back to original positions
                    isBoardSettling = false; // Board is not settling from this reverted swap
                    lastInteractedPoint = null; // Reset interaction point as the swap was invalid
                    invalidate(); // Show the reverted state
                }
            } else {
                // Tapped a non-adjacent candy: Deselect the old one, select this new one
                Log.d(TAG, "handleCellTouch: Tapped non-adjacent. Deselecting old, selecting new at (" + row + "," + col + ")");
                selectedRow = row; selectedCol = col; selectedCandyObject = touchedCandy;
                lastInteractedPoint = new Point(row, col); // Set interaction to this new selection
            }
        }
        invalidate(); // Redraw for selection changes or after swap processing
    }



    // MODIFIED: To use findAllMatchGroupsOnBoard and handle its output
    private boolean checkAndProcessMatchesAfterSwap(int r1, int c1, int r2, int c2) {
        isBoardSettling = true; // Board will be processing due to the swap and match check

        // lastInteractedPoint should have been set in handleCellTouch before this method was called.
        // It's crucial for findAllMatchGroupsOnBoard to determine the creationPoint for special candies.
        Log.d(TAG, "checkAndProcessMatchesAfterSwap: Checking board after swap. lastInteractedPoint: " + lastInteractedPoint);

        // Call the new method that returns List<MatchGroup>
        List<MatchGroup> matchGroupsFound = findAllMatchGroupsOnBoard();

        if (matchGroupsFound != null && !matchGroupsFound.isEmpty()) {
            Log.i(TAG, "Swap created " + matchGroupsFound.size() + " match group(s).");
            if (!matchGroupsFound.isEmpty()) {
                Log.d(TAG, "First group details: " + matchGroupsFound.get(0).toString()); // Log details of the first group
            }
            // Pass the List<MatchGroup> to the next processing step
            processMatchesAndContinueLoop(matchGroupsFound);
            return true; // Matches were found and are being processed
        } else {
            Log.i(TAG, "Swap created no matches.");
            isBoardSettling = false; // No matches, so board is not settling from this specific swap attempt
            // If the swap was invalid, handleCellTouch is responsible for swapping back
            // and resetting lastInteractedPoint.
            return false; // No matches found from this swap
        }
    }



    // MODIFIED: To handle List<MatchGroup>, create Bombs, and manage game loop
    private void processMatchesAndContinueLoop(List<MatchGroup> matchGroupsToProcess) { // <<< NEW SIGNATURE
        if (matchGroupsToProcess == null || matchGroupsToProcess.isEmpty()) {
            // This is the base case for the recursion: no more matches found.
            isBoardSettling = false;
            // lastInteractedPoint should be null here unless a swap just happened that yielded no matches
            // which is handled in checkAndProcessMatchesAfterSwap. If it was a cascade, it should be null.
            // Log.d(TAG, "processMatches: No more groups. lastInteractedPoint: " + lastInteractedPoint);

            // checkLevelCompletion(); // Level completion is currently disabled.
            invalidate(); // Ensure final board state is drawn.

            if (gameStateListener != null) {
                if (!hasAvailableMoves()) {
                    Log.d(TAG, "processMatches (empty): No moves found after processing, notifying listener.");
                    gameStateListener.onNoMovesAvailable();
                } else {
                    Log.d(TAG, "processMatches (empty): Moves available after processing, notifying listener.");
                    gameStateListener.onMovesAvailable();
                }
            }
            Log.d(TAG, "processMatchesAndContinueLoop: No more match groups to process. Board should be stable or checking for moves.");
            return;
        }

        Log.d(TAG, "processMatchesAndContinueLoop: Processing " + matchGroupsToProcess.size() + " match group(s). LastInteractedPoint: " + lastInteractedPoint);
        isBoardSettling = true; // Board is actively processing matches.

        Set<Point> allPointsToRemove = new HashSet<>();
        boolean specialCandyCreatedThisTurn = false;
        Point specialCandyCreationPoint = null; // Store the exact (r,c) where a special candy was made

        // --- Special Candy Creation Logic ---
        // Only attempt to create a special candy if this processing step
        // is the direct result of a user's swap (i.e., lastInteractedPoint is not null).
        // Cascading matches should not typically create more special candies in this basic model.
        if (lastInteractedPoint != null) {
            MatchGroup bestMatchForSpecialCandy = null;

            // Find the "best" match group that involves the lastInteractedPoint
            // Prefer longer matches if lastInteractedPoint is part of multiple groups
            for (MatchGroup group : matchGroupsToProcess) {
                if (group.points.contains(lastInteractedPoint)) {
                    if (bestMatchForSpecialCandy == null || group.length > bestMatchForSpecialCandy.length) {
                        bestMatchForSpecialCandy = group;
                    }
                }
            }

            if (bestMatchForSpecialCandy != null) {
                // The creation point for the special candy IS the lastInteractedPoint.
                Point creationPt = lastInteractedPoint;
                Log.d(TAG, "User interaction created match of length " + bestMatchForSpecialCandy.length +
                        " involving interaction point " + creationPt + ". This group's own creationPoint was: " + bestMatchForSpecialCandy.creationPoint);

                if (bestMatchForSpecialCandy.length == 4) { // Create Bomb for 4-in-a-row
                    Log.i(TAG, "Creating BOMB at user interaction point: " + creationPt + " from a 4-match.");
                    if (isValidCell(creationPt.r, creationPt.c) && candies.get(creationPt.r) != null) {
                        candies.get(creationPt.r).set(creationPt.c, new Candy(Candy.TYPE_BOMB));
                        specialCandyCreatedThisTurn = true;
                        specialCandyCreationPoint = creationPt; // Track where it was made
                    } else {
                        Log.e(TAG, "Error: Invalid cell or row list null when trying to create bomb at " + creationPt);
                    }
                }
                // TODO: Future - else if (bestMatchForSpecialCandy.length >= 5) { /* Create Mega Bomb */ }
            }
        }
        // --- End Special Candy Creation Logic ---

        // Collect all points from ALL found matches for removal
        for (MatchGroup group : matchGroupsToProcess) {
            allPointsToRemove.addAll(group.points);
        }

        // If a special candy was just created at a point, DO NOT remove that point this turn.
        if (specialCandyCreatedThisTurn && specialCandyCreationPoint != null) {
            boolean removed = allPointsToRemove.remove(specialCandyCreationPoint);
            if (removed) {
                Log.d(TAG, "Kept newly created special candy at " + specialCandyCreationPoint + " from removal list.");
            } else {
                // This could happen if the special candy creation point wasn't in any match group to begin with
                // which would be unusual but good to log.
                Log.w(TAG, "Newly created special candy at " + specialCandyCreationPoint + " was not in the points-to-remove list anyway.");
            }
        }

        // Check if there's anything to do (either remove points or a special was made changing the board)
        if (allPointsToRemove.isEmpty() && !specialCandyCreatedThisTurn) {
            Log.d(TAG, "processMatchesAndContinueLoop: No points to remove and no special candy created this cycle. Ending loop.");
            isBoardSettling = false; // Nothing effectively changed on the board from this call.
            lastInteractedPoint = null; // Reset as this interaction sequence (if any) is done.
            // Re-check for moves as board state is considered final for this processing step.
            if (gameStateListener != null) {
                if (!hasAvailableMoves()) {
                    gameStateListener.onNoMovesAvailable();
                } else {
                    gameStateListener.onMovesAvailable();
                }
            }
            invalidate();
            return;
        }

        // Proceed with board changes if points are to be removed
        if (!allPointsToRemove.isEmpty()) {
            Log.d(TAG, "Removing " + allPointsToRemove.size() + " candies from the board.");
            removeCandies(allPointsToRemove);
            gameScore += allPointsToRemove.size() * 10; // Basic scoring
            Log.d(TAG, "Score updated to: " + gameScore);
        }

        applyGravity();
        refillBoard(); // Refills with regular candies
        invalidate(); // Show changes from removal, gravity, and refill

        // IMPORTANT: Reset lastInteractedPoint *before* recursively calling to check for cascades.
        // Cascading matches are not direct user actions for special candy creation in this model.
        lastInteractedPoint = null;

        // Post a delay to continue the game loop, checking for new matches caused by gravity and refill.
        gameLoopHandler.postDelayed(() -> {
            Log.d(TAG, "processMatchesAndContinueLoop: Checking for cascade matches after delay.");
            // Call the new findAllMatchGroupsOnBoard for cascades
            List<MatchGroup> cascadeMatchGroups = findAllMatchGroupsOnBoard();
            processMatchesAndContinueLoop(cascadeMatchGroups); // Recursive call
        }, 200); // Delay in milliseconds (e.g., 200ms). Adjust for game feel.
    }



    private void swapCandies(int r1, int c1, int r2, int c2) {
        if (!isValidCell(r1, c1) || !isValidCell(r2, c2)) return;
        Candy candy1 = getCandyAt(r1, c1);
        Candy candy2 = getCandyAt(r2, c2);
        candies.get(r1).set(c1, candy2);
        candies.get(r2).set(c2, candy1);
    }

    private boolean isAdjacent(int r1, int c1, int r2, int c2) {
        return (Math.abs(r1 - r2) == 1 && c1 == c2) || (Math.abs(c1 - c2) == 1 && r1 == r2);
    }

    private boolean isValidCell(int r, int c) {
        return r >= 0 && r < gridRows && c >= 0 && c < gridCols;
    }

    private List<MatchGroup> findAllMatchGroupsOnBoard() {
        List<MatchGroup> allMatchGroups = new ArrayList<>();
        if (candies == null || gridRows <= 0 || gridCols <= 0) {
            Log.v(TAG, "findAllMatchGroupsOnBoard: Board not ready (candies null or grid invalid).");
            return allMatchGroups;
        }

        // This set tracks points that have already been included in any match group
        // to avoid creating overlapping or redundant groups from the same set of candies.
        // For example, an L-shape should ideally be one group or its parts handled once.
        Set<Point> pointsAlreadyInAConfirmedMatch = new HashSet<>();

        // Check horizontal matches
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; ) {
                // If this point is already part of a match found (e.g. a vertical one that extends here), skip.
                // This helps in L/T shape scenarios, but true L/T merging is more complex.
                if (pointsAlreadyInAConfirmedMatch.contains(new Point(r, c))) {
                    c++;
                    continue;
                }

                Candy firstCandy = getCandyAt(r, c);
                // Matches can only start with a regular, non-null candy
                if (firstCandy == null || !firstCandy.isRegularCandy()) {
                    c++;
                    continue;
                }

                Set<Point> currentHorizontalMatchPoints = new HashSet<>();
                currentHorizontalMatchPoints.add(new Point(r, c));
                int matchColEnd = c; // Tracks the end column of the current potential match

                // Look right for more candies of the same type
                while (matchColEnd + 1 < gridCols) {
                    Candy nextCandy = getCandyAt(r, matchColEnd + 1);
                    if (nextCandy != null && nextCandy.isRegularCandy() && nextCandy.getType() == firstCandy.getType()) {
                        currentHorizontalMatchPoints.add(new Point(r, matchColEnd + 1));
                        matchColEnd++;
                    } else {
                        break; // End of match or different candy type
                    }
                }

                if (currentHorizontalMatchPoints.size() >= 3) {
                    // Determine the creation point for a special candy
                    // If lastInteractedPoint is part of this match, it's the prime candidate
                    Point potentialCreationPoint = lastInteractedPoint;
                    if (potentialCreationPoint == null || !currentHorizontalMatchPoints.contains(potentialCreationPoint)) {
                        // Fallback if no interaction point or it's not in this match:
                        // Use the first point of this match.
                        // A more sophisticated approach might choose the middle point.
                        potentialCreationPoint = new Point(r, c);
                    }
                    allMatchGroups.add(new MatchGroup(new HashSet<>(currentHorizontalMatchPoints), potentialCreationPoint));
                    pointsAlreadyInAConfirmedMatch.addAll(currentHorizontalMatchPoints);
                    Log.v(TAG, "findAllMatchGroups: Found H-Match of length " + currentHorizontalMatchPoints.size() + " at r=" + r + " starting c=" + c);
                }
                c = matchColEnd + 1; // Continue checking from after the end of this found match/segment
            }
        }

        // Check vertical matches
        for (int c = 0; c < gridCols; c++) {
            for (int r = 0; r < gridRows; ) {
                if (pointsAlreadyInAConfirmedMatch.contains(new Point(r, c))) {
                    r++;
                    continue;
                }

                Candy firstCandy = getCandyAt(r, c);
                if (firstCandy == null || !firstCandy.isRegularCandy()) {
                    r++;
                    continue;
                }

                Set<Point> currentVerticalMatchPoints = new HashSet<>();
                currentVerticalMatchPoints.add(new Point(r, c));
                int matchRowEnd = r;

                while (matchRowEnd + 1 < gridRows) {
                    Candy nextCandy = getCandyAt(matchRowEnd + 1, c);
                    if (nextCandy != null && nextCandy.isRegularCandy() && nextCandy.getType() == firstCandy.getType()) {
                        currentVerticalMatchPoints.add(new Point(matchRowEnd + 1, c));
                        matchRowEnd++;
                    } else {
                        break;
                    }
                }

                if (currentVerticalMatchPoints.size() >= 3) {
                    Point potentialCreationPoint = lastInteractedPoint;
                    if (potentialCreationPoint == null || !currentVerticalMatchPoints.contains(potentialCreationPoint)) {
                        potentialCreationPoint = new Point(r, c);
                    }
                    allMatchGroups.add(new MatchGroup(new HashSet<>(currentVerticalMatchPoints), potentialCreationPoint));
                    pointsAlreadyInAConfirmedMatch.addAll(currentVerticalMatchPoints);
                    Log.v(TAG, "findAllMatchGroups: Found V-Match of length " + currentVerticalMatchPoints.size() + " at c=" + c + " starting r=" + r);
                }
                r = matchRowEnd + 1;
            }
        }

        // Note: This implementation finds straight-line matches (horizontal and vertical).
        // It uses `pointsAlreadyInAConfirmedMatch` to prevent finding the exact same line segment twice
        // if it could be considered part of both an H and V scan (e.g., a 1x3 line).
        // For true L or T shapes, this might result in two separate MatchGroup objects (e.g., one H, one V for an L).
        // `processMatchesAndContinueLoop` will currently handle these by adding all points from all groups.
        // A more complex merging logic would be needed here to combine overlapping/adjacent MatchGroups
        // of the same type into a single, larger MatchGroup before returning, if desired.
        // For special candy creation, we'll pick the best candidate group in processMatchesAndContinueLoop.

        if (allMatchGroups.isEmpty() && lastInteractedPoint != null) {
            // This situation means a swap occurred but didn't make any matches.
            // lastInteractedPoint might still be relevant if checkAndProcessMatchesAfterSwap needs it.
            Log.v(TAG, "findAllMatchGroupsOnBoard: No matches found, but lastInteractedPoint was: " + lastInteractedPoint);
        } else if (!allMatchGroups.isEmpty()){
            Log.d(TAG, "findAllMatchGroupsOnBoard: Found " + allMatchGroups.size() + " total match group(s).");
        } else {
            Log.v(TAG, "findAllMatchGroupsOnBoard: No matches found. lastInteractedPoint is null or irrelevant to current board state.");
        }
        return allMatchGroups;
    }

    private void removeCandies(Set<Point> pointsToRemove) {
        if (candies == null) return;
        for (Point p : pointsToRemove) {
            if (isValidCell(p.r, p.c)) {
                candies.get(p.r).set(p.c, null);
            }
        }
    }

    private void applyGravity() {
        if (candies == null) return;
        for (int c = 0; c < gridCols; c++) {
            int emptySlot = -1;
            for (int r = gridRows - 1; r >= 0; r--) {
                if (getCandyAt(r, c) == null) {
                    if (emptySlot == -1) {
                        emptySlot = r;
                    }
                } else if (emptySlot != -1) {
                    candies.get(emptySlot).set(c, getCandyAt(r, c));
                    candies.get(r).set(c, null);
                    emptySlot--;
                }
            }
        }
    }

    private void refillBoard() {
        if (candies == null || Candy.NUMBER_OF_REGULAR_CANDY_TYPES <= 0) {
            Log.e(TAG, "refillBoard: Cannot refill - candies list null or no regular candy types defined.");
            return;
        }
        for (int c = 0; c < gridCols; c++) {
            for (int r = 0; r < gridRows; r++) {
                if (candies.get(r) == null) { // Should not happen if rows are initialized
                    Log.e(TAG, "refillBoard: Row " + r + " is null!");
                    continue; // Avoid crash
                }
                if (getCandyAt(r, c) == null) {
                    // Generate ONLY regular candy types using the count from Candy.java
                    candies.get(r).set(c, new Candy(random.nextInt(Candy.NUMBER_OF_REGULAR_CANDY_TYPES)));
                }
            }
        }
        // Log.v(TAG, "Board refilled."); // Optional verbose log
    }



    // MODIFIED: To use new match finding and processing, and handle constants
    public void shuffleBoard() {
        Log.d(TAG, "shuffleBoard() called.");
        if (candies == null || candies.isEmpty() || gridRows <= 0 || gridCols <= 0 || isBoardSettling) {
            Log.w(TAG, "shuffleBoard: Cannot shuffle, board not ready or already settling. isBoardSettling=" + isBoardSettling);
            return;
        }

        isBoardSettling = true; // Prevent interaction during shuffle
        lastInteractedPoint = null; // Shuffle is not a user-initiated swap for special candy creation
        invalidate(); // Optional: Show a brief "settling" visual if needed

        // 1. Collect all non-null candies from the board
        List<Candy> allCandiesOnBoard = new ArrayList<>();
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                Candy currentCandy = getCandyAt(r, c);
                if (currentCandy != null) {
                    allCandiesOnBoard.add(currentCandy);
                }
            }
        }

        if (allCandiesOnBoard.isEmpty()) {
            Log.w(TAG, "shuffleBoard: No candies on board to shuffle.");
            isBoardSettling = false;
            invalidate();
            if (gameStateListener != null) {
                if (!hasAvailableMoves()) {
                    gameStateListener.onNoMovesAvailable();
                } else {
                    gameStateListener.onMovesAvailable();
                }
            }
            return;
        }

        // 2. Shuffle the collected candies
        Collections.shuffle(allCandiesOnBoard);

        // 3. Place the shuffled candies back onto the board
        int index = 0;
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                // Only replace spots that originally had candies.
                // If your game has fixed empty spots, this logic would need adjustment.
                if (getCandyAt(r,c) != null && index < allCandiesOnBoard.size()){
                    candies.get(r).set(c, allCandiesOnBoard.get(index++));
                } else if (getCandyAt(r,c) != null){ // Should not happen if counts match
                    Log.w(TAG,"shuffleBoard: Ran out of shuffled candies. Filling remaining with new random regular candy.");
                    // Fallback: new random REGULAR candy
                    candies.get(r).set(c, new Candy(random.nextInt(Candy.NUMBER_OF_REGULAR_CANDY_TYPES)));
                }
                // If getCandyAt(r,c) was null (an intentional empty spot perhaps), it remains null.
            }
        }
        // Check if all collected candies were used (should be if grid was full)
        if(index < allCandiesOnBoard.size()){
            Log.w(TAG, "shuffleBoard: " + (allCandiesOnBoard.size() - index) + " shuffled candies remaining. This might indicate an issue or empty spots not refilled by design.");
        }

        Log.d(TAG, "shuffleBoard: Candies shuffled.");
        // printGridState("After shuffle operation"); // Optional: for debugging

        invalidate(); // Show the shuffled board immediately

        gameLoopHandler.postDelayed(() -> {
            Log.d(TAG, "shuffleBoard: Checking for matches and available moves after shuffle delay.");
            // Ensure lastInteractedPoint is null before checking for matches after shuffle,
            // as these are not user-initiated for special candy creation.
            lastInteractedPoint = null;
            List<MatchGroup> matchesAfterShuffle = findAllMatchGroupsOnBoard(); // <<< NEW CALL

            if (matchesAfterShuffle != null && !matchesAfterShuffle.isEmpty()) {
                Log.d(TAG, "Shuffle created " + matchesAfterShuffle.size() + " immediate match group(s). Processing them.");
                processMatchesAndContinueLoop(matchesAfterShuffle); // <<< NEW PARAMETER TYPE
            } else {
                Log.d(TAG, "Shuffle did not create immediate matches.");
                isBoardSettling = false; // Board is stable if no matches from shuffle
                if (gameStateListener != null) {
                    if (!hasAvailableMoves()) {
                        Log.d(TAG, "shuffleBoard (no shuffle matches): No moves detected, notifying listener.");
                        gameStateListener.onNoMovesAvailable();
                    } else {
                        Log.d(TAG, "shuffleBoard (no shuffle matches): Moves available, notifying listener.");
                        gameStateListener.onMovesAvailable();
                    }
                }
                invalidate(); // Ensure UI is up-to-date
            }
        }, 150); // Small delay for visual consistency
    }



    // Optional: Helper for debugging
    private void printGridState(String message) {
        if (candies == null) { Log.d(TAG, message + ": Candies list is null."); return; }
        Log.d(TAG, "GRID STATE: " + message + " (Score: " + gameScore + ")");
        for (int r = 0; r < gridRows; r++) {
            StringBuilder rowStr = new StringBuilder("| ");
            if (r >= candies.size() || candies.get(r) == null) {
                rowStr.append("NULL ROW |");
            } else {
                for (int c = 0; c < gridCols; c++) {
                    Candy candy = getCandyAt(r,c);
                    rowStr.append(candy == null ? "N" : candy.getType()).append(" | ");
                }
            }
            Log.d(TAG, rowStr.toString());
        }
        Log.d(TAG,"---------------------------------");
    }
    public boolean hasAvailableMoves() {
        if (candies == null || gridRows <= 0 || gridCols <= 0) {
            Log.d(TAG, "hasAvailableMoves: Board not ready.");
            return false; // No moves if board isn't set up
        }
        Log.d(TAG, "hasAvailableMoves: Checking for available moves...");

        // Iterate through each cell
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                Candy currentCandy = getCandyAt(r, c);
                if (currentCandy == null) continue; // Skip empty cells if any

                // Try swapping with the candy to the right
                if (c + 1 < gridCols) {
                    if (wouldSwapCreateMatch(r, c, r, c + 1)) {
                        Log.i(TAG, "hasAvailableMoves: Found possible move by swapping (" + r + "," + c + ") with (" + r + "," + (c + 1) + ")");
                        return true;
                    }
                }
                // Try swapping with the candy below
                if (r + 1 < gridRows) {
                    if (wouldSwapCreateMatch(r, c, r + 1, c)) {
                        Log.i(TAG, "hasAvailableMoves: Found possible move by swapping (" + r + "," + c + ") with (" + (r + 1) + "," + c + ")");
                        return true;
                    }
                }
            }
        }

        Log.i(TAG, "hasAvailableMoves: No available moves found on the board.");
        return false; // No moves found
    }

    // MODIFIED: To use findAllMatchGroupsOnBoard for checking potential moves
    private boolean wouldSwapCreateMatch(int r1, int c1, int r2, int c2) {
        if (!isValidCell(r1, c1) || !isValidCell(r2, c2)) {
            Log.v(TAG, "wouldSwapCreateMatch: Invalid cells ("+r1+","+c1+") or ("+r2+","+c2+")");
            return false;
        }

        Candy candy1Original = getCandyAt(r1, c1);
        Candy candy2Original = getCandyAt(r2, c2);

        if (candy1Original == null || candy2Original == null) {
            Log.v(TAG, "wouldSwapCreateMatch: One of the candies is null for swap simulation.");
            return false;
        }

        // Simulate the swap
        candies.get(r1).set(c1, candy2Original);
        candies.get(r2).set(c2, candy1Original);

        // IMPORTANT: For the purpose of hasAvailableMoves, findAllMatchGroupsOnBoard
        // might use lastInteractedPoint. We should simulate the interaction point
        // as the destination of the proposed swap (r2, c2).
        Point originalLIP = lastInteractedPoint; // Save current state of the actual LIP
        lastInteractedPoint = new Point(r2, c2); // Simulate interaction for this check only

        // Check for matches using the new method
        List<MatchGroup> groupsAfterSimulatedSwap = findAllMatchGroupsOnBoard(); // <<< CORRECTED CALL
        boolean matchFound = (groupsAfterSimulatedSwap != null && !groupsAfterSimulatedSwap.isEmpty());

        // Revert the swap to restore original board state - VERY IMPORTANT
        candies.get(r1).set(c1, candy1Original);
        candies.get(r2).set(c2, candy2Original);
        lastInteractedPoint = originalLIP; // Restore the actual lastInteractedPoint

        if (matchFound) {
            Log.v(TAG, "wouldSwapCreateMatch: YES, swapping (" + r1 + "," + c1 + ") with (" + r2 + "," + c2 + ") would create a match.");
        } else {
            // Only log if you are debugging no moves issues, can be very spammy
            // Log.v(TAG, "wouldSwapCreateMatch: NO, swapping (" + r1 + "," + c1 + ") with (" + r2 + "," + c2 + ") would NOT create a match.");
        }
        return matchFound;
    }

    private void checkLevelCompletion() {
        if (!isLevelComplete && currentLevelConfig != null && gameScore >= currentLevelConfig.getTargetScore()) {
            Log.i(TAG, "Level " + currentLevelConfig.getLevelNumber() + " COMPLETED! Score: " + gameScore);
            //isLevelComplete = true;
            levelTimerRunning = false; // Stop the timer if one is running

            // These lines were for the dialog that was previously active.
            // If you are no longer using that dialog directly in GameGridView,
            // you might not need to set these, or you might use them for a
            // listener callback to FeedActivity.
            currentScoreForDialog = gameScore;
            timeTakenMillisForDialog = System.currentTimeMillis() - startTimeMillis;

            // If you intend for FeedActivity to know the level is complete
            // (e.g., to show its own UI or start the next level),
            // you would add a listener call here, for example:
            // if (gameStateListener != null && gameStateListener instanceof YourSpecificListenerInterface) {
            //     ((YourSpecificListenerInterface) gameStateListener).onLevelTargetReached(gameScore, timeTakenMillisForDialog);
            // }

            invalidate(); // Request a redraw, in case the UI should change (e.g., dialog appears)
        }
    }
    private static class MatchGroup {
        Set<Point> points;    // The set of (row, col) points that make up this match
        int length;           // The number of candies in this match (e.g., 3, 4, 5)
        Point creationPoint;  // The specific Point where a special candy might be created from this match

        MatchGroup(Set<Point> points, Point creationPoint) {
            this.points = points;
            this.length = points.size();
            this.creationPoint = creationPoint;
        }

        @Override
        public String toString() {
            // Helpful for debugging
            return "MatchGroup{length=" + length + ", points=" + points + ", creationPt=" + creationPoint + '}';
        }
    }


}
