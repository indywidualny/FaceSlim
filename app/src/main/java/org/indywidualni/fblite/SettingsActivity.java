package org.indywidualni.fblite;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class SettingsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fix for loading fragment into ActionBarActivity
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTheme(R.style.Theme_AppCompat_Preferences);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();
    }

}