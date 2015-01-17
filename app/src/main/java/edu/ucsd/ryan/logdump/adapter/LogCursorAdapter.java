package edu.ucsd.ryan.logdump.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import edu.ucsd.ryan.logdump.R;
import edu.ucsd.ryan.logdump.data.LogStructure;

/**
 * Created by ryan on 1/13/15.
 */
public class LogCursorAdapter extends CursorAdapter {
    private LayoutInflater mInflater;
    private boolean mShowExtra;

    public LogCursorAdapter(Context context, Cursor c, int flags, boolean showExtra) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);
        mShowExtra = showExtra;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.log_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        LogStructure structure = LogStructure.fromCursor(cursor);
        if (structure != null) {
            LogItemViewHolder holder = new LogItemViewHolder(view);
            holder.setViews(mShowExtra, structure);
        }

    }
}
