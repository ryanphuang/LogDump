package edu.ucsd.ryan.logdump.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import edu.ucsd.ryan.logdump.data.FilterSchema;

/**
 * Created by ryan on 1/11/15.
 */
public class FilterDBHelper extends SQLiteOpenHelper {
    public final static String DBNAME = "filters.db";
    public final static int DBVER = 1;

    private final static String TAG = "FilterDBHelper";

    public final static String SQL_CREATE_FILER = "CREATE TABLE " +
            FilterSchema.TABLE_NAME +
            "("+ FilterSchema._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            FilterSchema.COLUMN_PKGNAME + " TEXT UNIQUE," +
            FilterSchema.COLUMN_APP + " TEXT," +
            FilterSchema.COLUMN_CHECKED + " INTEGER" +
            ");";

    public final static String SQL_DROP_FILTER = "DROP TABLE IF EXISTS " +
            FilterSchema.TABLE_NAME;

    public FilterDBHelper(Context context) {
        super(context, DBNAME, null, DBVER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_FILER);
        Log.d(TAG, "Table " + FilterSchema.TABLE_NAME + " created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_FILTER);
        db.execSQL(SQL_CREATE_FILER);
        Log.d(TAG, "Table " + FilterSchema.TABLE_NAME + " upgraded");
    }
}
