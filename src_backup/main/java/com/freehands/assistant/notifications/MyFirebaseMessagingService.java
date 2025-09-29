
package com.freehands.assistant.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import android.util.Log;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FH-FCM";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (BuildConfig.DEBUG) { Log.d(TAG, "Received: " + remoteMessage.getData().toString()); }

    }

    @Override
    public void onNewToken(String token) {
        if (BuildConfig.DEBUG) { Log.d(TAG, "New token: " + token); }

    }
}
