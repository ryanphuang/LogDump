package edu.ucsd.ryan.logdump.data;

/**
 * Created by ryan on 1/17/15.
 */
public class FilterExpression {
    public String tag;
    public String priority;
    public FilterExpression(String t, String p) {
        tag = t;
        priority = p;
    }

    @Override
    public String toString() {
        return tag + ":" + priority;
    }
}
