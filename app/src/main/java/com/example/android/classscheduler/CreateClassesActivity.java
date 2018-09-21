package com.example.android.classscheduler;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.android.classscheduler.model.SchoolClass;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.android.classscheduler.utils.DateUtils.getFormattedTime;

/**
 * Activity for creating and saving new classes (SchoolClass objects)
 */

public class CreateClassesActivity extends AppCompatActivity {

    // Keys
    private static final String SCHEDULE_LIST_KEY = "schedule-list-key";
    public static final String CLASS_TITLE_INTENT_KEY = "class-title-intent-key";

    // Constants
    public static final int SUNDAY_INT = 1;
    public static final int MONDAY_INT = 2;
    public static final int TUESDAY_INT = 3;
    public static final int WEDNESDAY_INT = 4;
    public static final int THURSDAY_INT = 5;
    public static final int FRIDAY_INT = 6;
    public static final int SATURDAY_INT = 7;

    // Views
    @BindView(R.id.sunday_schedule_check_box)
    CheckBox mSundayCheckBox;
    @BindView(R.id.monday_schedule_check_box)
    CheckBox mMondayCheckBox;
    @BindView(R.id.tuesday_schedule_check_box)
    CheckBox mTuesdayCheckBox;
    @BindView(R.id.wednesday_schedule_check_box)
    CheckBox mWednesdayCheckBox;
    @BindView(R.id.thursday_schedule_check_box)
    CheckBox mThursdayCheckBox;
    @BindView(R.id.friday_schedule_check_box)
    CheckBox mFridayCheckBox;
    @BindView(R.id.saturday_schedule_check_box)
    CheckBox mSaturdayCheckBox;
    @BindView(R.id.sunday_schedule_time_text_view)
    TextView mSundayTimeTextView;
    @BindView(R.id.monday_schedule_time_text_view)
    TextView mMondayTimeTextView;
    @BindView(R.id.tuesday_schedule_time_text_view)
    TextView mTuesdayTimeTextView;
    @BindView(R.id.wednesday_schedule_time_text_view)
    TextView mWednesdayTimeTextView;
    @BindView(R.id.thursday_schedule_time_text_view)
    TextView mThursdayTimeTextView;
    @BindView(R.id.friday_schedule_time_text_view)
    TextView mFridayTimeTextView;
    @BindView(R.id.saturday_schedule_time_text_view)
    TextView mSaturdayTimeTextView;
    @BindView(R.id.title_edit_text)
    EditText mTitleEditText;
    @BindView(R.id.subject_edit_text)
    EditText mSubjectEditText;
    @BindView(R.id.teacher_edit_text)
    EditText mTeacherEditText;

    // Member variables
    private int mStartTimeHour;
    private String mStartTimeMinute;
    private int mEndTimeHour;
    private String mEndTimeMinute;
    private int mCurrentSelectedDay;
    private String mUserId;
    private boolean mIsEditClass;
    private String mOldTitle;

    // Firebase Instances
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    // Variables to build a SchoolClass object
    private String mTitle;
    private String mSubject;
    private String mTeacher;
    private List<String> mClassTimesList = new ArrayList<>();

