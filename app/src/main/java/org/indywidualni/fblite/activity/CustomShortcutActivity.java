package org.indywidualni.fblite.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.indywidualni.fblite.R;

public class CustomShortcutActivity extends Activity {

    public static final String ACTION = "com.android.launcher.action.INSTALL_SHORTCUT";
    public static final String NAME_FIELD = "name";
    public static final String URL_FIELD = "url";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url = "";
        String name = "";

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            if (extras.containsKey(URL_FIELD) && extras.containsKey(NAME_FIELD)) {
                url = extras.getString(URL_FIELD);
                name = extras.getString(NAME_FIELD);
            } else {
                finish();
            }
        } else {
            finish();
        }

        Log.v("Installing shortcut", url + ", " + name);
        Intent shortcutIntent = new Intent(getApplicationContext(), MainActivity.class);
        shortcutIntent.putExtra("start_url", url);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.mipmap.ic_launcher));
        addIntent.setAction(ACTION);
        getApplicationContext().sendBroadcast(addIntent);

        finish();
    }

}
