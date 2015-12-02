package org.indywidualni.fblite;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import net.grandcentrix.tray.TrayAppPreferences;
import java.net.URL;
import java.util.ArrayList;
import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

public class NotificationsService extends Service {

    private Handler handler = null;
    private static Runnable runnable = null;

    private String feedUrl;
    private int timeInterval;
    private TrayAppPreferences trayPreferences;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i("NotificationsService", "********** Service created! **********");

        // get TrayPreferences
        trayPreferences = new TrayAppPreferences(getApplicationContext());

        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                //Log.i("NotificationsService", "********** Service is still running **********");
                Log.i("NotificationsService", "isActivityVisible: " + Boolean.toString(trayPreferences.getBoolean("activity_visible", false)));

                // get the url and time interval from shared prefs
                feedUrl = trayPreferences.getString("feed_url", "");
                timeInterval = trayPreferences.getInt("interval_pref", 1800000);

                // start AsyncTask if there is internet connection
                if (Connectivity.isConnected(getApplicationContext())) {
                    Log.i("NotificationsService", "Internet connection active. Starting AsyncTask...");
                    new RssReaderTask().execute(feedUrl);
                } else
                    Log.i("NotificationsService", "No internet connection. Skip checking.");

                // set repeat time interval
                handler.postDelayed(runnable, timeInterval);
            }
        };

        // first run delay (3 seconds)
        handler.postDelayed(runnable, 3000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("NotificationsService", "********** Service stopped **********");
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // AsyncTask to get feed, process it and do all the actions needed later
    private class RssReaderTask extends AsyncTask<String, Void, ArrayList<RssItem>> {

        // max number of tries when something is wrong
        private static final int MAX_RETRY = 3;

        @Override
        protected ArrayList<RssItem> doInBackground(String... params) {

            ArrayList<RssItem> result = null;
            int tries = 0;

            while(tries++ < MAX_RETRY && result == null) {
                try {
                    Log.i("RssReaderTask", "********** doInBackground: Processing... Trial: " + tries);
                    URL url = new URL(params[0]);
                    RssFeed feed = RssReader.read(url);
                    result = feed.getRssItems();
                } catch (Exception ex) {
                    Log.i("RssReaderTask", "********** doInBackground: Feed error!");
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<RssItem> result) {

            /** The first service start ever will display a fake notification.
             *  Not fake actually - the latest one. I've been thinking instead
             *  of avoiding it, it's a nice example how it will work in the future.
             */

            // get the last PubDate (String) from TrayPreferences
            final String savedDate = trayPreferences.getString("saved_date", "nothing");

            // if the saved PubDate is different than the new one it means there is new notification
            // display it only when MainActivity is not active or 'Always notify' is checked
            try {
                if (!result.get(0).getPubDate().toString().equals(savedDate))
                    if (!trayPreferences.getBoolean("activity_visible", false) || trayPreferences.getBoolean("notifications_everywhere", true))
                        notifier(result.get(0).getTitle(), result.get(0).getDescription(), result.get(0).getLink());

                // save the latest PubDate (as a String) to TrayPreferences
                trayPreferences.put("saved_date", result.get(0).getPubDate().toString());

                // log success
                Log.i("RssReaderTask", "********** onPostExecute: Aight biatch ;)");
            } catch (NullPointerException ex) {
                Log.i("RssReaderTask", "********** onPostExecute: NullPointerException!");
            }
        }

    }

    private void notifier(String title, String summary, String url) {
        Log.i("NotificationsService", "notifier: Start notification");

        // start building a notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(title))
                        .setSmallIcon(R.drawable.ic_stat_fs)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(title)
                        .setTicker(title)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true);

        // see all the notifications button
        Intent allNotificationsIntent = new Intent(this, MainActivity.class);
        allNotificationsIntent.putExtra("start_url", "https://m.facebook.com/notifications");
        allNotificationsIntent.setAction("ALL_NOTIFICATIONS_ACTION");
        PendingIntent piAllNotifications = PendingIntent.getActivity(getApplicationContext(), 0, allNotificationsIntent, 0);
        mBuilder.addAction(0, getString(R.string.all_notifications), piAllNotifications);

        // notification sound
        Uri ringtoneUri = Uri.parse(trayPreferences.getString("ringtone", "content://settings/system/notification_sound"));
        mBuilder.setSound(ringtoneUri);

        // vibration
        if (trayPreferences.getBoolean("vibrate", false))
            mBuilder.setVibrate(new long[] {500, 500});

        // LED light
        if (trayPreferences.getBoolean("led_light", false))
            mBuilder.setLights(Color.CYAN, 1, 1);

        // priority for Heads-up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            mBuilder.setPriority(Notification.PRIORITY_HIGH);

        // intent with notification url in extra
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("start_url", url);
        intent.setAction("NOTIFICATION_URL_ACTION");

        // final notification building
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setOngoing(false);
        Notification note = mBuilder.build();

        // display a notification
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, note);
    }

    public static void cancelAllNotifications() {
        NotificationManager notificationManager = (NotificationManager)
                MyApplication.getContextOfApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

}