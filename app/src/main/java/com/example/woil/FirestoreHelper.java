package com.example.woil;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Firestore helper for creating users/{uid} and saving profiles/{uid}.
 * saveProfile returns Task<Void> so callers can attach success/failure listeners.
 */
public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Create or update users/{uid}
     * authProviders: e.g., ["phone"] ; phoneVerified true if OTP verified
     */
    public void createUserDoc(String uid, String phone, String role, String email, String username, boolean phoneVerified) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        if (phone != null) user.put("phone", phone);
        if (email != null && !email.isEmpty()) user.put("email", email);
        if (username != null && !username.isEmpty()) user.put("username", username);
        user.put("role", role);
        user.put("authProviders", Arrays.asList("phone")); // adapt if other providers added
        user.put("phoneVerified", phoneVerified);
        boolean emailVerified = false;
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                emailVerified = FirebaseAuth.getInstance().getCurrentUser().isEmailVerified();
            }
        } catch (Exception e) {
            emailVerified = false;
        }
        user.put("emailVerified", emailVerified);
        user.put("nicVerified", false);
        user.put("createdAt", FieldValue.serverTimestamp());
        user.put("lastSeenAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "users/" + uid + " created/updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create user doc", e));
    }

    /**
     * Save profile at profiles/{uid}
     * Returns the Task<Void> so caller can attach listeners.
     */
    public Task<Void> saveProfile(String uid, Map<String, Object> profileData) {
        return db.collection("profiles").document(uid)
                .set(profileData, SetOptions.merge());
    }
}
