package edu.ucsd.ryan.logdump.util;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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

    public static interface OnCommandExecutionListener {
        void onProcessCreated(Process process);
        void onOutputLine(String output);
        void onOutputDone();
    }

    public static List<String> execute(final String[] commands, final boolean superUser) {
        if (superUser) {
            return Shell.SU.run(commands);
        } else {
            return Shell.SH.run(commands);
        }
    }

    public static boolean execute(String[] commands, boolean superUser, final OnCommandExecutionListener listener) {
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
                        listener.onOutputLine(s);
                }
            }).open();
        return true;
    }

    public static boolean simpleExecute(String[] commands, boolean superUser, final OnCommandExecutionListener listener) {
        String shell;
        if (superUser)
            shell = "su";
        else
            shell = "sh";
        final List<String> errs = Collections.synchronizedList(new ArrayList<String>());
        boolean OK = true;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(shell);
            if (listener != null)
                listener.onProcessCreated(process);
            DataOutputStream STDIN = new DataOutputStream(process.getOutputStream());
            StreamGobbler STDOUT = new StreamGobbler(shell + "-",
                    process.getInputStream(), new StreamGobbler.OnLineListener() {
                @Override
                public void onLine(String s) {
                    if (listener != null)
                        listener.onOutputLine(s);
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
        } catch (IOException e) {
            OK = false;
            Log.e(TAG, "Fail to execute command:" + e.toString());
        } catch (InterruptedException e) {
            OK = false;
            e.printStackTrace();
        } finally {
            if (process != null)
                process.destroy();
        }
        if (errs.size() > 0)
            OK = false;
        if (OK && listener != null)
            listener.onOutputDone();
        return OK;
    }
}
