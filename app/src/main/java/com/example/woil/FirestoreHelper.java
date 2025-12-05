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

    // 2) create/update profiles/{uid}
    public void saveProfile(String uid, Map<String,Object> profileData,
                            OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (uid == null) { if (onFailure != null) onFailure.onFailure(new Exception("uid null")); return; }
        profileData.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("profiles").document(uid)
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(onSuccess == null ? aVoid -> Log.d(TAG,"profile saved") : onSuccess)
                .addOnFailureListener(onFailure == null ? e -> Log.e(TAG,"saveProfile failed", e) : onFailure);
    }

    // 3) create job
    public void createJob(Map<String,Object> jobData,
                          OnSuccessListener<DocumentReference> onSuccess,
                          OnFailureListener onFailure) {
        jobData.put("createdAt", FieldValue.serverTimestamp());
        db.collection("jobs").add(jobData)
                .addOnSuccessListener(onSuccess == null ? ref -> Log.d(TAG,"job created " + ref.getId()) : onSuccess)
                .addOnFailureListener(onFailure == null ? e -> Log.e(TAG,"createJob failed", e) : onFailure);
    }

    // 4) create match with a transaction (atomic: ensure job still OPEN)
    public void acceptJobWithTransaction(final String jobId, final String workerUid, final long wageAgreed,
                                         OnSuccessListener<String> onSuccess,
                                         OnFailureListener onFailure) {
        final DocumentReference jobRef = db.collection("jobs").document(jobId);
        db.runTransaction((Transaction.Function<String>) transaction -> {
            DocumentSnapshot jobSnap = transaction.get(jobRef);
            if (!jobSnap.exists()) throw new FirebaseFirestoreException("Job not found", FirebaseFirestoreException.Code.NOT_FOUND);
            String status = jobSnap.getString("status");
            if (status == null) status = "OPEN";
            if (!status.equals("OPEN")) {
                throw new FirebaseFirestoreException("Job not OPEN (status="+status+")", FirebaseFirestoreException.Code.ABORTED);
            }
            // create match doc
            Map<String,Object> match = new HashMap<>();
            match.put("jobId", jobId);
            match.put("workerUid", workerUid);
            match.put("clientUid", jobSnap.getString("clientUid"));
            match.put("acceptedAt", FieldValue.serverTimestamp());
            match.put("status", "ACTIVE");
            match.put("wageAgreed", wageAgreed);

            DocumentReference matchRef = db.collection("matches").document(); // auto-id
            transaction.set(matchRef, match);
            // update job
            transaction.update(jobRef, "assignedUid", workerUid, "status", "MATCHED", "matchedAt", FieldValue.serverTimestamp());
            return matchRef.getId();
        }).addOnSuccessListener(id -> {
            if (onSuccess != null) onSuccess.onSuccess(id);
            else Log.d(TAG, "Transaction success, matchId=" + id);
        }).addOnFailureListener(e -> {
            if (onFailure != null) onFailure.onFailure(e);
            else Log.e(TAG, "Transaction failed", e);
        });
    }

    // 5) ensure chat document exists and send message
    public void createOrEnsureChatAndSendMessage(String chatId, String jobId, String senderUid, Map<String,Object> msg,
                                                 OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        DocumentReference chatRef = db.collection("chats").document(chatId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snap = transaction.get(chatRef);
            if (!snap.exists()) {
                Map<String,Object> chat = new HashMap<>();
                chat.put("participants", Arrays.asList(msg.get("senderUid"), msg.get("receiverUid")));
                chat.put("jobId", jobId);
                chat.put("lastMessageAt", FieldValue.serverTimestamp());
                transaction.set(chatRef, chat);
            } else {
                transaction.update(chatRef, "lastMessageAt", FieldValue.serverTimestamp());
            }
            // add message in subcollection
            CollectionReference msgs = chatRef.collection("messages");
            msg.put("createdAt", FieldValue.serverTimestamp());
            // Use a new doc in subcollection
            DocumentReference newMsgRef = msgs.document();
            transaction.set(newMsgRef, msg);
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (onSuccess != null) onSuccess.onSuccess(aVoid);
            else Log.d(TAG,"message sent");
        }).addOnFailureListener(e -> {
            if (onFailure != null) onFailure.onFailure(e);
            else Log.e(TAG,"sendMessage failed", e);
        });
    }

    // 6) create panic alert
    public void createPanicAlert(Map<String,Object> alertData, OnSuccessListener<DocumentReference> onSuccess, OnFailureListener onFailure) {
        alertData.put("timestamp", FieldValue.serverTimestamp());
        alertData.put("status", "OPEN");
        db.collection("panic_alerts").add(alertData)
                .addOnSuccessListener(onSuccess == null ? ref -> Log.d(TAG,"panic created "+ref.getId()) : onSuccess)
                .addOnFailureListener(onFailure == null ? e -> Log.e(TAG,"panic create failed", e) : onFailure);
    }

    // 7) wearable event
    public void createWearableEvent(Map<String,Object> eventData, OnSuccessListener<DocumentReference> onSuccess, OnFailureListener onFailure) {
        eventData.put("timestamp", FieldValue.serverTimestamp());
        db.collection("wearable_events").add(eventData)
                .addOnSuccessListener(onSuccess == null ? ref -> Log.d(TAG,"event added "+ref.getId()) : onSuccess)
                .addOnFailureListener(onFailure == null ? e -> Log.e(TAG,"event failed", e) : onFailure);
    }
}
