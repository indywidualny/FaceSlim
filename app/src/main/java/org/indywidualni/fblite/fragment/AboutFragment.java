package org.indywidualni.fblite.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.indywidualni.fblite.BuildConfig;
import org.indywidualni.fblite.R;

public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // fill several TextView elements with strings
        String versionName = BuildConfig.VERSION_NAME;
        String authorInfo = getString(R.string.app_name) + " " + getString(R.string.author);

        //noinspection ConstantConditions
        TextView textAuthor = (TextView) getView().findViewById(R.id.textAuthor);
        TextView textSpecialThanksLink = (TextView) getView().findViewById(R.id.textSpecialThanksLink);
        TextView textGithub = (TextView) getView().findViewById(R.id.textGithub);
        TextView textFdroid = (TextView) getView().findViewById(R.id.textFdroid);
        TextView textIndywidualni = (TextView) getView().findViewById(R.id.textIndywidualni);
        TextView textVersion = (TextView) getView().findViewById(R.id.textVersion);

        textAuthor.setText(authorInfo);
        textSpecialThanksLink.setMovementMethod(LinkMovementMethod.getInstance());
        textGithub.setMovementMethod(LinkMovementMethod.getInstance());
        textFdroid.setMovementMethod(LinkMovementMethod.getInstance());
        textIndywidualni.setMovementMethod(LinkMovementMethod.getInstance());
        textVersion.setText(versionName);
    }

}