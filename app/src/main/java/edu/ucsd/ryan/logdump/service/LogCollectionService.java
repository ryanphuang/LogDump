package edu.ucsd.ryan.logdump.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ucsd.ryan.logdump.data.FilterExpression;
import edu.ucsd.ryan.logdump.data.FilterSchema;
import edu.ucsd.ryan.logdump.data.LogReadParam;
import edu.ucsd.ryan.logdump.data.LogSchema;
import edu.ucsd.ryan.logdump.data.LogStructure;
import edu.ucsd.ryan.logdump.util.FilterDBHelper;
import edu.ucsd.ryan.logdump.util.LogHandler;
import edu.ucsd.ryan.logdump.util.LogReader;
import edu.ucsd.ryan.logdump.util.PackageHelper;

public class LogCollectionService extends Service {
    public static final String TAG = "LogCollectionService";

    private static final String ACTION_COLLECT_LOG = "COLLECT_LOG";
    private static final String ACTION_CLEANUP_LOG = "CLEANUP_LOG";

    private static final int MILLIS_PER_SECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;

    private static final int LOG_COLLECTION_DELAY = 15 * MILLIS_PER_SECOND; // 5 seconds;
    private static final int LOG_CLEANUP_DELAY = 30 * MILLIS_PER_SECOND; // 20 seconds;

    private static final int LOG_COLLECTION_FREQUENCY = SECONDS_PER_MINUTE * MILLIS_PER_SECOND; // 1 minute

    private int mCleanUpThreshold = MINUTES_PER_HOUR * SECONDS_PER_MINUTE * MILLIS_PER_SECOND; // 1 hour
    private static final String ARCHIVE_CLEANUP_KEY = "log_archive_threshold";

    private Set<String> mPkgFilters;
    private List<FilterExpression> mExprFilters;

