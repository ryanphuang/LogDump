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

    public static void readLogs(Context context, LogReadParam readParam, LogHandler handler) {
        CommandExecutor.simpleExecute(new String[]{LOGCAT_COMMAND}, false,
                new LogOutputListener(context, readParam, handler));
    }

    private static class LogOutputListener implements CommandExecutor.OnCommandOutputListener {
        private LogReadParam mReadParam;
        private LogHandler mHandler;
        private List<String> mLogs;
        private Map<String, String> mPkgPidMap;

        public LogOutputListener(Context context, LogReadParam readParam, LogHandler handler) {
            mReadParam = readParam;
            mHandler = handler;
            mLogs = new ArrayList<>();
            mPkgPidMap = new HashMap<>();
            if (mReadParam.pkgFilters != null) {
                for (String pkg:mReadParam.pkgFilters) {
                    int pid = PackageHelper.getInstance(context).getPID(pkg);
                    if (pid >= 0) {
                        mPkgPidMap.put(pkg, String.valueOf(pid));
                        Log.d(TAG, pkg + " has pid filter " + pid);
                    }
                }
            }
        }

        @Override
        public void onCommandOutput(String output) {
            LogStructure structure = LogParser.parse(output);
            if (structure == null) {
                // Ill-formated log, no need to do filter
                mLogs.add("Bad: " + output);
                return;
            }
            for (Map.Entry<String, String> pidEntry:mPkgPidMap.entrySet()) {
                String pkg = pidEntry.getKey();
                String pid = pidEntry.getValue();
                if (structure.pid.equals(pid)) {
                    // Match one filter!
                    if (mHandler != null)
                        mHandler.newLog(pkg, structure);
                    if (DEBUG)
                        mLogs.add(output);
                    return;
                }
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
