package edu.ucsd.ryan.logdump.data;

import java.util.Set;

import edu.ucsd.ryan.logdump.util.LogLevel;

/**
 * Created by ryan on 1/13/15.
 */
public class LogReadParam {
    public String pkgFilter;
    public String tagFilter;
    public String levelFilter;
    public Set<String> bufferFilters;

    public LogReadParam(String pkg, String tag, String level, Set<String> buffers) {
        pkgFilter = pkg;
        tagFilter = tag;
        levelFilter = level;
        bufferFilters = buffers;
    }
}
