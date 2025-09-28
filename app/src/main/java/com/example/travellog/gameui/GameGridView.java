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

    private void initBitmaps(Context context) {
        if (NUMBER_OF_CANDY_TYPES <= 0) {
            Log.e(TAG, "CRITICAL ERROR: NUMBER_OF_CANDY_TYPES is not positive.");
            allBitmapsLoadedSuccessfully = false;
            return;
        }
        Log.i(TAG, "initBitmaps: Loading " + NUMBER_OF_CANDY_TYPES + " candy bitmaps.");
        candyBitmaps = new Bitmap[NUMBER_OF_CANDY_TYPES];
        int successfullyLoadedCount = 0;
        String resourceNameBase = "candy_type_";

        for (int i = 0; i < NUMBER_OF_CANDY_TYPES; i++) {
            String targetResourceName = resourceNameBase + i;
            int resourceId = context.getResources().getIdentifier(targetResourceName, "drawable", context.getPackageName());
            if (resourceId != 0) {
                try {
                    candyBitmaps[i] = BitmapFactory.decodeResource(context.getResources(), resourceId);
                    if (candyBitmaps[i] != null) successfullyLoadedCount++;
                    else Log.e(TAG, "initBitmaps: Failed to decode resource for '" + targetResourceName + "'.");
                } catch (Exception e) {
                    Log.e(TAG, "initBitmaps: Exception decoding " + targetResourceName, e);
                }
            } else {
                Log.e(TAG, "initBitmaps: Drawable resource '" + targetResourceName + "' NOT FOUND.");
            }
        }
        allBitmapsLoadedSuccessfully = successfullyLoadedCount == NUMBER_OF_CANDY_TYPES;
        if (!allBitmapsLoadedSuccessfully) {
            Log.e(TAG, "initBitmaps: CRITICAL FAILURE - NOT ALL BITMAPS LOADED. Loaded " + successfullyLoadedCount + "/" + NUMBER_OF_CANDY_TYPES);
        } else {
            Log.i(TAG, "initBitmaps: All candy bitmaps loaded successfully!");
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

    private void initCandiesAndStabilizeBoard() {
        if (!allBitmapsLoadedSuccessfully || currentLevelConfig == null || gridRows <= 0 || gridCols <= 0 || NUMBER_OF_CANDY_TYPES == 0) {
            Log.e(TAG, "Cannot initialize candies - prerequisites not met.");
            candies = new ArrayList<>(); // Ensure candies is empty or handle error
            isBoardSettling = false;
            return;
        }

        candies.clear();
        int[][] layout = currentLevelConfig.getCustomGridLayout();
        boolean useCustomLayout = layout != null && layout.length == gridRows && (gridRows > 0 && layout[0].length == gridCols);

        for (int i = 0; i < gridRows; i++) {
            List<Candy> rowList = new ArrayList<>(gridCols);
            for (int j = 0; j < gridCols; j++) {
                int candyType;
                if (useCustomLayout) {
                    candyType = layout[i][j];
                    if (candyType < 0 || candyType >= NUMBER_OF_CANDY_TYPES) candyType = 0; // Fallback
                } else {
                    candyType = random.nextInt(NUMBER_OF_CANDY_TYPES);
                }
                rowList.add(new Candy(candyType));
            }
            candies.add(rowList);
        }
        Log.i(TAG, "Candies initialized. Size: " + candies.size() + "x" + (candies.isEmpty() ? 0 : candies.get(0).size()));

        gameLoopHandler.post(this::stabilizationLoop);
    }

    private void stabilizationLoop() {
        Set<Point> initialMatches = findAllMatchesOnBoard();
        if (!initialMatches.isEmpty()) {
            Log.d(TAG, "Stabilization: Found " + initialMatches.size() + " initial matches. Processing...");
            removeCandies(initialMatches);
            applyGravity();
            refillBoard();
            invalidate();
            gameLoopHandler.postDelayed(this::stabilizationLoop, 100);
        } else {
            Log.i(TAG, "Board is stable. Initial stabilization complete.");
            isBoardSettling = false;
            invalidate();
            // <<< UNCOMMENTED: After stabilization, check for available moves >>>
            if (gameStateListener != null) {
                if (!hasAvailableMoves()) { // Make sure hasAvailableMoves() is implemented
                    Log.d(TAG, "stabilizationLoop: No moves detected, notifying listener.");
                    gameStateListener.onNoMovesAvailable();
                } else {
                    Log.d(TAG, "stabilizationLoop: Moves available, notifying listener.");
                    gameStateListener.onMovesAvailable();
                }
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
            canvas.drawText("Grid not ready", getWidth() / 2f - 150, getHeight() / 2f, paint);
            return;
        }

        for (int r = 0; r < gridRows; r++) {
            if (candies.get(r) == null) continue;
            for (int c = 0; c < gridCols; c++) {
                Candy currentCandy = getCandyAt(r, c);
                if (currentCandy != null) {
                    int candyType = currentCandy.getType();
                    if (candyType >= 0 && candyType < NUMBER_OF_CANDY_TYPES && candyBitmaps[candyType] != null) {
                        Bitmap candyBitmap = candyBitmaps[candyType];
                        int destLeft = gridOffsetX + c * cellSize;
                        int destTop = gridOffsetY + r * cellSize;
                        Rect destRect = new Rect(destLeft, destTop, destLeft + cellSize, destTop + cellSize);
                        canvas.drawBitmap(candyBitmap, null, destRect, null);
                    } else {
                        paint.setColor(Color.DKGRAY);
                        canvas.drawRect(gridOffsetX + c * cellSize, gridOffsetY + r * cellSize,
                                gridOffsetX + (c + 1) * cellSize, gridOffsetY + (r + 1) * cellSize, paint);
                    }
                }
                paint.setStyle(Paint.Style.STROKE); paint.setColor(Color.GRAY); paint.setStrokeWidth(1);
                canvas.drawRect(gridOffsetX + c * cellSize, gridOffsetY + r * cellSize,
                        gridOffsetX + (c + 1) * cellSize, gridOffsetY + (r + 1) * cellSize, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        if (selectedRow != -1 && selectedCol != -1) {
            canvas.drawRect(gridOffsetX + selectedCol * cellSize, gridOffsetY + selectedRow * cellSize,
                    gridOffsetX + (selectedCol + 1) * cellSize, gridOffsetY + (selectedRow + 1) * cellSize,
                    highlightPaint);
        }
        if (isLevelComplete) drawLevelCompleteDialog(canvas);
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

    private void handleCellTouch(int row, int col) {
        Candy touchedCandy = getCandyAt(row, col);
        if (touchedCandy == null) {
            if (selectedRow != -1) {
                selectedRow = -1; selectedCol = -1; selectedCandyObject = null; invalidate();
            }
            return;
        }

        if (selectedRow == -1) {
            selectedRow = row; selectedCol = col; selectedCandyObject = touchedCandy;
        } else {
            if (selectedRow == row && selectedCol == col) {
                selectedRow = -1; selectedCol = -1; selectedCandyObject = null;
            } else if (isAdjacent(row, col, selectedRow, selectedCol)) {
                int tempR = selectedRow, tempC = selectedCol;
                selectedRow = -1; selectedCol = -1; selectedCandyObject = null;

                swapCandies(tempR, tempC, row, col);
                if (checkAndProcessMatchesAfterSwap(tempR, tempC, row, col)) {
                    // Match found and processed
                } else {
                    Log.d(TAG, "No match from swap, swapping back.");
                    swapCandies(row, col, tempR, tempC);
                }
            } else {
                selectedRow = row; selectedCol = col; selectedCandyObject = touchedCandy;
            }
        }
        invalidate();
    }

    private boolean checkAndProcessMatchesAfterSwap(int r1, int c1, int r2, int c2) {
        isBoardSettling = true;
        Set<Point> matches = findAllMatchesOnBoard();

        if (!matches.isEmpty()) {
            Log.i(TAG, "Swap created " + matches.size() + " matches.");
            processMatchesAndContinueLoop(matches);
            return true;
        } else {
            isBoardSettling = false;
            return false;
        }
    }


    private void processMatchesAndContinueLoop(Set<Point> matchesToProcess) {
        if (matchesToProcess.isEmpty()) {
            isBoardSettling = false;
            checkLevelCompletion(); // <<< USES YOUR METHOD NAME
            invalidate();
            // <<< UNCOMMENTED: After board processing is complete, check for available moves >>>
            if (gameStateListener != null) {
                if (!hasAvailableMoves()) { // Make sure hasAvailableMoves() is implemented
                    Log.d(TAG, "processMatchesAndContinueLoop: No moves detected, notifying listener.");
                    gameStateListener.onNoMovesAvailable();
                } else {
                    Log.d(TAG, "processMatchesAndContinueLoop: Moves available, notifying listener.");
                    gameStateListener.onMovesAvailable();
                }
            }
            return;
        }

        // removeCandies, applyGravity, refillBoard, and postDelayed call remain the same
        removeCandies(matchesToProcess);
        gameScore += matchesToProcess.size() * 10;
        Log.d(TAG, "Score: " + gameScore);

        applyGravity();
        refillBoard();
        invalidate();

        gameLoopHandler.postDelayed(() -> {
            Set<Point> newMatches = findAllMatchesOnBoard();
            processMatchesAndContinueLoop(newMatches);
        }, 150);
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

    private Set<Point> findAllMatchesOnBoard() {
        Set<Point> matchedPoints = new HashSet<>();
        if (candies == null || gridRows <= 0 || gridCols <= 0) return matchedPoints;

        // Check horizontal matches
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols - 2; ) {
                Candy first = getCandyAt(r, c);
                if (first == null) { c++; continue; }
                if (getCandyAt(r, c + 1) != null && getCandyAt(r, c + 1).getType() == first.getType() &&
                        getCandyAt(r, c + 2) != null && getCandyAt(r, c + 2).getType() == first.getType()) {
                    int matchEnd = c + 2;
                    while (matchEnd + 1 < gridCols && getCandyAt(r, matchEnd + 1) != null && getCandyAt(r, matchEnd + 1).getType() == first.getType()) {
                        matchEnd++;
                    }
                    for (int i = c; i <= matchEnd; i++) matchedPoints.add(new Point(r, i));
                    c = matchEnd + 1;
                } else {
                    c++;
                }
            }
        }
        // Check vertical matches
        for (int c = 0; c < gridCols; c++) {
            for (int r = 0; r < gridRows - 2; ) {
                Candy first = getCandyAt(r, c);
                if (first == null) { r++; continue; }
                if (getCandyAt(r + 1, c) != null && getCandyAt(r + 1, c).getType() == first.getType() &&
                        getCandyAt(r + 2, c) != null && getCandyAt(r + 2, c).getType() == first.getType()) {
                    int matchEnd = r + 2;
                    while (matchEnd + 1 < gridRows && getCandyAt(matchEnd + 1, c) != null && getCandyAt(matchEnd + 1, c).getType() == first.getType()) {
                        matchEnd++;
                    }
                    for (int i = r; i <= matchEnd; i++) matchedPoints.add(new Point(i, c));
                    r = matchEnd + 1;
                } else {
                    r++;
                }
            }
        }
        return matchedPoints;
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
        if (candies == null || NUMBER_OF_CANDY_TYPES <= 0) return;
        for (int c = 0; c < gridCols; c++) {
            for (int r = 0; r < gridRows; r++) {
                if (getCandyAt(r, c) == null) {
                    candies.get(r).set(c, new Candy(random.nextInt(NUMBER_OF_CANDY_TYPES)));
                }
            }
        }
    }



    // <<< NEW: shuffleBoard() method (placeholder for now) >>>
    public void shuffleBoard() {
        Log.d(TAG, "shuffleBoard() called. Actual shuffling logic not yet implemented.");
        if (candies == null || candies.isEmpty() || gridRows <= 0 || gridCols <= 0) {
            Log.w(TAG, "shuffleBoard: Cannot shuffle, board not ready.");
            return;
        }

        // The actual logic to collect, shuffle, and reassign candies will go here in the next step.
        // For now, just log and redraw.
        isBoardSettling = true; // Prevent interaction during shuffle (conceptually)
        // printGridState("Before shuffle (placeholder)"); // Optional: for debugging

        // Placeholder for real shuffle logic
        // List<Candy> allCandiesOnBoard = new ArrayList<>();
        // for (int r = 0; r < gridRows; r++) {
        // for (int c = 0; c < gridCols; c++) {
        // if (getCandyAt(r, c) != null) {
        // allCandiesOnBoard.add(getCandyAt(r, c));
        // }
        // }
        // }
        // Collections.shuffle(allCandiesOnBoard);
        // int index = 0;
        // for (int r = 0; r < gridRows; r++) {
        // for (int c = 0; c < gridCols; c++) {
        // if (index < allCandiesOnBoard.size()) { // Check to prevent IndexOutOfBounds
        // candies.get(r).set(c, allCandiesOnBoard.get(index++));
        // } else {
        // candies.get(r).set(c, null); // Should not happen if counts match
        // }
        // }can
        // }

        invalidate(); // Force a redraw to show the (eventually) shuffled board
        // printGridState("After shuffle (placeholder)"); // Optional: for debugging

        // After shuffling, the board is considered "settled" from the shuffle operation itself.
        // Then, immediately check for new matches created by the shuffle, or available moves.
        gameLoopHandler.postDelayed(() -> {
            Set<Point> matchesAfterShuffle = findAllMatchesOnBoard();
            if (!matchesAfterShuffle.isEmpty()) {
                Log.d(TAG, "Shuffle created immediate matches. Processing them.");
                processMatchesAndContinueLoop(matchesAfterShuffle); // This will handle settling and further checks
            } else {
                Log.d(TAG, "Shuffle did not create immediate matches.");
                isBoardSettling = false; // Board is stable if no matches from shuffle
                // Now check for available moves
                // if (gameStateListener != null) {
                // if (!hasAvailableMoves()) { // hasAvailableMoves() is not yet implemented
                // gameStateListener.onNoMovesAvailable();
                // } else {
                // gameStateListener.onMovesAvailable();
                // }
                // }
            }
            invalidate(); // Ensure UI is up-to-date
        }, 150); // Small delay for visual consistency if matches occur
    }
    // <<< END NEW >>>


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

    private boolean wouldSwapCreateMatch(int r1, int c1, int r2, int c2) {
        if (!isValidCell(r1, c1) || !isValidCell(r2, c2)) {
            return false;
        }
        Candy candy1Original = getCandyAt(r1, c1);
        Candy candy2Original = getCandyAt(r2, c2);

        if (candy1Original == null || candy2Original == null) {
            // Cannot determine move if one of the spots is empty in this context,
            // unless your game logic allows swapping with empty spaces to form matches.
            // For typical match-3, this would not be a move.
            return false;
        }

        // Simulate the swap
        candies.get(r1).set(c1, candy2Original);
        candies.get(r2).set(c2, candy1Original);

        // Check for matches
        boolean matchFound = !findAllMatchesOnBoard().isEmpty(); // If any match is found

        // Revert the swap to restore original board state - VERY IMPORTANT
        candies.get(r1).set(c1, candy1Original);
        candies.get(r2).set(c2, candy2Original);

        return matchFound;
    }
    private void checkLevelCompletion() {
        if (!isLevelComplete && currentLevelConfig != null && gameScore >= currentLevelConfig.getTargetScore()) {
            Log.i(TAG, "Level " + currentLevelConfig.getLevelNumber() + " COMPLETED! Score: " + gameScore);
            isLevelComplete = true;
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

}
