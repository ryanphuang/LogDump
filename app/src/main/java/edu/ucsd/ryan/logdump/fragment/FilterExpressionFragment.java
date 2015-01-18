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
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import edu.ucsd.ryan.logdump.R;
import edu.ucsd.ryan.logdump.data.FilterExpression;
import edu.ucsd.ryan.logdump.data.FilterSchema;
import edu.ucsd.ryan.logdump.util.FilterDBHelper;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 * interface.
 */
public class FilterExpressionFragment extends DialogFragment {

    private static final String TITLE_KEY = "title";
    private static final String TAG = "FilterExpressionFragment";

    public interface FilterExprDialogListener {
        public void onFilterExprAdded(FilterExpression expression);
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FilterExpressionFragment() {

    }

    public static FilterExpressionFragment newInstance(String title) {
        FilterExpressionFragment frag = new FilterExpressionFragment();
        Bundle args = new Bundle();
        args.putString(TITLE_KEY, title);
        frag.setArguments(args);
        return frag;
    }

    private FilterExprDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (FilterExprDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement FilterExprDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FilterDBHelper dbHelper = new FilterDBHelper(getActivity());
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        String title = getArguments().getString(TITLE_KEY);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View dialogView = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_filter_expression, null);
        return builder.setTitle(title)
                .setView(dialogView).setPositiveButton("Add",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EditText editText = (EditText) dialogView.findViewById(R.id.tagText);
                                Spinner priority = (Spinner) dialogView.findViewById(R.id.priority);
                                FilterExpression expression = new FilterExpression(editText.getText().toString(),
                                        priority.getSelectedItem().toString());
                                db.delete(FilterSchema.TABLE_NAME, FilterSchema.COLUMN_TAG + "=? AND " +
                                                FilterSchema.COLUMN_PRIORITY + "=?", new String[]{
                                        expression.tag, expression.priority});
                                ContentValues values = new ContentValues();
                                values.put(FilterSchema.COLUMN_TAG, expression.tag);
                                values.put(FilterSchema.COLUMN_PRIORITY, expression.priority);
                                values.put(FilterSchema.COLUMN_CHECKED, 1);
                                long rowid = db.insert(FilterSchema.TABLE_NAME, null, values);
                                Log.d(TAG, "New expression " + expression + " at row " + rowid);
                                db.close();
                                if (mListener != null) {
                                    mListener.onFilterExprAdded(expression);
                                }
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.close();
                    }
                }).create();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