    // Checked Change Listener
    private CompoundButton.OnCheckedChangeListener mCheckedChangeListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        getCurrentSelectedDay(buttonView);
                        if (isSelectedDayTextViewEmpty()) {
                            getStartTime();
                        }
                    } else {
                        // If a day is unchecked, remove the time that was saved for it
                        removeTimeFromSelectedDay(buttonView);
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_classes);

        // Bind views
        ButterKnife.bind(this);

        // Get UserId
        SharedPreferences sharedPreferences = getSharedPreferences(MainMenu.SHARED_PREFS, MODE_PRIVATE);
        mUserId = sharedPreferences.getString(MainMenu.USER_ID_SHARED_PREF_KEY, null);

        // Set title
        setTitle(getString(R.string.create_new_class));

        // If this is an edit of an existing class, fill in the fields with the existing information
        // Only do this if there is
        if (getIntent().hasExtra(SchoolClassAdapter.CLASS_EXTRA_KEY)) {
            fillInCurrentClassInfo();
            mIsEditClass = true;
        }

        // Initialize Firebase instances
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference()
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_USERS)
                .child(mUserId)
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_CLASSES);

        // Show back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set onchecklisteners on checkboxes
        setOnCheckListeners();
    }

    // Prompt user to confirm student deletion
    private void showDeleteDialog() {

        // If the class is a new class, you don't need to delete, so just exit the activity
        if (!mIsEditClass) {
            finish();
            return;
        }

        // Create an AlertDialog.Builder and set the message and click listeners for the positive
        // and negative buttons.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.are_you_sure_delete_class_dialog);

        // Delete
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Delete from firebase database
                mDatabaseReference.child(mTitle).removeValue();

                // Call UpdateRegisterClassesService to remove class from students' class lists
                Intent intent = new Intent();
                intent.putExtra(CLASS_TITLE_INTENT_KEY, mTitle);
                UpdateRegisteredClassesService.enqueueWork(getApplicationContext(), intent);

                // Notify user class has been deleted
                Toast.makeText(CreateClassesActivity.this, R.string.class_deleted, Toast.LENGTH_SHORT).show();

                finish();
            }
        });

        // Don't delete
        builder.setNegativeButton(R.string.return_dialog, new DialogInterface.OnClickListener() {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_classes_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save_class) {
            saveNewClass();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_delete_class) {
            showDeleteDialog();
            return true;
        }
        return false;
    }

    // Helper method to fill in fields with existing class information
    private void fillInCurrentClassInfo() {
        // Set Title
        setTitle("Edit Class");

        // Get SchoolClass object from intent
        SchoolClass schoolClass = getIntent().getParcelableExtra(SchoolClassAdapter.CLASS_EXTRA_KEY);

        // Parse information from the object and fill in the views
        mTitle = schoolClass.getTitle();
        mOldTitle = schoolClass.getTitle();
        mTitleEditText.setText(mTitle);
        mSubjectEditText.setText(schoolClass.getSubject());
        mTeacherEditText.setText(schoolClass.getTeacher());

        mClassTimesList = schoolClass.getSchedule();
        fillInScheduleInfo();
    }

    // Helper method for determining if there is already a time for a selected day
    private boolean isSelectedDayTextViewEmpty() {
        switch(mCurrentSelectedDay) {
            case SUNDAY_INT:
                return TextUtils.isEmpty(mSundayTimeTextView.getText());
            case MONDAY_INT:
                return TextUtils.isEmpty(mMondayTimeTextView.getText());
            case TUESDAY_INT:
                return TextUtils.isEmpty(mTuesdayTimeTextView.getText());
            case WEDNESDAY_INT:
                return TextUtils.isEmpty(mWednesdayTimeTextView.getText());
            case THURSDAY_INT:
                return TextUtils.isEmpty(mThursdayTimeTextView.getText());
            case FRIDAY_INT:
                return TextUtils.isEmpty(mFridayTimeTextView.getText());
            case SATURDAY_INT:
                return TextUtils.isEmpty(mSaturdayTimeTextView.getText());
            default:
                throw new IllegalArgumentException("Invalid day of the week integer: " + mCurrentSelectedDay);
        }
    }

    // Helper method to fill in schedule info
    private void fillInScheduleInfo() {

        for (int i = 0; i < mClassTimesList.size(); i++) {
            String[] scheduleParts = mClassTimesList.get(i).split("/");

            // Get start time
            String startTime = getFormattedTime(scheduleParts[1]);
            String endTime = getFormattedTime(scheduleParts[2]);
            String time = startTime + "-" + endTime;

            // Get day of the week
            int dayOfWeek = Integer.parseInt(scheduleParts[0]);

            // Populate views
            switch (dayOfWeek) {
                case SUNDAY_INT:
                    mSundayCheckBox.setChecked(true);
                    mSundayTimeTextView.setText(time);
                    break;
                case MONDAY_INT:
                    mMondayCheckBox.setChecked(true);
                    mMondayTimeTextView.setText(time);
                    break;
                case TUESDAY_INT:
                    mTuesdayCheckBox.setChecked(true);
                    mTuesdayTimeTextView.setText(time);
                    break;
                case WEDNESDAY_INT:
                    mWednesdayCheckBox.setChecked(true);
                    mWednesdayTimeTextView.setText(time);
                    break;
                case THURSDAY_INT:
                    mThursdayCheckBox.setChecked(true);
                    mThursdayTimeTextView.setText(time);
                    break;
                case FRIDAY_INT:
                    mFridayCheckBox.setChecked(true);
                    mFridayTimeTextView.setText(time);
                    break;
                case SATURDAY_INT:
                    mSaturdayCheckBox.setChecked(true);
                    mSaturdayTimeTextView.setText(time);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid day of the week integer: " + dayOfWeek);
            }
        }
    }

    // Helper method to save the new SchoolClass object
    private void saveNewClass() {
        // First check if the user's input is valid
        if (!checkUserInputValidity()) {
            return;
        }

        if (mIsEditClass) {
            mDatabaseReference.child(mOldTitle).removeValue();
        }

        // Create new SchoolClass object
        SchoolClass schoolClass = new SchoolClass(mTitle, mSubject, mTeacher, mClassTimesList);

        // Save to Firebase database
        mDatabaseReference.child(mTitle).setValue(schoolClass, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                finish();
            }
        });

        //TODO check this
