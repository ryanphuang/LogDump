package edu.ucsd.ryan.logdump.data;

import android.database.Cursor;

/**
 * Created by ryan on 1/13/15.
 */
public class LogStructure {
    public long time;
    public String level;
    public String tag;
    public String pid;
    public String text;

    public LogStructure() {

    }

    public LogStructure(long time, String level, String tag, String pid, String text) {
        this.time = time;
        this.level = level;
        this.tag = tag;
        this.pid = pid;
        this.text = text;
    }

    public static LogStructure fromCursor(Cursor cursor) {
        LogStructure structure = new LogStructure();
        boolean valid = false;
        int index = cursor.getColumnIndex(LogSchema.COLUMN_TIME);
        if (index >= 0) {
            structure.time = cursor.getLong(index);
            valid = true;
        }
        index = cursor.getColumnIndex(LogSchema.COLUMN_LEVEL);
        if (index >= 0) {
            structure.level = cursor.getString(index);
            valid = true;
        }
        index = cursor.getColumnIndex(LogSchema.COLUMN_TAG);
        if (index >= 0) {
            structure.tag = cursor.getString(index);
            valid = true;
        }
        index = cursor.getColumnIndex(LogSchema.COLUMN_TEXT);
        if (index >= 0) {
            structure.text = cursor.getString(index);
            valid = true;
        }
        if (valid)
            return structure;
        return null;
    }
}
