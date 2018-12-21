package org.indywidualni.fblite.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.support.v4.graphics.drawable.IconCompat;
import android.util.Log;

import org.indywidualni.fblite.R;

public class CustomShortcutActivity extends Activity {

    //public static final String ACTION = "com.android.launcher.action.INSTALL_SHORTCUT";
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
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.putExtra("start_url", url);
        if(name == null){
            name = "FaceSlim Shortcut";
        }
        ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(getApplicationContext(), name)
                .setShortLabel(name)
                .setIcon(IconCompat.createWithResource(getApplicationContext(), R.mipmap.ic_launcher))
                .setIntent(shortcutIntent)
                .build();
        ShortcutManagerCompat.requestPinShortcut(getApplicationContext(), pinShortcutInfo, null);
        finish();
    }

}
