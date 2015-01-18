package edu.ucsd.ryan.logdump.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import edu.ucsd.ryan.logdump.R;
import edu.ucsd.ryan.logdump.data.FilterSchema;
import edu.ucsd.ryan.logdump.data.LogSchema;
import edu.ucsd.ryan.logdump.util.FilterDBHelper;
import edu.ucsd.ryan.logdump.util.LogDBHelper;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class FilterDrawerFragment extends Fragment {

    private static final String TAG = "FilterDrawerFragment";

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private FilterDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;

    private SQLiteDatabase mDB;
    private CursorAdapter mDrawerListAdapter;
    private boolean mDeleteBtnVisible;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }

        mDeleteBtnVisible = false;

        FilterDBHelper dbHelper = new FilterDBHelper(getActivity());
        mDB = dbHelper.getWritableDatabase();

        // Select either the default item (0) or the last selected item.
        selectItem(mCurrentSelectedPosition);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDB.close();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(
                R.layout.fragment_filter_drawer, container, false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.setSelected(true);
                selectItem(position);
            }
        });
        refreshDrawerList();
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        return mDrawerListView;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public void toggleDeleteButton() {
        for (int i = 0; i < mDrawerListView.getChildCount(); ++i) {
            View itemView = mDrawerListView.getChildAt(i);
            ImageButton button = (ImageButton) itemView.findViewById(R.id.deleteButton);
            if (mDeleteBtnVisible)
                button.setVisibility(View.GONE);
            else
                button.setVisibility(View.VISIBLE);
        }
        mDeleteBtnVisible = !mDeleteBtnVisible;
    }

    public void refreshDrawerList() {
        Log.d(TAG, "Refresh drawer list");
        String select = "((" + FilterSchema.COLUMN_TAG + " IS NOT NULL AND " +
                FilterSchema.COLUMN_PRIORITY + " IS NOT NULL) OR " +
                FilterSchema.COLUMN_PKGNAME + " IS NOT NULL)) AND " +
                FilterSchema.COLUMN_CHECKED + "=?";

        Cursor cursor = mDB.query(FilterSchema.TABLE_NAME, new String[]{
                        FilterSchema._ID, FilterSchema.COLUMN_PKGNAME,
                        FilterSchema.COLUMN_TAG,
                        FilterSchema.COLUMN_PRIORITY},
                select, new String[]{String.valueOf(1)},
                null, null, FilterSchema.COLUMN_PKGNAME);

        mDrawerListAdapter = new FilterCursorAdapter(getActivity(), cursor, 0);
        mDrawerListView.setAdapter(mDrawerListAdapter);
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        String filterPkg = null;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
            Cursor cursor = (Cursor) mDrawerListView.getItemAtPosition(position);
            if (!cursor.isBeforeFirst() && !cursor.isAfterLast())
                filterPkg = cursor.getString(1);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onFilterDrawerItemSelected(filterPkg);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (FilterDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface FilterDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onFilterDrawerItemSelected(String filter);
    }

    public class DeleteFilterTask extends AsyncTask<String, Void, Integer> {
        ProgressDialog mProgressDlg;

        @Override
        protected void onPreExecute() {
            mProgressDlg = new ProgressDialog(getActivity());
            mProgressDlg.setIndeterminate(true);
            mProgressDlg.setTitle("Deleting filter and its logs...");
            mProgressDlg.show();
        }

        @Override
        protected Integer doInBackground(String... params) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < params.length; ++i) {
                sb.append("?");
                if (i != params.length - 1)
                    sb.append(",");
            }
            String select = sb.toString();
            if (select.length() > 0) {
                select = FilterSchema.COLUMN_PKGNAME + " IN (" + select + ")";
                ContentValues values = new ContentValues();
                values.put(FilterSchema.COLUMN_CHECKED, 0);
                int rowsUpdated = mDB.update(FilterSchema.TABLE_NAME, values, select, params);
                LogDBHelper helper = new LogDBHelper(getActivity());
                SQLiteDatabase logDB = helper.getWritableDatabase();
                logDB.delete(LogSchema.TABLE_NAME, select, params);
                logDB.close();
                return rowsUpdated;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            mProgressDlg.dismiss();
            refreshDrawerList();
            selectItem(0);
            Toast.makeText(getActivity(), "Filter deleted",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public class FilterCursorAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public FilterCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.filter_list_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            int pkgIndex = cursor.getColumnIndex(FilterSchema.COLUMN_PKGNAME);
            final String pkgName = cursor.getString(pkgIndex);
            int tagIndex = cursor.getColumnIndex(FilterSchema.COLUMN_TAG);
            final String tag = cursor.getString(tagIndex);
            int priorityIndex = cursor.getColumnIndex(FilterSchema.COLUMN_PRIORITY);
            final String priority = cursor.getString(priorityIndex);
            TextView textView = (TextView) view.findViewById(R.id.filterText);
            String filter = null;
            if (TextUtils.isEmpty(pkgName)) {
                if (!TextUtils.isEmpty(tag)) {
                    filter = tag + ":" + priority;
                }
            } else {
                filter = pkgName;
            }
            if (!TextUtils.isEmpty(filter)) {
                textView.setText(filter);
            }
            ImageButton button = (ImageButton) view.findViewById(R.id.deleteButton);
            if (!TextUtils.isEmpty(filter)) {
                button.setOnClickListener(new DeleteFilterListener(filter));
            }
        }
    }

    private class DeleteFilterListener implements View.OnClickListener {
        private String mFilter;
        public DeleteFilterListener(String filter) {
            mFilter = filter;
        }

        @Override
        public void onClick(View v) {
            new AlertDialog.Builder(getActivity())
                    .setMessage("Delete filter " + mFilter + "?")
                    .setCancelable(true)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new DeleteFilterTask().execute(mFilter);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }
}
