package com.vibin.billy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MediaControl extends BroadcastReceiver {
    private static final String TAG = MediaControl.class.getSimpleName();

    public MediaControl() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                if (PlayerService.isRunning) {
                    Log.d(TAG,"passing media button data to service");
                    Intent serviceIntent = new Intent(context, PlayerService.class);
                    serviceIntent.putExtra("id","key");
                    serviceIntent.putExtra("keyevent", intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT));
                    context.startService(serviceIntent);
                }
            }
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }
}
