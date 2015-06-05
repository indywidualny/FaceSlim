package org.indywidualni.fblite;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class NotificationsSettingsFragment extends PreferenceFragment {

    private static Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the preferences from an XML resource
        addPreferencesFromResource(R.xml.notifications_preferences);

        // set context
        context = MyApplication.getContextOfApplication();
    }

    // debug TODO: remove it
    @Override
    public void onResume () {
        super.onResume();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Toast.makeText(context, preferences.getString("ringtone", "NOTHING"), Toast.LENGTH_LONG).show();
    }

}