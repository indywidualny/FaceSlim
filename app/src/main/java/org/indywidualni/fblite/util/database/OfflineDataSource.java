package org.indywidualni.fblite.util.database;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;

import java.util.ArrayList;

public class OfflineDataSource {

    private static volatile OfflineDataSource instance;

    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;

    private final SharedPreferences preferences = PreferenceManager
            .getDefaultSharedPreferences(MyApplication.getContextOfApplication());

    private OfflineDataSource() {}

    public static OfflineDataSource getInstance() {
        if (instance == null) {
            synchronized (OfflineDataSource.class) {
                if (instance == null)
                    instance = new OfflineDataSource();
            }
        }
        return instance;
    }

    private void open() throws SQLException {
        if (dbHelper == null) {
            dbHelper = new MySQLiteHelper();
            database = dbHelper.getWritableDatabase();
        }
    }

    private void close() {
        if (database != null) {
            trimDatabase();
            dbHelper.close();
            dbHelper = null;
            database = null;
        }
    }

    public void trimDatabase() {
        // a database has to be opened first
        int maxPages = 10;
        try {  // just in case
            maxPages = Integer.parseInt(preferences.getString("offline_keep_max", "10"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        // delete the oldest rows leaving the latest maxPages rows
        database.execSQL("DELETE FROM Pages WHERE ROWID IN (" +
                "SELECT ROWID FROM Pages ORDER BY ROWID DESC LIMIT -1 OFFSET " + maxPages + ");");
    }

    public synchronized void insertPage(String url, String html) throws SQLException {
        open();

        ContentValues insertValues = new ContentValues();
        insertValues.put("url", url);
        insertValues.put("html", html);

        // insert or replace, the default one is a broken shit
        if (!rowExists(url))
            database.insert("Pages", null, insertValues);
        else
            database.replace("Pages", null, insertValues);

        close();
    }

    // insertPage helper, the database have to be open to use it
    private boolean rowExists(String url) {
        Cursor cursor = null;
        boolean exists = false;

        try {
            cursor = database.rawQuery("select 1 from Pages where url=?;", new String[] {url});
            exists = (cursor.getCount() > 0);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return exists;
    }

    public synchronized String getPage(String url) throws SQLException {
        open();
        Cursor cursor = null;
        String html = MyApplication.getContextOfApplication().getString(R.string.not_found_offline);

        try {
            cursor = database.rawQuery("SELECT html FROM Pages WHERE url=?;", new String[] { url });
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                html = cursor.getString(cursor.getColumnIndex("html"));
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        close();
        return html;
    }

    public synchronized ArrayList<String> getAllPages() {
        open();
        Cursor cursor = null;
        ArrayList<String> list = new ArrayList<>();

        try {
            cursor = database.rawQuery("Select url from Pages ORDER BY ROWID DESC", new String[] {});
            if(cursor.getCount() > 0) {
                // retrieve the data to my custom model
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    String page = cursorToPages(cursor);
                    list.add(page);
                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        close();
        return list;
    }

    private String cursorToPages(Cursor cursor) {
        return cursor.getString(0);
    }

}