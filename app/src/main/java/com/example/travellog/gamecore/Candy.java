package com.example.travellog.gamecore; // Make sure this package name matches your project structure

public class Candy {
    private int type; // This integer will determine which image to use
    // e.g., 0 for candy_type_0.png, 1 for candy_type_1.png, etc.

    // Other properties might be added later, like:
    // private boolean isSelected;
    // private int row;
    // private int col;

    /**
     * Constructor for a Candy object.
     * @param type The type of the candy, corresponding to an image.
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

    /**
     * Optional: A string representation of the candy, useful for debugging.
     * @return A string like "Candy(T0)", "Candy(T1)".
     */
    @Override
    public String toString() {
        return "Candy(T" + type + ")";
    }

    // You could add equals() and hashCode() if you plan to store Candies in Sets or use them as Map keys,
    // but it's not strictly necessary for basic display and matching.
    // @Override
    // public boolean equals(Object o) {
    //     if (this == o) return true;
    //     if (o == null || getClass() != o.getClass()) return false;
    //     Candy candy = (Candy) o;
    //     return type == candy.type; // Simple equality based on type
    // }
    //
    // @Override
    // public int hashCode() {
    //     return type; // Simple hash code based on type
    // }
}
