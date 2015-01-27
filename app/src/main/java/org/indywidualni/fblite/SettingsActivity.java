package org.indywidualni.fblite;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

// TODO: Should be extending PreferenceActivity
public class SettingsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

}