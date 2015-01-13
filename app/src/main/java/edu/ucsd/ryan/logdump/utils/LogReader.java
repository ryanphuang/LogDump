package edu.ucsd.ryan.logdump.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import edu.ucsd.ryan.logdump.data.LogStructure;

/**
 * Created by ryan on 1/12/15.
 */
public class LogReader {
    private static final String LOGCAT_COMMAND = "logcat -d -v time -b main";
    private static final String TAG = "LogReader";

    public static void readLog(Context context, final String pkgFilter, final LogHandler handler) {
        CommandExecutor.execute(new String[]{LOGCAT_COMMAND}, false,
                new LogOutputListener(context, pkgFilter, handler));
    }

    private static class LogOutputListener implements CommandExecutor.OnCommandOutputListener {
        private String mPkgFilter;
        private Pattern mFilterPattern;
        private LogHandler mHandler;
        private List<String> mLogs;
        private String mPID;

        public LogOutputListener(Context context, String pkgFilter, LogHandler handler) {
            mPkgFilter = pkgFilter;
            mHandler = handler;
            mLogs = new ArrayList<>();
            mFilterPattern = null;
            mPID = null;
            if (!TextUtils.isEmpty(pkgFilter)) {
                int pid = PackageHelper.getInstance(context).getPID(pkgFilter);
                if (pid >= 0) {
                    mPID = String.valueOf(pid);
                    mFilterPattern = Pattern.compile("\\( *?" + mPID + "\\)");
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
                        mHandler.newLog(mPkgFilter, structure);
                        mLogs.add(output);
                    }
                }
            }
            /*
            if (mFilterPattern != null) {
                Matcher matcher = mFilterPattern.matcher(output);
                if (matcher.find()) {
                    mHandler.newLog(mPkgFilter, output);
                    mLogs.add(output);
                }
            }
            */
        }

        @Override
        public void onOutputDone() {
            mHandler.doneLoading();
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
