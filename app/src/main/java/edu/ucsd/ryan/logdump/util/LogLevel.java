package edu.ucsd.ryan.logdump.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ryan on 1/13/15.
 */
public enum LogLevel {
    UNKNOWN,
    SILENT,
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL;

    public static final Map<String, LogLevel> LEVEL_MAP;
    public static final Map<LogLevel, String> LETTER_MAP;

    static {
        LEVEL_MAP = new HashMap<>();
        LEVEL_MAP.put("V", VERBOSE);
        LEVEL_MAP.put("D", DEBUG);
        LEVEL_MAP.put("I", INFO);
        LEVEL_MAP.put("W", WARNING);
        LEVEL_MAP.put("E", ERROR);
        LEVEL_MAP.put("F", FATAL);
        LEVEL_MAP.put("S", SILENT);

        LETTER_MAP = new HashMap<>();
        LETTER_MAP.put(VERBOSE, "V");
        LETTER_MAP.put(DEBUG, "D");
        LETTER_MAP.put(INFO, "I");
        LETTER_MAP.put(WARNING, "W");
        LETTER_MAP.put(ERROR, "E");
        LETTER_MAP.put(FATAL, "F");
        LETTER_MAP.put(SILENT, "S");
        LETTER_MAP.put(UNKNOWN, "U");
    }

    public static String getLevelLetter(int ordinal) {
        LogLevel values[] = LogLevel.values();
        if (ordinal >=0 && ordinal < values.length)
            return getLevelLetter(values[ordinal]);
        return getLevelLetter(LogLevel.UNKNOWN);
    }

    public static LogLevel getLetterLevel(String letter) {
        return LEVEL_MAP.get(letter);
    }

    public static String getLevelLetter(LogLevel level) {
        return LETTER_MAP.get(level);
    }
}
