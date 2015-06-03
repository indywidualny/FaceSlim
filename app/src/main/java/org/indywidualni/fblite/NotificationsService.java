package org.indywidualni.fblite;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
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

    public Handler handler = null;
    public static Runnable runnable = null;

    private SharedPreferences preferences;
    private String feedUrl;
    private int timeInterval;
    private ArrayList<RssItem> rssItems;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, getString(R.string.facebook) + ": " + getString(R.string.notifications_service_created), Toast.LENGTH_LONG).show();
        Log.i("NotificationsService", "********** Service created! **********");

        // get shared preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                //Toast.makeText(context, "Service is still running", Toast.LENGTH_LONG).show();
                //Log.i("NotificationsService", "********** Service is still running **********");

                // get the url and time interval from shared prefs
                feedUrl = preferences.getString("feed_url", "");
                timeInterval = Integer.parseInt(preferences.getString("interval_pref", "3600000"));

                // start AsyncTask
                new RssReaderTask().execute(feedUrl);

                // set repeat time interval
                handler.postDelayed(runnable, timeInterval);
            }
        };

        // first run delay (10 seconds)
        handler.postDelayed(runnable, 10000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
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
            // display it only when MainActivity is not active - if it is it means we don't need a notification
            try {
                if (!MyApplication.isActivityVisible() && !rssItems.get(0).getTitle().equals(result.get(0).getTitle()))
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
        Integer mId = 0;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_f)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(title)
                        .setAutoCancel(true);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);
        mBuilder.setVibrate(new long[]{500, 500});

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("start_url", url);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setOngoing(false);
        Notification note = mBuilder.build();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mId, note);
    }

}