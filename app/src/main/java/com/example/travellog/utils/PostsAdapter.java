package com.example.travellog.utils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Import ImageView
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travellog.R;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private static final String TAG = "PostsAdapter";

    public PostsAdapter() {
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post, parent, false);
        return new PostViewHolder(view);
    }

    // --- STEP 2: EXPANDED onBindViewHolder ---
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: binding post item #" + position);

        // Set text for all TextViews using the 'position' to make each item unique
        holder.titleTextView.setText("Post #" + position);
        holder.descriptionTextView.setText("Description #" + position);
        holder.createdAtTextView.setText("Created at #" + position);
        holder.nicknameTextView.setText("Nickname #" + position);

        // ====================================================================
        // זהו הלוגיקה החדשה לשינוי התמונה בהתבסס על המיקום (position).
        // ====================================================================

        // אופרטור המודולו (%) מחזיר את שארית החלוקה.
        // אם השארית של חלוקת המיקום ב-2 היא 0, המספר זוגי.
        if (position % 2 == 0) {
            // קבע את התמונה עבור מיקומים זוגיים
            holder.postImageView.setImageResource(R.drawable.ic_launcher_foreground);
        } else {
            // קבע את התמונה עבור מיקומים אי-זוגיים
            holder.postImageView.setImageResource(R.drawable.ic_launcher_background);
        }
    }


    @Override
    public int getItemCount() {
        return 100;
    }

    // --- STEP 1: EXPANDED PostViewHolder ---
    static class PostViewHolder extends RecyclerView.ViewHolder {

        // Declare all the necessary UI components from your post.xml layout
        ImageView postImageView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView createdAtTextView;
        TextView nicknameTextView;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            // Link each variable to its view using findViewById
            postImageView = itemView.findViewById(R.id.iv_post_image);
            titleTextView = itemView.findViewById(R.id.tv_post_title);
            descriptionTextView = itemView.findViewById(R.id.tv_post_description);
            createdAtTextView = itemView.findViewById(R.id.tv_post_created_at);
            nicknameTextView = itemView.findViewById(R.id.tv_post_nickname);
        }
    }
}
