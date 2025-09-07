package com.example.travellog.utils;
import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegistrationManager {
    private static final String TAG = "RegistrationManager";

    private static final int REGISTRATION_PHASE_VALIDATE_USER_INFO = 0;
    private static final int REGISTRATION_PHASE_CREATE_USER = 1;
    private static final int REGISTRATION_PHASE_UPLOAD_PIC = 2;
    private static final int REGISTRATION_PHASE_UPLOAD_DATA = 3;
    private static final int REGISTRATION_PHASE_DONE = 4;
    private int registrationPhase;


    String email;
    FirebaseAuth auth;
    String userId;
    String password;

    Activity activity;

    OnResultCallback onResultCallback;

    public RegistrationManager(Activity activity) {
        Log.d(TAG, "RegistrationManager: started");
        this.activity = activity;


        registrationPhase = REGISTRATION_PHASE_VALIDATE_USER_INFO;
        auth = FirebaseAuth.getInstance();

    }

    public void startRegistration(String email,
                                  String password,
                                  OnResultCallback onResultCallback)
    {
        this.onResultCallback = onResultCallback;
        this.email = email;
        this.password = password;

        executeNextPhase();
    }


    public interface OnResultCallback {
        void onResult(boolean success, String message);
    }


    private void phaseDone()
    {
        registrationPhase++;
        executeNextPhase();
    }

    private void phaseFailed(String message)
    {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.delete();
        }

        Log.e(TAG, "phaseFailed: registration failed: message: " + message);
        registrationPhase = REGISTRATION_PHASE_VALIDATE_USER_INFO;
        onResultCallback.onResult(false, message);
    }

    private void executeNextPhase()
    {
        Log.d(TAG, "executeNextPhase: executing phase: " + registrationPhase);

        if(registrationPhase == REGISTRATION_PHASE_VALIDATE_USER_INFO)
        {
            Log.i(TAG, "executeNextPhase: fetching user info from form");
            validateUserInfo();
        }
        else if(registrationPhase == REGISTRATION_PHASE_CREATE_USER)
        {
            Log.i(TAG, "executeNextPhase: Creating user with Firebase Auth");
            createUser();
        }
        else if(registrationPhase == REGISTRATION_PHASE_UPLOAD_PIC)
        {
            Log.i(TAG, "executeNextPhase: Uploading profile picture to supabase");
            uploadProfilePictureToSupabase();
        }
        else if(registrationPhase == REGISTRATION_PHASE_UPLOAD_DATA)
        {
            Log.i(TAG, "executeNextPhase: Uploading user data to firestore");
            saveUserToFirestore();
        }
        else if(registrationPhase == REGISTRATION_PHASE_DONE)
        {
            Log.i(TAG, "executeNextPhase: Registration done");
            auth.signOut();
            onResultCallback.onResult(true, "Registration successful!");
        }
    }

    private void validateUserInfo() {
        Log.d(TAG, "Starting registration for email: " + email );

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)  ) {
            Log.w(TAG, "Validation failed: missing fields");
            phaseFailed("Please fill in all fields");
            return;
        }

        phaseDone();
    }

    private void createUser()
    {
        Log.d(TAG, "createUser: Creating user with Firebase Auth");

// Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                userId = user.getUid();
                                Log.i(TAG, "Firebase Auth registration successful. UID: " + userId);
                                phaseDone();
                            } else {
                                Log.e(TAG, "Firebase Auth registration succeeded but user is null");
                                phaseFailed("user is null");
                            }
                        } else {
                            Log.e(TAG, "Firebase Auth registration failed", task.getException());
                            phaseFailed(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                        }
                    }
                });

    }

    private void uploadProfilePictureToSupabase() {
        phaseDone();
    }


    private void saveUserToFirestore() {
        phaseDone();
    }

}
