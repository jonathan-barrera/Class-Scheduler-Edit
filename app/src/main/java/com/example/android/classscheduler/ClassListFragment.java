package com.example.android.classscheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.classscheduler.model.SchoolClass;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;


/**
 * A simple {@link Fragment} subclass.
 */

public class ClassListFragment extends Fragment {

    // Constants
    public static final String BUNDLE_RECYCLER_VIEW_KEY = "bundle-recycler-view-key";

    // List to hold School Class objects
    private List<SchoolClass> mClassObjectList;
    private List<SchoolClass> mMatchedClassObjectList;

    // Member variables
    private SchoolClassAdapter mAdapter;
    private Parcelable mSavedState;
    private DatabaseReference mClassesDatabaseReference;
    private ValueEventListener mValueEventListener;

    // Views
    @BindView(R.id.class_list_recycler_view)
    RecyclerView mClassListRecyclerView;
    @BindView(R.id.class_list_empty_view)
    TextView mEmptyView;
    @BindView(R.id.class_list_progress_bar)
    ProgressBar mProgressBar;
    @BindView(R.id.class_list_fab)
    FloatingActionButton mFab;

    public ClassListFragment() {} // Empty Constructor

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("oncreateview called");
        View rootView = inflater.inflate(R.layout.fragment_class_list, container, false);
        super.onCreate(savedInstanceState);

        // Set options menu
        setHasOptionsMenu(true);

        // Bind views
        ButterKnife.bind(this, rootView);

        // Set onclicklistener on FAB
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCreateClassesActivity();
            }
        });

        // Get UserId
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(
                MainMenu.SHARED_PREFS, MODE_PRIVATE);
        String mUserId = sharedPreferences.getString(MainMenu.USER_ID_SHARED_PREF_KEY, null);

        // Initialize Firebase instances
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        mClassesDatabaseReference = mFirebaseDatabase.getReference()
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_USERS)
                .child(mUserId)
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_CLASSES);

        // Get data from intent
        mClassObjectList = new ArrayList<>();
        mMatchedClassObjectList = new ArrayList<>();

        // Set Layout Manager
        mClassListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter and set to Recycler View
        mAdapter = new SchoolClassAdapter();
        mClassListRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set classobjectlist to the adapter
        mAdapter.setClassData(mClassObjectList);

        // Check for connectivity
        if (!checkNetworkConnectivity()) {
            mProgressBar.setVisibility(View.GONE);
            showEmptyTextView();
        }

        // Query firebase database for class info
        if (mValueEventListener == null) {
            mValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Timber.d("ondatachange classlistfragment called");
                    Iterable<DataSnapshot> children = dataSnapshot.getChildren();

                    for (DataSnapshot child : children) {
                        SchoolClass schoolClass = child.getValue(SchoolClass.class);
                        mClassObjectList.add(schoolClass);
                    }

                    mProgressBar.setVisibility(View.GONE);
                    mAdapter.notifyDataSetChanged();
                    if (mSavedState == null) {
                        Timber.d("msavedstate is null called");
                    }
                    mClassListRecyclerView.getLayoutManager().onRestoreInstanceState(mSavedState);

                    if (mClassObjectList == null || mClassObjectList.size() == 0) {
                        showEmptyTextView();
                    } else {
                        mEmptyView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };

            mClassesDatabaseReference.addValueEventListener(mValueEventListener);
        }
    }

    // Method for opening Create Classes Activity
    public void openCreateClassesActivity() {
        Intent intent = new Intent(getContext(), CreateClassesActivity.class);
        startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.class_list_menu, menu);

        // Set SearchView
        MenuItem search = menu.findItem(R.id.action_search_classes);
        final SearchView searchView = (SearchView) search.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Clear list first
                mMatchedClassObjectList.clear();

                // Show a list of names that could match
                mAdapter.setClassData(mMatchedClassObjectList);

                // Loop through all students to get list of matched classes
                for (int i = 0; i < mClassObjectList.size(); i++) {
                    SchoolClass schoolClass = mClassObjectList.get(i);
                    String title = schoolClass.getTitle();
                    if ((title.toLowerCase()).contains(newText.toLowerCase().trim())) {
                        mMatchedClassObjectList.add(schoolClass);
                        mAdapter.notifyDataSetChanged();
                    }
                }
                return true;
            }

        });
    }

    @Override
    public void onPause() {
        super.onPause();

        // Clear ClassObjectList
        mClassObjectList.clear();

        // Detach ValueEventListener
        if (mValueEventListener != null) {
            mClassesDatabaseReference.removeEventListener(mValueEventListener);
            mValueEventListener = null;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Timber.d("onsaveinstancestate classlistfragment called");
        super.onSaveInstanceState(outState);
        mSavedState = mClassListRecyclerView.getLayoutManager().onSaveInstanceState();
        outState.putParcelable(BUNDLE_RECYCLER_VIEW_KEY, mSavedState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        Timber.d("onviewstaterestored classlistfragment called");
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mSavedState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_VIEW_KEY);
        }
    }

    // Method for checking network connectivity
    private boolean checkNetworkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    // Method for showing the empty text view
    private void showEmptyTextView() {
        if (mAdapter.getItemCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);

            if (!checkNetworkConnectivity()) {
                mEmptyView.setText(R.string.no_internet_connection);
            } else {
                mEmptyView.setText(R.string.no_classes_found);
            }
        }
    }

    @Override
    public void onDestroyView() {
        Timber.d("view destroyed called");
        super.onDestroyView();
    }
}
