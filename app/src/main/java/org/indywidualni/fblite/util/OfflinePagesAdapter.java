package org.indywidualni.fblite.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.indywidualni.fblite.R;

import java.util.ArrayList;

public class OfflinePagesAdapter extends ArrayAdapter<String> {

    public OfflinePagesAdapter(Context context, ArrayList<String> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String item = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_offline_page, parent, false);

        TextView page = (TextView) convertView.findViewById(R.id.page);

        @SuppressLint("DefaultLocale") String formattedId = "" + String.format("%02d", position + 1);
        String stringToSet = formattedId + ":  " + item;

        page.setText(stringToSet);

        return convertView;
    }

}