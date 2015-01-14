package edu.ucsd.ryan.logdump.task;

import android.os.AsyncTask;

import edu.ucsd.ryan.logdump.data.LogStructure;

/**
 * Created by ryan on 1/13/15.
 */
public class LogReadTask extends AsyncTask<LogReadParam, LogStructure, Void> {

    private Object mLock = new Object();
    private boolean mPaused;

    @Override
    protected void onPreExecute() {
        mPaused = false;
    }

    @Override
    protected Void doInBackground(LogReadParam... params) {
        for (LogReadParam param:params) {

        }
        return null;
    }

    @Override
    protected void onProgressUpdate(LogStructure... values) {

    }

    @Override
    protected void onPostExecute(Void result) {

    }

    public void pause() {
        synchronized (mLock) {
            mPaused = true;
        }

    }

    public void resume() {
        synchronized (mLock) {
            mPaused = false;
            mLock.notify();
        }
    }
}
