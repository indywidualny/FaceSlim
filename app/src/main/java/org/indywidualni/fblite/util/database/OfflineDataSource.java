package org.indywidualni.fblite.util.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

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
            cursor = database.rawQuery("select 1 from Pages where url=?;", new String[]{url});
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
        // TODO: make it a resource string
        String html = "<center><h1>This page was not found in offline database</h1></center>";

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

    // for debugging
    public synchronized ArrayList<PageModel> getAllPages() {
        open();
        Cursor cursor = null;
        ArrayList<PageModel> list = new ArrayList<>();

        try {
            cursor = database.rawQuery("Select url, html from Pages", new String[] {});
            if(cursor.getCount() > 0) {
                // retrieve the data to my custom model
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    PageModel page = cursorToPages(cursor);
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

    private PageModel cursorToPages(Cursor cursor) {
        return new PageModel(cursor.getString(0), cursor.getString(1));
    }

}