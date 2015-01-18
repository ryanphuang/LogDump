package edu.ucsd.ryan.logdump;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

import edu.ucsd.ryan.logdump.data.FilterExpression;
import edu.ucsd.ryan.logdump.fragment.FilterAppDialogFragment;
import edu.ucsd.ryan.logdump.fragment.FilterDrawerFragment;
import edu.ucsd.ryan.logdump.fragment.FilterExpressionFragment;
import edu.ucsd.ryan.logdump.fragment.LogHistoryFragment;
import edu.ucsd.ryan.logdump.fragment.LogRealtimeFragment;
import edu.ucsd.ryan.logdump.service.LogCollectionService;
import edu.ucsd.ryan.logdump.util.FilterDBRunnable;
import edu.ucsd.ryan.logdump.util.PackageHelper;


public class MainActivity extends ActionBarActivity
        implements FilterDrawerFragment.FilterDrawerCallbacks,
        FilterAppDialogFragment.FilterAppDialogListener,
        FilterExpressionFragment.FilterExprDialogListener,
        LogHistoryFragment.OnLogEntrySelectedListener {

    private static final String TAG = "MainActivity";
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private FilterDrawerFragment mFilterDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private boolean mBound;
    private LogCollectionService mService;

    private Fragment mMainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new FilterDBRunnable(MainActivity.this)).start();

        mFilterDrawerFragment = (FilterDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mFilterDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        diplayDefaultFragment();
        bindService(new Intent(this, LogCollectionService.class),
                mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    private void diplayDefaultFragment() {
        mTitle = getTitle();
        mMainFragment = LogRealtimeFragment.newInstance(null, null, null);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, mMainFragment)
                .commit();
    }

    @Override
    public void onFilterDrawerItemSelected(String filter) {
        // update the main content by replacing fragments
        Log.d(TAG, "Filter " + filter);
        if (TextUtils.isEmpty(filter)) {
            diplayDefaultFragment();
        } else {
            int index = filter.indexOf(':');
            String pkg = null;
            String tag = null;
            String priority = null;
            if (index >= 0) {
                // tag:priority filter
                tag = filter.substring(0, index);
                priority = filter.substring(index + 1);
                mTitle = filter;
            } else {
                pkg = filter;
                // package filter
                mTitle = PackageHelper.getInstance(MainActivity.this).getName(filter);
            }
            mMainFragment = LogHistoryFragment.newInstance(pkg, tag, priority);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, mMainFragment)
                    .commit();
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mFilterDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            MenuItem refreshItem = menu.findItem(R.id.action_refresh);
            MenuItem pauseItem = menu.findItem(R.id.action_pause);
            if (mMainFragment instanceof LogRealtimeFragment) {
                refreshItem.setVisible(false);
                pauseItem.setVisible(true);
            } else if (mMainFragment instanceof LogHistoryFragment) {
                // refreshItem.setVisible(true);
                refreshItem.setVisible(false); // we can automatically refresh already
                pauseItem.setVisible(false);
            }
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add_section:
                showFilterDialog();
                break;

            case R.id.action_delete_section:
                mFilterDrawerFragment.toggleDeleteButton();
                break;

            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    public void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final CharSequence[] choiceList = {"By app name", "By tag"};
        builder.setTitle("Choose filter type")
                .setSingleChoiceItems(choiceList, 0, null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                        dialog.dismiss();
                        if (selectedPosition == 0) {
                            String title = getResources().getString(R.string.title_filter_app_dialog);
                            FilterAppDialogFragment fragment = FilterAppDialogFragment.newInstance(title);
                            fragment.show(getFragmentManager(), "FilterDialogFragment");
                        } else {
                            String title = getResources().getString(R.string.title_filter_tag_dialog);
                            FilterExpressionFragment fragment = FilterExpressionFragment.newInstance(title);
                            fragment.show(getFragmentManager(), "FilterExpressionFragment");
                        }
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onFilterAppUpdated(Set<String> selectedPkgs) {
        mFilterDrawerFragment.refreshDrawerList();
        if (mBound) {
            mService.updatePkgFilters();
        }
    }

    @Override
    public void onFilterExprAdded(FilterExpression expression) {
        mFilterDrawerFragment.refreshDrawerList();
        if (mBound) {
            mService.updateExprFilters();
        }
    }

    @Override
    public void onLogEntrySelected(Object data) {

    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            LogCollectionService.LocalBinder binder = (LogCollectionService.LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            mService = null;
        }
    };
}
