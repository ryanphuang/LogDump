package edu.ucsd.ryan.logdump.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ucsd.ryan.logdump.data.LogStructure;

/**
 * Created by ryan on 1/12/15.
 */
public class LogParser {
    public static String TAG = "LogParser";

    public static String PATTERN_STR = "([0-9-:. ]+?) ([VDIWEFS])/(.+?)\\( *?(\\d+)\\):(.+)";
    public static Pattern PATTERN = Pattern.compile(PATTERN_STR);
    private static final ThreadLocal<SimpleDateFormat> formatter = new
            ThreadLocal<SimpleDateFormat>() {
                @Override
                public SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("MM-DD HH:MM:SS.SSS");
                }
            };

    public static long parseTime(String timeStr) {
        try {
            Date date = formatter.get().parse(timeStr);
            date.setYear(new Date().getYear());
            /*
            Now we can remove the check with ThreadLocal
            if (date.after(new Date())) {
                Log.e(TAG, "Impossible date: " + timeStr + ", " + date);
                return -1;
            }
            */
            return date.getTime();
        } catch (ParseException e) {
            Log.e(TAG, "Wrong date format: " + timeStr);
        }
        return -1;
    }

    public static LogStructure parse(String log) {
        Matcher m = PATTERN.matcher(log);
        if (m.find()) {
            long time = parseTime(m.group(1));
            if (time > 0) {
                return new LogStructure(time,
                        m.group(2), m.group(3), m.group(4), m.group(5));
            }
        }
        return null;
    }

    public static LogLevel getLevel(String level) {
        if (level.equalsIgnoreCase("V")) {
            return LogLevel.VERBOSE;
        }
        if (level.equalsIgnoreCase("D")) {
            return LogLevel.DEBUG;
        }
        if (level.equalsIgnoreCase("I")) {
            return LogLevel.INFO;
        }
        if (level.equalsIgnoreCase("W")) {
            return LogLevel.WARNING;
        }
        if (level.equalsIgnoreCase("E")) {
            return LogLevel.ERROR;
        }
        if (level.equalsIgnoreCase("F")) {
            return LogLevel.FATAL;
        }
        if (level.equalsIgnoreCase("S")) {
            return LogLevel.SILENT;
        }
        return LogLevel.UNKNOWN;
    }
}
