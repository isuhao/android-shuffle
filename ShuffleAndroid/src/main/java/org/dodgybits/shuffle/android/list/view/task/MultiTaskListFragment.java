package org.dodgybits.shuffle.android.list.view.task;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.util.OSUtils;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.model.ListTitles;

import java.util.List;

/**
 * Switch between multiple different task list views via a spinner in the action bar.
 */
public class MultiTaskListFragment extends TaskListFragment {
    private static final String TAG = "MultiTaskListFragment";
    public static final String SELECTED_INDEX = "SELECTED_INDEX";

    private MultiTaskListContext mListContext;
    private SpinnerAdapter mAdapter;
    private ActionBar.OnNavigationListener mListener;
    private int mOldOptions = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<ListQuery> queries = getMultiTaskListContext().getListQueries();
        List<String> names = Lists.transform(queries, new Function<ListQuery, String>() {
            @Override
            public String apply(ListQuery input) {
                return getString(ListTitles.getTitleId(input));
            }
        });
        int spinnerResId = OSUtils.atLeastHoneycomb() ? android.R.layout.simple_spinner_dropdown_item : android.R.layout.simple_list_item_1;
        mAdapter = new ArrayAdapter(getActivity(), spinnerResId, names);
        mListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                Log.d(TAG, "Navigated to item " + itemPosition);
                mListContext.setListIndex(itemPosition);
                restartLoading();
                onVisibilityChange();
                return true;
            }
        };
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Fragment doesn't have this method.  Call it manually.
            restoreInstanceState(savedInstanceState);
        } else {
            // first time here - check if a list was specified on the intent
            Intent intent = getActivity().getIntent();
            String queryName = intent.getStringExtra(MainActivity.QUERY_NAME);
            if (queryName != null) {
                ListQuery query = ListQuery.valueOf(queryName);
                int index = getMultiTaskListContext().getListQueries().indexOf(query);
                if (index > -1) {
                    mListContext.setListIndex(index);
                }
            }
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected void onVisibilityChange() {
        Log.d(TAG, "Visibility change to " + getUserVisibleHint());
        if (getUserVisibleHint()) {
            ActionBar actionBar = getRoboActionBarActivity().getSupportActionBar();
            if (mOldOptions == -1) {
                mOldOptions = actionBar.getDisplayOptions();
                actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME);
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                actionBar.setListNavigationCallbacks(mAdapter, mListener);
            }
            if (actionBar.getSelectedNavigationIndex() != getSelectedIndex()) {
                actionBar.setSelectedNavigationItem(getSelectedIndex());
            }
        } else {
            if (getActivity() != null && mOldOptions != -1) {
                ActionBar actionBar = getRoboActionBarActivity().getSupportActionBar();
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                actionBar.setDisplayOptions(mOldOptions);
                mOldOptions = -1;
            }
        }

        super.onVisibilityChange();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_INDEX, mListContext.getListIndex());
    }

    @Override
    void restoreInstanceState(Bundle savedInstanceState) {
        super.restoreInstanceState(savedInstanceState);
        
        int savedIndex = savedInstanceState.getInt(SELECTED_INDEX, 0);
        mListContext.setListIndex(savedIndex);
    }

    private MultiTaskListContext getMultiTaskListContext() {
        initializeArgCache();
        return mListContext;
    }
    
    private int getSelectedIndex() {
        initializeArgCache();
        return mListContext.getListIndex();
    }

    private void initializeArgCache() {
        if (mListContext != null) return;
        mListContext = getArguments().getParcelable(ARG_LIST_CONTEXT);
        mListContext.setListIndex(getArguments().getInt(SELECTED_INDEX, 0));
    }

    
}
