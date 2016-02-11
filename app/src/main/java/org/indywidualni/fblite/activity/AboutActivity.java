package org.indywidualni.fblite.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.indywidualni.fblite.R;
import org.indywidualni.fblite.util.Miscellany;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // set a toolbar to replace the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.about_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.email_me:
                // contact with author (sends some data about device)
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "koras@indywidualni.org", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + ": " + getString(R.string.title_activity_about));
                emailIntent.putExtra(Intent.EXTRA_TEXT, "\n\n--" + Miscellany.getDeviceInfo(this));
                startActivity(Intent.createChooser(emailIntent, getString(R.string.choose_email_client)));
                return true;
            case R.id.me_google_play:
                // author at Google Play
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=Indywidualni")));
                return true;
            case R.id.donate_google:
                // donate with Google Play
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.indywidualni.faceslim_donation")));
                return true;
            case R.id.paypal:
                // donate with PayPal
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.paypal_url))));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}