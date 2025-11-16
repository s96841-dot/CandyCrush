package com.example.travellog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travellog.FeedActivity;
import com.example.travellog.R;
import com.example.travellog.utils.PostsAdapter;
import com.example.travellog.utils.TravelPost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.util.ArrayList;
import java.util.List;

public class MyPostsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostsAdapter postsAdapter;

    private List<TravelPost> posts;

    private String nickname;
    private int level;

    private static final String TAG = "MyPostsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_posts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        readUserData();

        TextView tvNickname = findViewById(R.id.title);
        tvNickname.setText(nickname + " (Lvl. " + level + ")");

        Button addPostBtn = findViewById(R.id.btn_back);
        addPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MyPostsActivity.this, FeedActivity.class);
                startActivity(intent);
                finish();
            }
        });
        posts = new ArrayList<>();
        initRecyclerView();
        loadPosts();
    }

    private void initRecyclerView()
    {
        recyclerView = findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsAdapter = new PostsAdapter(posts);
        recyclerView.setAdapter(postsAdapter);
    }


    private void readUserData(){
        Log.d(TAG, "readUserData: start");
        //about to read data from userInfo.xml
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);

        // nickname - "N/A" is a default value if nickname is not found in the file
        nickname = sharedPreferences.getString("nickname", "N/A");
        Log.d(TAG, "readUserData: nickname: " + nickname);

        // level - 1 is a default value if level is not found in the file
        level = sharedPreferences.getInt("level", 1);
        Log.d(TAG, "readUserData: level: " + level);
    }

    private void loadPosts() {
        Log.d(TAG, "loadPosts: start");

        String userId = FirebaseAuth.getInstance().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .whereEqualTo("ownerUid", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    posts.clear();
                    Log.d(TAG, "loadPosts succeeded: " + queryDocumentSnapshots.size() + " documents");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TravelPost post = doc.toObject(TravelPost.class);
                        posts.add(post);
                    }

                    postsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load posts: " + e.getMessage()));
    }

}