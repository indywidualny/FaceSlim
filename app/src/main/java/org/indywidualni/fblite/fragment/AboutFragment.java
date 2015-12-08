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
        TextView textFirst = (TextView) getView().findViewById(R.id.textFirst);
        TextView textSecond = (TextView) getView().findViewById(R.id.textSecond);
        TextView textThird = (TextView) getView().findViewById(R.id.textThird);
        TextView textFourth = (TextView) getView().findViewById(R.id.textFourth);
        TextView textVersion = (TextView) getView().findViewById(R.id.textVersion);

        textFirst.setText(authorInfo);
        textSecond.setMovementMethod(LinkMovementMethod.getInstance());
        textThird.setMovementMethod(LinkMovementMethod.getInstance());
        textFourth.setMovementMethod(LinkMovementMethod.getInstance());
        textVersion.setText(versionName);
    }

}