//        // Close activity
//        finish();
    }

    // Helper method for validating the user's input before saving a new SchoolClass object
    private boolean checkUserInputValidity() {
        mTitle = mTitleEditText.getText().toString().trim();
        mSubject = mSubjectEditText.getText().toString().trim();
        mTeacher = mTeacherEditText.getText().toString().trim();

        if (TextUtils.isEmpty(mTitle)) {
            Toast.makeText(this, R.string.please_enter_valid_title, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(mSubject)) {
            Toast.makeText(this, R.string.please_enter_valid_subject, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(mTeacher)) {
            Toast.makeText(this, R.string.please_enter_valid_teacher, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (mClassTimesList.size() == 0) {
            Toast.makeText(this, R.string.please_enter_schedule_times, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void getStartTime() {
        final Calendar myCalender = Calendar.getInstance();
        final int hour = myCalender.get(Calendar.HOUR_OF_DAY);
        int minute = myCalender.get(Calendar.MINUTE);

        TimePickerDialog.OnTimeSetListener myTimeListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (view.isShown()) {
                    mStartTimeHour = hourOfDay;

                    // Properly format minutes from 0-9
                    if (minute >= 0 && minute < 10) {
                        mStartTimeMinute = "0" + minute;
                    } else {
                        mStartTimeMinute = String.valueOf(minute);
                    }

                    getEndTime();
                }
            }
        };

        TimePickerDialog timePickerDialog = new TimePickerDialog(CreateClassesActivity.this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar, myTimeListener, hour, minute, false);
        timePickerDialog.setTitle(getString(R.string.choose_start_time));
        timePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        timePickerDialog.show();
    }

    private void getEndTime() {
        final Calendar myCalender = Calendar.getInstance();
        final int hour = myCalender.get(Calendar.HOUR_OF_DAY);
        int minute = myCalender.get(Calendar.MINUTE);

        TimePickerDialog.OnTimeSetListener myTimeListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (view.isShown()) {
                    mEndTimeHour = hourOfDay;

                    // Properly format minutes 0-9
                    if (minute >= 0 && minute < 10) {
                        mEndTimeMinute = "0" + minute;
                    } else {
                        mEndTimeMinute = String.valueOf(minute);
                    }

                    // Make a string to hold the schedule info
                    String chosenTimeAndDay = mCurrentSelectedDay + "/" + mStartTimeHour + ":" +
                            mStartTimeMinute + "/" + mEndTimeHour + ":" + mEndTimeMinute;

                    // Add string to list of strings containing all class times for this class
                    mClassTimesList.add(chosenTimeAndDay);

                    // Change the text view to reflect the time slot
                    updateTimeSlotTextView();
                }
            }
        };

        TimePickerDialog timePickerDialog = new TimePickerDialog(CreateClassesActivity.this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar, myTimeListener, hour, minute, false);
        timePickerDialog.setTitle(getString(R.string.choose_end_time));
        timePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        timePickerDialog.show();
    }

    // Helper method to update the text view for a checkbox to show the chosen time slot
    private void updateTimeSlotTextView() {
        String timeInterval = formatTimeInterval();
        switch (mCurrentSelectedDay) {
            case SUNDAY_INT:
                mSundayTimeTextView.setText(timeInterval);
                break;
            case MONDAY_INT:
                mMondayTimeTextView.setText(timeInterval);
                break;
            case TUESDAY_INT:
                mTuesdayTimeTextView.setText(timeInterval);
                break;
            case WEDNESDAY_INT:
                mWednesdayTimeTextView.setText(timeInterval);
                break;
            case THURSDAY_INT:
                mThursdayTimeTextView.setText(timeInterval);
                break;
            case FRIDAY_INT:
                mFridayTimeTextView.setText(timeInterval);
                break;
            case SATURDAY_INT:
                mSaturdayTimeTextView.setText(timeInterval);
                break;
            default:
                throw new IllegalArgumentException("Invalid Day of the Week integer: " +
                        mCurrentSelectedDay);
        }
    }

    // Helper method for formatting the time interval of a class
    private String formatTimeInterval() {
        int startTimeHour = mStartTimeHour;
        String startTimeMin = String.valueOf(mStartTimeMinute);
        int endTimeHour = mEndTimeHour;
        String endTimeMin = String.valueOf(mEndTimeMinute);
        String timeOfDayStart = "am";
        String timeOfDayEnd = "am";

        // Format starting hour
        if (mStartTimeHour == 0) {
            startTimeHour = 12;
        } else if (mStartTimeHour > 12) {
            startTimeHour = mStartTimeHour - 12;
            timeOfDayStart = "pm";
        } else if (mStartTimeHour == 12) {
            timeOfDayStart = "pm";
        }

        // Format ending hour
        if (mEndTimeHour == 0) {
            endTimeHour = 12;
        } else if (mEndTimeHour > 12) {
            endTimeHour = mEndTimeHour - 12;
            timeOfDayEnd = "pm";
        } else if (mEndTimeHour == 12) {
            timeOfDayEnd = "pm";
        }

        return startTimeHour + ":" + startTimeMin + timeOfDayStart + "-" +
                endTimeHour + ":" + endTimeMin + timeOfDayEnd;
    }

    // Helper method to set onCheckListeners on each CheckBox
    private void setOnCheckListeners() {
        mSundayCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
        mMondayCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
        mTuesdayCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
        mWednesdayCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
        mThursdayCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
        mFridayCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
        mSaturdayCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
    }

    // Helper method to determine which day of the week is having its time slot set
    private void getCurrentSelectedDay(View view) {
        switch(view.getId()) {
            case R.id.sunday_schedule_check_box:
                mCurrentSelectedDay = SUNDAY_INT;
                break;
            case R.id.monday_schedule_check_box:
                mCurrentSelectedDay = MONDAY_INT;
                break;
            case R.id.tuesday_schedule_check_box:
                mCurrentSelectedDay = TUESDAY_INT;
                break;
            case R.id.wednesday_schedule_check_box:
                mCurrentSelectedDay = WEDNESDAY_INT;
                break;
            case R.id.thursday_schedule_check_box:
                mCurrentSelectedDay = THURSDAY_INT;
                break;
            case R.id.friday_schedule_check_box:
                mCurrentSelectedDay = FRIDAY_INT;
                break;
            case R.id.saturday_schedule_check_box:
                mCurrentSelectedDay = SATURDAY_INT;
                break;
            default:
                throw new IllegalArgumentException("Invalid View selected: " + view.getId());
        }
    }

    // Helper method for removing a time from the schedule
    private void removeTimeFromSelectedDay(View view) {
        // Get the currently selected day
        getCurrentSelectedDay(view);

        // Loop through the class times list and remove any time that beings with the integer
        // assigned to a day of the week (ex. Sunday (1), Monday (2), etc.)
        for (int i = 0; i < mClassTimesList.size(); i++) {
            String classTime = mClassTimesList.get(i);
            if (classTime.startsWith(String.valueOf(mCurrentSelectedDay))) {
                mClassTimesList.remove(i);
            }
        }

        // Clear the associated text view
        clearTimeSlotTextView();
    }

    // Helper method to clear the textview for the timeslot of a day that has been unchecked
    private void clearTimeSlotTextView() {
        switch (mCurrentSelectedDay) {
            case SUNDAY_INT:
                mSundayTimeTextView.setText(null);
                break;
            case MONDAY_INT:
                mMondayTimeTextView.setText(null);
                break;
            case TUESDAY_INT:
                mTuesdayTimeTextView.setText(null);
                break;
            case WEDNESDAY_INT:
                mWednesdayTimeTextView.setText(null);
                break;
            case THURSDAY_INT:
                mThursdayTimeTextView.setText(null);
                break;
            case FRIDAY_INT:
                mFridayTimeTextView.setText(null);
                break;
            case SATURDAY_INT:
                mSaturdayTimeTextView.setText(null);
                break;
            default:
                throw new IllegalArgumentException("Invalid Day of the Week integer: " +
                        mCurrentSelectedDay);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SCHEDULE_LIST_KEY, (ArrayList<String>) mClassTimesList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mClassTimesList = savedInstanceState.getStringArrayList(SCHEDULE_LIST_KEY);
            fillInScheduleInfo();
        }
    }
}
