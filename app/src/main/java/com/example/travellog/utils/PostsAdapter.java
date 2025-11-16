package com.example.travellog.utils;

import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Import ImageView
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travellog.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private static final String TAG = "PostsAdapter";
    private List<TravelPost> posts;

    public PostsAdapter(List<TravelPost> posts) {
        this.posts = posts;
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
        TravelPost post = posts.get(position);

        Log.d(TAG, "onBindViewHolder: binding post item #" + position);

        // Set text for all TextViews using the 'position' to make each item unique
        holder.titleTextView.setText(post.getTitle());
        holder.descriptionTextView.setText(post.getDescription());
        holder.createdAtTextView.setText( timestampToString(post.getCreatedAt()));
        holder.nicknameTextView.setText(post.getOwnerNickname());
        String profilePicturePath = "images/profile-pics/" + post.getOwnerUid() + ".jpg";
        String profilePictureUrl = SupabaseStorageHelper.getFileSupabaseUrl(profilePicturePath);

        Glide.with(holder.itemView)
                .load(profilePictureUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.postImageView);

    }


    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: num of posts: " + posts.size());
        return posts.size();
    }
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
            nicknameTextView = itemView.findViewById(R.id.tv_post_owner);
        }
    }
    private String timestampToString(Timestamp timestamp) {

        Date messageDate = timestamp.toDate();

        boolean isToday = DateUtils.isToday(messageDate.getTime());

        SimpleDateFormat fmt;
        if (isToday) {
            // only show hour:minute, e.g. "14:35"
            fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else {
            // only show date, e.g. "Aug 03, 2025"
            fmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        }

        return fmt.format(messageDate);
    }

}
