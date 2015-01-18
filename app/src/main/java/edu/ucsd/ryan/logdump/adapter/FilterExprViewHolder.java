package edu.ucsd.ryan.logdump.adapter;

import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by ryan on 1/17/15.
 */
public class FilterExprViewHolder {
    public TextView tagView;
    public TextView priorityView;

    public FilterExprViewHolder(TextView tagView, TextView priorityView) {
        this.tagView = tagView;
        this.priorityView = priorityView;
    }
}
