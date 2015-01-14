package edu.ucsd.ryan.logdump.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import edu.ucsd.ryan.logdump.R;
import edu.ucsd.ryan.logdump.data.FilterSchema;
import edu.ucsd.ryan.logdump.util.FilterDBHelper;

/**
 * Created by ryan on 1/11/15.
 */
public class FilterDialogFragment extends DialogFragment {
    public interface FilterDialogListener {
        public void onFilterDialogPositiveClick(DialogFragment dialog);
        public void onFilterDialogNegativeClick(DialogFragment dialog);
    }

    private static final String TITLE_KEY = "title";
    private FilterDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (FilterDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    public static FilterDialogFragment newInstance(int title) {
        FilterDialogFragment frag = new FilterDialogFragment();
        Bundle args = new Bundle();
        args.putInt(TITLE_KEY, title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt(TITLE_KEY);
        final FilterDBHelper dbHelper = new FilterDBHelper(getActivity());
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final Cursor cursor = db.query(FilterSchema.TABLE_NAME, FilterSchema.DEFAULT_PROJECTION,
                null, null, null, null, FilterSchema.COLUMN_APP);
        final Set<String> selectedPkgs = new HashSet<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String pkg = cursor.getString(FilterSchema.COLUMN_PKGNAME_INDEX);
            int checked = cursor.getInt(FilterSchema.COLUMN_CHECKED_INDEX);
            if (checked == 1)
                selectedPkgs.add(pkg);
            cursor.moveToNext();
        }
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_menu_more)
                .setTitle(title)
                .setMultiChoiceItems(cursor, FilterSchema.COLUMN_CHECKED,
                        FilterSchema.COLUMN_APP,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if (cursor.moveToPosition(which)) {
                                    String pkg = cursor.getString(FilterSchema.COLUMN_PKGNAME_INDEX);
                                    if (isChecked) {
                                        selectedPkgs.add(pkg);
                                    } else {
                                        selectedPkgs.remove(pkg);
                                    }
                                } else {
                                    Toast.makeText(getActivity(), "Fail to head cursor",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                .setPositiveButton(R.string.filter_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ContentValues values = new ContentValues();
                                values.put(FilterSchema.COLUMN_CHECKED, 0);
                                db.update(FilterSchema.TABLE_NAME, values, null, null);
                                for (String pkg:selectedPkgs) {
                                    values.clear();
                                    values.put(FilterSchema.COLUMN_CHECKED, 1);
                                    db.update(FilterSchema.TABLE_NAME, values,
                                            FilterSchema.COLUMN_PKGNAME + "=?",
                                            new String[]{pkg});
                                }
                                mListener.onFilterDialogPositiveClick(FilterDialogFragment.this);
                                db.close();
                            }
                        })
                .setNegativeButton(R.string.filter_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mListener.onFilterDialogNegativeClick(FilterDialogFragment.this);
                                db.close();
                            }
                        })
                .create();
    }


}
