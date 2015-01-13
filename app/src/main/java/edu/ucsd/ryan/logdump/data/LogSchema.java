package edu.ucsd.ryan.logdump.data;

import android.provider.BaseColumns;

/**
 * Created by ryan on 1/12/15.
 */
public class LogSchema implements BaseColumns {
    public static final String TABLE_NAME = "log";

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