    @Override
    public void onCreate() {
        PreferenceManager.getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(spChangeListener);
        int result = PackageHelper.checkReadLogPermission(this, getPackageName());
        if (result == PackageHelper.READLOG_PERMISSION_GRANTED) {
            Toast.makeText(this, "Read logs permission granted!",
                    Toast.LENGTH_SHORT).show();
        }
        mPkgFilters = new HashSet<>();
        mExprFilters = new ArrayList<>();
        updatePkgFilters();
        updateExprFilters();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_COLLECT_LOG);
        filter.addAction(ACTION_CLEANUP_LOG);
        registerReceiver(mLogTaskReceiver, filter);
        scheduleCollectionTask();
        scheduleCleanupTask();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        descheduleCollectionTask();
        descheduleCleanupTask();
        unregisterReceiver(mLogTaskReceiver);
    }

    private int getCleanupMillisPref(SharedPreferences prefs) {
        try {
            int freq = Integer.valueOf(prefs.getString("log_archive_threshold", "60"));
            return freq * MINUTES_PER_HOUR * SECONDS_PER_MINUTE * MILLIS_PER_SECOND; // clean up every hour
        } catch (NumberFormatException exception) {
            Log.e(TAG, "Invalid threshold");
        }
        return -1;
    }

    private void scheduleCleanupTask() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                LogCollectionService.this);
        int freq = getCleanupMillisPref(prefs);
        if (freq > 0) {
            mCleanUpThreshold = freq;
            Intent intent = new Intent(ACTION_CLEANUP_LOG);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(LogCollectionService.this, 0, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + LOG_CLEANUP_DELAY,
                    mCleanUpThreshold, pendingIntent);
            Log.d(TAG, "Log cleanup task scheduled");
        }
    }

    private void descheduleCleanupTask() {
        Intent intent = new Intent(ACTION_CLEANUP_LOG);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(LogCollectionService.this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Log cleanup task descheduled");
    }

    private void scheduleCollectionTask() {
        Intent intent = new Intent(ACTION_COLLECT_LOG);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(LogCollectionService.this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + LOG_COLLECTION_DELAY,
                LOG_COLLECTION_FREQUENCY, pendingIntent);
        Log.d(TAG, "Log collection task scheduled");
    }

    private void descheduleCollectionTask() {
        Intent intent = new Intent(ACTION_COLLECT_LOG);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(LogCollectionService.this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Log collection task descheduled");
    }

    private BroadcastReceiver mLogTaskReceiver = new BroadcastReceiver() {

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
            } else if (action.equals(ACTION_CLEANUP_LOG)) {
                Log.d(TAG, "Cleanup task request received");
                new Thread() {
                    @Override
                    public void run() {
                        cleanupLogs();
                    }
                }.start();
            } else {
                Log.e(TAG, "Unrecognized action");
            }
        }
    };

    private Runnable mUpdatePkgFilterRunnable = new Runnable() {
        @Override
        public void run() {
            FilterDBHelper filterDBHelper = new FilterDBHelper(LogCollectionService.this);
            SQLiteDatabase filterDB = filterDBHelper.getWritableDatabase();
            String select = FilterSchema.COLUMN_PKGNAME + " IS NOT NULL AND " +
                    FilterSchema.COLUMN_CHECKED + "=?";
            Cursor cursor = filterDB.query(FilterSchema.TABLE_NAME,
                    new String[] {FilterSchema.COLUMN_TAG, FilterSchema.COLUMN_APP},
                    select, new String[] {String.valueOf(1)},
                    null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                mPkgFilters.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();
            filterDB.close();
            Log.d(TAG, "Got package filters: " + mPkgFilters);
            collectLogs();
        }
    };

    private Runnable mUpdateExprFilterRunnable = new Runnable() {
        @Override
        public void run() {
            mExprFilters.clear();
            FilterDBHelper filterDBHelper = new FilterDBHelper(LogCollectionService.this);
            SQLiteDatabase filterDB = filterDBHelper.getWritableDatabase();
            String select = FilterSchema.COLUMN_TAG + " IS NOT NULL AND " +
                    FilterSchema.COLUMN_PRIORITY + " IS NOT NULL AND " +
                    FilterSchema.COLUMN_CHECKED + "=?";
            Cursor cursor = filterDB.query(FilterSchema.TABLE_NAME,
                    new String[] {FilterSchema.COLUMN_TAG, FilterSchema.COLUMN_PRIORITY},
                    select, new String[] {String.valueOf(1)},
                    null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                mExprFilters.add(new FilterExpression(cursor.getString(0), cursor.getString(1)));
                cursor.moveToNext();
            }
            cursor.close();
            filterDB.close();
            Log.d(TAG, "Got filters expressions: " + mExprFilters);
            collectLogs();
        }
    };

    private class LogCollectionRunnable implements Runnable {
        private List<LogReadParam> mParams;
        private LogHandler mHandler;

        public LogCollectionRunnable(List<LogReadParam> params) {
            mParams = params;
            mHandler = new LogHandler() {
                @Override
                public void newLog(String pkg, LogStructure structure) {
                    insertLog(pkg, structure);
                }

                @Override
                public void doneLoading() {
                }
            };
        }

        @Override
        public void run() {
            LogReader reader = new LogReader(LogCollectionService.this, mParams, mHandler);
            reader.start();
        }
    }

    public void updatePkgFilters() {
        Log.d(TAG, "Update filters");
        new Thread(mUpdatePkgFilterRunnable).start();
    }

    public void updateExprFilters() {
        Log.d(TAG, "Update expression filters");
        new Thread(mUpdateExprFilterRunnable).start();
    }

    public void cleanupLogs() {
        long cutoff = System.currentTimeMillis() - mCleanUpThreshold;
        int deleted = getContentResolver().delete(LogSchema.CONTENT_URI,
                LogSchema.COLUMN_TIME + "<?", new String[]{String.valueOf(cutoff)});
        Log.d(TAG, deleted + " rows deleted");
    }

    public void collectLogs() {
        List<LogReadParam> params = new ArrayList<>();
        for (String pkg: mPkgFilters) {
            params.add(new LogReadParam(pkg, null, null, null));
        }
        for (FilterExpression expr:mExprFilters) {
            params.add(new LogReadParam(null, expr.tag, expr.priority, null));
        }

        Log.d(TAG, "Collect logs for packages " + mPkgFilters + " and expressions " + mExprFilters);
        new Thread(new LogCollectionRunnable(params)).start();
    }

    private void insertLog(String pkg, LogStructure structure) {
        StringBuilder sb = new StringBuilder();
        sb.append(LogSchema.COLUMN_TAG + "=? AND ");
        sb.append(LogSchema.COLUMN_TIME + "=? AND ");
        sb.append(LogSchema.COLUMN_LEVEL + "=? AND ");
        sb.append(LogSchema.COLUMN_TEXT + "=?");

        Cursor cursor = getContentResolver().query(LogSchema.CONTENT_URI,
                new String[] {"COUNT(*) AS COUNT"},
                sb.toString(), new String[] {structure.tag,
                        String.valueOf(structure.time),
                        structure.level, structure.text},
                null);
        cursor.moveToFirst();
        boolean exists = false;
        if (!cursor.isBeforeFirst() && !cursor.isAfterLast()) {
            int cnt = cursor.getInt(0);
            exists = cnt > 0;
        }
        cursor.close();
        if (!exists) {
            // Avoid duplicate insert
            ContentValues values = new ContentValues();
            values.put(LogSchema.COLUMN_PKGNAME, pkg);
            values.put(LogSchema.COLUMN_APP, PackageHelper.getInstance(this).getName(pkg));
            values.put(LogSchema.COLUMN_TIME, structure.time);
            values.put(LogSchema.COLUMN_LEVEL, structure.level);
            values.put(LogSchema.COLUMN_TAG, structure.tag);
            values.put(LogSchema.COLUMN_TEXT, structure.text);
            getContentResolver().insert(LogSchema.CONTENT_URI, values);
        } else {
            // Log.d(TAG, "Log exists!");
        }
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

    SharedPreferences.OnSharedPreferenceChangeListener spChangeListener = new
            SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(ARCHIVE_CLEANUP_KEY)) {
                mCleanUpThreshold = getCleanupMillisPref(sharedPreferences);
                Log.d(TAG, "Clean up frequency changed");
                descheduleCleanupTask();
                scheduleCleanupTask();
            }
         }
    };
}
