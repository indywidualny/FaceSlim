package org.indywidualni.fblite.util.database;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class OfflineDataSource {

    private static volatile OfflineDataSource instance;
    private static final int MAX_PAGES = 50;

    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;

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
            dbHelper.close();
            dbHelper = null;
            database = null;
        }
    }

    // todo: trim the database during every app start when offline mode is enabled to store MAX_PAGES values
    // todo: or better trim it every onStop() to avoid (relatively) huge storage consumption

    public synchronized void insertPage(String url, String html) throws SQLException {
        open();
        database.rawQuery("INSERT or REPLACE INTO Pages (url, html) " +
                "values (?, ?);", new String[] { url, html });
        close();
    }

    public synchronized String getPage(String url) throws SQLException {
        open();
        Cursor cursor = null;
        // TODO: make it a resource string
        String html = "<center><h1>This page was not found in offline database</h1></center>";

        try {
            cursor = database.rawQuery("SELECT html FROM Pages WHERE url=?", new String[] { url });
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

}