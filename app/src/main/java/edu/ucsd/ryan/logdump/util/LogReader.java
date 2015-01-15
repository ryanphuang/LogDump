package edu.ucsd.ryan.logdump.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ucsd.ryan.logdump.data.LogStructure;
import edu.ucsd.ryan.logdump.data.LogReadParam;

/**
 * Created by ryan on 1/12/15.
 */
public class LogReader {
    private static final String LOGCAT_COMMAND = "logcat -d -v time -b main";
    private static final String TAG = "LogReader";
    private static final boolean DEBUG = true;

    private Context mContext;
    private List<LogReadParam> mParams;
    private LogHandler mHandler;
    private Map<String, String> mPkgPidMap;
    private Process mProcess;

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
        mParams = readParams;
        mHandler = handler;
        mPkgPidMap = new HashMap<>();
        mProcess = null;
        mPaused = false;
        mFinished = false;
        mFiltered = true;
    }

    public void start() {
        updatePIDs();
        CommandExecutor.simpleExecute(new String[]{LOGCAT_COMMAND}, false,
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

    private void updatePIDs() {
        if (mParams == null)
            return;
        for (LogReadParam param:mParams) {
            if (!TextUtils.isEmpty(param.pkgFilter)) {
                int pid = PackageHelper.getInstance(mContext).getPID(param.pkgFilter);
                if (pid >= 0) {
                    mPkgPidMap.put(param.pkgFilter, String.valueOf(pid));
                    Log.d(TAG, param.pkgFilter + " has pid filter " + pid);
                }
            }
        }
    }

    private class LogExecutionListener implements CommandExecutor.OnCommandExecutionListener {
        private List<String> mLogs;

        public LogExecutionListener() {
            mLogs = new ArrayList<>();
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
                for (Map.Entry<String, String> pidEntry : mPkgPidMap.entrySet()) {
                    String pkg = pidEntry.getKey();
                    String pid = pidEntry.getValue();
                    if (structure.pid.equals(pid)) {
                        owner = pkg;
                        break;
                    }
                }
            }

            if (owner != null || !mFiltered) {
                // Either we find the match owner or it's unfilterred
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
            if (DEBUG) {
                if (mLogs.size() > 0) {
                    Log.i(TAG, "Collected " + mLogs.size() + " logs");
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
                } else {
                    Log.e(TAG, "No logs");
                }
            }
        }
    }
}
