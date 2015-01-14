package edu.ucsd.ryan.logdump.util;

import edu.ucsd.ryan.logdump.data.LogStructure;

/**
 * Created by ryan on 1/12/15.
 */
public interface LogHandler {
    void newLog(String pkg, LogStructure structure);
    void doneLoading();
}
