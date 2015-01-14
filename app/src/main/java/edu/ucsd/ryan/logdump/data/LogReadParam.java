package edu.ucsd.ryan.logdump.data;

import java.util.Collection;
import java.util.List;

import edu.ucsd.ryan.logdump.util.LogLevel;

/**
 * Created by ryan on 1/13/15.
 */
public class LogReadParam {
    public Collection<String> pkgFilters;
    public Collection<LogLevel> levelFilters;
    public Collection<String> bufferFilters;

    public LogReadParam(Collection<String> pkgs, Collection<LogLevel> levels, Collection<String> buffers) {
        pkgFilters = pkgs;
        levelFilters = levels;
        bufferFilters = buffers;
    }
}
