package edu.ucsd.ryan.logdump.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

import edu.ucsd.ryan.logdump.util.LogDBHelper;

/**
 * Created by ryan on 1/12/15.
 */
public class LogContentProvider extends ContentProvider {

    public static final String AUTHORITY = "edu.ucsd.ryan.logdump.logprovider";
    public static final String SCHEME = "content://";
    public static final String VENDOR = "vnd.edu.ucsd.ryan.logdump";

    public final static String LIMIT_KEY = "LIMIT";

    private static final int LOG = 1;
    private static final int LOG_ID = 2;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final HashMap<String, String> mLogProjectionMap;

    static {
        sUriMatcher.addURI(AUTHORITY, LogSchema.TABLE_NAME, LOG);
        sUriMatcher.addURI(AUTHORITY, LogSchema.TABLE_NAME + "/#", LOG_ID);
        mLogProjectionMap = new HashMap<>();
        for (String column:LogSchema.DEFAULT_PROJECTION) {
            mLogProjectionMap.put(column, column);
        }
    }

    private LogDBHelper mDBHelper;

    @Override
    public boolean onCreate() {
        mDBHelper = new LogDBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(LogSchema.TABLE_NAME);
        String limit = null;
        switch (sUriMatcher.match(uri)) {
            case LOG:
                queryBuilder.setProjectionMap(mLogProjectionMap);
                limit = uri.getQueryParameter(LIMIT_KEY);
                break;
            case LOG_ID:
                queryBuilder.setProjectionMap(mLogProjectionMap);
                queryBuilder.appendWhere(LogSchema._ID + "=?");
                DatabaseUtils.appendSelectionArgs(selectionArgs,
                        new String[]{uri.getLastPathSegment()});
                break;
            default:
                throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = LogSchema.DEFAULT_SORT_ORDER;
        }
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null,
                sortOrder, limit);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case LOG:
                return LogSchema.CONTENT_TYPE;
            case LOG_ID:
                return LogSchema.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (sUriMatcher.match(uri) != LOG) {
            // Can only insert into to log URI.
            throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        long id = db.insert(LogSchema.TABLE_NAME, null, values);
        if (id > 0) {
            Uri newUri = ContentUris.withAppendedId(LogSchema.CONTENT_ID_URI_BASE, id);
            getContext().getContentResolver().notifyChange(uri, null);
            return newUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int rowsDeleted;
        switch (sUriMatcher.match(uri)) {
            case LOG:
                rowsDeleted = db.delete(LogSchema.TABLE_NAME, selection, selectionArgs);
                break;

            case LOG_ID:
                String finaSelection = DatabaseUtils.concatenateWhere(
                    LogSchema._ID + " = " + ContentUris.parseId(uri), selection);
                rowsDeleted = db.delete(LogSchema.TABLE_NAME, finaSelection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int rowsUpdated;
        switch (sUriMatcher.match(uri)) {
            case LOG:
                rowsUpdated = db.update(LogSchema.TABLE_NAME, values,
                        selection, selectionArgs);
                break;

            case LOG_ID:
                String finaSelection = DatabaseUtils.concatenateWhere(
                        LogSchema._ID + " = " + ContentUris.parseId(uri), selection);
                rowsUpdated = db.update(LogSchema.TABLE_NAME, values,
                        finaSelection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
