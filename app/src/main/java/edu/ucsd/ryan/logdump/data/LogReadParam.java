package edu.ucsd.ryan.logdump.data;

import java.util.Set;

import edu.ucsd.ryan.logdump.util.LogLevel;

/**
 * Created by ryan on 1/13/15.
 */
public class LogReadParam {
    public String pkgFilter;
    public LogLevel levelFilter;
    public Set<String> bufferFilters;

    public LogReadParam(String pkg, LogLevel level, Set<String> buffers) {
        pkgFilter = pkg;
        levelFilter = level;
        bufferFilters = buffers;
    }
}
