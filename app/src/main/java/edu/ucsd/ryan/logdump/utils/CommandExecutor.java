package edu.ucsd.ryan.logdump.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

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
        final List<String> result = new ArrayList<>();
        final OnCommandOutputListener listener = new OnCommandOutputListener() {
            @Override
            public void onCommandOutput(String output) {
                result.add(output);
            }

            @Override
            public void onOutputDone() {
            }

        };
        if (execute(commands, superUser, listener))
            return result;
        else
            return null;
    }

    public static boolean execute(String[] commands, boolean superUser, OnCommandOutputListener listener) {
        String shell;
        if (superUser)
            shell = "su";
        else
            shell = "sh";
        PrintStream stdin = null;
        try {
            Process process = Runtime.getRuntime().exec(shell);
            stdin = new PrintStream(process.getOutputStream());
            for (String command:commands) {
                stdin.println(command);
                stdin.flush();
            }
            stdin.close();
            BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int lines = 0;
            while ((line = stdout.readLine()) != null) {
                if (listener != null)
                    listener.onCommandOutput(line);
            }
            if (listener != null)
                listener.onOutputDone();
            stdout.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        } finally {
            if (stdin != null)
                stdin.close();
        }
        return false;
    }
}
