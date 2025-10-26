package com.example.travellog.gamecore; // Make sure this package name matches your project structure

public class Candy {
    private int type;

    // --- Define Candy Types as Constants ---
    // Regular, matchable candy types
    public static final int TYPE_NORMAL_0 = 0;
    public static final int TYPE_NORMAL_1 = 1;public static final int TYPE_NORMAL_2 = 2;
    public static final int TYPE_NORMAL_3 = 3;
    public static final int TYPE_NORMAL_4 = 4;
    public static final int TYPE_NORMAL_5 = 5;
    // Add more if you have more than 6 regular candy graphics (e.g., candy_type_0 to candy_type_5)

    // Special candy types
    public static final int TYPE_BOMB = 6;
    public static final int TYPE_EXPLODING_BOMB = 7; // Visual state for bomb (will add sprite later)
    public static final int TYPE_MEGA_BOMB = 8;      // For 5-in-a-row

    // --- Counts for managing candy types ---
    // Number of distinct regular candy graphics/types that can be matched
    public static final int NUMBER_OF_REGULAR_CANDY_TYPES = 6; // Assuming 0-5 are your regular candies
    public static final int TYPE_ROCKET = 9; // Previously a placeholder
    // Total number of unique sprites we might need to load (regular + special)
    // Update this as we add more special types
    public static final int TOTAL_NUMBER_OF_SPRITES = 10; // 6 regular + Bomb + Exploding_Bomb + Mega_Bomb

    /**
     * Constructor for a Candy object.
     * @param type The type of the candy, corresponding to a constant like TYPE_NORMAL_0, TYPE_BOMB, etc.
     */
    public Candy(int type) {
        this.type = type;
    }

    /**
     * Gets the type of the candy.
     * @return An integer representing the candy's type.
     */
    public int getType() {
        return type;
    }
    public boolean isRocket() {
        return type == TYPE_ROCKET;
    }
    /**
     * Sets the type of the candy.
     * @param type The new type for the candy.
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Checks if this candy is a regular, matchable type.
     * @return true if it's a normal candy, false otherwise.
     */
    public boolean isRegularCandy() {
        return type >= TYPE_NORMAL_0 && type < NUMBER_OF_REGULAR_CANDY_TYPES;
    }

    /**
     * Checks if this candy is any kind of special (non-regular) candy.
     * @return true if it's a special candy, false otherwise.
     */
    public boolean isSpecialCandy() {
        return isBomb() || isMegaBomb() || isRocket(); // Add isRocket()
    }

    /**
     * Checks if this candy is a standard bomb.
     * @return true if the candy type is TYPE_BOMB.
     */
    public boolean isBomb() {
        return type == TYPE_BOMB;
    }

    /**
     * Checks if this candy is a mega bomb.
     * @return true if the candy type is TYPE_MEGA_BOMB.
     */
    public boolean isMegaBomb() {
        return type == TYPE_MEGA_BOMB;
    }

    /**
     * Optional: A string representation of the candy, useful for debugging.
     * @return A string like "Candy(T0)", "Candy(BOMB)".
     */
    @Override
    public String toString() {
        if (isRegularCandy()) {
            return "Candy(T" + type + ")";
        }
        switch (type) {
            case TYPE_BOMB:
                return "Candy(BOMB)";
            case TYPE_EXPLODING_BOMB:
                return "Candy(XPLODE)"; // Short for exploding
            case TYPE_MEGA_BOMB:
                return "Candy(MEGA)";
            default:
                return "Candy(Unknown:" + type + ")";
        }
    }
    public Candy clone() {
        return new Candy(this.type);
    }

    // equals() and hashCode() ... (same as before, still optional for now)
    // @Override
    // public boolean equals(Object o) {
    //     if (this == o) return true;
    //     if (o == null || getClass() != o.getClass()) return false;
    //     Candy candy = (Candy) o;
    //     return type == candy.type;
    // }
    //
    // @Override
    // public int hashCode() {
    //     return type;
    // }
}
