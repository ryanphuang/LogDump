package edu.ucsd.ryan.logdump.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ryan on 1/12/15.
 */
public class PackageHelper {

    private static PackageHelper ourInstance = null;

    private Map<String, String> mNameMap;
    private PackageManager mPM;
    private ActivityManager mAM;

    public static PackageHelper getInstance(Context context) {
        if (ourInstance == null)
            ourInstance = new PackageHelper(context);
        return ourInstance;
    }

    private PackageHelper(Context context) {
        mPM = context.getPackageManager();
        mAM = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mNameMap = new HashMap<>();
    }

    public int getPID(String pkgName) {
        if (TextUtils.isEmpty(pkgName))
            return -1;
        List<ActivityManager.RunningAppProcessInfo> runInfo = mAM.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info:runInfo) {
            int pid = -1;
            for (String pkg:info.pkgList) {
                if (pkg.equals(pkgName)) {
                    pid = info.pid;
                    break;
                }
            }
            if (pid >= 0)
                return pid;
        }
        return -1;
    }

    public String getName(String pkg) {
        String app = mNameMap.get(pkg);
        if (app == null) {
            try {
                ApplicationInfo info = mPM.getApplicationInfo(pkg, 0);
                app = info.loadLabel(mPM).toString();
            } catch (PackageManager.NameNotFoundException exception) {
                app = pkg;
            }
            mNameMap.put(pkg, app);
        }
        return app;
    }
}
