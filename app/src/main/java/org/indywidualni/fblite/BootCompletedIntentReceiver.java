package org.indywidualni.fblite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootCompletedIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // debug line
        Log.v("BootCompletedIntent", "Boot time!");

        // get shared preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // start notifications service when it's activated at Settings
        if (preferences.getBoolean("notifications_activated", false)) {
            Intent serviceIntent = new Intent(context, NotificationsService.class);
            context.startService(serviceIntent);
        }
    }

}