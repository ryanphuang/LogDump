package edu.ucsd.ryan.logdump.fragment;

/**
 * Created by ryan on 1/14/15.
 */

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.ucsd.ryan.logdump.adapter.LogCircularBufferAdapter;
import edu.ucsd.ryan.logdump.data.LogReadParam;
import edu.ucsd.ryan.logdump.data.LogStructure;
import edu.ucsd.ryan.logdump.util.CircularBuffer;
import edu.ucsd.ryan.logdump.util.LogHandler;
import edu.ucsd.ryan.logdump.util.LogReader;


public class LogRealtimeFragment extends LogBaseFragment  {

    public static final String TAG = "LogRealtimeFragment";

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private LogCircularBufferAdapter mAdapter;

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
        CircularBuffer buffer = new CircularBuffer(MAX_LOGS);
        mAdapter = new LogCircularBufferAdapter(getActivity(), buffer, mFlushTop);
        mListView.setAdapter(mAdapter);
        setListShown(false);

        LogReadTask task = new LogReadTask(false);
        if (!TextUtils.isEmpty(mFilterPkg)) {
            task.execute(new LogReadParam(mFilterPkg, null, null));
        } else {
            task.execute();
        }
    }

    public void reloadLogs() {

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

    private void loadFinished() {
        mAdapter.notifyDataSetChanged();
        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    public class LogAsyncLoader extends AsyncTaskLoader<List<LogStructure>> {

        public LogAsyncLoader(Context context) {
            super(context);
        }

        @Override
        public List<LogStructure> loadInBackground() {
            return null;
        }
    }

    public class LogReadTask extends AsyncTask<LogReadParam, LogStructure, Void> {

        private LogReader mReader;
        private volatile boolean mPaused;
        private final Object mLock = new Object();
        private boolean mRealTime;

        public LogReadTask(boolean realTime) {
            mRealTime = realTime;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(LogReadParam... params) {
            List<LogReadParam> args = Arrays.asList(params);
            if (args.size() == 0) {
                mReader = new LogReader(getActivity(), mHandler);
            } else {
                mReader = new LogReader(getActivity(), args, mHandler);
            }
            mReader.start();
            return null;
        }

        private LogHandler mHandler = new LogHandler() {
            @Override
            public void newLog(String pkg, LogStructure structure) {
                synchronized (mLock) {
                    while (mPaused) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
                publishProgress(structure);
            }

            @Override
            public void doneLoading() {

            }
        };

        public void pause() {
            synchronized (mLock) {
                mPaused = true;
            }
        }

        public void resume() {
            if (mPaused) {
                synchronized (mLock) {
                    mPaused = false;
                }
            }
        }

        @Override
        protected void onProgressUpdate(LogStructure... values) {
            for (LogStructure value:values) {
                mAdapter.addNewLog(value);
                if (mRealTime)
                    mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!mRealTime)
                loadFinished();
        }
    }
}
