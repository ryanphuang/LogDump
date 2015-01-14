package edu.ucsd.ryan.logdump.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;

import java.util.ArrayList;
import java.util.List;

import edu.ucsd.ryan.logdump.adapter.LogCursorAdapter;
import edu.ucsd.ryan.logdump.data.LogContentProvider;
import edu.ucsd.ryan.logdump.data.LogSchema;
import edu.ucsd.ryan.logdump.util.LogDBHelper;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link LogHistoryFragment.OnLogEntrySelectedListener}
 * interface.
 */
public class LogHistoryFragment extends LogBaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "LogHistoryFragment";

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private CursorAdapter mAdapter;

    public static LogHistoryFragment newInstance(String filterPkg) {
        LogHistoryFragment fragment = new LogHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PKG, filterPkg);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LogHistoryFragment() {
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
        getLoaderManager().initLoader(0, null, LogHistoryFragment.this);
    }

    @Override
    public void reloadLogs() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public String getTAG() {
        return TAG;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String select = "(" + LogSchema.COLUMN_PKGNAME + "=?)";
        List<String> sArgs = new ArrayList<>();
        sArgs.add(mFilterPkg);
        String levelSelect = LogDBHelper.getLevelFilterSelect(mLevelFilter);
        if (!TextUtils.isEmpty(levelSelect)) {
            select = select + " AND (" + levelSelect + ")";
            Log.d(TAG, "Level select " + levelSelect);
        }
        if (!TextUtils.isEmpty(mContentFilter)) {
            select = select + " AND (" + LogSchema.COLUMN_TEXT + " LIKE ? COLLATE NOCASE)";
            sArgs.add("%" + mContentFilter + "%");
        }
        String[] selectArgs = sArgs.toArray(new String[sArgs.size()]);
        String limit;
        String sortOrder;
        if (mFlushTop) {
            sortOrder = LogSchema._ID + " DESC";
            limit = String.valueOf(MAX_LOGS);
        } else {
            Cursor cursor = getActivity().getContentResolver().query(LogSchema.CONTENT_URI,
                    new String[]{"count(*) AS count"},
                    select, selectArgs, null);
            cursor.moveToFirst();
            int offset = 0;
            if (!cursor.isAfterLast()) {
                int cnt = cursor.getInt(0);
                if (cnt > MAX_LOGS)
                    offset = cnt - MAX_LOGS;
                Log.d(TAG, "offset=" + offset);
                cursor.close();
            }
            sortOrder = LogSchema._ID + " ASC";
            limit = offset + "," + String.valueOf(MAX_LOGS);
        }
        Uri uri = LogSchema.CONTENT_URI.buildUpon().appendQueryParameter(
                LogContentProvider.LIMIT_KEY, limit).build();

        return new CursorLoader(getActivity(), uri,
                LogSchema.DEFAULT_PROJECTION, select,selectArgs, sortOrder);
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

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
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
}
