package com.example.CandyCrush.gameui;

import android.animation.ValueAnimator;
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
import android.view.animation.LinearInterpolator;
import com.example.CandyCrush.FeedActivity;

import androidx.annotation.Nullable;

import com.example.CandyCrush.gamecore.Candy;
import com.example.CandyCrush.gamecore.LevelConfig;

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
    private int currentCollectedTargetCount = 0; // מעקב אחרי איסוף לצורך המשימה
    private int remainingMoves = 0; // מהלכים שנותרו לשחקן
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
    private boolean hasPlayerInteracted = false;
    private Handler gameLoopHandler = new Handler(Looper.getMainLooper());

    // <<< NEW: GameStateListener interface and variable >>>
    private GameStateListener gameStateListener;
    private Point lastInteractedPoint = null;

    //animations
    private List<AnimationInfo> activeAnimations = new ArrayList<>();
    private ValueAnimator animationDriver;
    private int targetScore;
    private int movesRemaining;
    private int gridSize;

    public interface GameStateListener {
        void onScoreChanged(int newScore);
        void onMissionUpdate(int current, int target, int movesRemaining); // שורה חדשה
        void onNoMovesAvailable();
        void onMovesAvailable();
    }
    public void setTargetScore(int targetScore) {
        this.targetScore = targetScore;
        Log.d("GameGridView", "Target score set to: " + targetScore);
    }

    // פונקציה להגדרת כמות המהלכים
    public void setMovesRemaining(int movesLimit) {
        this.movesRemaining = movesLimit;
        // אם יש לך UI בתוך ה-View שמעדכן מהלכים, קרא לו כאן
        invalidate(); // גורם ללוח להצטייר מחדש אם צריך
    }
    public void setupGrid(int size) {
        this.gridSize = size;
        // כאן אתה צריך להוסיף את הלוגיקה שבונה את הלוח (initGrid)
        // אם כבר יש לך פונקציה כזו, פשוט קרא לה:
        //initGrid(size);
    }

    public void setupGridFromRemote(LevelConfig config) {
        if (config == null) {
            Log.e(TAG, "setupGridFromRemote: config is null.");
            return;
        }

        Log.d(TAG, "setupGridFromRemote: Setting up remote level " + config.getLevelNumber());
        isLevelComplete = false;
        gameScore = 0;
        startTimeMillis = System.currentTimeMillis();
        levelTimerRunning = true;
        isBoardSettling = true;

        currentLevelConfig = config;
        this.gridRows = config.getRows();
        this.gridCols = config.getCols();
        this.gridSize = Math.max(gridRows, gridCols);

        this.remainingMoves = config.getMaxMoves();
        this.currentCollectedTargetCount = 0;

        if (gridRows <= 0) this.gridRows = 1;
        if (gridCols <= 0) this.gridCols = 1;

        initCandiesAndStabilizeBoard();
        checkGameStatus();

        selectedRow = -1;
        selectedCol = -1;
        selectedCandyObject = null;
        requestLayout();
        invalidate();
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

    // In GameGridView.java

    private void init(Context context) {
        // --- Initialize all the Paint objects first ---
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#E0E0E0"));

        highlightPaint = new Paint();
        highlightPaint.setColor(Color.YELLOW);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(8);
        highlightPaint.setAntiAlias(true);

        dialogBackgroundPaint = new Paint();
        dialogBackgroundPaint.setColor(Color.argb(220, 0, 0, 0));
        dialogBackgroundPaint.setStyle(Paint.Style.FILL);

        dialogTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialogTextPaint.setColor(Color.WHITE);
        dialogTextPaint.setTextAlign(Paint.Align.CENTER);

        dialogScoreTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialogScoreTextPaint.setColor(Color.rgb(127, 255, 0));
        dialogScoreTextPaint.setTextAlign(Paint.Align.CENTER);

        // --- Initialize game data and bitmaps ---
        candies = new ArrayList<>();
        initBitmaps(context);

        // --- Initialize the Animation Driver ---
        animationDriver = ValueAnimator.ofFloat(0f, 1f);
        animationDriver.setDuration(1000); // A long duration, it will repeat.
        animationDriver.setRepeatCount(ValueAnimator.INFINITE);
        animationDriver.setInterpolator(new LinearInterpolator()); // This should now work with the import
        animationDriver.addUpdateListener(animator -> {
            // This code runs on every animation frame
            if (!activeAnimations.isEmpty()) {
                invalidate(); // If there are active animations, force a redraw
            }
        });
    }
    /**
     * Adds points to the total game score and handles combo multipliers.
     * This is the central method for all scoring logic.
     *
     * @param numberOfCandies The number of candies being cleared.
     * @param isCombo A boolean that is true if this match is part of a cascading combo.
     */
    private void addScore(int numberOfCandies, boolean isCombo) {
        int basePointsPerCandy = 100;
        int pointsToAdd = numberOfCandies * basePointsPerCandy;

        if (isCombo) {
            pointsToAdd *= 2; // Double points for combos!
            Log.d(TAG, "Applying COMBO bonus! Points doubled to: " + pointsToAdd);
        }

        this.gameScore += pointsToAdd;
        Log.d(TAG, "Score added: " + pointsToAdd + ". New total score: " + this.gameScore);

        // --- NEW: NOTIFY THE ACTIVITY ABOUT THE SCORE CHANGE ---
        if (gameStateListener != null) {
            gameStateListener.onScoreChanged(this.gameScore);
        }

        // We no longer need to invalidate() just for the score text drawn in this view.
        // invalidate(); // This call is no longer essential for the score part.
    }

    /**
     * Processes a valid player swap, scores the initial match, and starts the stabilization process.
     * @param swappedFrom The point where the swap originated.
     * @param swappedTo The point where the candy was moved to.
     */
    private void processPlayerSwap(Point swappedFrom, Point swappedTo) {
        // ביצוע ההחלפה בפועל בלוח הנתונים
        Candy candy1 = getCandyAt(swappedFrom.r, swappedFrom.c);
        Candy candy2 = getCandyAt(swappedTo.r, swappedTo.c);
        candies.get(swappedFrom.r).set(swappedFrom.c, candy2);
        candies.get(swappedTo.r).set(swappedTo.c, candy1);

        Log.d(TAG, "Processing player swap between " + swappedFrom + " and " + swappedTo);
        isBoardSettling = true; // חוסם נגיעות בזמן העיבוד

        // מציאת כל ההתאמות שנוצרו בעקבות ההחלפה
        Set<Point> pointsToClear = new HashSet<>();
        List<MatchGroup> matchGroups = findAllMatchGroupsOnBoard();
        if (matchGroups != null && !matchGroups.isEmpty()) {
            for (MatchGroup group : matchGroups) {
                pointsToClear.addAll(group.points);
            }
        }

        if (!pointsToClear.isEmpty()) {
            // --- החלפה מוצלחת! ---

            // 1. הורדת מהלך אחד מהשחקן
            if (remainingMoves > 0) {
                remainingMoves--;
            }

            // 2. הוספת ניקוד
            addScore(pointsToClear.size(), false);

            // 3. עדכון הממשק (הטקסט הכתום והאדום) מיד
            checkGameStatus();

            // 4. התחלת תהליך הפיצוץ והנפילה
            animateAndRemoveCandies(pointsToClear);

        } else {
            // אם ההחלפה לא יצרה מאץ' - מחזירים חזרה (Swap Back)
            Log.d(TAG, "Invalid move. Swapping back.");
            candies.get(swappedFrom.r).set(swappedFrom.c, candy1);
            candies.get(swappedTo.r).set(swappedTo.c, candy2);
            isBoardSettling = false; // שחרור הלוח לנגיעות
        }

        // איפוס הבחירה
        selectedRow = -1;
        selectedCol = -1;
        selectedCandyObject = null;
        invalidate();
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
                targetResourceName = "mega_bomb";
            } else if (i == Candy.TYPE_ROCKET) { // <<< ADD THIS BLOCK
                targetResourceName = "rocket";// Expects mega_bomb.png
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
        isBoardSettling = true;
        hasPlayerInteracted = false;

        currentLevelConfig = LevelConfig.getConfigForLevel(levelNumber);
        if (currentLevelConfig == null) {
            Log.e(TAG, "Failed to get LevelConfig for level: " + levelNumber + ". Using fallback.");
            this.gridRows = 5; this.gridCols = 5;
            this.currentLevelConfig = new LevelConfig(levelNumber, this.gridRows, this.gridCols, 500);
        } else {
            this.gridRows = currentLevelConfig.getRows();
            this.gridCols = currentLevelConfig.getCols();

            // --- הוסף את השורות האלו כאן ---
            this.remainingMoves = currentLevelConfig.getMaxMoves();
            this.currentCollectedTargetCount = 0;
            // ------------------------------
        }

        if (gridRows <= 0) this.gridRows = 1;
        if (gridCols <= 0) this.gridCols = 1;

        initCandiesAndStabilizeBoard();

        // עדכון ראשוני של הטקסטים במסך (אדום וכתום)
        checkGameStatus();

        selectedRow = -1; selectedCol = -1; selectedCandyObject = null;
        requestLayout();
        invalidate();
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
                    if (candyType == -1) {
                        candyType = random.nextInt(Candy.NUMBER_OF_REGULAR_CANDY_TYPES);
                    }
                    // Validate custom layout types against *all* loaded sprites
                    if (candyType < 0 || candyType >= Candy.TOTAL_NUMBER_OF_SPRITES) {
                        Log.w(TAG, "Custom layout type " + candyType + " out of bounds for loaded sprites. Defaulting to regular candy.");
                        candyType = random.nextInt(Candy.NUMBER_OF_REGULAR_CANDY_TYPES);                    }
                } else {
                    // For random fill, generate ONLY regular candy types
                    candyType = random.nextInt(Candy.NUMBER_OF_REGULAR_CANDY_TYPES);
                }
                rowList.add(new Candy(candyType));
            }
            candies.add(rowList);
        }
        Log.i(TAG, "Candies initialized. Size: " + candies.size() + "x" + (candies.isEmpty() ? 0 : candies.get(0).size()));

        if (hasPlayerInteracted) {
            isBoardSettling = true; // Board needs to stabilize after initialization
            gameLoopHandler.post(this::stabilizationLoop);
        } else {
            isBoardSettling = false;
            finishStabilization();
        }
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

