package edu.ucsd.ryan.logdump.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

import edu.ucsd.ryan.logdump.data.FilterSchema;
import edu.ucsd.ryan.logdump.utils.FilterDBHelper;

/**
 * Created by ryan on 1/11/15.
 */
public class FilterDBRunnable implements Runnable {
    private Context mContext;

    public FilterDBRunnable(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        PackageManager manager = mContext.getPackageManager();
        if (manager != null) {
            List<PackageInfo> linfo = manager.getInstalledPackages(0);
            FilterDBHelper helper = new FilterDBHelper(mContext);
            SQLiteDatabase db = helper.getWritableDatabase();
            for (PackageInfo info:linfo) {
                String app;
                try {
                    CharSequence label = info.applicationInfo.loadLabel(manager);
                    app = label.toString();
                } catch (Exception e) {
                    app = info.packageName;
                }
                ContentValues values = new ContentValues();
                values.put(FilterSchema.COLUMN_PKGNAME, info.packageName);
                values.put(FilterSchema.COLUMN_APP, app);
                values.put(FilterSchema.COLUMN_CHECKED, 0);
                db.insertWithOnConflict(FilterSchema.TABLE_NAME, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.close();
        }
    }
}
