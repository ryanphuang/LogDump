package edu.ucsd.ryan.logdump.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private static final SimpleDateFormat SDF = new SimpleDateFormat("MM-DD HH:MM:SS.SSS");
    private static final int YEAR = new Date().getYear();

    public static LogStructure parse(String log) {
        Matcher m = PATTERN.matcher(log);
        if (m.find()) {
            try {
                Date date = SDF.parse(m.group(1));
                date.setYear(YEAR);
                return new LogStructure(date.getTime(), m.group(2), m.group(3), m.group(4), m.group(5));
            } catch (ParseException e) {
                Log.e(TAG, "Wrong date format: " + m.group(1));
                return null;
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
