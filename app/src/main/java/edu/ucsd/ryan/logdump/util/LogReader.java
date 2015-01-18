package edu.ucsd.ryan.logdump.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.ucsd.ryan.logdump.data.LogStructure;
import edu.ucsd.ryan.logdump.data.LogReadParam;

/**
 * Created by ryan on 1/12/15.
 */
public class LogReader {
    private static final String DEFAULT_LOGCAT_COMMAND = "logcat -d -v time";
    private static final String TAG = "LogReader";
    private static final boolean DEBUG = false;

    private Context mContext;
    private LogHandler mHandler;
    private HashMap<String, String> mPidPkgMap;
    private HashSet<String> mTagSet;
    private Process mProcess;

    private String mCommand;

    private boolean mFiltered;
    private boolean mFinished;
    private volatile boolean mPaused;

    private final Object mLock = new Object();

    public LogReader(Context context, LogHandler handler) {
        this(context, null, handler);
        mFiltered = false;
    }

    public LogReader(Context context, List<LogReadParam> readParams, LogHandler handler) {
        mContext = context;
        mHandler = handler;
        mPidPkgMap = new HashMap<>();
        mTagSet = new HashSet<>();
        mProcess = null;
        mPaused = false;
        mFinished = false;
        mFiltered = true;
        mCommand = DEFAULT_LOGCAT_COMMAND;
        prepareArgs(readParams);
    }

    public void start() {
        if (mFiltered && mTagSet.isEmpty() && mPidPkgMap.isEmpty()) {
            // no need to collect for empty filter
            Log.d(TAG, "Empty filter, return");
            return;
        }
        CommandExecutor.simpleExecute(new String[]{mCommand}, false,
                new LogExecutionListener());
    }

    public void stop() {
        if (mFinished)
            return;
        if (mProcess != null)
            mProcess.destroy();
        mFinished = true;
    }

    public void pause() {
        synchronized (mLock) {
            mPaused = true;
        }
    }

    public void resume() {
        if (mPaused) {
            synchronized (mLock) {
                mPaused = false;
                mLock.notify();
            }
        }
    }

    private void prepareArgs(List<LogReadParam> params) {
        if (params == null)
            return;
        StringBuilder sb = new StringBuilder();
        for (LogReadParam param:params) {
            if (!TextUtils.isEmpty(param.pkgFilter)) {
                int pid = PackageHelper.getInstance(mContext).getPID(param.pkgFilter);
                if (pid >= 0) {
                    mPidPkgMap.put(String.valueOf(pid), param.pkgFilter);
                    Log.d(TAG, param.pkgFilter + " has pid filter " + pid);
                }
            } else if (!TextUtils.isEmpty(param.tagFilter) &&
                    !TextUtils.isEmpty(param.levelFilter)) {
                mTagSet.add(param.tagFilter);
                sb.append(param.tagFilter);
                sb.append(":");
                sb.append(param.levelFilter);
                sb.append(" ");
            }
        }
        if (sb.length() > 0) {
            sb.append("*:S"); // suppress other tags
            mCommand = DEFAULT_LOGCAT_COMMAND + " " + sb.toString();
        }
    }

    private class LogExecutionListener implements CommandExecutor.OnCommandExecutionListener {
        private List<String> mLogs;
        private int nLogs;

        public LogExecutionListener() {
            mLogs = new ArrayList<>();
            nLogs = 0;
        }

        @Override
        public void onProcessCreated(Process process) {
            mProcess = process;
        }

        @Override
        public void onOutputLine(String output) {
            synchronized (mLock) {
                while (mPaused) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace(); // Oops
                        return;
                    }
                }
            }
            LogStructure structure = LogParser.parse(output);
            if (structure == null) {
                // Ill-formated log, no need to do filter
                mLogs.add("Bad: " + output);
                return;
            }
            String owner = null;
            if (mFiltered) {
                owner = mPidPkgMap.get(structure.pid);
                if (owner == null && mTagSet.contains(structure.tag)) {
                    owner = structure.tag;
                }
            }
            if (owner != null || !mFiltered) {
                // Either we find the match owner or it's unfilterred
                nLogs++;
                if (mHandler != null)
                    mHandler.newLog(owner, structure);
                if (DEBUG)
                    mLogs.add(output);
            }
        }

        @Override
        public void onOutputDone() {
            if (mHandler != null)
                mHandler.doneLoading();
            if (nLogs > 0)
                Log.i(TAG, "Collected " + nLogs + " logs with command " + mCommand);
            else
                Log.e(TAG, "No logs for command " + mCommand);
            if (DEBUG) {
                if (mLogs.size() > 0) {
                    File f = new File("/sdcard/logs.txt");
                    try {
                        FileWriter fileWriter = new FileWriter(f);
                        for (String log : mLogs) {
                            fileWriter.write(log + "\n");
                        }
                        fileWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
