package org.indywidualni.fblite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;
import java.io.File;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // listener for clearing cache preference
        findPreference("clear_cache").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.v("SettingsFragment", "clearing cache...");
                // clear cache dirs
                deleteCache(getActivity().getApplicationContext());
                // restart the app (really ugly way of doing it but...)
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
            }
        });

        // listener for changing transparent_nav preference
        findPreference("transparent_nav").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.v("SettingsFragment", "transparent_nav changed");
                // notify user about relaunching the app
                Toast.makeText(getActivity(), getString(R.string.applying_changes), Toast.LENGTH_SHORT).show();
                // sending intent to onNewIntent() of MainActivity
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra("core_settings_changed", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }
        });

        // listener for changing hardware_acceleration preference
        findPreference("hardware_acceleration").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.v("SettingsFragment", "hardware_acceleration changed");
                // notify user about relaunching the app
                Toast.makeText(getActivity(), getString(R.string.applying_changes), Toast.LENGTH_SHORT).show();
                // sending intent to onNewIntent() of MainActivity
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra("core_settings_changed", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }
        });
    }

    // methods for clearing cache
    public static void deleteCache(Context context) {
        File cache = context.getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                if (!s.equals("lib") && !s.equals("shared_prefs")) {
                    deleteDir(new File(appDir, s));
                    Log.i("TAG", "**************** File /data/data/APP_PACKAGE/" + s + " DELETED *******************");
                }
            }
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

}