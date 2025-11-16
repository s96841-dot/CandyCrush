package com.example.travellog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button; // Import the Button class
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
    private Button submitButton; // Add a Button reference
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
        Button addPostButton = findViewById(R.id.btn_submit_post);
        addPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Add Post button clicked. Navigating to AddPostActivity.");
                sendPost();
            }
        });

        // 2. קישור התכונות לרכיבים על ידי findViewById
        editTextPostTitle = findViewById(R.id.et_post_title);
        editTextPostDescription = findViewById(R.id.et_post_description);
        submitButton = findViewById(R.id.btn_submit_post); // Link the button

        // 3. הגדרת OnClickListener עבור הכפתור
        submitButton.setOnClickListener(v -> {
            sendPost(); // קריאה לפעולה החדשה
        });
    }

    /**
     * פעולה זו אוספת את הנתונים ושולחת אותם ל-Firestore.
     * הפעולה נקראת כעת מתוך ה-OnClickListener.
     */
    private void sendPost() {
        Log.d(TAG, "sendPost: start");

        // Basic validation
        String title = editTextPostTitle.getText().toString().trim();
        String description = editTextPostDescription.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Title and description cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        TravelPost post = createTravelPost(title, description);

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
     * @param title הכותרת של הפוסט.
     * @param description התיאור של הפוסט.
     * @return אובייקט TravelPost המכיל את כל המידע.
     */
    private TravelPost createTravelPost(String title, String description) {
        String ownerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        String ownerNickname = sharedPreferences.getString("nickname", "Anonymous");

        Timestamp createdAt = new Timestamp(new Date());
        return new TravelPost(title, description, ownerUid, ownerNickname, createdAt);
    }
}
