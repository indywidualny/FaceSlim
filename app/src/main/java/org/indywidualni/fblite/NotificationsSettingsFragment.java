package org.indywidualni.fblite;

//import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class NotificationsSettingsFragment extends PreferenceFragment {

    //private static Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the preferences from an XML resource
        addPreferencesFromResource(R.xml.notifications_preferences);

        // set context
        //context = MyApplication.getContextOfApplication();
    }

}