package edu.ucsd.ryan.logdump.util;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by ryan on 1/12/15.
 */
public class PackageHelper {

    public static final int READLOG_PERMISSION_GRANTED = 1;
    public static final int READLOG_PERMISSION_DENIED = 2;
    public static final int READLOG_PERMISSION_OWNED = 3;

    private static final String TAG = "PackageHelper";

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

    public static int checkReadLogPermission(Context context, String pkgName) {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (context.getPackageManager().checkPermission(Manifest.permission.READ_LOGS, pkgName) != 0) {
                if (!Shell.SU.available()) {
                    result = READLOG_PERMISSION_DENIED;
                    Log.e(TAG, "Don't have root permission to read logs");
                } else {
                    String grantCommand = "pm grant "+ pkgName + " "+ android.Manifest.permission.READ_LOGS;
                    Log.d(TAG, "Requesting for read logs permission " + pkgName);
                    boolean ok = false;
                    if (Shell.SU.run(new String[]{grantCommand}) != null) {
                        Log.d(TAG, "Execution succeeded");
                        if (context.getPackageManager().checkPermission(
                                Manifest.permission.READ_LOGS, pkgName) != 0) {
                            ok = true;
                        }
                    }
                    if (ok) {
                        result = READLOG_PERMISSION_GRANTED;
                        Log.i(TAG, "Read logs permission granted!");
                    }
                    else {
                        result = READLOG_PERMISSION_DENIED;
                        Log.e(TAG, "Fail to grant read logs permission");
                    }
                }
            } else {
                result = READLOG_PERMISSION_OWNED;
                Log.d(TAG, "We have read logs permission");
            }
        } else {
            result = READLOG_PERMISSION_OWNED;
            Log.d(TAG, "Never mind, we are safe to read global logs");
        }
        return result;
    }
}
