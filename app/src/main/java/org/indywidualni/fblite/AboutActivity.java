package org.indywidualni.fblite;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class AboutActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        String versionName = BuildConfig.VERSION_NAME;
        String authorInfo = getString(R.string.app_name) + " " + getString(R.string.author);

        TextView textFirst = (TextView)findViewById(R.id.textFirst);
        TextView textSecond = (TextView)findViewById(R.id.textSecond);
        TextView textThird = (TextView)findViewById(R.id.textThird);
        TextView textFourth = (TextView)findViewById(R.id.textFourth);
        TextView textVersion = (TextView)findViewById(R.id.textVersion);

        textFirst.setText(authorInfo);
        textSecond.setMovementMethod(LinkMovementMethod.getInstance());
        textThird.setMovementMethod(LinkMovementMethod.getInstance());
        textFourth.setMovementMethod(LinkMovementMethod.getInstance());
        textVersion.setText(versionName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.about_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.email_me:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "koras@indywidualni.org", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + ": " + getString(R.string.title_activity_about));
                startActivity(Intent.createChooser(emailIntent, getString(R.string.choose_email_client)));
                return true;
            case R.id.notifications:
                // for notifications I recommend Notiface
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.aquasoup.notiface")));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}