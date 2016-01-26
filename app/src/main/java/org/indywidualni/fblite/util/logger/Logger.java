package org.indywidualni.fblite.util.logger;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import net.grandcentrix.tray.TrayAppPreferences;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;

import java.io.File;
import java.lang.ref.WeakReference;

/** Singleton pattern */
public final class Logger {

    private static volatile Logger instance;
    private static final int MSG_SHOW_TOAST = 1;
    private static final Context context = MyApplication.getContextOfApplication();
    private final TrayAppPreferences trayPreferences;
    private final String logFilePath;
    private final MyHandler messageHandler;

    private Logger() {
        messageHandler = new MyHandler(this);
        trayPreferences = new TrayAppPreferences(context);
        logFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + context.getString(R.string.app_name).replace(" ", "") + ".log";
    }

    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null)
                    instance = new Logger();
            }
        }
        return instance;
    }

    private boolean checkStoragePermission() {
        String storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int hasPermission = ContextCompat.checkSelfPermission(context, storagePermission);
        return hasPermission == PackageManager.PERMISSION_GRANTED;
    }

    private static class MyHandler extends Handler {
        private final WeakReference<Logger> mLogger;

        public MyHandler(Logger logger) {
            mLogger = new WeakReference<>(logger);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger logger = mLogger.get();
            if (logger != null) {
                if (msg.what == MSG_SHOW_TOAST) {
                    String message = (String) msg.obj;
                    Toast.makeText(context, message , Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void displayStoragePermissionRefused() {
        Message msg = new Message();
        msg.what = MSG_SHOW_TOAST;
        msg.obj = context.getString(R.string.file_logger_needs_permission);
        messageHandler.sendMessage(msg);
    }

    public synchronized void i(String tag, String msg) {
        boolean fileLoggingEnabled = false;
        final boolean mounted = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        final boolean storageReady = mounted && checkStoragePermission();

        try {
            fileLoggingEnabled = trayPreferences.getBoolean("file_logging", false);
        } catch (IllegalStateException e) {
            Log.e("Logger", "An extremely rare IllegalStateException caught");
            e.printStackTrace();
        }

        if (fileLoggingEnabled && storageReady) {
            FileLog.open(logFilePath, Log.VERBOSE, 1000000);  // 1 megabyte
            FileLog.i(tag, msg);
            FileLog.close();
        } else if (fileLoggingEnabled) {
            displayStoragePermissionRefused();
            Log.i(tag, msg);
        } else {
            // use a standard logger instead
            Log.i(tag, msg);
        }
    }

}
