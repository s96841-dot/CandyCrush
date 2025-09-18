package com.example.travellog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.travellog.utils.TravelPost;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class AddPostActivity extends AppCompatActivity {

    // 1. הוספת תכונות עבור הרכיבים הגרפיים
    private EditText editTextPostTitle;
    private EditText editTextPostDescription;
    private static final String TAG = "AddPostActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_post);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. קישור התכונות לרכיבים על ידי findViewById
        editTextPostTitle = findViewById(R.id.et_post_title); // יש לוודא שזה ה-ID הנכון בקובץ ה-XML
        editTextPostDescription = findViewById(R.id.et_post_description); // יש לוודא שזה ה-ID הנכון בקובץ ה-XML
    }

    /**
     * פעולה זו נקראת בעת לחיצה על כפתור השליחה.
     * יש להגדיר בקובץ ה-XML של הכפתור: android:onClick="sendPost"
     * @param view האובייקט של הכפתור שנלחץ
     */
    public void sendPost(View view) {
        Log.d(TAG, "sendPost: start");
        TravelPost post = createTravelPost();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    Toast.makeText(AddPostActivity.this, "Log saved successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close this activity and return to FeedActivity
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(AddPostActivity.this, "Error saving log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        Log.d(TAG, "sendPost: done");

    }

    /**
     * אוספת את כל הנתונים הנדרשים מהטופס, מהמשתמש המחובר ומ-SharedPreferences,
     * ויוצרת אובייקט TravelPost חדש.
     * @return אובייקט TravelPost המכיל את כל המידע.
     */
    private TravelPost createTravelPost() {
        // איסוף המידע מהטופס
        String title = editTextPostTitle.getText().toString();
        String description = editTextPostDescription.getText().toString();

        // איסוף המידע מ-Firebase Authentication
        String ownerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // איסוף המידע מקובץ SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo.xml", MODE_PRIVATE);
        // יש לוודא שהמפתח "nickname" הוא הנכון
        String ownerNickname = sharedPreferences.getString("nickname", "Anonymous");

        // יצירת חותמת זמן נוכחית
        Timestamp createdAt = new Timestamp(new Date());

        // יצירת והחזרת אובייקט TravelPost
        // (בהנחה שקיים בנאי מתאים במחלקה TravelPost)
        return new TravelPost(title, description, ownerUid, ownerNickname, createdAt);
    }
}
