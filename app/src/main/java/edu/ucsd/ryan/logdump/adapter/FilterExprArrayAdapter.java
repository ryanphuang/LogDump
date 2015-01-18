package edu.ucsd.ryan.logdump.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import edu.ucsd.ryan.logdump.R;
import edu.ucsd.ryan.logdump.data.FilterExpression;

/**
 * Created by ryan on 1/17/15.
 */
public class FilterExprArrayAdapter extends BaseAdapter {
    private List<FilterExpression> mExpressions;
    private LayoutInflater mInflater;

    public FilterExprArrayAdapter(Context context, List<FilterExpression> expressions) {
        mExpressions = expressions;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mExpressions.size();
    }

    @Override
    public FilterExpression getItem(int position) {
        if (position >= 0 && position < mExpressions.size())
            return mExpressions.get(position);
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FilterExprViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.filter_expression_list_item,
                    parent, false);
            TextView tag = (TextView) convertView.findViewById(R.id.tagText);
            TextView spinner = (TextView) convertView.findViewById(R.id.priority);
            viewHolder = new FilterExprViewHolder(tag, spinner);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FilterExprViewHolder) convertView.getTag();
        }
        FilterExpression expression = getItem(position);
        if (expression != null) {
            viewHolder.tagView.setText(expression.tag);
            viewHolder.priorityView.setText(expression.priority);
        }
        return convertView;
    }
}
