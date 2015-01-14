package edu.ucsd.ryan.logdump.fragment;

/**
 * Created by ryan on 1/14/15.
 */

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;

import java.util.List;

import edu.ucsd.ryan.logdump.adapter.LogCursorAdapter;
import edu.ucsd.ryan.logdump.data.LogReadParam;
import edu.ucsd.ryan.logdump.data.LogStructure;
import edu.ucsd.ryan.logdump.util.LogHandler;
import edu.ucsd.ryan.logdump.util.LogReader;


public class LogRealtimeFragment extends LogBaseFragment implements
        LoaderManager.LoaderCallbacks<List<LogStructure>> {

    public static final String TAG = "LogRealtimeFragment";

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private CursorAdapter mAdapter;

    public static LogRealtimeFragment newInstance(String filterPkg) {
        LogRealtimeFragment fragment = new LogRealtimeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PKG, filterPkg);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LogRealtimeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFilterPkg = getArguments().getString(ARG_PKG);
        }
        mLevelFilter = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText("No logs yet");
        mAdapter = new LogCursorAdapter(getActivity(), null, 0);
        mListView.setAdapter(mAdapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, LogRealtimeFragment.this);
    }

    public void reloadLogs() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public String getTAG() {
        return null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            // mListener.onLogEntrySelected(cursor);
        }
    }

    @Override
    public Loader<List<LogStructure>> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<LogStructure>> loader, List<LogStructure> data) {

    }

    @Override
    public void onLoaderReset(Loader<List<LogStructure>> loader) {

    }


    public class RealTimeLogReadTask extends AsyncTask<LogReadParam, LogStructure, Void> {

        private Object mLock = new Object();
        private volatile boolean mPaused;
        private Runnable mOnLoaded;

        @Override
        protected void onPreExecute() {
            mPaused = false;
        }

        @Override
        protected Void doInBackground(LogReadParam... params) {
            LogHandler handler = new LogHandler() {
                @Override
                public void newLog(String pkg, LogStructure structure) {
                    synchronized (mLock) {
                        if (mPaused)
                            try {
                                mLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                    }
                    publishProgress(structure);
                }

                @Override
                public void doneLoading() {

                }
            };
            for (LogReadParam param:params) {
                LogReader.readLogs(getActivity(), param, handler);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(LogStructure... values) {

        }

        @Override
        protected void onPostExecute(Void result) {
            if (mPaused)
                resume();
            if (mOnLoaded != null)
                mOnLoaded.run();
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

        public void setOnLoadedRunnable(Runnable loaded) {
            mOnLoaded = loaded;
        }
    }
}
