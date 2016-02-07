package org.indywidualni.fblite.util.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.indywidualni.fblite.MyApplication;

public class MySQLiteHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "offline.db";
    private static final int DATABASE_VERSION = 10;

    public MySQLiteHelper() {
        super(MyApplication.getContextOfApplication(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        Log.v("SQLiteDatabase", "Creating database");
        database.execSQL("CREATE TABLE Pages (" +
                "url TEXT PRIMARY KEY, " +
                "html TEXT" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS Pages;");
        onCreate(db);
    }

}