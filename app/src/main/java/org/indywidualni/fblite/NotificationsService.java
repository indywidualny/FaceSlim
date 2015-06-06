package org.indywidualni.fblite;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;
import java.net.URL;
import java.util.ArrayList;
import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

public class NotificationsService extends Service {

    public static boolean isRunning;
    public Handler handler = null;
    public static Runnable runnable = null;

    private String feedUrl;
    private int timeInterval;
    private ArrayList<RssItem> rssItems;
    private SharedPreferences preferences;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, getString(R.string.facebook) + ": " + getString(R.string.notifications_service_created), Toast.LENGTH_LONG).show();
        Log.i("NotificationsService", "********** Service created! **********");
        isRunning = true;

        // get shared preferences (for a multi process app)
        preferences = getSharedPreferences(getApplicationContext().getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);

        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                //Toast.makeText(context, "Service is still running", Toast.LENGTH_LONG).show();
                //Log.i("NotificationsService", "********** Service is still running **********");

                // get the url and time interval from shared prefs
                feedUrl = preferences.getString("feed_url", "");
                timeInterval = Integer.parseInt(preferences.getString("interval_pref", "1800000"));

                // start AsyncTask
                new RssReaderTask().execute(feedUrl);

                // set repeat time interval
                handler.postDelayed(runnable, timeInterval);
            }
        };

        // first run delay (5 seconds)
        handler.postDelayed(runnable, 5000);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
        Log.i("NotificationsService", "********** Service stopped **********");
    }

    // AsyncTask to get feed, process it and do all the actions needed later
    private class RssReaderTask extends AsyncTask<String, Void, ArrayList<RssItem>> {
        @Override
        protected ArrayList<RssItem> doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                RssFeed feed = RssReader.read(url);
                return feed.getRssItems();
            } catch (Exception ex) {
                Log.i("RssReaderTask", "********** doInBackground: Feed error! ********** ");
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<RssItem> result) {
            // first run, try to get rid of that null
            if (rssItems == null)
                rssItems = result;

            // if the latest title is different than the new one it means there is new notification
            // display it only when MainActivity is not active or 'Always notify' is checked
            try {
                if (!rssItems.get(0).getTitle().equals(result.get(0).getTitle()))
                    if (!MyApplication.isActivityVisible() || preferences.getBoolean("notifications_everywhere", true))
                        notifier(result.get(0).getTitle(), result.get(0).getDescription(), result.get(0).getLink());
            } catch (NullPointerException ex) {
                Log.i("RssReaderTask", "********** onPostExecute: NullPointerException! ********** ");
            }

            // update rssItems with the result of feed processing
            rssItems = result;
        }
    }

    private void notifier(String title, String summary, String url) {
        Log.i("NotificationsService", "notifier: Start notification");

        // start building a notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(title))
                        .setSmallIcon(R.drawable.ic_stat_f)
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
        Uri ringtoneUri = Uri.parse(preferences.getString("ringtone", "content://settings/system/notification_sound"));
        mBuilder.setSound(ringtoneUri);

        // vibration
        if (preferences.getBoolean("vibrate", false))
            mBuilder.setVibrate(new long[] {500, 500});

        // LED light
        if (preferences.getBoolean("led_light", false))
            mBuilder.setLights(Color.BLUE, 1, 1);

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

        /** Display it - only one notification is needed for this app.
         *  The most recent one. Update it every time it's changed.
         *  It's ID is always 0. It may be done differently in the future */
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, note);
    }

    public static void cancelAllNotifications() {
        NotificationManager notificationManager = (NotificationManager)
                MyApplication.getContextOfApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

}