package org.indywidualni.fblite.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;
import org.indywidualni.fblite.activity.MainActivity;
import org.indywidualni.fblite.service.NotificationsService;
import org.indywidualni.fblite.util.FileOperation;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();
    private static final int REQUEST_STORAGE = 1;
    private static Context context;
    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener;
    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // get context
        context = MyApplication.getContextOfApplication();

        // get Tray Preferences and Shared Preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // set onPreferenceClickListener for a few preferences
        Preference notificationsSettingsPref = findPreference("notifications_settings");
        Preference clearCachePref = findPreference("clear_cache");
        notificationsSettingsPref.setOnPreferenceClickListener(this);
        clearCachePref.setOnPreferenceClickListener(this);

        // dependencies (dark_theme cannot be enabled when basic_mode is enabled)
        Preference preference_dark = findPreference("dark_theme");
        preference_dark.setEnabled(!preferences.getBoolean("basic_mode", false));
        Preference preference_basic = findPreference("basic_mode");
        preference_basic.setEnabled(!preferences.getBoolean("dark_theme", false) && !preferences.getBoolean("touch_mode", false));
        Preference preference_touch = findPreference("touch_mode");
        preference_touch.setEnabled(!preferences.getBoolean("basic_mode", false));

        // listener for changing preferences (works after the value change)
        prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // service intent (start, stop)
                final Intent intent = new Intent(context, NotificationsService.class);

                switch (key) {
                    case "notifications_activated":
                        if (prefs.getBoolean("notifications_activated", false) && preferences.getBoolean("message_notifications", false)) {
                            context.stopService(intent);
                            context.startService(intent);
                        } else //noinspection StatementWithEmptyBody
                            if (!prefs.getBoolean("notifications_activated", false) && preferences.getBoolean("message_notifications", false)) {
                                // ignore this case
                            } else if (prefs.getBoolean("notifications_activated", false) && !preferences.getBoolean("message_notifications", false)) {
                                context.startService(intent);
                            } else
                                context.stopService(intent);
                        break;
                    case "message_notifications":
                        if (prefs.getBoolean("message_notifications", false) && preferences.getBoolean("notifications_activated", false)) {
                            context.stopService(intent);
                            context.startService(intent);
                        } else //noinspection StatementWithEmptyBody
                            if (!prefs.getBoolean("message_notifications", false) && preferences.getBoolean("notifications_activated", false)) {
                                // ignore this case
                            } else if (prefs.getBoolean("message_notifications", false) && !preferences.getBoolean("notifications_activated", false)) {
                                context.startService(intent);
                            } else
                                context.stopService(intent);
                        break;
                    case "basic_mode":
                        Preference preference_dark = findPreference("dark_theme");
                        preference_dark.setEnabled(!prefs.getBoolean("basic_mode", false));
                        Preference preference_touch = findPreference("touch_mode");
                        preference_touch.setEnabled(!prefs.getBoolean("basic_mode", false));
                        break;
                    case "touch_mode":
                        Preference preference_basic = findPreference("basic_mode");
                        preference_basic.setEnabled(!prefs.getBoolean("touch_mode", false));
                        break;
                    case "dark_theme":
                        Preference basic = findPreference("basic_mode");
                        basic.setEnabled(!prefs.getBoolean("dark_theme", false));
                        break;
                    case "file_logging":
                        if (prefs.getBoolean("file_logging", false))
                            requestStoragePermission();
                        break;
                    case "transparent_nav":
                    case "drawer_pos":
                    case "no_images":
                    case "keyboard_fix":
                    case "hardware_acceleration":
                    case "custom_user_agent":
                        relaunch();
                        break;
                }

                // what's going on, dude?
                Log.v("SharedPreferenceChange", key + " changed in SettingsFragment");
            }
        };
    }

    // a few preferences are just buttons to click on, let's make them work
    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        Log.v("OnPreferenceClick", key + " clicked in SettingsFragment");

        switch (key) {
            case "notifications_settings":
                //noinspection ResourceType
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, 0)
                        .addToBackStack(null).replace(R.id.content_frame,
                        new NotificationsSettingsFragment()).commit();
                return true;
            case "clear_cache":
                // clear cache dirs
                FileOperation.deleteCache(getActivity().getApplicationContext());
                // restart the app (really ugly way of doing it but...)
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // register the listener
        preferences.registerOnSharedPreferenceChangeListener(prefChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        // unregister the listener
        preferences.unregisterOnSharedPreferenceChangeListener(prefChangeListener);
    }

    // relaunch the app
    private void relaunch() {
        // notify user about relaunching the app
        Toast.makeText(getActivity(), getString(R.string.applying_changes), Toast.LENGTH_SHORT).show();
        // sending intent to onNewIntent() of MainActivity
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("core_settings_changed", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    // request storage permission
    private void requestStoragePermission() {
        String storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int hasPermission = ContextCompat.checkSelfPermission(context, storagePermission);
        String[] permissions = new String[] { storagePermission };
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No storage permission at the moment. Requesting...");
            ActivityCompat.requestPermissions(getActivity(), permissions, REQUEST_STORAGE);
        } else
            Log.e(TAG, "We already have storage permission. Yay!");
    }

}