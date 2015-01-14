package edu.ucsd.ryan.logdump.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import eu.chainfire.libsuperuser.StreamGobbler;

/**
 * Created by ryan on 1/12/15.
 */
public class CommandExecutor {

    public static final int MAX_OUTLINES = 20000;

    private static final String TAG = "CommandExecutor";

    public static interface OnCommandOutputListener {
        void onCommandOutput(String output);
        void onOutputDone();
    }

    public static List<String> execute(final String[] commands, final boolean superUser) {
        if (superUser) {
            return Shell.SU.run(commands);
        } else {
            return Shell.SH.run(commands);
        }
    }

    public static boolean execute(String[] commands, boolean superUser, final OnCommandOutputListener listener) {
        Shell.Builder builder = new Shell.Builder();
        if (superUser)
            builder.useSU();
        else
            builder.useSH();
        builder.setMinimalLogging(true)
            .addCommand(commands)
            .setOnSTDOUTLineListener(new StreamGobbler.OnLineListener() {
                @Override
                public void onLine(String s) {
                    if (listener != null)
                        listener.onCommandOutput(s);
                }
            }).open();
        return true;
    }

    public static boolean simpleExecute(String[] commands, boolean superUser, final OnCommandOutputListener listener) {
        String shell;
        if (superUser)
            shell = "su";
        else
            shell = "sh";
        final List<String> errs = Collections.synchronizedList(new ArrayList<String>());
        boolean OK = true;
        try {
            Process process = Runtime.getRuntime().exec(shell);
            DataOutputStream STDIN = new DataOutputStream(process.getOutputStream());
            StreamGobbler STDOUT = new StreamGobbler(shell + "-",
                    process.getInputStream(), new StreamGobbler.OnLineListener() {
                @Override
                public void onLine(String s) {
                    if (listener != null)
                        listener.onCommandOutput(s);
                }
            });
            StreamGobbler STDERR = new StreamGobbler(shell + "*",
                    process.getErrorStream(), new StreamGobbler.OnLineListener() {
                @Override
                public void onLine(String s) {
                    errs.add(s);
                }
            });
            STDOUT.start();
            STDERR.start();
            try {
                for (String command:commands) {
                    STDIN.write((command + "\n").getBytes());
                    STDIN.flush();
                }
                STDIN.write(("exit\n").getBytes());
                STDIN.flush();
            } catch (IOException e) {
                throw e;
            }
            process.waitFor();
            try {
                STDIN.close();
            } catch (IOException e) {

            }
            STDOUT.join();
            STDERR.join();
            process.destroy();
        } catch (IOException e) {
            OK = false;
            Log.e(TAG, "Fail to execute command:" + e.toString());
        } catch (InterruptedException e) {
            OK = false;
            e.printStackTrace();
        }
        if (errs.size() > 0)
            OK = false;
        if (OK && listener != null)
            listener.onOutputDone();
        return OK;
    }
}
