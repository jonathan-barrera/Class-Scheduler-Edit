package com.example.android.classscheduler;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.classscheduler.model.Student;
import com.example.android.classscheduler.utils.DateUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.apache.commons.text.WordUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.android.classscheduler.EditStudentInfo.FIREBASE_CHILD_KEY_STUDENTS;
import static com.example.android.classscheduler.EditStudentInfo.FIREBASE_CHILD_KEY_USERS;
import static com.example.android.classscheduler.EditStudentInfo.SEX_FEMALE_INT;
import static com.example.android.classscheduler.EditStudentInfo.SEX_MALE_INT;

public class StudentProfile extends AppCompatActivity {

    // Constants
    public static final String STUDENT_EXTRA_KEY = "student-extra-key";
    public static final String CLASS_LIST_EXTRA_KEY = "class-list-extra-key";
    public static final String STUDENT_NAME_EXTRA_KEY = "student-name-extra-key";

    // Member variables
    private Student mCurrentStudent;
    private String mStudentId;
    private ArrayList<String> mChosenClassList;
    private String mUserId;

    // Firebase instances
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mStudentDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mPhotoStorageReference;

    // Views
    @BindView(R.id.student_profile_picture_view)
    ImageView mStudentPictureView;
    @BindView(R.id.student_profile_name_view)
    TextView mStudentNameView;
    @BindView(R.id.student_profile_sex_view)
    TextView mStudentSexView;
    @BindView(R.id.student_profile_age_view)
    TextView mStudentAgeView;
    @BindView(R.id.student_profile_grade_view)
    TextView mStudentGradeView;
    @BindView(R.id.student_profile_classes_view)
    TextView mStudentClassesView;
    @BindView(R.id.student_profile_plus_image_view)
    ImageView mPlusImageView;
    @BindView(R.id.student_profile_add_photo_text_view)
    TextView mAddPhotoTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        // Set title
        setTitle(getString(R.string.student_profile));

        // Bind views
        ButterKnife.bind(this);

        // Get UserId
        SharedPreferences sharedPreferences = getSharedPreferences(MainMenu.SHARED_PREFS, MODE_PRIVATE);
        mUserId = sharedPreferences.getString(MainMenu.USER_ID_SHARED_PREF_KEY, null);

        // Retrieve the data sent with the intent
        Intent intent = getIntent();
        mStudentId = intent.getStringExtra(StudentAdapter.STUDENT_ID_EXTRA_KEY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize Firebase instances
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mStudentDatabaseReference = mFirebaseDatabase.getReference()
                .child(FIREBASE_CHILD_KEY_USERS)
                .child(mUserId)
                .child(FIREBASE_CHILD_KEY_STUDENTS)
                .child(mStudentId);

        //Populate the views with student data
        mStudentDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mCurrentStudent = dataSnapshot.getValue(Student.class);
                populateViews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.student_profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.edit_action:
                // Intent to open EditStudentInfo activity
                Intent intent = new Intent(this, EditStudentInfo.class);
                intent.putExtra(STUDENT_EXTRA_KEY, mCurrentStudent);
                startActivity(intent);
                break;
            case R.id.delete_action:
                showDeleteDialog();
                break;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                throw new IllegalArgumentException("Invalid Option Selected.");
        }
        return super.onOptionsItemSelected(item);
    }

    // Prompt user to confirm student deletion
    private void showDeleteDialog() {
        // Create an AlertDialog.Builder and set the message and click listeners for the positive
        // and negative buttons.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_student_dialog);

        // Delete
        builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteStudent();
            }
        });

        // Don't delete
        builder.setNegativeButton(getString(R.string.return_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Delete student from the database
    private void deleteStudent() {
        // Delete student from Firebase database
        mStudentDatabaseReference.removeValue();

        // Delete Student's photo from Firebase storage (only if photo exists)
        if (!TextUtils.isEmpty(mCurrentStudent.getPhotoUrl())) {
            mFirebaseStorage = FirebaseStorage.getInstance();
            mPhotoStorageReference = mFirebaseStorage.getReferenceFromUrl(mCurrentStudent.getPhotoUrl());
            mPhotoStorageReference.delete();
        }

        Toast.makeText(StudentProfile.this, R.string.student_deleted, Toast.LENGTH_SHORT).show();

        // Close activity
        finish();
    }

    private void populateViews() {
        // Extract data from the StudentLocalDatabase object
        String name = WordUtils.capitalizeFully(mCurrentStudent.getName());
        int sex = mCurrentStudent.getSex();
        long birthdate = mCurrentStudent.getBirthdate();
        int grade = mCurrentStudent.getGrade();
        mChosenClassList = (ArrayList<String>) mCurrentStudent.getClasses();
        String classes = "";
        if (mChosenClassList != null) {
            classes = TextUtils.join(", ", mChosenClassList);
        }
        String photoUrl = mCurrentStudent.getPhotoUrl();

        // Populate views
        mStudentNameView.setText(name);
        mStudentAgeView.setText(DateUtils.convertDateLongToString(birthdate));
        mStudentGradeView.setText(String.valueOf(grade));
        if (TextUtils.isEmpty(classes)) {
            classes = getString(R.string.no_classes_added);
            mStudentClassesView.setTypeface(mStudentClassesView.getTypeface(), Typeface.ITALIC);
        }
        mStudentClassesView.setText(classes);

        // Get the sex of the student (male/female)
        String sexString;
        if (sex == SEX_MALE_INT) {
            sexString = getString(R.string.male);
        } else if (sex == SEX_FEMALE_INT) {
            sexString = getString(R.string.female);
        } else {
            throw new IllegalArgumentException("Invalid sex");
        }
        mStudentSexView.setText(sexString);

        // Only show the student pic if it exists
        if (!TextUtils.isEmpty(photoUrl)) {
            Glide.with(mStudentPictureView.getContext())
                    .load(photoUrl)
                    .into(mStudentPictureView);

            // Hide the Add Photo image and text
            mPlusImageView.setVisibility(View.GONE);
            mAddPhotoTextView.setVisibility(View.GONE);
        } else {
            // Show "add picture" views
            mStudentPictureView.setImageResource(R.drawable.gray_circle);
            mPlusImageView.setVisibility(View.VISIBLE);
            mAddPhotoTextView.setVisibility(View.VISIBLE);
        }

    }

    // Method for opening the Class Details page
    public void openClassDetailsActivity(View v) {
        // Don't take User to class details activity if there are no classes listed
        if (mChosenClassList == null) {
            Toast.makeText(this, R.string.no_classes_to_show, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ClassDetailsActivity.class);
        intent.putStringArrayListExtra(CLASS_LIST_EXTRA_KEY, mChosenClassList);
        intent.putExtra(STUDENT_NAME_EXTRA_KEY, mCurrentStudent.getName());
        startActivity(intent);
    }
}
