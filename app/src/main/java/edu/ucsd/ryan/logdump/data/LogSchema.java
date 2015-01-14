package edu.ucsd.ryan.logdump.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by ryan on 1/12/15.
 */
public class LogSchema implements BaseColumns {
    public static final String TABLE_NAME = "log";

    public final static String URI_PREFIX = LogContentProvider.SCHEME +
            LogContentProvider.AUTHORITY + "/";

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI =  Uri.parse(URI_PREFIX + TABLE_NAME);

    /**
     * The content URI base for a single row of data. Callers must
     * append a numeric row id to this Uri to retrieve a row
     */
    public static final Uri CONTENT_ID_URI_BASE = Uri.parse(URI_PREFIX + TABLE_NAME + "/");

    /**
     * The MIME type of {@link #CONTENT_URI}.
     */
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/" + LogContentProvider.VENDOR;

    /**
     * The MIME type of a {@link #CONTENT_URI} sub-directory of a single row.
     */
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + LogContentProvider.VENDOR;

    public static final String DEFAULT_SORT_ORDER = "_ID DESC";


    public static final String COLUMN_PKGNAME = "package";
    public static final String COLUMN_APP = "app";
    public static final String COLUMN_LEVEL = "level";
    public static final String COLUMN_TAG = "tag";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_TEXT = "text";



    public static final String[] DEFAULT_PROJECTION = new String[] {
            LogSchema._ID,
            COLUMN_PKGNAME,
            COLUMN_APP,
            COLUMN_LEVEL,
            COLUMN_TAG,
            COLUMN_TIME,
            COLUMN_TEXT
    };
}
