package edu.ucsd.ryan.logdump.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.ucsd.ryan.logdump.R;
import edu.ucsd.ryan.logdump.adapter.LogCursorAdapter;
import edu.ucsd.ryan.logdump.data.LogContentProvider;
import edu.ucsd.ryan.logdump.data.LogSchema;
import edu.ucsd.ryan.logdump.data.LogStructure;
import edu.ucsd.ryan.logdump.data.LogReadParam;
import edu.ucsd.ryan.logdump.util.LogDBHelper;
import edu.ucsd.ryan.logdump.util.LogHandler;
import edu.ucsd.ryan.logdump.util.LogLevel;
import edu.ucsd.ryan.logdump.util.LogReader;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link LogViewFragment.OnLogEntrySelectedListener}
 * interface.
 */
public class LogViewFragment extends Fragment implements AbsListView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    public static final String TAG = "LogViewFragment";

    private static final int MAX_LOGS = 100;

    private static final String ARG_PKG = "pkg";

    private String mFilterPkg;

    private OnLogEntrySelectedListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    private SearchView mSearchView;

    private String mContentFilter;
    private LogLevel mLevelFilter;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private CursorAdapter mAdapter;

    public static LogViewFragment newInstance(String filterPkg) {
        LogViewFragment fragment = new LogViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PKG, filterPkg);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LogViewFragment() {
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
        getLoaderManager().initLoader(0, null, LogViewFragment.this);
    }

    public void reloadLogs() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public boolean onClose() {
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prevents restarting the loader when restoring state.
        if (mContentFilter == null && newFilter == null) {
            return true;
        }
        if (mContentFilter != null && mContentFilter.equals(newFilter)) {
            return true;
        }
        mContentFilter = newFilter;
        reloadLogs();
        return true;
    }

    public static class LogSearchView extends SearchView {
        public LogSearchView(Context context) {
            super(context);
        }
        @Override
        public void onActionViewCollapsed() {
            setQuery("", false);
            super.onActionViewCollapsed();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Place an action bar item for searching.
        MenuItem refreshItem = menu.findItem(R.id.action_refresh);
        if (refreshItem != null) {
            refreshItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Toast.makeText(getActivity(), "Refreshing...", Toast.LENGTH_LONG).show();
                    reloadLogs();
                    return true;
                }
            });
        }

        MenuItem filterItem = menu.findItem(R.id.action_filter);
        if (filterItem != null) {
            filterItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    final int[] selected = new int[1];
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Log level filter")
                            .setCancelable(true)
                            .setSingleChoiceItems(getResources().getStringArray(R.array.log_levels), 0,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            selected[0] = which;
                                        }
                                    }
                            )
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mLevelFilter = LogLevel.values()[selected[0] + 1];
                                    Log.d(TAG, "Filter by " + mLevelFilter);
                                    reloadLogs();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    builder.show();
                    return false;
                }
            });
        }

        MenuItem item = menu.add("Search");
        item.setIcon(android.R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        mSearchView = new LogSearchView(getActivity());
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.setIconifiedByDefault(true);
        item.setActionView(mSearchView);
    }



    private boolean mListShown;
    private View mProgressContainer;
    private View mListContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);

        setHasOptionsMenu(true);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        View emptyView = view.findViewById(R.id.internalEmpty);
        mListView.setEmptyView(emptyView);

        mListContainer =  view.findViewById(R.id.listContainer);
        mProgressContainer = view.findViewById(R.id.progressContainer);
        mListShown = true;
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLogEntrySelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            Cursor cursor = mAdapter.getCursor();
            if (cursor.moveToPosition(position)) {
                mListener.onLogEntrySelected(cursor);
            }
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        } else {
            Log.e(TAG, "No empty view");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String select = "(" + LogSchema.COLUMN_PKGNAME + "=?)";
        List<String> selectArgs = new ArrayList<>();
        selectArgs.add(mFilterPkg);
        String levelSelect = LogDBHelper.getLevelFilterSelect(mLevelFilter);
        if (!TextUtils.isEmpty(levelSelect)) {
            select = select + " AND (" + levelSelect + ")";
            Log.d(TAG, "Level select " + levelSelect);
        }
        if (!TextUtils.isEmpty(mContentFilter)) {
            select = select + " AND (" + LogSchema.COLUMN_TEXT + " LIKE ? COLLATE NOCASE)";
            selectArgs.add("%" + mContentFilter + "%");
        }
        Uri uri = LogSchema.CONTENT_URI.buildUpon().appendQueryParameter(LogContentProvider.LIMIT_KEY,
                String.valueOf(MAX_LOGS)).build();
        return new CursorLoader(getActivity(), uri,
                LogSchema.DEFAULT_PROJECTION, select,
                selectArgs.toArray(new String[selectArgs.size()]),
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    public void setListShown(boolean shown, boolean animate){
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.INVISIBLE);
        }
    }
    public void setListShown(boolean shown){
        setListShown(shown, true);
    }
    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnLogEntrySelectedListener {
        public void onLogEntrySelected(Cursor cursor);
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
