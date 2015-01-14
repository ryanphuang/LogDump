package edu.ucsd.ryan.logdump.data;

import java.util.List;

/**
 * Created by ryan on 1/13/15.
 */
public class LogReadParam {
    public String pkgFilter;
    public String levelFilter;
    public List<String> bufferFilter;

    public LogReadParam(String pkg, String level, List<String> buffer) {
        pkgFilter = pkg;
        levelFilter = level;
        bufferFilter = buffer;
    }
}
