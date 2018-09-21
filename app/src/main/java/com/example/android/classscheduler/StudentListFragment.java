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

import com.example.android.classscheduler.model.Student;
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
public class StudentListFragment extends Fragment {

    // Member variables
    private StudentAdapter mAdapter;
    private String mUserId;
    private Parcelable mSavedState;

    // Views
    @BindView(R.id.student_recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.student_list_empty_view)
    TextView mEmptyView;
    @BindView(R.id.student_list_progress_bar)
    ProgressBar mProgressBar;
    @BindView(R.id.student_list_fab)
    FloatingActionButton mFab;

    private DatabaseReference mStudentDatabaseReference;
    private ValueEventListener mValueEventListener;

    // List of Student Objects
    private List<Student> mStudentList;
    private List<Student> mMatchedStudentList;

    // Keys
    public static final String BUNDLE_RECYCLER_VIEW_KEY = "bundle-recycler-view-key";

    public StudentListFragment() {} // Required empty constructor

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("oncreateview called");
        // Inflate view
        View rootView = inflater.inflate(R.layout.fragment_student_list, container, false);
        super.onCreate(savedInstanceState);

        // Set optionsmenu
        setHasOptionsMenu(true);

        // BindViews
        ButterKnife.bind(this, rootView);

        // Set OnClickListener on FAB
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditStudentInfo();
            }
        });

        // Get the UserId
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                MainMenu.SHARED_PREFS, MODE_PRIVATE);
        mUserId = sharedPreferences.getString(MainMenu.USER_ID_SHARED_PREF_KEY, null);

        // New list to contain student names
        mMatchedStudentList = new ArrayList<>();
        mStudentList = new ArrayList<>();

        // Initialize Firebase instances
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        mStudentDatabaseReference = mFirebaseDatabase.getReference()
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_USERS)
                .child(mUserId)
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_STUDENTS);

        // Set a layoutmanager to the recyclerview
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter and set to Recycler View
        mAdapter = new StudentAdapter();
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mAdapter.setClassData(mStudentList);

        // Check network
        if (!checkNetworkConnectivity()) {
            mProgressBar.setVisibility(View.GONE);
            showEmptyTextView();
        }

        // Query Database for Student info
        if (mValueEventListener == null) {
            mValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Timber.d("ondatachange studentlistfragment called");
                    Iterable<DataSnapshot> children = dataSnapshot.getChildren();

                    for (DataSnapshot child : children) {
                        Student student = child.getValue(Student.class);
                        mStudentList.add(student);
                    }

                    mProgressBar.setVisibility(View.GONE);
                    mAdapter.notifyDataSetChanged();
                    if (mSavedState == null) {
                        Timber.d("msavedstate is null called");
                    }
                    mRecyclerView.getLayoutManager().onRestoreInstanceState(mSavedState);

                    if (mStudentList == null || mStudentList.size() == 0) {
                        showEmptyTextView();
                    } else {
                        mEmptyView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            };
            mStudentDatabaseReference.addValueEventListener(mValueEventListener);
        }
    }

    // When the FAB is clicked, take user to the student profile page
    public void openEditStudentInfo() {
        Intent intent = new Intent(getContext(), EditStudentInfo.class);
        startActivity(intent);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Clear Student Object list
        mStudentList.clear();

        if (mValueEventListener != null) {
            mStudentDatabaseReference.removeEventListener(mValueEventListener);
            mValueEventListener = null;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Timber.d("onsaveinstancestate studentlistfragment called");
        super.onSaveInstanceState(outState);
        mSavedState = mRecyclerView.getLayoutManager().onSaveInstanceState();
        if (mSavedState == null) {
            Timber.d("mSavedState is null in onsaveinstancestate called");
        }
        outState.putParcelable(BUNDLE_RECYCLER_VIEW_KEY, mSavedState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        Timber.d("onviewstaterestored studentlistfragment called");
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mSavedState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_VIEW_KEY);
            if (mSavedState == null) {
                Timber.d("mSavedState is null in onviewrestored called");
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.student_list_menu, menu);

        // Set SearchView
        MenuItem search = menu.findItem(R.id.action_search_students);
        final SearchView searchView = (SearchView) search.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Clear list first
                mMatchedStudentList.clear();

                // Show a list of names that could match
                mAdapter.setClassData(mMatchedStudentList);

                // Loop through all students to get list of matched classes
                for (int i = 0; i < mStudentList.size(); i++) {
                    Student student = mStudentList.get(i);
                    String title = student.getName();
                    if ((title.toLowerCase()).contains(newText.toLowerCase().trim())) {
                        mMatchedStudentList.add(student);
                        mAdapter.notifyDataSetChanged();
                    }
                }
                return true;
            }

        });
    }

    // Method for showing the empty text view
    private void showEmptyTextView() {
        mEmptyView.setVisibility(View.VISIBLE);

        if (!checkNetworkConnectivity()) {
            mEmptyView.setText(R.string.no_internet_connection);
        } else {
            mEmptyView.setText(R.string.no_students_found);
        }
    }

    // Method for checking network connectivity
    private boolean checkNetworkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    @Override
    public void onDestroyView() {
        Timber.d("view destroyed called");
        super.onDestroyView();
    }
}
