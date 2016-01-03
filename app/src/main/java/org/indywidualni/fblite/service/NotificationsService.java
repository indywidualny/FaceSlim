package org.indywidualni.fblite.service;

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
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import net.grandcentrix.tray.TrayAppPreferences;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;
import org.indywidualni.fblite.activity.MainActivity;
import org.indywidualni.fblite.util.Connectivity;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

public class NotificationsService extends Service {

    // max number of trials when something is wrong
    private static final int MAX_RETRY = 3;
    private static final String MESSAGE_URL = "https://m.facebook.com/messages";

    private Handler handler = null;
    private static Runnable runnable = null;

    private TrayAppPreferences trayPreferences;
    private int timeInterval;

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

                // sync cookies to get the right data
                syncCookies();

                // get time interval from shared prefs
                timeInterval = trayPreferences.getInt("interval_pref", 1800000);

                // start AsyncTasks if there is internet connection
                if (Connectivity.isConnected(getApplicationContext())) {
                    Log.i("NotificationsService", "Internet connection active. Starting AsyncTasks...");
                    new RssReaderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
                    if (trayPreferences.getBoolean("message_notifications", false))
                        new CheckMessagesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
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
    private class RssReaderTask extends AsyncTask<Void, Void, ArrayList<RssItem>> {

        @Override
        protected ArrayList<RssItem> doInBackground(Void... params) {
            ArrayList<RssItem> result = null;
            String feedUrl = null;
            int tries = 0;

            while (tries++ < MAX_RETRY && result == null) {
                // get cookie needed to generate feed url
                String cookie = CookieManager.getInstance().getCookie("https://m.facebook.com");

                try {
                    Elements elements = Jsoup.connect("https://facebook.com/notifications")
                            .cookie("https://m.facebook.com", cookie).get().select("div._li")
                            .select("div#globalContainer").select("div.fwn").select("a:matches(RSS)");
                    String pattern = elements.attr("href");
                    // generate feed url needed by RssReader
                    feedUrl = "https://www.facebook.com" + pattern;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                try {
                    Log.i("RssReaderTask", "********** doInBackground: Processing... Trial: " + tries);
                    //noinspection ConstantConditions
                    URL url = new URL(feedUrl);
                    RssFeed feed = RssReader.read(url);
                    result = feed.getRssItems();
                } catch (MalformedURLException ex) {
                    Log.i("RssReaderTask", "********** doInBackground: URL error!");
                } catch (SAXException | IOException ex) {
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
                        notifier(result.get(0).getTitle(), result.get(0).getDescription(), result.get(0).getLink(), false);

                // save the latest PubDate (as a String) to TrayPreferences
                trayPreferences.put("saved_date", result.get(0).getPubDate().toString());

                // log success
                Log.i("RssReaderTask", "********** onPostExecute: Aight biatch ;)");
            } catch (NullPointerException ex) {
                Log.i("RssReaderTask", "********** onPostExecute: NullPointerException!");
            }
        }

    }

    // AsyncTask to get message notifications
    private class CheckMessagesTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String result = null;
            int tries = 0;

            while (tries++ < MAX_RETRY && result == null) {
                try {
                    Log.i("CheckMessagesTask", "********** doInBackground: Processing... Trial: " + tries);

                    Elements message = Jsoup.connect("https://m.facebook.com").cookie("https://m.facebook.com",
                            CookieManager.getInstance().getCookie("https://m.facebook.com")).get()
                            .select("div#viewport").select("div#page").select("div._129-")
                            .select("#messages_jewel").select("span._59tg");

                    result = message.html();
                } catch (IOException ex) {
                    Log.i("CheckMessagesTask", "********** doInBackground: Shit!");
                    ex.printStackTrace();
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                // parse a number of unread messages
                int newMessages = Integer.parseInt(result);

                if (!trayPreferences.getBoolean("activity_visible", false) || trayPreferences.getBoolean("notifications_everywhere", true)) {
                    if (newMessages == 1)
                        notifier(getString(R.string.you_have_one_message), null, MESSAGE_URL, true);
                    else if (newMessages > 1)
                        notifier(String.format(getString(R.string.you_have_n_messages), newMessages), null, MESSAGE_URL, true);
                }

                // save the latest message count
                trayPreferences.put("last_message_count", newMessages);

                // log success
                Log.i("CheckMessagesTask", "********** onPostExecute: Aight biatch ;)");
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }

    }

    /** CookieSyncManager was deprecated in API level 21.
     *  We need it for API level lower than 21 though.
     */
    @SuppressWarnings("deprecation")
    private void syncCookies() {
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.createInstance(getApplicationContext());
            CookieSyncManager.getInstance().sync();
        } else {
            // flush to force sync cookies (needed for the first run)
            CookieManager.getInstance().flush();
        }
    }

    private void notifier(String title, String summary, String url, boolean isMessage) {
        // let's display a notification, dude!
        final String contentTitle;
        if (isMessage)
            contentTitle = getString(R.string.app_name) + ": " + getString(R.string.messages);
        else
            contentTitle = getString(R.string.app_name) + ": " + getString(R.string.notifications);

        // log line (show what type of notification is about to be displayed)
        Log.i("NotificationsService", "notifier: Start notification ********** isMessage: " + isMessage);

        // start building a notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(title))
                        .setSmallIcon(R.mipmap.ic_stat_fs)
                        .setContentTitle(contentTitle)
                        .setContentText(title)
                        .setTicker(title)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true);

        // see all the notifications button (if it's not a message)
        if (!isMessage) {
            Intent allNotificationsIntent = new Intent(this, MainActivity.class);
            allNotificationsIntent.putExtra("start_url", "https://m.facebook.com/notifications");
            allNotificationsIntent.setAction("ALL_NOTIFICATIONS_ACTION");
            PendingIntent piAllNotifications = PendingIntent.getActivity(getApplicationContext(), 0, allNotificationsIntent, 0);
            mBuilder.addAction(0, getString(R.string.all_notifications), piAllNotifications);
        }

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

        // don't vibrate or play a sound for duplicated message notifications
        if (isMessage) {
            int lastMessageCount = trayPreferences.getInt("last_message_count", 0);
            int currentCount = 1;
            if (title.matches("[\\d]+[A-Za-z]?"))
                currentCount = Integer.parseInt(title.replaceAll("[\\D]", ""));
            if (currentCount == lastMessageCount)
                note.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
        }

        // display a notification
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // because message notifications are displayed separately
        if (isMessage)
            mNotificationManager.notify(1, note);
        else
            mNotificationManager.notify(0, note);
    }

    public static void cancelAllNotifications() {
        NotificationManager notificationManager = (NotificationManager)
                MyApplication.getContextOfApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

}