package org.indywidualni.fblite.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.service.NotificationsService;

public class BootCompletedIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i("BroadcastReceiver", "********** Boot time or package replaced! **********");
        context = MyApplication.getContextOfApplication();

        Intent startIntent = new Intent(context, NotificationsService.class);

        // get shared preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // start notifications service when it's activated at Settings
        if (preferences.getBoolean("notifications_activated", false) || preferences.getBoolean("message_notifications", false))
            context.startService(startIntent);
    }

}