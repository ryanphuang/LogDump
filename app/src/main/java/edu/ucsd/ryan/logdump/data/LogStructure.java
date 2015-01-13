package edu.ucsd.ryan.logdump.data;

/**
 * Created by ryan on 1/13/15.
 */
public class LogStructure {
    public String time;
    public String level;
    public String tag;
    public String pid;
    public String text;
    public LogStructure(String time, String level, String tag, String pid, String text) {
        this.time = time;
        this.level = level;
        this.tag = tag;
        this.pid = pid;
        this.text = text;
    }
}
