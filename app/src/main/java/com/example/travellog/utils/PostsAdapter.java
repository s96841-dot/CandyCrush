package com.example.travellog.utils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: adding post item #" + position);
        holder.titleTextView.setText("Post #" + position);
    }

    @Override
    public int getItemCount()
    {
        return 100;
    }



    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_post_title);
        }
    }

}
