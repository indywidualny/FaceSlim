package org.indywidualni.fblite.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;
import org.indywidualni.fblite.activity.MainActivity;
import org.indywidualni.fblite.util.Connectivity;
import org.indywidualni.fblite.util.logger.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class NotificationsService extends Service {

    // Facebook URL constants
    private static final String BASE_URL = "https://mobile.facebook.com";
    private static final String NOTIFICATIONS_URL = "https://m.facebook.com/notifications.php";
    private static final String MESSAGES_URL = "https://m.facebook.com/messages";
    private static final String MESSAGES_URL_BACKUP = "https://mobile.facebook.com/messages";
    private static final String NOTIFICATION_MESSAGE_URL = "https://www.messenger.com/login";
    private static final String NOTIFICATION_OLD_MESSAGE_URL = "https://m.facebook.com/messages#";

    // number of trials during notifications or messages checking
    private static final int MAX_RETRY = 3;
    private static final int JSOUP_TIMEOUT = 10000;
    private static final String TAG;

    // HandlerThread, Handler (final to allow synchronization) and its runnable
    private final HandlerThread handlerThread;
    private final Handler handler;
    private static Runnable runnable;

    // volatile boolean to safely skip checking while service is being stopped
    private volatile boolean shouldContinue = true;
    private static String userAgent;
    private SharedPreferences preferences;

    /* Well, bad practice. Object name starting with a capital, but it's convenient.
    In order to use my custom logger I just removed Log import and I'm getting an
    instance of my Logger here. Its usage is exactly the same as the usage of Log */
    private final Logger Log;

    // static initializer
    static {
        TAG = NotificationsService.class.getSimpleName();
    }

    // class constructor, starts a new thread in which checkers are being run
    public NotificationsService() {
        handlerThread = new HandlerThread("Handler Thread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        Log = Logger.getInstance();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "********** Service created! **********");
        super.onCreate();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // create a runnable needed by a Handler
        runnable = new HandlerRunnable();

        // start a repeating checking, first run delay (3 seconds)
        handler.postDelayed(runnable, 3000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: Service stopping...");
        super.onDestroy();

        synchronized (handler) {
            shouldContinue = false;
            handler.notify();
        }

        handler.removeCallbacksAndMessages(null);
        handlerThread.quit();
    }

    /** A runnable used by the Handler to schedule checking. */
    private class HandlerRunnable implements Runnable {

        public void run() {
            try {
                // get time interval from tray preferences
                final int timeInterval = Integer.parseInt(preferences.getString("interval_pref", "1800000"));
                Log.i(TAG, "Time interval: " + (timeInterval / 1000) + " seconds");

                // time since last check = now - last check
                final long now = System.currentTimeMillis();
                final long sinceLastCheck = now - preferences.getLong("last_check", now);
                final boolean ntfLastStatus = preferences.getBoolean("ntf_last_status", false);
                final boolean msgLastStatus = preferences.getBoolean("msg_last_status", false);

                if ((sinceLastCheck < timeInterval) && ntfLastStatus && msgLastStatus) {
                    final long waitTime = timeInterval - sinceLastCheck;
                    if (waitTime >= 1000) {  // waiting less than a second is just stupid
                        Log.i(TAG, "I'm going to wait. Resuming in: " + (waitTime / 1000) + " seconds");

                        synchronized (handler) {
                            try {
                                handler.wait(waitTime);
                            } catch (InterruptedException ex) {
                                Log.i(TAG, "Thread interrupted");
                            } finally {
                                Log.i(TAG, "Lock is now released");
                            }
                        }

                    }
                }

                // when onDestroy() is run and lock is released, don't go on
                if (shouldContinue) {
                    // start AsyncTasks if there is internet connection
                    if (Connectivity.isConnected(getApplicationContext())) {
                        Log.i(TAG, "Internet connection active. Starting AsyncTask...");
                        String connectionType = "Wi-Fi";
                        if (Connectivity.isConnectedMobile(getApplicationContext()))
                            connectionType = "Mobile";
                        Log.i(TAG, "Connection Type: " + connectionType);
                        userAgent = preferences.getString("webview_user_agent", System.getProperty("http.agent"));
                        Log.i(TAG, "User Agent: " + userAgent);

                        if (preferences.getBoolean("notifications_activated", false))
                            new CheckNotificationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
                        if (preferences.getBoolean("message_notifications", false))
                            new CheckMessagesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

                        // save current time (last potentially successful checking)
                        preferences.edit().putLong("last_check", System.currentTimeMillis()).apply();
                    } else
                        Log.i(TAG, "No internet connection. Skip checking.");

                    // set repeat time interval
                    handler.postDelayed(runnable, timeInterval);
                } else
                    Log.i(TAG, "Notified to stop running. Exiting...");

            } catch (RuntimeException re) {
                Log.i(TAG, "RuntimeException caught");
                restartItself();
            }
        }

    }

    /** Notifications checker task: it checks Facebook notifications only. */
    private class CheckNotificationsTask extends AsyncTask<Void, Void, Element> {

        boolean syncProblemOccurred = false;

        private Element getElement(String connectUrl) {
            try {
                return Jsoup.connect(connectUrl).userAgent(userAgent).timeout(JSOUP_TIMEOUT)
                        .cookie("https://mobile.facebook.com", CookieManager.getInstance().getCookie("https://mobile.facebook.com")).get()
                        .select("a.touchable").not("a._19no").not("a.button").first();
            } catch (IllegalArgumentException ex) {
                Log.i("CheckNotificationsTask", "Cookie sync problem occurred");
                if (!syncProblemOccurred) {
                    syncProblemToast();
                    syncProblemOccurred = true;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected Element doInBackground(Void... params) {
            Element result = null;
            int tries = 0;

            syncCookies();

            while (tries++ < MAX_RETRY && result == null) {
                Log.i("CheckNotificationsTask", "doInBackground: Processing... Trial: " + tries);
                Log.i("CheckNotificationsTask", "Trying: " + NOTIFICATIONS_URL);
                Element notification = getElement(NOTIFICATIONS_URL);
                if (notification != null)
                    result = notification;
            }

            return result;
        }

        @Override
        protected void onPostExecute(Element result) {
            try {
                if (result == null)
                    return;
                if (result.text() == null)
                    return;

                String time = result.select("span.mfss.fcg").text();
                String text = result.text().replace(time, "");

                if (!preferences.getBoolean("activity_visible", false) || preferences.getBoolean("notifications_everywhere", true)) {
                    if (!preferences.getString("last_notification_text", "").equals(text))
                        notifier(text, BASE_URL + result.attr("href"), false);
                    preferences.edit().putString("last_notification_text", text).apply();
                }

                // save this check status
                preferences.edit().putBoolean("ntf_last_status", true).apply();
                Log.i("CheckNotificationsTask", "onPostExecute: Aight biatch ;)");
            } catch (NumberFormatException ex) {
                // save this check status
                preferences.edit().putBoolean("ntf_last_status", false).apply();
                Log.i("CheckNotificationsTask", "onPostExecute: Failure");
            }
        }

    }

    /** Messages checker task: it checks new messages only. */
    private class CheckMessagesTask extends AsyncTask<Void, Void, String> {

        boolean syncProblemOccurred = false;

        private String getNumber(String connectUrl) {
            try {
                Elements message = Jsoup.connect(connectUrl).userAgent(userAgent).timeout(JSOUP_TIMEOUT)
                        .cookie("https://m.facebook.com", CookieManager.getInstance().getCookie("https://m.facebook.com")).get()
                        .select("div#viewport").select("div#page").select("div._129-")
                        .select("#messages_jewel").select("span._59tg");

                return message.html();
            } catch (IllegalArgumentException ex) {
                Log.i("CheckMessagesTask", "Cookie sync problem occurred");
                if (!syncProblemOccurred) {
                    syncProblemToast();
                    syncProblemOccurred = true;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return "failure";
        }

        @Override
        protected String doInBackground(Void... params) {
            String result = null;
            int tries = 0;

            // sync cookies to get the right data
            syncCookies();

            while (tries++ < MAX_RETRY && result == null) {
                Log.i("CheckMessagesTask", "doInBackground: Processing... Trial: " + tries);

                // try to generate rss feed address
                Log.i("CheckMsgTask:getNumber", "Trying: " + MESSAGES_URL);
                String number = getNumber(MESSAGES_URL);
                if (!number.matches("^[+-]?\\d+$")) {
                    Log.i("CheckMsgTask:getNumber", "Trying: " + MESSAGES_URL_BACKUP);
                    number = getNumber(MESSAGES_URL_BACKUP);
                }
                if (number.matches("^[+-]?\\d+$"))
                    result = number;
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                // parse a number of unread messages
                int newMessages = Integer.parseInt(result);

                if (!preferences.getBoolean("activity_visible", false) || preferences.getBoolean("notifications_everywhere", true)) {
                    if (newMessages == 1)
                        notifier(getString(R.string.you_have_one_message), NOTIFICATION_OLD_MESSAGE_URL, true);
                    else if (newMessages > 1)
                        notifier(String.format(getString(R.string.you_have_n_messages), newMessages), NOTIFICATION_OLD_MESSAGE_URL, true);
                }

                // save this check status
                preferences.edit().putBoolean("msg_last_status", true).apply();
                Log.i("CheckMessagesTask", "onPostExecute: Aight biatch ;)");
            } catch (NumberFormatException ex) {
                // save this check status
                preferences.edit().putBoolean("msg_last_status", false).apply();
                Log.i("CheckMessagesTask", "onPostExecute: Failure");
            }
        }

    }

    /** CookieSyncManager was deprecated in API level 21.
     *  We need it for API level lower than 21 though.
     *  In API level >= 21 it's done automatically.
     */
    @SuppressWarnings("deprecation")
    private void syncCookies() {
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.createInstance(getApplicationContext());
            CookieSyncManager.getInstance().sync();
        }
    }

    // show a Sync Problem Toast while not being on UI Thread
    private void syncProblemToast() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), getString(R.string.sync_problem),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // restart the service from inside the service
    private void restartItself() {
        final Context context = MyApplication.getContextOfApplication();
        final Intent intent = new Intent(context, NotificationsService.class);
        context.stopService(intent);
        context.startService(intent);
    }

    // create a notification and display it
    private void notifier(String title, String url, boolean isMessage) {
        // let's display a notification, dude!
        final String contentTitle;
        if (isMessage)
            contentTitle = getString(R.string.app_name) + ": " + getString(R.string.messages);
        else
            contentTitle = getString(R.string.app_name) + ": " + getString(R.string.notifications);

        // log line (show what type of notification is about to be displayed)
        Log.i(TAG, "Start notification - isMessage: " + isMessage);

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

        // ringtone
        String ringtoneKey = "ringtone";
        if (isMessage)
            ringtoneKey = "ringtone_msg";

        Uri ringtoneUri = Uri.parse(preferences.getString(ringtoneKey, "content://settings/system/notification_sound"));
        mBuilder.setSound(ringtoneUri);

        // vibration
        if (preferences.getBoolean("vibrate", false))
            mBuilder.setVibrate(new long[] {500, 500});
        else
            mBuilder.setVibrate(new long[] {0L});

        // LED light
        if (preferences.getBoolean("led_light", false)) {
            Resources resources = getResources(), systemResources = Resources.getSystem();
            mBuilder.setLights(Color.CYAN,
                    resources.getInteger(systemResources.getIdentifier("config_defaultNotificationLedOn", "integer", "android")),
                    resources.getInteger(systemResources.getIdentifier("config_defaultNotificationLedOff", "integer", "android")));
        }

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

        // LED light flag
        if (preferences.getBoolean("led_light", false))
            note.flags |= Notification.FLAG_SHOW_LIGHTS;

        // display a notification
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // because message notifications are displayed separately
        if (isMessage)
            mNotificationManager.notify(1, note);
        else
            mNotificationManager.notify(0, note);
    }

    // cancel all the notifications which are visible at the moment
    public static void cancelAllNotifications() {
        NotificationManager notificationManager = (NotificationManager)
                MyApplication.getContextOfApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

}
