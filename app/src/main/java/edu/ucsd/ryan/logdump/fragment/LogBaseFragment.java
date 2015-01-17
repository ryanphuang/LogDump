package edu.ucsd.ryan.logdump.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.SearchView;
import android.widget.TextView;

import edu.ucsd.ryan.logdump.MainActivity;
import edu.ucsd.ryan.logdump.R;
import edu.ucsd.ryan.logdump.util.LogLevel;

/**
 * Created by ryan on 1/14/15.
 */

/**
 * A fragment representing a list of logs.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link LogBaseFragment.OnLogEntrySelectedListener}
 * interface.
 */
public abstract class LogBaseFragment extends Fragment implements AbsListView.OnItemClickListener,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    protected static final int MAX_LOGS = 512;

    protected static final String ARG_PKG = "pkg";

    protected String mFilterPkg;

    protected OnLogEntrySelectedListener mListener;

    protected boolean mFlushTop = true;

    /**
     * The fragment's ListView/GridView.
     */
    protected AbsListView mListView;

    protected SearchView mSearchView;

    protected String mContentFilter;
    protected LogLevel mLevelFilter;

    protected boolean mListShown;
    protected View mProgressContainer;
    protected View mListContainer;
    protected boolean mShowExtra;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LogBaseFragment() {

    }


    public abstract void reloadLogs();
    public abstract String getTAG();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFilterPkg = getArguments().getString(ARG_PKG);
        }
        mLevelFilter = null;
    }

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
        MenuItem filterItem = menu.findItem(R.id.action_filter);
        if (filterItem != null) {
            filterItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    showFilter();
                    return true;
                }
            });
        }

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            mSearchView = new LogSearchView(getActivity());
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnCloseListener(this);
            mSearchView.setIconifiedByDefault(true);
            searchItem.setActionView(mSearchView);
        }

        MenuItem homeItem = menu.findItem(R.id.action_home);
        if (homeItem != null) {
            homeItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    startActivity(new Intent(getActivity(), MainActivity.class));
                    return true;
                }
            });
        }
    }

    public void showFilter() {
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
                        Log.d(getTAG(), "Filter by " + mLevelFilter);
                        reloadLogs();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.show();
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
            Log.e(getTAG(), "No empty view");
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
        public void onLogEntrySelected(Object data);
    }
}
