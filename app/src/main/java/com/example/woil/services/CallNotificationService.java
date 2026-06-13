package com.example.woil.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.woil.R;
import com.example.woil.ui.CallActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class CallNotificationService extends Service {

    private static final String TAG = "CallNotificationServ";
    private static final String SERVICE_CHANNEL_ID = "woil_service_channel";
    private static final String CALL_CHANNEL_ID = "incoming_call_channel";
    private static final int SERVICE_NOTIFICATION_ID = 1001;
    private static final int CALL_NOTIFICATION_ID = 1002;

    private ListenerRegistration incomingCallListener;
    private String activeCallId = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        createNotificationChannels();
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            startCallListener(user.getUid());
        } else {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        
        Notification notification = createServiceNotification();
        startForeground(SERVICE_NOTIFICATION_ID, notification);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            startCallListener(user.getUid());
        } else {
            stopSelf();
        }

        return START_STICKY;
    }

    private void startCallListener(String currentUid) {
        if (incomingCallListener != null) return;

        Log.d(TAG, "Starting Firestore listener for calls. ReceiverUid: " + currentUid);
        incomingCallListener = FirebaseFirestore.getInstance().collection("calls")
                .whereEqualTo("receiverUid", currentUid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Incoming call listener failed.", e);
                        return;
                    }
                    if (snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        DocumentSnapshot doc = dc.getDocument();
                        String callId = doc.getString("callId");
                        String status = doc.getString("status");
                        String callerUid = doc.getString("callerUid");
                        String callerName = doc.getString("callerName");
                        String callType = doc.getString("type");

                        Log.d(TAG, "Call update detected. CallId: " + callId + ", Status: " + status + ", Change: " + dc.getType());

                        if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                            if ("INITIATED".equals(status)) {
                                activeCallId = callId;
                                triggerIncomingCallNotification(callId, callerUid, callerName, callType);
                                launchCallActivity(callId, callerUid, callerName, callType);
                            } else if (callId != null && callId.equals(activeCallId)) {
                                Log.d(TAG, "Active call status changed to " + status + ". Canceling notification.");
                                cancelCallNotification();
                            }
                        } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            if (callId != null && callId.equals(activeCallId)) {
                                Log.d(TAG, "Active call document removed. Canceling notification.");
                                cancelCallNotification();
                            }
                        }
                    }
                });
    }

    private void triggerIncomingCallNotification(String callId, String callerUid, String callerName, String callType) {
        Log.d(TAG, "Triggering incoming call notification for CallId: " + callId);
        
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra("callId", callId);
        intent.putExtra("contactUid", callerUid);
        intent.putExtra("contactName", callerName);
        intent.putExtra("callType", callType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                callId.hashCode(),
                intent,
                pendingIntentFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle("Incoming " + (("audio".equalsIgnoreCase(callType)) ? "Voice" : "Video") + " Call")
                .setContentText(callerName != null ? callerName : "Someone is calling you")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(CALL_NOTIFICATION_ID, builder.build());
        }
    }

    private void cancelCallNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(CALL_NOTIFICATION_ID);
        }
        activeCallId = null;
    }

    private void launchCallActivity(String callId, String callerUid, String callerName, String callType) {
        Log.d(TAG, "Directly launching CallActivity for CallId: " + callId);
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra("callId", callId);
        intent.putExtra("contactUid", callerUid);
        intent.putExtra("contactName", callerName);
        intent.putExtra("callType", callType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private Notification createServiceNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_woil_guard)
                .setContentTitle("Woil Service Active")
                .setContentText("Monitoring for incoming calls...")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setOngoing(true);

        return builder.build();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            // 1. Silent Service Channel
            NotificationChannel serviceChannel = new NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    "Woil Call Monitor Service",
                    NotificationManager.IMPORTANCE_MIN
            );
            serviceChannel.setDescription("Keeps Woil call listener running in the background");
            serviceChannel.setSound(null, null);
            serviceChannel.enableLights(false);
            serviceChannel.enableVibration(false);
            manager.createNotificationChannel(serviceChannel);

            // 2. High-priority Call Alert Channel
            NotificationChannel callChannel = new NotificationChannel(
                    CALL_CHANNEL_ID,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );
            callChannel.setDescription("Alerts for incoming voice and video calls");
            callChannel.enableLights(true);
            callChannel.enableVibration(true);
            manager.createNotificationChannel(callChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        if (incomingCallListener != null) {
            incomingCallListener.remove();
            incomingCallListener = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
