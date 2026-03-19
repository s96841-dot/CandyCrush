package com.example.CandyCrush;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class CandyCrushApplication extends Application {
    private static final String TAG = "CandyCrushApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
            Log.i(TAG, "App Check initialized with Debug provider.");
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );
            Log.i(TAG, "App Check initialized with Play Integrity provider.");
        }
    }
}