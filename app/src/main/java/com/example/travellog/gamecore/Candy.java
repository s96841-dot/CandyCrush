package com.example.travellog.gamecore; // Should already be correct

import android.graphics.Color; // Needed for the color definitions
import java.util.Random;     // Needed for random number generation

public class Candy {
    private int id;
    private int color; // This variable will store the candy's color
    private int row;   // This variable will store the candy's row
    private int col;   // This variable will store the candy's column

    // Static array of possible candy colors for getRandomCandyColor()
    private static final int[] CANDY_COLORS = {
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA
            // You can add more Color.XXX constants here
    };

    private static final Random random = new Random(); // For generating random numbers

    // Constructor
    public Candy(int id, int color, int row, int col) {
        this.id = id;
        this.color = color;
        this.row = row;
        this.col = col;
    }

    // --- GETTER METHODS ---
    // GameGridView will call these

    public int getId() {
        return id;
    }

    /**
     * Returns the Color integer for this candy.
     * Make sure this method exists and is public.
     */
    public int getColor() { // <--- For "Cannot resolve method 'getColor' in 'Candy'"
        return this.color;
    }

    /**
     * Returns the column index of this candy in the grid.
     * Make sure this method exists and is public.
     */
    public int getCol() {   // <--- For "Cannot resolve method 'getCol' in 'Candy'"
        return this.col;
    }

    /**
     * Returns the row index of this candy in the grid.
     * Make sure this method exists and is public.
     */
    public int getRow() {   // <--- For "Cannot resolve method 'getRow' in 'Candy'"
        return this.row;
    }


    // --- STATIC METHOD for random color ---
    // GameGridView will call this as Candy.getRandomCandyColor()

    /**
     * Returns a random color integer from the CANDY_COLORS array.
     * Make sure this method exists, is public, and is static.
     */
    public static int getRandomCandyColor() { // <--- For "Cannot resolve method 'getRandomCandyColor' in 'Candy'"
        return CANDY_COLORS[random.nextInt(CANDY_COLORS.length)];
    }


    // --- Optional: SETTER METHODS ---
    // You might need these later if candies can change properties

    public void setRow(int row) {
        this.row = row;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setId(int id) {
        this.id = id;
    }
}
