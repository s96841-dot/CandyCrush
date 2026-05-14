package com.example.CandyCrush.utils;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles Firebase account creation and Firestore profile saving for new players.
 */
public class RegistrationManager {
    private static final String TAG = "RegistrationManager";

    private final Activity activity;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public RegistrationManager(Activity activity) {
        this.activity = activity;
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public interface OnResultCallback {
        void onResult(boolean success, String message);
    }

    public void startRegistration(
            String email,
            String password,
            String nickname,
            int age,
            int level,
            OnResultCallback callback
    ) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(nickname)) {
            callback.onResult(false, "Please fill in all fields");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (!task.isSuccessful()) {
                        String message = task.getException() == null
                                ? "Registration failed."
                                : task.getException().getMessage();
                        Log.w(TAG, "Firebase Auth registration failed", task.getException());
                        callback.onResult(false, message);
                        return;
                    }

                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser == null) {
                        callback.onResult(false, "Registration succeeded but failed to get user details.");
                        return;
                    }

                    updateAuthProfileAndSaveData(firebaseUser, email, nickname, age, level, callback);
                });
    }

    private void updateAuthProfileAndSaveData(
            FirebaseUser firebaseUser,
            String email,
            String nickname,
            int age,
            int level,
            OnResultCallback callback
    ) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(nickname)
                .build();

        firebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Failed to update Firebase Auth display name", task.getException());
                    }
                    saveUserToFirestore(firebaseUser, email, nickname, age, level, callback);
                });
    }

    private void saveUserToFirestore(
            FirebaseUser firebaseUser,
            String email,
            String nickname,
            int age,
            int level,
            OnResultCallback callback
    ) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", firebaseUser.getUid());
        user.put("email", email);
        user.put("nickname", nickname);
        user.put("age", age);
        user.put("level", level);

        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "User data successfully written to Firestore.");
                    callback.onResult(true, "Registration successful!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error writing user document to Firestore", e);
                    firebaseUser.delete();
                    callback.onResult(false, "Failed to save user data: " + e.getMessage());
                });
    }
}