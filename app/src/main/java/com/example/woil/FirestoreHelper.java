package com.example.woil;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.Transaction.Function;
import com.google.firebase.firestore.Transaction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {
    private static final String TAG = "FirestoreHelper";
    private final FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    // 1) create or update users/{uid} after auth
    public void createUserDoc(String uid, String phone, String role,
                              OnSuccessListener<Void> onSuccess,
                              OnFailureListener onFailure) {
        if (uid == null) { if (onFailure != null) onFailure.onFailure(new Exception("uid null")); return; }
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("phone", phone);
        user.put("role", role);
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(onSuccess == null ? aVoid -> Log.d(TAG,"createUserDoc OK") : onSuccess)
                .addOnFailureListener(onFailure == null ? e -> Log.e(TAG, "createUserDoc failed", e) : onFailure);
    }


}
