package com.example.travellog.gameui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.travellog.gamecore.Candy;
import com.example.travellog.gamecore.LevelConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GameGridView extends View {
    private static final String TAG = "GameGridView";

    private Paint paint;
    private Paint backgroundPaint;
    private Paint highlightPaint;

    private Paint dialogBackgroundPaint;
    private Paint dialogTextPaint;
    private Paint dialogScoreTextPaint;

    private int gridRows = 0;
    private int gridCols = 0;
    private List<List<Candy>> candies;
    private Random random = new Random();

    private int cellSize = 0;
    private int gridOffsetX = 0;
    private int gridOffsetY = 0;

    private Bitmap[] candyBitmaps;
    private static final int NUMBER_OF_CANDY_TYPES = 6;
    private LevelConfig currentLevelConfig;
    private boolean allBitmapsLoadedSuccessfully = false;

    private Candy selectedCandyObject = null;
    private int selectedRow = -1;
    private int selectedCol = -1;

    // --- Game State Variables ---
    private int gameScore = 0;
    private long startTimeMillis = 0;
    private boolean levelTimerRunning = false;
    // --- End Game State Variables ---

    private boolean isLevelComplete = false;
    private int currentScoreForDialog = 0;
    private long timeTakenMillisForDialog = 0;


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

    public GameGridView(Context context) { super(context); init(context, null); }
    public GameGridView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(context, attrs); }
    public GameGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(context, attrs); }

    private void init(Context context, @Nullable AttributeSet attrs) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.TRANSPARENT);
        highlightPaint = new Paint();
        highlightPaint.setColor(Color.YELLOW);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(8);
        highlightPaint.setAntiAlias(true);

        dialogBackgroundPaint = new Paint();
        dialogBackgroundPaint.setColor(Color.argb(200, 0, 0, 0));
        dialogBackgroundPaint.setStyle(Paint.Style.FILL);

        dialogTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialogTextPaint.setColor(Color.WHITE);
        dialogTextPaint.setTextAlign(Paint.Align.CENTER);

        dialogScoreTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialogScoreTextPaint.setColor(Color.rgb(127, 255, 0)); // Lime Green
        dialogScoreTextPaint.setTextAlign(Paint.Align.CENTER);
        /*
        הסבר בעברית: init (קטע אתחול צבעים לדיאלוג)
        (אין שינוי מהגרסה הקודמת)
        */

        candies = new ArrayList<>();
        if (NUMBER_OF_CANDY_TYPES <= 0) {
            Log.e(TAG, "CRITICAL ERROR: NUMBER_OF_CANDY_TYPES is not positive.");
            allBitmapsLoadedSuccessfully = false;
        } else {
            initBitmaps(context);
        }
    }

    private void initBitmaps(Context context) {
        Log.i(TAG, "initBitmaps: Loading " + NUMBER_OF_CANDY_TYPES + " candy bitmaps.");
        candyBitmaps = new Bitmap[NUMBER_OF_CANDY_TYPES];
        int successfullyLoadedCount = 0;
        String resourceNameBase = "candy_type_";

        for (int i = 0; i < NUMBER_OF_CANDY_TYPES; i++) {
            String targetResourceName = resourceNameBase + i;
            int resourceId = context.getResources().getIdentifier(targetResourceName, "drawable", context.getPackageName());
            if (resourceId != 0) {
                try {
                    Bitmap decodedBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
                    if (decodedBitmap != null) {
                        candyBitmaps[i] = decodedBitmap;
                        successfullyLoadedCount++;
                    } else {
                        Log.e(TAG, "initBitmaps: Failed to decode resource for '" + targetResourceName + "' (ID: " + resourceId + ").");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "initBitmaps: Exception decoding resource ID " + resourceId + " for " + targetResourceName, e);
                }
            } else {
                Log.e(TAG, "initBitmaps: Drawable resource named '" + targetResourceName + "' NOT FOUND.");
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
        isLevelComplete = false;
        gameScore = 0;
        startTimeMillis = System.currentTimeMillis();
        levelTimerRunning = true;

        currentLevelConfig = LevelConfig.getConfigForLevel(levelNumber);
        if (currentLevelConfig == null) {
            Log.e(TAG, "Failed to get LevelConfig for level: " + levelNumber + ". Using fallback for level 1 if available or default 3x3.");
            currentLevelConfig = LevelConfig.getConfigForLevel(1); // Try to fallback to level 1
            if (currentLevelConfig == null) { // Absolute fallback if level 1 also not found
                Log.e(TAG, "Fallback to level 1 also failed. Using default 3x3, target 500.");
                this.gridRows = 3; this.gridCols = 3;
                this.currentLevelConfig = new LevelConfig(levelNumber, 3, 3, 500);
            } else {
                Log.i(TAG, "Fell back to Level 1 configuration.");
                this.gridRows = currentLevelConfig.getRows();
                this.gridCols = currentLevelConfig.getCols();
            }
        } else {
            this.gridRows = currentLevelConfig.getRows();
            this.gridCols = currentLevelConfig.getCols();
        }
        // Ensure currentLevelConfig is not null before accessing target score
        if (currentLevelConfig != null) {
            Log.i(TAG, "Setting up Level " + currentLevelConfig.getLevelNumber() + " with target score: " + currentLevelConfig.getTargetScore());
        } else { // Should not happen if fallback logic above is sound
            Log.e(TAG, "CRITICAL: currentLevelConfig is null after setup attempt for level " + levelNumber);
            // Handle this critical error, maybe by not starting the game or showing an error.
            // For now, it might crash if other parts expect currentLevelConfig to be non-null.
            return;
        }

        if (gridRows <= 0) this.gridRows = 1; if (gridCols <= 0) this.gridCols = 1;

        initCandiesAndStabilize();
        selectedRow = -1; selectedCol = -1; selectedCandyObject = null;
        requestLayout();
        invalidate();
    }
    /*
    הסבר בעברית: setupGridForLevel (שינויים למצב משחק)
    - `isLevelComplete` מאופס ל-false.
    - `gameScore` (ניקוד המשחק הנוכחי) מאופס ל-0.
    - `startTimeMillis` נשמר לזמן הנוכחי כדי להתחיל למדוד את זמן המשחק בשלב.
    - `levelTimerRunning` מוגדר ל-true.
    - נוספה בדיקה לוגית משופרת למקרה ש-`LevelConfig.getConfigForLevel` מחזיר null, כולל ניסיון לחזור לרמה 1.
    - בהמשך, כאשר `currentLevelConfig` נטען, אנו מדפיסים ללוג את ניקוד המטרה של השלב (רק אם `currentLevelConfig` אינו null).
    */

    private void initCandiesAndStabilize() {
        if (!allBitmapsLoadedSuccessfully) {
            Log.e(TAG, "initCandies: ABORTING, not all bitmaps loaded.");
            candies = new ArrayList<>();
            return;
        }
        if (currentLevelConfig == null || gridRows <= 0 || gridCols <= 0) {
            Log.w(TAG, "initCandies: Config null or invalid dimensions. Cannot initialize candies.");
            candies = new ArrayList<>();
            return;
        }

        candies = new ArrayList<>();
        int[][] layout = currentLevelConfig.getCustomGridLayout();
        boolean useCustomLayout = layout != null && layout.length == gridRows && (gridRows > 0 && layout[0].length == gridCols);

        if (useCustomLayout) {
            Log.d(TAG, "Initializing candies using custom layout for level " + currentLevelConfig.getLevelNumber());
            for (int i = 0; i < gridRows; i++) {
                List<Candy> rowList = new ArrayList<>(gridCols);
                for (int j = 0; j < gridCols; j++) {
                    int candyType = layout[i][j];
                    if (candyType < 0 || candyType >= NUMBER_OF_CANDY_TYPES) candyType = 0;
                    rowList.add(new Candy(candyType));
                }
                candies.add(rowList);
            }
        } else {
            if (layout != null) Log.w(TAG, "Custom layout dimensions mismatch. Initializing randomly.");
            else Log.d(TAG, "No custom layout for level " + currentLevelConfig.getLevelNumber() + ". Initializing randomly.");
            if (NUMBER_OF_CANDY_TYPES == 0) { Log.e(TAG, "No candy types to generate random candies."); return; }
            for (int i = 0; i < gridRows; i++) {
                List<Candy> rowList = new ArrayList<>(gridCols);
                for (int j = 0; j < gridCols; j++) {
                    rowList.add(new Candy(random.nextInt(NUMBER_OF_CANDY_TYPES)));
                }
                candies.add(rowList);
            }
        }

        Log.i(TAG, "Stabilizing initial board...");
        printGridState("Before Initial Stabilization");
        int stabilizationCycles = 0;
        final int MAX_STABILIZATION_CYCLES = 25;

        while (stabilizationCycles < MAX_STABILIZATION_CYCLES) {
            Set<Point> initialMatches = findAllMatchesOnBoard();
            if (initialMatches.isEmpty()) {
                Log.i(TAG, "Board is stable. No initial matches found after " + stabilizationCycles + " cycles.");
                break;
            }
            Log.i(TAG, "Stabilization Cycle " + stabilizationCycles + ": Found " + initialMatches.size() + " initial matches.");
            // Score is not added during stabilization's removeMatchedCandies call
            // We need a way to differentiate or pass a flag if we want to change this behavior.
            // For now, let's assume stabilization matches don't add to gameScore directly.
            for (Point p : initialMatches) { // Manual nulling for stabilization without scoring
                if (getCandyAt(p.r, p.c) != null) candies.get(p.r).set(p.c, null);
            }
            applyGravity();
            refillBoardForStabilization();
            stabilizationCycles++;
        }
        if (stabilizationCycles == MAX_STABILIZATION_CYCLES) {
            Log.w(TAG, "Max stabilization cycles reached. Board might still have matches.");
        }
        Log.i(TAG, "Initial board stabilization complete.");
        printGridState("After Initial Stabilization");
    }

    private void refillBoardForStabilization() {
        if (candies == null || gridRows <= 0 || gridCols <= 0 || NUMBER_OF_CANDY_TYPES <= 0) return;
        for (int j = 0; j < gridCols; j++) {
            for (int i = 0; i < gridRows; i++) {
                if (getCandyAt(i, j) == null) {
                    candies.get(i).set(j, new Candy(random.nextInt(NUMBER_OF_CANDY_TYPES)));
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (gridCols <= 0 || gridRows <= 0) { setMeasuredDimension(parentWidth, parentHeight); return; }
        cellSize = Math.min(parentWidth / gridCols, parentHeight / gridRows);
        if (cellSize <= 0) cellSize = 1;
        setMeasuredDimension(cellSize * gridCols, cellSize * gridRows);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (gridCols <= 0 || gridRows <= 0 || w <= 0 || h <= 0) { cellSize = 0; gridOffsetX = 0; gridOffsetY = 0; return; }
        cellSize = Math.min(w / gridCols, h / gridRows);
        if (cellSize <= 0) cellSize = 1;
        gridOffsetX = (w - (cellSize * gridCols)) / 2;
        gridOffsetY = (h - (cellSize * gridRows)) / 2;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isLevelComplete) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isLevelComplete = false;
                levelTimerRunning = false;
                Log.d(TAG, "Level complete dialog dismissed.");
                if (currentLevelConfig != null) {
                    int nextLevel = currentLevelConfig.getLevelNumber() + 1;
                    if (nextLevel <= LevelConfig.getMaxLevels()) { // Check if next level exists
                        Log.i(TAG, "Attempting to load next level: " + nextLevel);
                        setupGridForLevel(nextLevel);
                    } else {
                        Log.i(TAG, "All levels completed or next level not defined!");
                        // TODO: Handle game completion (e.g., show a "You Win!" screen, go to main menu)
                        // For now, just log and stay on the (now empty/reset) view.
                        // Or you could try restarting level 1: setupGridForLevel(1);
                    }
                } else {
                    Log.e(TAG, "Cannot proceed to next level, currentLevelConfig is null.");
                }
                invalidate();
                return true;
            }
            return true;
        }
        /*
        הסבר בעברית: onTouchEvent (חלק דיאלוג "סיום שלב")
        - כאשר הדיאלוג מוצג, לחיצה תגרום לו להיעלם.
        - `levelTimerRunning` יוגדר ל-false.
        - נוספה בדיקה האם הרמה הבאה קיימת (`nextLevel <= LevelConfig.getMaxLevels()`) לפני טעינתה.
          אם לא, נרשמת הודעה וניתן להוסיף לוגיקה לסיום המשחק או חזרה לתפריט.
        */

        if (!allBitmapsLoadedSuccessfully || candies == null || candies.isEmpty() || cellSize == 0) return super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int col = (int) ((event.getX() - gridOffsetX) / cellSize);
            int row = (int) ((event.getY() - gridOffsetY) / cellSize);
            if (row >= 0 && row < gridRows && col >= 0 && col < gridCols) {
                handleCellTouch(row, col);
            } else {
                if (selectedRow != -1) { selectedRow = -1; selectedCol = -1; selectedCandyObject = null; invalidate(); }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleCellTouch(int row, int col) {
        if (isLevelComplete) return;

        Candy touchedCandy = getCandyAt(row, col);
        if (touchedCandy == null && selectedRow == -1) return;

        if (selectedRow == -1) {
            if (touchedCandy != null) {
                selectedRow = row; selectedCol = col; selectedCandyObject = touchedCandy;
                invalidate();
            }
        } else {
            if (selectedRow == row && selectedCol == col) {
                selectedRow = -1; selectedCol = -1; selectedCandyObject = null;
            } else if (touchedCandy != null && isAdjacent(row, col, selectedRow, selectedCol)) {
                int tempR = selectedRow, tempC = selectedCol;
                selectedRow = -1; selectedCol = -1; selectedCandyObject = null;

                swapCandies(tempR, tempC, row, col);
                Set<Point> matches = checkForMatchesBySwap(row, col, tempR, tempC);
                if (!matches.isEmpty()) {
                    Log.i(TAG, "Swap created matches: " + matches.size());
                    processMove(matches);
                } else {
                    Log.i(TAG, "No match from swap, swapping back.");
                    swapCandies(row, col, tempR, tempC);
                }
            } else if (touchedCandy != null) {
                selectedRow = row; selectedCol = col; selectedCandyObject = touchedCandy;
            } else {
                selectedRow = -1; selectedCol = -1; selectedCandyObject = null;
            }
            invalidate();
        }
    }

    private boolean isAdjacent(int r1, int c1, int r2, int c2) {
        return (r1 == r2 && Math.abs(c1 - c2) == 1) || (c1 == c2 && Math.abs(r1 - r2) == 1);
    }

    private void swapCandies(int r1, int c1, int r2, int c2) {
        Candy candy1 = getCandyAt(r1,c1); Candy candy2 = getCandyAt(r2,c2);
        if (candies != null && r1 < candies.size() && candies.get(r1) != null &&
                r2 < candies.size() && candies.get(r2) != null) { // Added more robust checks
            candies.get(r1).set(c1, candy2);
            candies.get(r2).set(c2, candy1);
            Log.d(TAG, "Swapped ("+r1+","+c1+") with ("+r2+","+c2+")");
        } else {
            Log.e(TAG, "Error in swapCandies: candies list, or row list is null/out of bounds for r1 or r2");
        }
    }

    private Set<Point> checkForMatchesBySwap(int rA, int cA, int rB, int cB) {
        Set<Point> allMatched = new HashSet<>();
        checkLineForMatches(rA, cA, true, allMatched); checkLineForMatches(rA, cA, false, allMatched);
        if(rA != rB || cA != cB) {
            checkLineForMatches(rB, cB, true, allMatched);
            checkLineForMatches(rB, cB, false, allMatched);
        }
        return allMatched;
    }

    private void checkLineForMatches(int r, int c, boolean horizontal, Set<Point> matchesSet) {
        Candy center = getCandyAt(r,c); if (center == null) return;
        ArrayList<Point> line = new ArrayList<>(); line.add(new Point(r,c));
        int type = center.getType();

        for (int i = 1; i < Math.max(gridRows, gridCols); i++) {
            int curR = horizontal ? r : r + i; int curC = horizontal ? c + i : c;
            if (curR < 0 || curR >= gridRows || curC < 0 || curC >= gridCols) break;
            Candy current = getCandyAt(curR, curC);
            if (current != null && current.getType() == type) line.add(new Point(curR,curC)); else break;
        }
        for (int i = 1; i < Math.max(gridRows, gridCols); i++) {
            int curR = horizontal ? r : r - i; int curC = horizontal ? c - i : c;
            if (curR < 0 || curR >= gridRows || curC < 0 || curC >= gridCols) break;
            Candy current = getCandyAt(curR, curC);
            if (current != null && current.getType() == type) line.add(new Point(curR,curC)); else break;
        }
        if (line.size() >= 3) matchesSet.addAll(line);
    }

    private Set<Point> findAllMatchesOnBoard() {
        Set<Point> allMatches = new HashSet<>();
        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                checkLineForMatches(i,j,true,allMatches);
                checkLineForMatches(i,j,false,allMatches);
            }
        }
        return allMatches;
    }

    private void removeMatchedCandies(Set<Point> matchedPoints) {
        if (matchedPoints == null || matchedPoints.isEmpty()) return;
        if (isLevelComplete) return; // Don't add score if level already marked complete

        int pointsToAdd = matchedPoints.size() * 10;
        gameScore += pointsToAdd;
        Log.i(TAG, "Removed " + matchedPoints.size() + " candies. Added " + pointsToAdd + " points. Total score: " + gameScore);

        for (Point p : matchedPoints) {
            if (getCandyAt(p.r, p.c) != null) {
                candies.get(p.r).set(p.c, null);
            }
        }
    }
    /*
    הסבר בעברית: removeMatchedCandies (הוספת ניקוד)
    - כאשר סוכריות מוסרות, אנו מוסיפים ניקוד לשחקן.
    - נוספה בדיקה: אם `isLevelComplete` כבר true, לא מוסיפים עוד ניקוד (למנוע ניקוד כפול אם יש התאמות נוספות בזמן שהדיאלוג עולה).
    - `pointsToAdd`: חישוב פשוט של 10 נקודות לכל סוכריה שהותאמה.
    - `gameScore` מתעדכן עם הנקודות החדשות.
    - הודעה נרשמת ללוג על עדכון הניקוד.
    */

    private void applyGravity() {
        if (candies == null || gridRows <= 0 || gridCols <= 0) return;
        //Log.d(TAG, "Applying gravity (ORIGINAL)..."); // Less verbose logging for gravity
        for (int j = 0; j < gridCols; j++) {
            int writeRow = gridRows - 1;
            for (int readRow = gridRows - 1; readRow >= 0; readRow--) {
                Candy candyToMove = getCandyAt(readRow, j);
                if (candyToMove != null) {
                    if (readRow != writeRow) {
                        candies.get(writeRow).set(j, candyToMove);
                        candies.get(readRow).set(j, null);
                    }
                    writeRow--;
                }
            }
            for (int r = writeRow; r >= 0; r--) {
                if (getCandyAt(r,j) != null) {
                    candies.get(r).set(j, null);
                }
            }
        }
    }

    private void processMove(Set<Point> matchedPoints) {
        if (isLevelComplete) return; // Don't process moves if level is already complete and dialog showing/pending

        Log.i(TAG, "PROCESS MOVE (Checks Level Complete): Matched points: " + (matchedPoints != null ? matchedPoints.size() : 0) + " - " + matchedPoints);
        // printGridState("Before any action in PROCESS MOVE (Checks Level Complete)"); // Can be verbose

        if (matchedPoints != null && !matchedPoints.isEmpty()) {
            removeMatchedCandies(matchedPoints); // Score is added here
            // printGridState("After removeMatchedCandies in PROCESS MOVE");

            applyGravity();
            // printGridState("After applyGravity in PROCESS MOVE");

            invalidate();
            Log.i(TAG, "PROCESS MOVE: Candies removed, gravity applied. gameScore: " + gameScore);

            if (currentLevelConfig != null && gameScore >= currentLevelConfig.getTargetScore()) {
                if (!isLevelComplete) {
                    levelTimerRunning = false;
                    long elapsedTime = System.currentTimeMillis() - startTimeMillis;
                    showLevelCompleteDialog(gameScore, elapsedTime);
                    Log.i(TAG, "LEVEL COMPLETE! Score: " + gameScore + " Target: " + currentLevelConfig.getTargetScore() + " Time: " + elapsedTime + "ms");
                }
            }
        } else {
            Log.i(TAG, "PROCESS MOVE: No initial matches to process from swap.");
        }
    }
    /*
    הסבר בעברית: processMove (בדיקת סיום שלב)
    - נוספה בדיקה בתחילת הפונקציה: אם `isLevelComplete` כבר true, לא מעבדים את המהלך (למנוע פעולות נוספות בזמן שהדיאלוג מוצג או אמור להיות מוצג).
    - לאחר הסרת הסוכריות (שם מתווסף הניקוד) והחלת כוח המשיכה:
    - אנו בודקים אם `currentLevelConfig` אינו null.
    - ואם `gameScore` גדול או שווה ל-`currentLevelConfig.getTargetScore()`.
    - וגם, אם הדיאלוג `isLevelComplete` עדיין לא מוצג.
    - אם כל התנאים מתקיימים:
        - `levelTimerRunning` מוגדר ל-false.
        - `elapsedTime` מחושב.
        - `showLevelCompleteDialog` נקראת עם הניקוד והזמן שהושגו.
        - הודעה נרשמת ללוג שהשלב הושלם.
    */

    private Candy getCandyAt(int r, int c) {
        if (r >= 0 && r < gridRows && c >= 0 && c < gridCols &&
                candies != null && r < candies.size() && candies.get(r) != null && c < candies.get(r).size()) {
            return candies.get(r).get(c);
        }
        return null;
    }

    private void printGridState(String label) {
        Log.d(TAG, "---- GRID STATE: " + label + " ----");
        if (candies == null || candies.isEmpty()) { Log.d(TAG, "Grid is null or empty."); return; }
        for (int i = 0; i < gridRows; i++) {
            StringBuilder sb = new StringBuilder().append("Row ").append(i).append(": ");
            if (i >= candies.size() || candies.get(i) == null) { sb.append("INVALID ROW DATA"); }
            else {
                for (int j = 0; j < gridCols; j++) {
                    if (j >= candies.get(i).size()){ sb.append("X! "); continue; }
                    Candy c = getCandyAt(i,j);
                    sb.append(c == null ? "N  " : String.format("%-3s", c.getType()));
                }
            }
            Log.d(TAG, sb.toString());
        }
        Log.d(TAG, "-----------------------------");
    }

    public void showLevelCompleteDialog(int score, long timeMillis) {
        this.currentScoreForDialog = score;
        this.timeTakenMillisForDialog = timeMillis;
        this.isLevelComplete = true;
        invalidate();
    }
    /*
    הסבר בעברית: showLevelCompleteDialog
    (אין שינוי מהגרסה הקודמת)
    */

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas == null) return;

        if (!allBitmapsLoadedSuccessfully) {
            paint.setColor(Color.RED); canvas.drawRect(0,0,getWidth(),getHeight(),paint);
            paint.setColor(Color.WHITE); paint.setTextSize(40); canvas.drawText("BITMAPS FAILED", 50, getHeight()/2f, paint);
            return;
        }
        if (candies == null || candies.isEmpty() || cellSize <= 0) {
            if (currentLevelConfig == null && !isLevelComplete) { // Only show "Grid not ready" if not in level complete transition
                paint.setColor(Color.LTGRAY); canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
                paint.setColor(Color.BLACK); paint.setTextSize(40); canvas.drawText("Grid not ready", getWidth()/2f - 150, getHeight()/2f, paint);
            } else if (isLevelComplete) {
                // If level complete is showing, we might want a clean background or current game state
                // For now, let it be, dialog will draw over
            }
            // If candies are null/empty but we are expecting to draw a dialog, ensure background is drawn
            if (isLevelComplete && (candies == null || candies.isEmpty())) {
                canvas.drawRect(0,0, getWidth(),getHeight(), backgroundPaint); // Ensure background for dialog
            } else if (candies == null || candies.isEmpty()){
                return; // Grid not ready and not showing dialog, nothing to draw
            }
        }


        if (!(candies == null || candies.isEmpty())) { // Only draw grid if candies exist
            canvas.drawRect(0,0, getWidth(),getHeight(), backgroundPaint);
            for (int i = 0; i < gridRows; i++) {
                if (candies.size() <= i || candies.get(i) == null) continue;
                for (int j = 0; j < gridCols; j++) {
                    if (candies.get(i).size() <= j) continue;
                    Candy candy = getCandyAt(i,j);
                    if (candy == null) continue;

                    int type = candy.getType();
                    if (type < 0 || type >= candyBitmaps.length || candyBitmaps[type] == null) {
                        drawPlaceholder(canvas,i,j, Color.rgb(100,0,100));
                        continue;
                    }
                    Bitmap bmp = candyBitmaps[type];
                    int left = gridOffsetX + j * cellSize; int top = gridOffsetY + i * cellSize;
                    canvas.drawBitmap(bmp, null, new Rect(left, top, left + cellSize, top + cellSize), paint);
                    if (i == selectedRow && j == selectedCol) {
                        canvas.drawRect(left, top, left + cellSize, top + cellSize, highlightPaint);
                    }
                }
            }
        }


        if (isLevelComplete) {
            float dialogWidth = getWidth() * 0.8f;
            float dialogHeight = getHeight() * 0.5f;
            float dialogLeft = (getWidth() - dialogWidth) / 2;
            float dialogTop = (getHeight() - dialogHeight) / 2;

            RectF dialogRect = new RectF(dialogLeft, dialogTop, dialogLeft + dialogWidth, dialogTop + dialogHeight);
            canvas.drawRoundRect(dialogRect, 30f, 30f, dialogBackgroundPaint);

            dialogTextPaint.setTextSize(dialogHeight * 0.18f);
            dialogScoreTextPaint.setTextSize(dialogHeight * 0.12f);

            float textY = dialogTop + dialogHeight * 0.28f;
            canvas.drawText("כל הכבוד!", getWidth() / 2f, textY, dialogTextPaint);

            textY += dialogHeight * 0.22f;
            canvas.drawText("ניקוד: " + currentScoreForDialog, getWidth() / 2f, textY, dialogScoreTextPaint);

            textY += dialogHeight * 0.18f;
            String formattedTime = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(timeTakenMillisForDialog),
                    TimeUnit.MILLISECONDS.toSeconds(timeTakenMillisForDialog) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeTakenMillisForDialog))
            );
            canvas.drawText("זמן: " + formattedTime, getWidth() / 2f, textY, dialogScoreTextPaint);

            textY += dialogHeight * 0.18f;
            dialogTextPaint.setTextSize(dialogHeight * 0.10f);
            canvas.drawText("לחץ להמשך", getWidth() / 2f, textY, dialogTextPaint);
        }
        /*
        הסבר בעברית: onDraw (חלק דיאלוג "סיום שלב")
        (אין שינוי מהגרסה הקודמת, מלבד שימוש במשתנים ששמם שונה: currentScoreForDialog, timeTakenMillisForDialog)
        - נוספה לוגיקה קטנה כדי לטפל במקרה שהלוח ריק אבל הדיאלוג עדיין צריך להיות מוצג.
        */
    }

    private void drawPlaceholder(Canvas canvas, int r, int c, int color) {
        Paint p = new Paint(); p.setColor(color); p.setStyle(Paint.Style.FILL);
        int left = gridOffsetX + c * cellSize; int top = gridOffsetY + r * cellSize;
        canvas.drawRect(left,top,left+cellSize, top+cellSize, p);
        p.setColor(Color.BLACK);p.setTextSize(cellSize/2f);p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("?", left+cellSize/2f, top+cellSize/2f + p.getTextSize()/3f, p);
    }
}
