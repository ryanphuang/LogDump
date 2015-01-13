package edu.ucsd.ryan.logdump.utils;

/**
 * Created by ryan on 1/13/15.
 */
public enum LogLevel {
    UNKNOWN,
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL,
    SILENT;

    public static String getLevelLetter(int ordinal) {
        LogLevel values[] = LogLevel.values();
        if (ordinal >=0 && ordinal < values.length)
            return getLevelLetter(values[ordinal]);
        return getLevelLetter(LogLevel.UNKNOWN);
    }

    public static String getLevelLetter(LogLevel level) {
        switch (level) {
            case VERBOSE:
                return "V";
            case DEBUG:
                return "D";
            case INFO:
                return "I";
            case WARNING:
                return "W";
            case ERROR:
                return "E";
            case FATAL:
                return "F";
            case SILENT:
                return "S";
            default:
                return "U";
        }
    }
}
