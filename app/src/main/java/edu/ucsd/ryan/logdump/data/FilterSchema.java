package edu.ucsd.ryan.logdump.data;

import android.provider.BaseColumns;

/**
 * Created by ryan on 1/11/15.
 */
public class FilterSchema  implements BaseColumns {
    public static final String TABLE_NAME = "filter";
    public static final String COLUMN_TAG = "tag";
    public static final String COLUMN_PRIORITY = "priority";
    public static final String COLUMN_PKGNAME = "package";
    public static final String COLUMN_APP = "app";
    public static final String COLUMN_CHECKED = "checked";

    public static final int COLUMN_TAG_INDEX = 1;
    public static final int COLUMN_PRIORITY_INDEX = 2;
    public static final int COLUMN_PKGNAME_INDEX = 3;
    public static final int COLUMN_APP_INDEX = 4;
    public static final int COLUMN_CHECKED_INDEX = 5;

    public static final String[] DEFAULT_PROJECTION = new String[] {
            FilterSchema._ID,
            COLUMN_TAG,
            COLUMN_PRIORITY,
            COLUMN_PKGNAME,
            COLUMN_APP,
            COLUMN_CHECKED
    };
}