// ... inside stabilizationLoop
            if (!pointsToClear.isEmpty()) {
                // --- SCORE THE COMBO MATCH ---// 'true' because these matches are part of a cascade.
                addScore(pointsToClear.size(), true);

                // Animate the destruction of these candies
                animateAndRemoveCandies(pointsToClear);
                // The loop will continue after the animations are finished.

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

        // --- Initial Check ---
        if (cellSize <= 0 || !allBitmapsLoadedSuccessfully) {
            paint.setColor(Color.RED);
            paint.setTextSize(50);
            String message = !allBitmapsLoadedSuccessfully ? "Bitmaps loading error!" : "Grid not ready";
            float textWidth = paint.measureText(message);
            canvas.drawText(message, (getWidth() - textWidth) / 2f, getHeight() / 2f, paint);
            return;
        }

        // --- 1. PREPARE FOR DRAWING ---
        // Create a set of points that are being animated to avoid double-drawing.
        Set<Point> animatedPoints = new HashSet<>();
        for (AnimationInfo anim : activeAnimations) {
            // A shrinking candy is still at its point until it's gone.
            // A falling candy "occupies" its destination cell, so we don't draw a static candy there.
            animatedPoints.add(anim.point);
        }

        // --- 2. DRAW THE STATIC CANDY GRID ---
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                // Draw grid lines for the background cell
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.GRAY);
                paint.setStrokeWidth(1);
                canvas.drawRect(gridOffsetX + c * cellSize, gridOffsetY + r * cellSize,
                        gridOffsetX + (c + 1) * cellSize, gridOffsetY + (r + 1) * cellSize, paint);
                paint.setStyle(Paint.Style.FILL);

                // Only draw the candy from the main grid if it's NOT being animated.
                if (!animatedPoints.contains(new Point(r, c))) {
                    Candy currentCandy = getCandyAt(r, c);
                    if (currentCandy != null) {
                        drawCandy(canvas, currentCandy, r, c, 1.0f); // Draw at full size
                    }
                }
            }
        }

        // --- 3. DRAW ACTIVE ANIMATIONS ---
        java.util.Iterator<AnimationInfo> iterator = activeAnimations.iterator();
        while (iterator.hasNext()) {
            AnimationInfo anim = iterator.next();
            float progress = anim.getProgress();

            if (anim.type == AnimationInfo.AnimationType.SHRINK_FADE_OUT) {
                float scale = 1.0f - progress;
                drawCandy(canvas, anim.candy, anim.point.r, anim.point.c, scale);
            } else if (anim.type == AnimationInfo.AnimationType.FALL) {
                float startY = gridOffsetY + anim.fallStartPoint.r * cellSize;
                float endY = gridOffsetY + anim.point.r * cellSize;
                float currentY = startY + (endY - startY) * progress;
                float x = gridOffsetX + anim.point.c * cellSize;
                drawCandyAtPosition(canvas, anim.candy, x, currentY, 1.0f);
            }

            // If animation is finished, handle its completion and remove it.
            if (progress >= 1.0f) {
                handleAnimationCompletion(anim);
                iterator.remove(); // Safely remove from the list
            }
        }

        // --- 4. DRAW SELECTION HIGHLIGHT ---
        if (selectedRow != -1 && selectedCol != -1) {
            canvas.drawRect(gridOffsetX + selectedCol * cellSize, gridOffsetY + selectedRow * cellSize,
                    gridOffsetX + (selectedCol + 1) * cellSize, gridOffsetY + (selectedRow + 1) * cellSize,
                    highlightPaint);
        }


        // --- 6. DRAW LEVEL COMPLETE DIALOG ---
        if (isLevelComplete) {
            drawLevelCompleteDialog(canvas);
        }
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

        if (isLevelComplete) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (getContext() instanceof FeedActivity) {
                    ((FeedActivity) getContext()).handleLevelCompleteNavigation();
                } else {
                    Log.e(TAG, "Context is not FeedActivity!");
                }
                return true;
            }
            return true;
        }


        if (!allBitmapsLoadedSuccessfully || candies == null || candies.isEmpty() || cellSize == 0) {
            return super.onTouchEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isBoardSettling) {
                Log.d(TAG, "onTouchEvent: Ignoring touch while board is settling.");
                return true;
            }
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

    // MODIFIED: To handle direct bomb taps and correctly set lastInteractedPoint
    // MODIFIED: To select Bomb on first tap, explode on second.
    // Replace your entire existing handleCellTouch method with this one.
    private boolean hasImmediateMatches() {
        Point originalInteraction = lastInteractedPoint;
        lastInteractedPoint = null;
        List<MatchGroup> lineMatches = findAllMatchGroupsOnBoard();
        boolean hasLineMatch = lineMatches != null && !lineMatches.isEmpty();
        boolean hasSquareMatch = !findSquareMatches().isEmpty();
        lastInteractedPoint = originalInteraction;
        return hasLineMatch || hasSquareMatch;
    }
    private void handleCellTouch(int row, int col) {
        Log.d(TAG, "handleCellTouch: Touched cell (" + row + "," + col + ")");

        // Ignore touches if the board is settling from a previous move.
        if (isBoardSettling) {
            Log.d(TAG, "handleCellTouch: Board is settling, ignoring touch.");
            return;
        }

        if (!hasPlayerInteracted) {
            hasPlayerInteracted = true;
            if (hasImmediateMatches()) {
                Log.d(TAG, "handleCellTouch: First interaction triggers stabilization.");
                isBoardSettling = true;
                stabilizationLoop();
                return;
            }
        }


        Candy touchedCandy = getCandyAt(row, col);

        if (touchedCandy == null) {
            Log.w(TAG, "handleCellTouch: Touched null candy at (" + row + "," + col + "), possibly an empty space.");
            if (selectedRow != -1) { // If a candy was selected, deselect it.
                Log.d(TAG, "handleCellTouch: Deselecting due to touch on empty space.");
                selectedRow = -1;
                selectedCol = -1;
                selectedCandyObject = null;
                lastInteractedPoint = null; // Clear interaction on deselect.
                invalidate();
            }
            return;
        }

        if (selectedRow == -1) {
            // --- 1. FIRST TAP: SELECT THE CANDY ---
            // No candy currently selected, so select this one.
            selectedRow = row;
            selectedCol = col;
            selectedCandyObject = touchedCandy;
            lastInteractedPoint = new Point(row, col);
            Log.d(TAG, "handleCellTouch: Selected candy (type: " + touchedCandy.getType() + ") at (" + row + "," + col + ")");

        } else {
            // --- 2. A CANDY IS ALREADY SELECTED ---

            if (selectedRow == row && selectedCol == col) {
                // --- 2A. TAPPED THE SAME CANDY AGAIN ---
                Log.d(TAG, "handleCellTouch: Tapped the selected candy again at (" + row + "," + col + ")");

                if (selectedCandyObject != null && selectedCandyObject.isBomb()) {
                    // If the re-tapped candy is a Bomb, explode it.
                    Log.i(TAG, "Bomb re-tapped at (" + row + "," + col + "). Triggering explosion.");
                    isBoardSettling = true; // Board will change.
                    triggerBombExplosion(row, col, 3); // Standard 3x3 explosion.
                } else if (selectedCandyObject != null && selectedCandyObject.isRocket()) {
                    // If the re-tapped candy is a Rocket, fire it.
                    Log.i(TAG, "Rocket re-tapped at (" + row + "," + col + "). Triggering row/col clear.");
                    isBoardSettling = true;
                    triggerRocketEffect(row, col);
                }

                // Deselect after the action (or if no action was taken).
                Log.d(TAG, "handleCellTouch: Deselecting candy at (" + row + "," + col + ")");
                selectedRow = -1;
                selectedCol = -1;
                selectedCandyObject = null;
                lastInteractedPoint = null;

            } else if (isAdjacent(row, col, selectedRow, selectedCol)) {
                // --- 2B. TAPPED AN ADJACENT CANDY (A SWAP ATTEMPT) ---
                Candy firstCandy = getCandyAt(selectedRow, selectedCol);
                Candy secondCandy = getCandyAt(row, col);
                boolean specialSwapHandled = true; // Assume a special swap will be handled.

                // --- MEGA BOMB & SPECIAL SWAP LOGIC ---
                if (firstCandy.isMegaBomb() && secondCandy.isMegaBomb()) {
                    triggerMegaBombBoardClear();
                } else if ((firstCandy.isMegaBomb() && secondCandy.isBomb())) {
                    triggerBombExplosion(row, col, 5); // 5x5 at the Bomb's location.
                } else if ((secondCandy.isMegaBomb() && firstCandy.isBomb())) {
                    triggerBombExplosion(selectedRow, selectedCol, 5); // 5x5 at the Bomb's location.
                } else if (firstCandy.isMegaBomb() && secondCandy.isRegularCandy()) {
                    triggerMegaBombColorClear(firstCandy, secondCandy);
                } else if (secondCandy.isMegaBomb() && firstCandy.isRegularCandy()) {
                    triggerMegaBombColorClear(secondCandy, firstCandy);
                } else if ((firstCandy.isMegaBomb() && secondCandy.isRocket())) {
                    triggerMegaRocketEffect(row, col); // Trigger at the rocket's location.
                } else if ((secondCandy.isMegaBomb() && firstCandy.isRocket())) {
                    triggerMegaRocketEffect(selectedRow, selectedCol); // Trigger at the rocket's location.
                } else {
                    specialSwapHandled = false; // No special swap combination was found.
                }
                // --- END SPECIAL SWAP LOGIC ---

                if (specialSwapHandled) {
                    // A special swap was handled, so deselect and settle the board.
                    Log.d(TAG, "handleCellTouch: Special swap handled.");
                    deselectAndSettleBoard(); // A helper to clean up state.
                } else {
                    // --- REGULAR SWAP LOGIC ---
                    // If no special swap was handled, perform a regular swap check.
                    Log.d(TAG, "handleCellTouch: No special swap. Attempting regular swap.");
                    processPlayerSwap(selectedRow, selectedCol, row, col);
                }

            } else {
                // --- 2C. TAPPED A NON-ADJACENT CANDY ---
                // Deselect the old one, select this new one.
                Log.d(TAG, "handleCellTouch: Tapped non-adjacent. Deselecting old, selecting new at (" + row + "," + col + ")");
                selectedRow = row;
                selectedCol = col;
                selectedCandyObject = touchedCandy;
                lastInteractedPoint = new Point(row, col);
            }
        }
        invalidate(); // Redraw the board to show selection/deselection highlights.
    }

    /**
     * Processes a valid player swap, scores the initial match, creates special candies,
     * and starts the stabilization process. If the swap results in no match, it reverts the swap.
     */
    private void processPlayerSwap(int r1, int c1, int r2, int c2) {
        Log.d(TAG, "Attempting to swap (" + r1 + "," + c1 + ") with (" + r2 + "," + c2 + ")");
        isBoardSettling = true; // Prevent player input during processing

        // Perform the swap in the data grid
        Candy candy1 = getCandyAt(r1, c1);
        Candy candy2 = getCandyAt(r2, c2);
        candies.get(r1).set(c1, candy2);
        candies.get(r2).set(c2, candy1);

        // After swapping, check if any matches were formed
        List<MatchGroup> matchGroups = findAllMatchGroupsOnBoard();

        if (matchGroups == null || matchGroups.isEmpty()) {
            // --- INVALID SWAP: NO MATCHES ---
            Log.w(TAG, "Invalid swap: No matches formed. Swapping back.");
            // Swap back immediately
            candies.get(r1).set(c1, candy1);
            candies.get(r2).set(c2, candy2);
            isBoardSettling = false; // Allow player input again

        } else {
            // --- VALID SWAP: MATCHES FOUND ---
            Log.i(TAG, "Valid swap! Found " + matchGroups.size() + " match group(s).");
            Set<Point> pointsToClear = new HashSet<>();
            Point specialCandyCreationPoint = null;

            // --- SPECIAL CANDY CREATION LOGIC ---
            // Find the best match group created by the player's swap to create a special candy
            MatchGroup bestMatchForSpecial = null;
            for (MatchGroup group : matchGroups) {
                if (group.points.contains(new Point(r1, c1)) || group.points.contains(new Point(r2, c2))) {
                    if (bestMatchForSpecial == null || group.length > bestMatchForSpecial.length) {
                        bestMatchForSpecial = group;
                    }
                }
            }

            if (bestMatchForSpecial != null) {
                if (bestMatchForSpecial.length >= 5) {
                    Log.i(TAG, "Creating MEGA BOMB from 5+ match.");
                    specialCandyCreationPoint = new Point(r2, c2); // Create at destination of swap
                    candies.get(r2).set(c2, new Candy(Candy.TYPE_MEGA_BOMB));
                } else if (bestMatchForSpecial.length == 4) {
                    Log.i(TAG, "Creating BOMB from 4-match.");
                    specialCandyCreationPoint = new Point(r2, c2); // Create at destination of swap
                    candies.get(r2).set(c2, new Candy(Candy.TYPE_BOMB));
                }
            }
            // --- END SPECIAL CANDY LOGIC ---

            // Collect all points to be cleared from all found matches
            for (MatchGroup group : matchGroups) {
                pointsToClear.addAll(group.points);
            }

            // If we created a special candy, don't clear it this turn
            if (specialCandyCreationPoint != null) {
                pointsToClear.remove(specialCandyCreationPoint);
            }

            // --- SCORE THE INITIAL MATCH ---
            addScore(pointsToClear.size(), false); // 'false' because this is the first move

            // Animate the destruction and start the cascade
            animateAndRemoveCandies(pointsToClear);
        }

        // Deselect candies after the swap attempt
        deselectAndSettleBoard();
    }

    /**
     * Handles the logic for a bomb explosion. It identifies all candies in a 3x3 area
     * and passes them to the main game loop for processing.
     * @param bombRow The row of the exploding bomb.
     * @param bombCol The column of the exploding bomb.
     */
    private void triggerBombExplosion(int bombRow, int bombCol, int size) {
        Log.i(TAG, "BOOM! Preparing " + size + "x" + size + " explosion for bomb at (" + bombRow + "," + bombCol + ").");

        Set<Point> explosionArea = new HashSet<>();
        int radius = (size - 1) / 2; // For 3x3, radius is 1. For 5x5, radius is 2.

        // Add all valid cells in the grid around the bomb to the set.
        for (int r = bombRow - radius; r <= bombRow + radius; r++) {
            for (int c = bombCol - radius; c <= bombCol + radius; c++) {
                if (isValidCell(r, c)) {
                    explosionArea.add(new Point(r, c));
                }
            }
        }


        Log.d(TAG, "Bomb effect area covers " + explosionArea.size() + " points.");

        // Package points into a MatchGroup to use the existing game loop.
        MatchGroup explosionEffectGroup = new MatchGroup(explosionArea, new Point(bombRow, bombCol));
        List<MatchGroup> effectGroups = new ArrayList<>();
        effectGroups.add(explosionEffectGroup);

        // Call the main processing loop.
        processMatchesAndContinueLoop(effectGroups);

        // Invalidate to ensure the board redraws.
        invalidate();
    }
    private void triggerRocketEffect(int row, int col) {
        Log.i(TAG, "ROCKET ACTION: Clearing row " + row + " and column " + col);
        Set<Point> pointsToClear = new HashSet<>();

        // Add all candies in the column
        for (int r = 0; r < gridRows; r++) {
            pointsToClear.add(new Point(r, col));
        }

        // Add all candies in the row
        for (int c = 0; c < gridCols; c++) {
            pointsToClear.add(new Point(row, c));
        }

        // Use the standard game loop to process the clearance
        MatchGroup effectGroup = new MatchGroup(pointsToClear, new Point(row, col));
        List<MatchGroup> effectList = new ArrayList<>();
        effectList.add(effectGroup);
        processMatchesAndContinueLoop(effectList);
    }
    // In GameGridView.java

    /**
     * Triggers the Mega Bomb + Rocket swap effect.* Clears 3 rows and 3 columns centered on the rocket's original position.
     * @param rocketRow The row where the rocket was.
     * @param rocketCol The column where the rocket was.
     */
    private void triggerMegaRocketEffect(int rocketRow, int rocketCol) {
        Log.i(TAG, "MEGA-ROCKET ACTION: Clearing 3 rows and 3 columns around (" + rocketRow + "," + rocketCol + ")");
        Set<Point> pointsToClear = new HashSet<>();

        // Add 3 columns: the rocket's, the one to the left, and the one to the right
        for (int c = rocketCol - 1; c <= rocketCol + 1; c++) {
            if (c >= 0 && c < gridCols) { // Check if column is valid
                for (int r = 0; r < gridRows; r++) {
                    pointsToClear.add(new Point(r, c));
                }
            }
        }

        // Add 3 rows: the rocket's, the one above, and the one below
        for (int r = rocketRow - 1; r <= rocketRow + 1; r++) {
            if (r >= 0 && r < gridRows) { // Check if row is valid
                for (int c = 0; c < gridCols; c++) {
                    pointsToClear.add(new Point(r, c));
                }
            }
        }

        // Use the standard game loop to process the clearance
        MatchGroup effectGroup = new MatchGroup(pointsToClear, new Point(rocketRow, rocketCol));
        List<MatchGroup> effectList = new ArrayList<>();
        effectList.add(effectGroup);
        processMatchesAndContinueLoop(effectList);
    }

    private void triggerMegaBombBoardClear() {
        Log.i(TAG, "MEGA BOMB ACTION: Mega + Mega swap. Clearing entire board!");

        Set<Point> pointsToClear = new HashSet<>();
        // Add every single valid cell on the grid to the clear list
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                if (getCandyAt(r, c) != null) {
                    pointsToClear.add(new Point(r, c));
                }
            }
        }

        // Package into a MatchGroup and process
        MatchGroup effectGroup = new MatchGroup(pointsToClear, new Point(selectedRow, selectedCol));
        List<MatchGroup> effectList = new ArrayList<>();
        effectList.add(effectGroup);
        processMatchesAndContinueLoop(effectList);
    }
    /**
     * Helper method to trigger the Mega Bomb + regular candy effect.
     * It finds all candies of the target type and starts the clearing process.
     * @param megaBomb The Mega Bomb involved in the swap.
     * @param targetCandy The regular candy, whose type will be cleared.
     */
    private void triggerMegaBombColorClear(Candy megaBomb, Candy targetCandy) {
        int targetType = targetCandy.getType();
        Log.i(TAG, "MEGA BOMB ACTION: Clearing all candies of type " + targetType);

        Set<Point> pointsToClear = new HashSet<>();
        // Find all candies on the board with the target type
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                Candy candy = getCandyAt(r, c);
                // Also clear the mega bomb itself and the candy it was swapped with
                if (candy == megaBomb || candy == targetCandy || (candy != null && candy.getType() == targetType)) {
                    pointsToClear.add(new Point(r, c));
                }
            }
        }

        // Package into a MatchGroup and process
        // Use the selected candy's location as the origin point for the effect
        MatchGroup effectGroup = new MatchGroup(pointsToClear, new Point(selectedRow, selectedCol));
        List<MatchGroup> effectList = new ArrayList<>();
        effectList.add(effectGroup);
        processMatchesAndContinueLoop(effectList);
    }

    /**
     * Helper method to reset selection state and mark the board as settling.
     */
    private void deselectAndSettleBoard() {
        selectedRow = -1;
        selectedCol = -1;
        selectedCandyObject = null;
        isBoardSettling = true; // The board will now process the special effect
        invalidate();
    }





    // MODIFIED: To also check for 2x2 square creation after a swap
    private boolean checkAndProcessMatchesAfterSwap(int r1, int c1, int r2, int c2) {
        isBoardSettling = true; // Board will be processing due to the swap and match check

        Log.d(TAG, "checkAndProcessMatchesAfterSwap: Checking board after swap. lastInteractedPoint: " + lastInteractedPoint);

        // First, check for standard line matches (3-in-a-row, etc.)
        List<MatchGroup> matchGroupsFound = findAllMatchGroupsOnBoard();

        if (matchGroupsFound != null && !matchGroupsFound.isEmpty()) {
            // --- A. A LINE MATCH WAS CREATED ---
            Log.i(TAG, "Swap created " + matchGroupsFound.size() + " standard match group(s).");
            processMatchesAndContinueLoop(matchGroupsFound);
            return true; // The swap was valid because it created a line match.
        } else {
            // --- B. NO LINE MATCH WAS FOUND. NOW, CHECK FOR A 2x2 SQUARE ---
            Log.d(TAG, "Swap created no standard matches. Now checking for 2x2 squares...");
            List<MatchGroup> squareMatchesFound = findSquareMatches();

            if (squareMatchesFound != null && !squareMatchesFound.isEmpty()) {
                // --- B1. A SQUARE MATCH WAS CREATED ---
                Log.i(TAG, "Swap successfully created a 2x2 square. Processing it.");
                // lastInteractedPoint is already set correctly from the user's swap
                processMatchesAndContinueLoop(squareMatchesFound);
                return true; // The swap was valid because it created a square.
            } else {
                // --- B2. NEITHER A LINE NOR A SQUARE WAS CREATED ---
                Log.i(TAG, "Swap created no line matches AND no square matches. Invalid swap.");
                isBoardSettling = false; // The board is not settling, the move is invalid.
                return false; // No matches of any kind were found. The swap will be reversed.
            }
        }
    }





    // MODIFIED: To handle List<MatchGroup>, create Bombs, and manage game loop
    // MODIFIED: To handle bombs within match groups
    // MODIFIED: To handle bombs within match groups and correctly find adjacent bombs
    private void processMatchesAndContinueLoop(List<MatchGroup> matchGroupsToProcess) {
        // <<< CHANGE 1: ADDED CHECK FOR SQUARE MATCHES >>>
        if (matchGroupsToProcess == null || matchGroupsToProcess.isEmpty()) {
            // If there are no line matches, check for square matches before stopping
            List<MatchGroup> squareMatches = findSquareMatches();
            if (!squareMatches.isEmpty()) {
                Log.d(TAG, "No line matches, but found " + squareMatches.size() + " square(s). Processing them.");
                processMatchesAndContinueLoop(squareMatches); // Re-run the loop with the square groups
                return;
            }
            // <<< END OF CHANGE 1 >>>

            // Base case: No more matches found, board is stable.
            isBoardSettling = false;
            invalidate();
            if (gameStateListener != null) {
                if (!hasAvailableMoves()) {
                    gameStateListener.onNoMovesAvailable();
                } else {
                    gameStateListener.onMovesAvailable();
                }
            }
            Log.d(TAG, "processMatches: No more groups. Board stable.");
            return;
        }


        Log.d(TAG, "processMatches: Processing " + matchGroupsToProcess.size() + " groups. LIP: " + lastInteractedPoint);
        isBoardSettling = true;

        Set<Point> pointsForStandardRemoval = new HashSet<>();
        Set<Point> activatedBombLocations = new HashSet<>();
        boolean specialCandyCreatedThisTurn = false;
        Point specialCandyCreationPoint = null;

        // --- Special Candy Creation Logic (from user's swap) ---
        if (lastInteractedPoint != null) {
            // This part only runs if the matches were caused by a direct user swap.
            // It won't run for cascade matches or square matches found automatically.
            Candy candyAtLIP = getCandyAt(lastInteractedPoint.r, lastInteractedPoint.c);
            if (candyAtLIP != null && !candyAtLIP.isSpecialCandy()) {
                MatchGroup bestUserMatch = null;
                for (MatchGroup group : matchGroupsToProcess) {
                    // Make sure we're not trying to create a special candy from a square match group
                    if (group.points.contains(lastInteractedPoint) && !group.isRocketCreationGroup()) {
                        if (bestUserMatch == null || group.length > bestUserMatch.length) {
                            bestUserMatch = group;
                        }
                    }
                }

                if (bestUserMatch != null) {
                    Point creationPt = lastInteractedPoint;
                    if (bestUserMatch.length >= 5) { // Create Mega Bomb for 5+
                        Log.i(TAG, "Creating MEGA BOMB at user interaction point: " + creationPt);
                        candies.get(creationPt.r).set(creationPt.c, new Candy(Candy.TYPE_MEGA_BOMB));
                        specialCandyCreatedThisTurn = true;
                        specialCandyCreationPoint = creationPt;
                    } else if (bestUserMatch.length == 4) { // Create Bomb for 4-in-a-row
                        Log.i(TAG, "Creating BOMB at user interaction point: " + creationPt);
                        candies.get(creationPt.r).set(creationPt.c, new Candy(Candy.TYPE_BOMB));
                        specialCandyCreatedThisTurn = true;
                        specialCandyCreationPoint = creationPt;
                    }
                }
            }
        }
        // --- End Special Candy Creation Logic ---


        // --- Find all points from the initial matches ---
        for (MatchGroup group : matchGroupsToProcess) {
            // <<< CHANGE 2: ADDED ROCKET CREATION LOGIC >>>
            if (group.isRocketCreationGroup()) {
                Log.i(TAG, "Creating ROCKET from 2x2 square at " + group.creationPoint);
                // Turn the creation point candy into a rocket
                Point rocketCreationPt = group.creationPoint;
                candies.get(rocketCreationPt.r).set(rocketCreationPt.c, new Candy(Candy.TYPE_ROCKET));

                // Add the other 3 points of the square to be cleared
                for (Point p : group.points) {
                    if (!p.equals(rocketCreationPt)) {
                        pointsForStandardRemoval.add(p);
                    }
                }
                // The rocket creation point itself is NOT removed, so we skip adding it to the list.
                // We use 'continue' to skip the standard "addAll" below for this group.
                continue;
            }
            // <<< END OF CHANGE 2 >>>

            pointsForStandardRemoval.addAll(group.points);
        }

        // --- NEW LOGIC: Find bombs adjacent to any cleared points or swapped bombs ---
        // If the user swapped a bomb to make a match, that bomb must explode.
        if (lastInteractedPoint != null) {
            Candy swappedCandy = getCandyAt(lastInteractedPoint.r, lastInteractedPoint.c);
            // This condition handles swapping a bomb into a position that CREATES a match.
            if (swappedCandy != null && swappedCandy.isBomb()) {
                Log.d(TAG, "Bomb at lastInteractedPoint " + lastInteractedPoint + " is being activated due to swap.");
                activatedBombLocations.add(lastInteractedPoint);
            }
        }

        // Now, check for bombs adjacent to any of the points being cleared by a standard match.
        for (Point clearedPoint : pointsForStandardRemoval) {
            // Check all 4 neighbors of the point that is being cleared
            int r = clearedPoint.r;
            int c = clearedPoint.c;
            Point[] neighbors = {new Point(r - 1, c), new Point(r + 1, c), new Point(r, c - 1), new Point(r, c + 1)};
            for (Point neighbor : neighbors) {
                if (isValidCell(neighbor.r, neighbor.c)) {
                    Candy neighborCandy = getCandyAt(neighbor.r, neighbor.c);
                    if (neighborCandy != null && neighborCandy.isBomb()) {
                        activatedBombLocations.add(neighbor);
                        Log.d(TAG, "Bomb at " + neighbor + " is adjacent to a cleared candy. Activating it.");
                    }
                }
            }
        }
        // --- END NEW LOGIC ---


        // If a special candy was just created, prevent it from being removed or activated this turn.
        if (specialCandyCreationPoint != null) { // This check is now sufficient
            Log.d(TAG, "Kept new special candy at " + specialCandyCreationPoint + " from being cleared/activated.");
            pointsForStandardRemoval.remove(specialCandyCreationPoint);
            activatedBombLocations.remove(specialCandyCreationPoint);
        }

        // --- Combine all points that need to be cleared ---
        Set<Point> allPointsToClear = new HashSet<>(pointsForStandardRemoval);

        // For every bomb that was activated, add its 3x3 explosion area to the set of points to clear.
        if (!activatedBombLocations.isEmpty()) {
            for (Point bombLocation : activatedBombLocations) {
                Log.i(TAG, "Bomb at " + bombLocation + " is exploding its 3x3 area.");
                for (int r = bombLocation.r - 1; r <= bombLocation.r + 1; r++) {
                    for (int c = bombLocation.c - 1; c <= bombLocation.c + 1; c++) {
                        if (isValidCell(r, c)) {
                            allPointsToClear.add(new Point(r, c));
                        }
                    }
                }
            }
        }

        // --- Process the board changes ---
        if (allPointsToClear.isEmpty() && !specialCandyCreatedThisTurn && pointsForStandardRemoval.isEmpty()) {
            // Nothing to do, end the loop.
            // A rocket could have been created without any other points being removed, so we check both.
            Log.d(TAG, "No points to clear and no special created. Ending cycle.");
            isBoardSettling = false;
            lastInteractedPoint = null;
            invalidate();
            checkAllMatchesAndContinue(); // Final check for stability
            return;
        }

        if (!allPointsToClear.isEmpty()) {
            removeCandies(allPointsToClear);
            gameScore += allPointsToClear.size() * 10; // Basic scoring
            Log.d(TAG, "Score: " + gameScore + " (cleared " + allPointsToClear.size() + " candies)");
        }

        applyGravityAndRefill(this::checkAllMatchesAndContinue);
    }

    // Helper method to apply gravity and refill, then continue the loop.
    private void applyGravityAndRefill(Runnable onComplete) {
        applyGravity();
        refillBoard();
        invalidate();

        // IMPORTANT: Reset lastInteractedPoint for cascades.
        // Cascading matches are not direct user actions.
        lastInteractedPoint = null;

        // Post a delay to continue the game loop, checking for new matches caused by gravity and refill.
        gameLoopHandler.postDelayed(onComplete, 250);
    }

    // Helper method to check for all matches and continue the loop.
    private void checkAllMatchesAndContinue() {
        Log.d(TAG, "Checking for cascade matches after delay...");
        List<MatchGroup> cascadeMatchGroups = findAllMatchGroupsOnBoard();
        // Here, we re-enter the main loop. It will first check for squares again.
        processMatchesAndContinueLoop(cascadeMatchGroups);
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
    /**
     * Finds all 2x2 square matches on the board to create Rockets.
     * @return A list of MatchGroups, where each group represents a 2x2 square found.
     */
    private List<MatchGroup> findSquareMatches() {
        List<MatchGroup> squareGroups = new ArrayList<>();
        Set<Point> usedInSquare = new HashSet<>(); // Prevents candies from being part of multiple squares

        // Iterate through each possible top-left corner of a 2x2 square
        for (int r = 0; r < gridRows - 1; r++) {
            for (int c = 0; c < gridCols - 1; c++) {
                Point topLeft = new Point(r, c);
                if (usedInSquare.contains(topLeft)) {
                    continue; // This candy has already been used in another square
                }

                Candy candy = getCandyAt(r, c);
                if (candy == null || !candy.isRegularCandy()) {
                    continue; // Squares can only be made from regular candies
                }
                int type = candy.getType();

                Point topRight = new Point(r, c + 1);
                Point bottomLeft = new Point(r + 1, c);
                Point bottomRight = new Point(r + 1, c + 1);

                // Check if all 4 candies are the same type and not already used
                if (isSameType(type, topRight) && isSameType(type, bottomLeft) && isSameType(type, bottomRight) &&
                        !usedInSquare.contains(topRight) && !usedInSquare.contains(bottomLeft) && !usedInSquare.contains(bottomRight)) {

                    Log.i(TAG, "Found a 2x2 square of type " + type + " at (" + r + "," + c + ")");

                    Set<Point> squarePoints = new HashSet<>();
                    squarePoints.add(topLeft);
                    squarePoints.add(topRight);
                    squarePoints.add(bottomLeft);
                    squarePoints.add(bottomRight);

                    // Create a MatchGroup, using the top-left as the origin where the rocket will be created
                    MatchGroup squareGroup = new MatchGroup(squarePoints, topLeft);
                    squareGroup.setCreatesRocket(true); // Set our new flag!
                    squareGroups.add(squareGroup);

                    // Mark all 4 points as used to avoid overlapping square detection
                    usedInSquare.addAll(squarePoints);
                }
            }
        }
        return squareGroups;
    }

    /**
     * Helper to check if a candy at a point is a specific REGULAR type.
     */
    private boolean isSameType(int type, Point p) {
        Candy candy = getCandyAt(p.r, p.c);
        return candy != null && candy.isRegularCandy() && candy.getType() == type;
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
        long fallDuration = 300; // 300ms for candies to fall

        // Iterate through each column to process falls
        for (int c = 0; c < gridCols; c++) {
            List<Point> emptySlots = new ArrayList<>();
            // From bottom to top, find all empty slots and candies that need to fall
            for (int r = gridRows - 1; r >= 0; r--) {
                if (getCandyAt(r, c) == null) {
                    emptySlots.add(new Point(r, c)); // Record empty slot
                } else if (!emptySlots.isEmpty()) {
                    // This candy needs to fall. Find its destination.
                    Point destination = emptySlots.remove(0); // The highest empty slot below it
                    Candy candyToMove = getCandyAt(r, c);

                    // Create a fall animation
                    AnimationInfo fallAnim = new AnimationInfo(destination, candyToMove, AnimationInfo.AnimationType.FALL, fallDuration);
                    fallAnim.fallStartPoint = new Point(r, c); // Record where it started
                    activeAnimations.add(fallAnim);

                    // Move the candy in the data grid
                    candies.get(destination.r).set(destination.c, candyToMove);
                    candies.get(r).set(c, null); // The original spot is now empty

                    // The original spot now becomes an empty slot for candies above it
                    emptySlots.add(new Point(r, c));
                    // Keep emptySlots sorted (highest row index first)
                    emptySlots.sort((p1, p2) -> Integer.compare(p2.r, p1.r));
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

            // ... inside the gameLoopHandler.postDelayed block in shuffleBoard
            if (matchesAfterShuffle != null && !matchesAfterShuffle.isEmpty()) {
                Log.d(TAG, "Shuffle created " + matchesAfterShuffle.size() + " immediate match group(s). Processing them.");

                // --- NEW LOGIC ---
                // Collect all points from the new matches
                Set<Point> pointsToClear = new HashSet<>();
                for (MatchGroup group : matchesAfterShuffle) {
                    pointsToClear.addAll(group.points);
                }

                // Score these matches as a combo, since the player didn't make them directly
                addScore(pointsToClear.size(), true);

                // Start the animation and cascade loop
                animateAndRemoveCandies(pointsToClear);
                // --- END NEW LOGIC ---

            } else {
// ...
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
        Set<Point> points;
        int length;
        Point creationPoint;
        private boolean createsRocket = false; // <<< ADD THIS LINE

        MatchGroup(Set<Point> points, Point creationPoint) {
            this.points = points;
            this.length = points.size();
            this.creationPoint = creationPoint;
        }

        // <<< ADD THESE TWO METHODS INSIDE THE MatchGroup CLASS >>>
        public void setCreatesRocket(boolean createsRocket) {
            this.createsRocket = createsRocket;
        }

        public boolean isRocketCreationGroup() {
            return this.createsRocket;
        }
        // <<< END OF NEW METHODS >>>

        @Override
        public String toString() {
            // Your existing toString method...
            // We can even update it to be more helpful for debugging:
            return "MatchGroup{length=" + length + ", createsRocket=" + createsRocket + ", points=" + points + ", creationPt=" + creationPoint + '}';
        }
    }
    // At the bottom of GameGridView.java, after the MatchGroup class

    /**
     * A class to hold the state and properties of a single, active animation on the grid.
     */
    private static class AnimationInfo {
        enum AnimationType {
            SHRINK_FADE_OUT, // For clearing candies
            FALL      ,       // For gravity
            SHAKE
        }

        final Point point;        // The grid position (r, c) of the animation's destination
        final Candy candy;        // A clone of the candy being animated
        final AnimationType type;
        final long startTime;
        final long duration;

        // Field specific to FALL animations
        Point fallStartPoint; // Where the candy started falling from

        AnimationInfo(Point point, Candy candy, AnimationType type, long duration) {
            this.point = point;
            // IMPORTANT: We animate a *clone* of the candy to prevent visual bugs
            this.candy = (candy != null) ? candy.clone() : null;
            this.type = type;
            this.duration = duration;
            this.startTime = System.currentTimeMillis();
        }

        /**
         * Calculates the progress of the animation from 0.0 (start) to 1.0 (end).
         */
        public float getProgress() {
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime >= duration) {
                return 1.0f;
            }
            return (float) elapsedTime / duration;
        }

    }
    // Add these 4 new methods into GameGridView.java

    /**     * Ensures the animation driver is running.
     */
    private void startAnimations() {
        if (!animationDriver.isRunning()) {
            animationDriver.start();
        }
    }

    /**
     * Draws a candy at a specific GRID CELL (r, c) with a given scale.
     */
    private void drawCandy(Canvas canvas, Candy candy, int r, int c, float scale) {
        if (candy == null || candyBitmaps == null) return;
        int candySpriteIndex = candy.getType();

        if (candySpriteIndex >= 0 && candySpriteIndex < candyBitmaps.length && candyBitmaps[candySpriteIndex] != null) {
            Bitmap candyBitmap = candyBitmaps[candySpriteIndex];
            float scaledSize = cellSize * scale;
            float offset = (cellSize - scaledSize) / 2;
            int destLeft = (int)(gridOffsetX + c * cellSize + offset);
            int destTop = (int)(gridOffsetY + r * cellSize + offset);
            Rect destRect = new Rect(destLeft, destTop, (int)(destLeft + scaledSize), (int)(destTop + scaledSize));
            canvas.drawBitmap(candyBitmap, null, destRect, null);
        }
    }

    /**
     * Draws a candy at a specific PIXEL (x, y) location with a given scale. Used for animations.
     */
    private void drawCandyAtPosition(Canvas canvas, Candy candy, float x, float y, float scale) {
        if (candy == null || candyBitmaps == null) return;
        int candySpriteIndex = candy.getType();

        if (candySpriteIndex >= 0 && candySpriteIndex < candyBitmaps.length && candyBitmaps[candySpriteIndex] != null) {
            Bitmap candyBitmap = candyBitmaps[candySpriteIndex];
            float scaledSize = cellSize * scale;
            float offset = (cellSize - scaledSize) / 2; // Center the scaled bitmap

            // Here x and y are the top-left of the cell, so we apply the offset
            int destLeft = (int)(x + offset);
            int destTop = (int)(y + offset);

            Rect destRect = new Rect(destLeft, destTop, (int)(destLeft + scaledSize), (int)(destTop + scaledSize));
            canvas.drawBitmap(candyBitmap, null, destRect, null);
        }
    }

    /**
     * This method is called when an animation finishes.
     */
    private void handleAnimationCompletion(AnimationInfo anim) {
        if (anim.type == AnimationInfo.AnimationType.FALL) {
            // A candy finished falling. Place it in its new spot in the main grid.
            Log.d(TAG, "FALL animation completed for candy at " + anim.point);
            if (isValidCell(anim.point.r, anim.point.c)) {
                // Only place the candy if the spot is still empty (to prevent overwrites)
                if (getCandyAt(anim.point.r, anim.point.c) == null) {
                    candies.get(anim.point.r).set(anim.point.c, anim.candy);
                }
            }
        }
        // When SHRINK_FADE_OUT completes, we don't need to do anything extra,
        // as the candy is already null in the grid.
    }
    /**
     * Starts animations for clearing candies, removes them from the grid,
     * and then triggers the next phase of the game loop (gravity and stabilization).
     * @param pointsToClear The set of points corresponding to candies that should be removed.
     */
    private void animateAndRemoveCandies(Set<Point> pointsToClear) {
        if (pointsToClear == null || pointsToClear.isEmpty()) {
            // If there's nothing to clear, just end the settling state.
            isBoardSettling = false;
            return;
        }

        Log.d(TAG, "animateAndRemoveCandies: Animating and removing " + pointsToClear.size() + " candies.");
        long animationDuration = 300; // 300ms for candies to shrink

        for (Point p : pointsToClear) {
            if (isValidCell(p.r, p.c)) {
                Candy candyToAnimate = getCandyAt(p.r, p.c);

                if (candyToAnimate != null) {
                    // --- לוגיקת איסוף סוכריות למשימה ---
                    // אם הסוכריה שמתפוצצת היא מהסוג שהוגדר כיעד לשלב - אנחנו סופרים אותה
                    if (currentLevelConfig != null && candyToAnimate.getType() == currentLevelConfig.getTargetCandyType()) {
                        currentCollectedTargetCount++;
                        Log.d(TAG, "Collected target candy! Total: " + currentCollectedTargetCount + "/" + currentLevelConfig.getTargetCandyCount());
                    }
                    // ----------------------------------

                    // Add a "shrink and fade" animation for this candy.
                    activeAnimations.add(new AnimationInfo(p, candyToAnimate, AnimationInfo.AnimationType.SHRINK_FADE_OUT, animationDuration));
                }
            }
        }

        // Immediately remove the candies from the data grid after creating the animations for them.
        removeCandies(pointsToClear);

        startAnimations(); // Make sure the animation driver is running.

        // בדיקה: האם הגענו ליעד האיסוף או ליעד הניקוד?
        checkGameStatus();

        // --- CRITICAL GAME LOOP STEP ---
        // Post a delayed task that will execute *after* the shrink animations are complete.
        gameLoopHandler.postDelayed(() -> {
            Log.d(TAG, "Post-animation: Applying gravity and refilling.");
            applyGravity(); // Let candies fall into empty spaces.
            refillBoard();  // Fill new empty spaces at the top.

            // After refilling, we must check for new matches that have formed.
            // This is how we detect and score combos.
            stabilizationLoop();

        }, animationDuration); // The delay MUST match the animation duration.
    }

    /**
     * בודק אם השחקן ניצח (עמד במשימה או בניקוד) או הפסיד (נגמרו המהלכים)
     */
    private void checkGameStatus() {
        if (currentLevelConfig == null) return;

        // --- עדכון הממשק (Activity) על המצב הנוכחי ---
        if (gameStateListener != null) {
            // אם המשימה היא איסוף סוכריות - נשלח את כמות האיסוף, אחרת נשלח את הניקוד
            int target = (currentLevelConfig.getTargetCandyType() != -1) ?
                    currentLevelConfig.getTargetCandyCount() : currentLevelConfig.getTargetScore();

            int current = (currentLevelConfig.getTargetCandyType() != -1) ?
                    currentCollectedTargetCount : gameScore;

            // שליחת הנתונים ל-FeedActivity (משימה ומהלכים)
            gameStateListener.onMissionUpdate(current, target, remainingMoves);
        }

        // 1. בדיקת ניצחון לפי איסוף סוכריות (למשל: 30 כחולות)
        boolean collectedEnough = (currentLevelConfig.getTargetCandyType() != -1 &&
                currentCollectedTargetCount >= currentLevelConfig.getTargetCandyCount());

        // 2. בדיקת ניצחון לפי ניקוד (Target Score)
        boolean reachedScore = (gameScore >= currentLevelConfig.getTargetScore());

        if (collectedEnough || reachedScore) {
            Log.i(TAG, "Victory! Goal achieved.");
            triggerLevelComplete();
            return;
        }

        // 3. בדיקת הפסד: אם נגמרו המהלכים והשחקן לא ניצח
        if (remainingMoves <= 0 && currentLevelConfig.getMaxMoves() > 0) {
            Log.i(TAG, "Game Over! Out of moves.");
            isLevelComplete = true;
            invalidate();
        }
    }


    /**
     * מפעיל את מצב סיום השלב ומציג את הדיאלוג
     */
    private void triggerLevelComplete() {
        if (isLevelComplete) return; // מונע קריאה כפולה

        isLevelComplete = true;
        levelTimerRunning = false;

        // שמירת נתונים לדיאלוג (אם אתה משתמש ב-drawLevelCompleteDialog)
        currentScoreForDialog = gameScore;
        timeTakenMillisForDialog = System.currentTimeMillis() - startTimeMillis;

        Log.d(TAG, "Level Complete triggered. Score: " + gameScore);

        // רענון המסך להצגת הדיאלוג
        invalidate();
    }

}