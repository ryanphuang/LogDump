package edu.ucsd.ryan.logdump.fragment;

/**
 * Created by ryan on 1/14/15.
 */

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.ucsd.ryan.logdump.R;
import edu.ucsd.ryan.logdump.adapter.LogCircularBufferAdapter;
import edu.ucsd.ryan.logdump.data.LogReadParam;
import edu.ucsd.ryan.logdump.data.LogStructure;
import edu.ucsd.ryan.logdump.util.CircularBuffer;
import edu.ucsd.ryan.logdump.util.LogHandler;
import edu.ucsd.ryan.logdump.util.LogReader;


public class LogRealtimeFragment extends LogBaseFragment  {

    public static final String TAG = "LogRealtimeFragment";
    public static final String SHOW_EXTRA_KEY = "show_extra_log_info";

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private LogCircularBufferAdapter mAdapter;

    private LogReadTask mCurrentTask;

    private boolean mPlayState;

    private Menu mMenu;

    public static LogRealtimeFragment newInstance(String pkg, String tag, String priority) {
        LogRealtimeFragment fragment = new LogRealtimeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PKG, pkg);
        args.putString(ARG_TAG, tag);
        args.putString(ARG_PRIORITY, priority);
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
        mCurrentTask = null;
        mPlayState = true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText("No logs yet");
        CircularBuffer buffer = new CircularBuffer(MAX_LOGS);
        boolean showExtra = PreferenceManager.getDefaultSharedPreferences(
                getActivity()).getBoolean(SHOW_EXTRA_KEY, false);
        mAdapter = new LogCircularBufferAdapter(getActivity(), buffer, showExtra, mFlushTop);
        mListView.setAdapter(mAdapter);
        setListShown(false);

        mCurrentTask = new LogReadTask(true);
        if (!TextUtils.isEmpty(mPkgFilter)) {
            mCurrentTask.execute(new LogReadParam(mPkgFilter, null, null, null));
        } else if (!TextUtils.isEmpty(mTagFilter) &&
                !TextUtils.isEmpty(mPriorityFilter)) {
            mCurrentTask.execute(new LogReadParam(null, mTagFilter, mPriorityFilter, null));
        } else {
            mCurrentTask.execute();
        }
    }

    private void togglePause() {
        if (mPlayState) {
            Toast.makeText(getActivity(), "Pausing...", Toast.LENGTH_LONG).show();
            if (mCurrentTask != null)
                mCurrentTask.pause();
            if (mMenu != null) {
                MenuItem pauseItem = mMenu.findItem(R.id.action_pause);
                if (pauseItem != null) {
                    pauseItem.setIcon(android.R.drawable.ic_media_play);
                    pauseItem.setTitle(R.string.action_play);
                }
            }
            mPlayState = false;

        } else {
            Toast.makeText(getActivity(), "Resuming...", Toast.LENGTH_LONG).show();
            if (mCurrentTask != null)
                mCurrentTask.resume();
            if (mMenu != null) {
                MenuItem pauseItem = mMenu.findItem(R.id.action_pause);
                if (pauseItem != null) {
                    pauseItem.setIcon(android.R.drawable.ic_media_pause);
                    pauseItem.setTitle(R.string.action_pause);
                }
            }
            mPlayState = true;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenu = menu;
        MenuItem pauseItem = menu.findItem(R.id.action_pause);
        if (pauseItem != null) {
            pauseItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    togglePause();
                    return true;
                }
            });
        }
    }

    public void reloadLogs() {

    }

    @Override
    public String getTAG() {
        return TAG;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            LogStructure value = (LogStructure) mAdapter.getItem(position);
            if (value != null) {
                mListener.onLogEntrySelected(value);
                TextView extraTV = (TextView) view.findViewById(R.id.extraText);
                if (extraTV != null) {
                    if (extraTV.getVisibility() == View.GONE) {
                        extraTV.setVisibility(View.VISIBLE);
                    } else {
                        extraTV.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private void loadFinished() {
        mAdapter.notifyDataSetChanged();
        // The list should now be shown.
        showList();
    }

    private void showList() {
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
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
            if (!mPaused) {
                mReader.pause();
                synchronized (mLock) {
                    mPaused = true;
                }
            }
        }

        public void resume() {
            if (mPaused) {
                mReader.resume();
                synchronized (mLock) {
                    mPaused = false;
                    mLock.notify();
                }
            }
        }

        @Override
        protected void onProgressUpdate(LogStructure... values) {
            for (LogStructure value:values) {
                mAdapter.addNewLog(value);
                if (mRealTime) {
                    showList();
                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!mRealTime)
                loadFinished();
        }
    }
}
