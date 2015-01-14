package edu.ucsd.ryan.logdump.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import edu.ucsd.ryan.logdump.R;
import edu.ucsd.ryan.logdump.data.LogStructure;
import edu.ucsd.ryan.logdump.util.CircularBuffer;

/**
 * Created by ryan on 1/13/15.
 */
public class LogCircularBufferAdapter extends BaseAdapter implements Filterable {
    private CircularBuffer mBuffer;
    private LayoutInflater mInflater;
    private boolean mReverse;

    public LogCircularBufferAdapter(Context context, CircularBuffer buffer, boolean reverse) {
        mBuffer = buffer;
        mInflater = LayoutInflater.from(context);
        mReverse = reverse;
    }

    @Override
    public int getCount() {
        return mBuffer.size();
    }

    @Override
    public Object getItem(int position) {
        if (mReverse)
            return mBuffer.tail(position);
        return mBuffer.head(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void addNewLog(LogStructure structure) {
        mBuffer.add(structure);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        LogItemViewHolder viewHolder;
        if (convertView == null) {
            v = mInflater.inflate(R.layout.log_list_item, parent, false);
            viewHolder = new LogItemViewHolder(v);
            v.setTag(viewHolder);
        } else {
            viewHolder = (LogItemViewHolder) v.getTag();
        }
        LogStructure structure = (LogStructure) getItem(position);
        if (structure != null) {
            viewHolder.setViews(structure);
        }
        return v;
    }

    @Override
    public Filter getFilter() {
        return null;
    }
}
