package com.example.guyintrepid.hackday;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by GuyIntrepid on 10/3/14.
 */
public class TestListenerService extends WearableListenerService {
    GoogleApiClient mGoogleApiClient;
    String header = "ww/ww/ww";
    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        // If the message received matches our header, fire off intent to tell watch what to display
        if (messageEvent.getPath().equals(header)) {
            Intent intent = new Intent();
            intent.setAction("MY_ACTION");
            intent.putExtra("EXTRA", new String(messageEvent.getData()));
            sendBroadcast(intent);
        }

    }
}
