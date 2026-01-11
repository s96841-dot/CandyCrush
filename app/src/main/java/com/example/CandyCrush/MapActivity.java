package com.example.CandyCrush;

import android.content.Intent;import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MapActivity extends AppCompatActivity {

    private int lastUnlockedLevel = 1; // זה בהמשך יגיע מה-Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // מוצאים את הגריד שהגדרנו ב-XML
        GridLayout levelsGrid = findViewById(R.id.levelsGrid);

        // לולאה שיוצרת 12 שלבים
        for (int i = 1; i <= 12; i++) {
            Button levelButton = new Button(this);
            levelButton.setText("Level " + i);
            final int levelNum = i;

            // יצירת הגדרות עיצוב לכפתור בתוך הגריד (כאן אנחנו קובעים את הרווחים)
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();

            // קביעת רווח (Margin) של 16 פיקסלים מכל צד
            int marginInPx = (int) (8 * getResources().getDisplayMetrics().density);
            params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);

            // הגדרת משקל שווה לכל כפתור כדי שיתפרסו יפה
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

            levelButton.setLayoutParams(params);

            // בדיקה אם השלב פתוח או נעול
            if (i > lastUnlockedLevel) {
                levelButton.setEnabled(false);
                levelButton.setAlpha(0.5f); // הופך את הכפתור לחצי שקוף (נראה נעול)
            }

            // מה קורה כשלוחצים על שלב
            levelButton.setOnClickListener(v -> {
                Intent intent = new Intent(MapActivity.this, FeedActivity.class);
                intent.putExtra("SELECTED_LEVEL", levelNum);
                startActivity(intent);
            });

            // הוספת הכפתור לגריד
            levelsGrid.addView(levelButton);
        }
    }
}
