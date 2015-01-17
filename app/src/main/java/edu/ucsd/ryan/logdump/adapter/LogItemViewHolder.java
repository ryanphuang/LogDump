package edu.ucsd.ryan.logdump.adapter;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import edu.ucsd.ryan.logdump.R;
import edu.ucsd.ryan.logdump.data.LogStructure;
import edu.ucsd.ryan.logdump.util.LogLevel;
import edu.ucsd.ryan.logdump.util.LogParser;

/**
 * Created by ryan on 1/13/15.
 */

public class LogItemViewHolder {
    private static final String TAG = "LogItemViewHolder";
    public TextView levelTV;
    public TextView tagTV;
    public TextView contentTV;
    public TextView extraTV;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-DD HH:MM:SS.SSS");

    private static int mDefaultColor = -1;

    public LogItemViewHolder(View view) {
        levelTV = (TextView) view.findViewById(R.id.levelText);
        tagTV = (TextView) view.findViewById(R.id.tagText);
        contentTV = (TextView) view.findViewById(R.id.contentText);
        extraTV = (TextView) view.findViewById(R.id.extraText);
        if (mDefaultColor < 0)
            mDefaultColor = view.getResources().getColor(R.color.log_text_color);
    }

    public void setViews(boolean showExtra, LogStructure structure) {
        if (structure != null) {
            levelTV.setText(structure.level);
            tagTV.setText(structure.tag);
            contentTV.setText(structure.text);
            extraTV.setText(SDF.format(new Date(structure.time)));
            if (showExtra)
                extraTV.setVisibility(View.VISIBLE);
            else
                extraTV.setVisibility(View.GONE);
            LogLevel l = LogParser.getLevel(structure.level);
            if (l.ordinal() >= LogLevel.ERROR.ordinal()) {
                levelTV.setTextColor(Color.RED);
                contentTV.setTextColor(Color.RED);
            } else {
                levelTV.setTextColor(mDefaultColor);
                contentTV.setTextColor(mDefaultColor);
            }
        }
    }
}
