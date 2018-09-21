package com.example.android.classscheduler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.android.classscheduler.model.SchoolClass;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.text.WordUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Activity for displaying the details for all of the classes a student is in
 */

public class ClassDetailsActivity extends AppCompatActivity //implements SchoolClassAdapter.onItemClickListener
{
    // Constants
    public static final String BUNDLE_RECYCLER_VIEW_KEY = "bundle-recycler-view-key";

    // List to hold list of chosen class titles
    private List<String> mChosenClassList;

    // List to hold School Class objects
    private List<SchoolClass> mClassObjectList;

    // Views
    @BindView(R.id.class_details_recycler_view)
    RecyclerView mClassDetailsRecyclerView;

    // Firebase instances
    private FirebaseDatabase mFirebaseDatabase;

    // Member variables
    private String mUserId;
    private Parcelable mSavedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_details);

        // Bind views
        ButterKnife.bind(this);

        // Get UserId
        SharedPreferences sharedPreferences = getSharedPreferences(MainMenu.SHARED_PREFS, MODE_PRIVATE);
        mUserId = sharedPreferences.getString(MainMenu.USER_ID_SHARED_PREF_KEY, null);

        // Initialize Firebase instances
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference classesDatabaseReference = mFirebaseDatabase.getReference()
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_USERS)
                .child(mUserId)
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_CLASSES);

        // Get data from intent
        Intent intent = getIntent();
        mChosenClassList = intent.getStringArrayListExtra(StudentProfile.CLASS_LIST_EXTRA_KEY);
        mClassObjectList = new ArrayList<>();

        // Set Title
        String title = WordUtils.capitalizeFully(intent
                .getStringExtra(StudentProfile.STUDENT_NAME_EXTRA_KEY)) + getString(R.string.s_classes);
        setTitle(title);

        // Set Layout Manager
        mClassDetailsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter and set to Recycler View
        final SchoolClassAdapter schoolClassAdapter = new SchoolClassAdapter();
        mClassDetailsRecyclerView.setAdapter(schoolClassAdapter);
        schoolClassAdapter.setClassData(mClassObjectList);

        // Use this data to download relevant SchoolClass objects from Firebase Database
        // Initialize Firebase instances
        classesDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();

                for (DataSnapshot child : children) {
                    SchoolClass schoolClass = child.getValue(SchoolClass.class);
                    if (mChosenClassList.contains(schoolClass.getTitle())) {
                        mClassObjectList.add(schoolClass);
                    }
                }
                schoolClassAdapter.notifyDataSetChanged();
                mClassDetailsRecyclerView.getLayoutManager().onRestoreInstanceState(mSavedState);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BUNDLE_RECYCLER_VIEW_KEY, mClassDetailsRecyclerView.getLayoutManager().
                onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mSavedState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_VIEW_KEY);
            if (mSavedState != null) {
                mClassDetailsRecyclerView.getLayoutManager().onRestoreInstanceState(mSavedState);
            }

        }
    }
}
