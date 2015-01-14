package edu.ucsd.ryan.logdump.service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import edu.ucsd.ryan.logdump.data.FilterSchema;
import edu.ucsd.ryan.logdump.data.LogSchema;
import edu.ucsd.ryan.logdump.data.LogStructure;
import edu.ucsd.ryan.logdump.util.FilterDBHelper;
import edu.ucsd.ryan.logdump.util.LogDBHelper;
import edu.ucsd.ryan.logdump.util.LogHandler;
import edu.ucsd.ryan.logdump.util.LogReader;
import edu.ucsd.ryan.logdump.util.PackageHelper;
import eu.chainfire.libsuperuser.Shell;

public class LogCollectionService extends Service {
    public static final String TAG = "LogCollectionService";

    private static final String ACTION_COLLECT_LOG = "COLLECT_LOG";

    private static final int LOG_COLLECTION_FREQUENCY = 60 * 1000; // 1 minute

    private Set<String> mFilters;


    @Override
    public void onCreate() {
        checkReadLogPermission();
        mFilters = new HashSet<>();
        updateFilters();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_COLLECT_LOG);
        registerReceiver(mCollectLogReceiver, filter);
        scheduleCollectionTask();
    }

    public void checkReadLogPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            String pkgName = getPackageName();
            if (getPackageManager().checkPermission(Manifest.permission.READ_LOGS, pkgName) != 0) {
                if (!Shell.SU.available()) {
                    Log.e(TAG, "Don't have root permission to read logs");
                } else {
                    String grantCommand = "pm grant "+ pkgName + " "+ android.Manifest.permission.READ_LOGS;
                    boolean ok = false;
                    if (Shell.SU.run(new String[]{grantCommand}) != null) {
                        if (getPackageManager().checkPermission(Manifest.permission.READ_LOGS, pkgName) != 0) {
                            Toast.makeText(LogCollectionService.this, "Read logs permission granted!",
                                    Toast.LENGTH_SHORT).show();
                            ok = true;
                        }
                    }
                    if (ok)
                        Log.i(TAG, "Read logs permission granted!");
                    else
                        Log.e(TAG, "Fail to grant read logs permission");
                }
            } else {
                Log.d(TAG, "We have read logs permission");
            }
        } else {
            Log.d(TAG, "Never mind, we are safe to read global logs");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        descheduleCollectionTask();
        unregisterReceiver(mCollectLogReceiver);
    }

    private void scheduleCollectionTask() {
        Log.d(TAG, "Scheduling collection task");
        Intent intent = new Intent(ACTION_COLLECT_LOG);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(LogCollectionService.this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                LOG_COLLECTION_FREQUENCY, pendingIntent);
    }

    private void descheduleCollectionTask() {
        Log.d(TAG, "Descheduling collection task");
        Intent intent = new Intent(ACTION_COLLECT_LOG);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(LogCollectionService.this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    private BroadcastReceiver mCollectLogReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_COLLECT_LOG)) {
                Log.d(TAG, "Collection task request received");
                new Thread() {
                    @Override
                    public void run() {
                        collectLogs();
                    }
                }.start();
            } else {
                Log.e(TAG, "Unrecognized action");
            }
        }
    };

    private Runnable mUpdateFilterRunnable = new Runnable() {
        @Override
        public void run() {
            FilterDBHelper filterDBHelper = new FilterDBHelper(LogCollectionService.this);
            SQLiteDatabase filterDB = filterDBHelper.getWritableDatabase();
            Cursor cursor = filterDB.query(FilterSchema.TABLE_NAME,
                    new String[] {FilterSchema.COLUMN_PKGNAME},
                    FilterSchema.COLUMN_CHECKED + "=?", new String[] {String.valueOf(1)},
                    null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                mFilters.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();
            filterDB.close();
            Log.d(TAG, "Got filters: " + mFilters);
            collectLogs();
        }
    };

    private class LogInsertDBHandler implements LogHandler {
        private OnLogsLoadedListener mListener;
        private SQLiteDatabase mLogDB;

        public LogInsertDBHandler(OnLogsLoadedListener listener) {
            LogDBHelper logDBHelper = new LogDBHelper(LogCollectionService.this);
            mLogDB = logDBHelper.getWritableDatabase();
            mListener = listener;
        }

        @Override
        public void newLog(String pkg, LogStructure structure) {
            insertLog(mLogDB, pkg, structure);
        }

        @Override
        public void doneLoading() {
            mLogDB.close();
            if (mListener != null)
                mListener.onLogsLoaded();
        }
    }

    private class LogCollectionRunnable implements Runnable {
        private String mPKG;
        private LogHandler mHandler;

        public LogCollectionRunnable(String pkg, OnLogsLoadedListener listener) {
            mPKG = pkg;
            mHandler = new LogInsertDBHandler(listener);
        }

        @Override
        public void run() {
            LogReader.readLog(LogCollectionService.this, mPKG, mHandler);
        }
    }

    public void updateFilters() {
        Log.d(TAG, "Update filters");
        new Thread(mUpdateFilterRunnable).start();
    }

    public void collectLogs() {
        for (String filter:mFilters) {
            Log.d(TAG, "Collect logs for " + filter);
            collectLog(filter, null);
        }
    }

    public void collectLog(String pkg, OnLogsLoadedListener listener) {
        new Thread(new LogCollectionRunnable(pkg, listener)).start();
    }

    public static interface OnLogsLoadedListener {
        void onLogsLoaded();
    }

    private void insertLog(SQLiteDatabase db, String pkg, LogStructure structure) {
        ContentValues values = new ContentValues();
        values.put(LogSchema.COLUMN_PKGNAME, pkg);
        values.put(LogSchema.COLUMN_APP, PackageHelper.getInstance(this).getName(pkg));
        values.put(LogSchema.COLUMN_TIME, structure.time);
        values.put(LogSchema.COLUMN_LEVEL, structure.level);
        values.put(LogSchema.COLUMN_TAG, structure.tag);
        values.put(LogSchema.COLUMN_TEXT, structure.text);
        db.insert(LogSchema.TABLE_NAME, null, values);
    }

    public class LocalBinder extends Binder {
        public LogCollectionService getService() {
            return LogCollectionService.this;
        }
    }

    private LocalBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
