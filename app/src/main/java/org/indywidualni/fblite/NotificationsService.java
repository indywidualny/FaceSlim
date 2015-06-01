package org.indywidualni.fblite;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    public Context context = this;
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
        Toast.makeText(this, "Service created!", Toast.LENGTH_LONG).show();
        Log.i("NotificationsService", "********** Service created! **********");

        // get shared preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                Toast.makeText(context, "Service is still running", Toast.LENGTH_LONG).show();
                Log.i("NotificationsService", "********** Service is still running **********");

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

        //handler.removeCallbacks(runnable);
        handler.removeCallbacksAndMessages(null);

        Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show();
        Log.i("NotificationsService", "********** Service stopped **********");
    }

    @Override
    public void onStart(Intent intent, int startid) {
        Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();
        Log.i("NotificationsService", "********** Service started by user **********");
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

            // if last title is different than the new one it means there is new notification
            // display it only when MainActivity is not active - if it's not it means we don't need a notification
            try {
                if (!MyApplication.isActivityVisible() && !rssItems.get(0).getTitle().equals(result.get(0).getTitle()))
                    notifier(result.get(0).getTitle(), result.get(0).getDescription(), "");
            } catch (NullPointerException ex) {
                Log.i("RssReaderTask", "********** onPostExecute: NullPointerException! ********** ");
            }

            try {
                // update rssItems with the result of feed processing
                rssItems = result;

                // debug line, display all the items
                for (RssItem rssItem : result)
                    Log.i("RssReader:onPostExecute", rssItem.getPubDate() + " # " + rssItem.getTitle() + " # " + rssItem.getLink());
            } catch (NullPointerException ex) {
                Log.i("RssReaderTask", "********** onPostExecute: NullPointerException! ********** ");
            }
        }
    }

    private void notifier(String title, String summary, String url) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(summary);
        // creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);
        // adds url to the latest notification
        resultIntent.setData(Uri.parse(url));

        /** The stack builder object will contain an artificial back stack for the
         *  started Activity.
         *  This ensures that navigating backward from the Activity leads out of
         *  your application to the Home screen.
         */
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // notify now (notification #0 will be displayed)
        mNotificationManager.notify(0, mBuilder.build());
    }

}