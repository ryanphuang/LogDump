package edu.ucsd.ryan.logdump.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import edu.ucsd.ryan.logdump.utils.LogDBHelper;
import edu.ucsd.ryan.logdump.data.LogSchema;

/**
 * Created by ryan on 1/12/15.
 */
public class LogContentProvider extends ContentProvider {

    private static final String AUTHORITY = "edu.ucsd.ryan.logdump.logprovider";
    private static final String BASE_PATH = "log";
    private static final String SCHEME = "content://";

    public final static String CONTENT_URI_STR = SCHEME + AUTHORITY + "/" + BASE_PATH;
    public final static Uri CONTENT_URI = Uri.parse(CONTENT_URI_STR);

    public final static String LIMIT_KEY = "LIMIT";

    private static final int LOGS = 1;
    private static final int LOG_ID = 2;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, BASE_PATH, LOGS);
        sUriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", LOG_ID);
    }

    private LogDBHelper mDBHelper;

    @Override
    public boolean onCreate() {
        mDBHelper = new LogDBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String tableName;
        String where = null;
        String limit = null;
        int type = sUriMatcher.match(uri);
        switch (type) {
            case LOGS:
                tableName = LogSchema.TABLE_NAME;
                limit = uri.getQueryParameter(LIMIT_KEY);
                if (TextUtils.isEmpty(sortOrder))
                    sortOrder = "_ID DESC";
                break;
            case LOG_ID:
                tableName = LogSchema.TABLE_NAME;
                where = LogSchema._ID + "="
                        + uri.getLastPathSegment();
                break;
            default:
                throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(tableName);
        if (!TextUtils.isEmpty(where)) {
            queryBuilder.appendWhere(where);
        }
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null,
                sortOrder, limit);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int type = sUriMatcher.match(uri);
        long id;
        switch (type) {
            case LOGS:
                id = db.insert(LogSchema.TABLE_NAME, null, values);
                if (id >= 0) {
                    Uri newUri = ContentUris.withAppendedId(CONTENT_URI, id);
                    getContext().getContentResolver().notifyChange(uri, null);
                    return newUri;
                }
                break;
            default:
                throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int type = sUriMatcher.match(uri);
        int rowsDeleted = 0;
        switch (type) {
            case LOGS:
                rowsDeleted = db.delete(LogSchema.TABLE_NAME, selection, selectionArgs);
                break;

            case LOG_ID:
                String where = LogSchema._ID + " = " +
                        uri.getLastPathSegment();
                if (!TextUtils.isEmpty(selection)) {
                    where = where + " AND " + selection;
                }
                rowsDeleted = db.delete(LogSchema.TABLE_NAME, where, selectionArgs);
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
        int type = sUriMatcher.match(uri);
        int rowsUpdated = 0;
        switch (type) {
            case LOGS:
                rowsUpdated = db.update(LogSchema.TABLE_NAME, values,
                        selection, selectionArgs);
                break;

            case LOG_ID:
                String where = LogSchema._ID + " = " +
                        uri.getLastPathSegment();
                if (!TextUtils.isEmpty(selection)) {
                    where = where + " AND " + selection;
                }
                rowsUpdated = db.update(LogSchema.TABLE_NAME, values,
                        where, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
