package org.indywidualni.fblite;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;

public class NotificationsSettingsFragment extends PreferenceFragment {

    private static Context context;
    SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the preferences from an XML resource
        addPreferencesFromResource(R.xml.notifications_preferences);

        context = MyApplication.getContextOfApplication();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onResume () {
        super.onResume();

        // update ringtone preference summary
        String ringtoneString = preferences.getString("ringtone", "content://settings/system/notification_sound");
        Uri ringtoneUri = Uri.parse(ringtoneString);
        Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
        String name = ringtone.getTitle(context);
        if ("".equals(ringtoneString))
            name = getString(R.string.silent);
        RingtonePreference rp = (RingtonePreference) findPreference("ringtone");
        rp.setSummary(getString(R.string.notification_sound_description) + name);
    }

}