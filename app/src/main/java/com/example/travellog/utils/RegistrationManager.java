package com.example.travellog.utils;
import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

public class RegistrationManager {
    private static final String TAG = "RegistrationManager";

    private static final int REGISTRATION_PHASE_VALIDATE_USER_INFO = 0;
    private static final int REGISTRATION_PHASE_CREATE_USER = 1;
    private static final int REGISTRATION_PHASE_UPLOAD_PIC = 2;
    private static final int REGISTRATION_PHASE_UPLOAD_DATA = 3;
    private static final int REGISTRATION_PHASE_DONE = 4;
    private int registrationPhase;


    String email;
    String password;

    Activity activity;

    OnResultCallback onResultCallback;

    public RegistrationManager(Activity activity) {
        Log.d(TAG, "RegistrationManager: started");
        this.activity = activity;


        registrationPhase = REGISTRATION_PHASE_VALIDATE_USER_INFO;

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
        phaseDone();
    }

    private void uploadProfilePictureToSupabase() {
        phaseDone();
    }


    private void saveUserToFirestore() {
        phaseDone();
    }

}
