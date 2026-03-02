package com.example.CandyCrush.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;

import com.example.CandyCrush.R;

public final class BackgroundMusicManager {

    private static final String TAG = "BackgroundMusicManager";
    private static MediaPlayer mediaPlayer;
    private static int activeScreens;

    private BackgroundMusicManager() {}

    public static synchronized void onScreenStart(Context context) {
        activeScreens++;
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.background_music);
            if (mediaPlayer == null) {
                Log.e(TAG, "Could not create MediaPlayer for background music.");
                return;
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0.35f, 0.35f);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
        }

        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public static synchronized void onScreenStop() {
        if (activeScreens > 0) activeScreens--;
        if (activeScreens == 0 && mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }
    }

    public static synchronized void release() {
        activeScreens = 0;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}