package com.example.woil.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                Intent serviceIntent = new Intent(context, CallNotificationService.class);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
    }
}
