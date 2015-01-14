package edu.ucsd.ryan.logdump.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import edu.ucsd.ryan.logdump.data.LogStructure;

/**
 * Created by ryan on 1/12/15.
 */
public class LogReader {
    private static final String LOGCAT_COMMAND = "logcat -d -v time -b main";
    private static final String TAG = "LogReader";
    private static final boolean DEBUG = true;

    public static void readLog(Context context, final String pkgFilter, final LogHandler handler) {
        CommandExecutor.simpleExecute(new String[]{LOGCAT_COMMAND}, false,
                new LogOutputListener(context, pkgFilter, handler));
    }

    private static class LogOutputListener implements CommandExecutor.OnCommandOutputListener {
        private String mPkgFilter;
        private LogHandler mHandler;
        private List<String> mLogs;
        private String mPID;

        public LogOutputListener(Context context, String pkgFilter, LogHandler handler) {
            mPkgFilter = pkgFilter;
            mHandler = handler;
            mLogs = new ArrayList<>();
            mPID = null;
            if (!TextUtils.isEmpty(pkgFilter)) {
                int pid = PackageHelper.getInstance(context).getPID(pkgFilter);
                if (pid >= 0) {
                    mPID = String.valueOf(pid);
                    Log.d(TAG, "PID filter: " + mPID);
                }
            }
        }

        @Override
        public void onCommandOutput(String output) {
            if (!TextUtils.isEmpty(mPID) && output.contains(mPID)) {
                LogStructure structure = LogParser.parse(output);
                if (structure != null) {
                    if (structure.pid.equals(mPID)) {
                        if (mHandler != null)
                            mHandler.newLog(mPkgFilter, structure);
                        mLogs.add(output);
                    }
                }
            }
        }

        @Override
        public void onOutputDone() {
            if (mHandler != null)
                mHandler.doneLoading();
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
                } else {
                    Log.e(TAG, "No logs");
                }
            }
        }
    }
}
