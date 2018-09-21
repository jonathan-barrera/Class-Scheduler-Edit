package com.example.android.classscheduler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.classscheduler.model.SchoolClass;
import com.example.android.classscheduler.model.Student;
import com.example.android.classscheduler.utils.BitmapUtils;
import com.example.android.classscheduler.utils.DateUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.commons.text.WordUtils;

import java.text.DateFormatSymbols;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Activity for editing a Student's info (or adding a new Student)
 */

public class EditStudentInfo extends AppCompatActivity
        implements ClassPickerFragment.onItemClickListener {

    // App Constants
    public static final int SEX_MALE_INT = 0;
    public static final int SEX_FEMALE_INT = 1;
    public static final String FIREBASE_CHILD_KEY_USERS = "users";
    public static final String FIREBASE_CHILD_KEY_STUDENTS = "students";
    public static final String FIREBASE_CHILD_KEY_CLASSES = "classes";
    public static final String FIREBASE_CHILD_KEY_STUDENT_PHOTOS = "student_photos";

    // List containing all classes offered by the school
    private ArrayList<String> mFullClassList;

    // List containing classes chosen for this student
    private ArrayList<String> mChosenClassesList;

    // Keys
    public static final String FULL_CLASS_LIST_KEY = "full-class-list-key";
    public static final String CHOSEN_CLASS_LIST_KEY = "chosen-class-list-key";
    private static final String BIRTHDATE_INSTANCE_STATE_KEY = "birthday-instance-state-key";
    private static final String CLASS_LIST_INSTANCE_STATE_KEY = "class-list-instance-state-key";

    // Fragment related variables
    private android.support.v4.app.FragmentManager mFragmentManager;
    private ClassPickerFragment mClassPickerFragment;

    // Integer variable to keep track of the student's sex. Put male as the default.
    private int mStudentSex = SEX_MALE_INT;

    // Integer variable to keep track of student's birthdate
    private long mStudentBirthdate;

    // Variable for current student (if editing a student's information)
    private Student mCurrentStudent;
    private String mUserId;

    // Boolean variable to keep track of whether user is editing a student or adding a new student
    private boolean isEditStudent;

    // Boolean variable to keep track of whether there is a photo or not
    private boolean studentHasPhoto;

    // Photo capture/selection function constants and variables
    private static final int PICK_PHOTO_GALLERY = 1;
    private static final int CAPTURE_PHOTO = 2;
    private Bitmap mBitmap;
    private Uri mFirebaseStoragePhotoUri;
    private String mTempPhotoPath;
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.classscheduler.fileprovider";
    private StudentPicViewModel mViewModel;

    // Boolean to keep track of changes made to student's info
    private boolean mChangesMade = false;

    // Constant variables
    private final static String SEX_MALE = "Male";
    private final static String SEX_FEMALE = "Female";

    // Firebase Instances
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mStudentsDatabaseReference;
    private DatabaseReference mClassesDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStudentPhotosStorageReference;

    // Views
    @BindView(R.id.student_name_edit_text)
    EditText mStudentNameEditText;
    @BindView(R.id.student_sex_spinner)
    Spinner mStudentSexSpinner;
    @BindView(R.id.student_birthdate_text_view)
    TextView mStudentBirthdateTextView;
    @BindView(R.id.student_grade_edit_text)
    EditText mStudentGradeEditText;
    @BindView(R.id.gray_circle_background_view)
    ImageView mAddPhotoView;
    @BindView(R.id.add_photo_label_text_view)
    TextView mAddPhotoLabelTextView;
    @BindView(R.id.add_sign_image_view)
    ImageView mAddPhotoImageView;
    @BindView(R.id.saving_student_progress_bar)
    ProgressBar mSavingProgressBar;
    @BindView(R.id.student_birthdate_relative_layout)
    RelativeLayout mStudentBirthdateRelativeLayout;
    @BindView(R.id.student_classes_text_view)
    TextView mStudentClassesTextView;
    @BindView(R.id.edit_student_add_class_image_button)
    TextView mStudentAddClassView;
    @BindView(R.id.edit_student_remove_class_image_button)
    TextView mStudentRemoveClassView;

    // OnTouchListener to listen for user touches on Edit Text views. A touch indicates that
    // a change has probably been made to the info.
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            mChangesMade = true;
            return false;
        }
    };

    // Listener for text changes (used on the edit text views)
    private TextWatcher mTextListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mChangesMade = true;
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_student_info);

        // Initialize View Model
        mViewModel = ViewModelProviders.of(this).get(StudentPicViewModel.class);

        // Bind the views
        ButterKnife.bind(this);

        // Get Userid
        SharedPreferences sharedPreferences = getSharedPreferences(MainMenu.SHARED_PREFS, MODE_PRIVATE);
        mUserId = sharedPreferences.getString(MainMenu.USER_ID_SHARED_PREF_KEY, null);

        // Initialize member variable lists
        mFullClassList = new ArrayList<>();
        mChosenClassesList = new ArrayList<>();

        // Initialize the fragment manager
        mFragmentManager = getSupportFragmentManager();
        mClassPickerFragment = new ClassPickerFragment();

        // Initialize Firebase references
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mStudentsDatabaseReference = mFirebaseDatabase.getReference()
                .child(FIREBASE_CHILD_KEY_USERS)
                .child(mUserId)
                .child(FIREBASE_CHILD_KEY_STUDENTS);
        mClassesDatabaseReference = mFirebaseDatabase.getReference()
                .child(FIREBASE_CHILD_KEY_USERS)
                .child(mUserId)
                .child(FIREBASE_CHILD_KEY_CLASSES);
        mFirebaseStorage = FirebaseStorage.getInstance();
        mStudentPhotosStorageReference = mFirebaseStorage.getReference()
                .child(FIREBASE_CHILD_KEY_USERS)
                .child(mUserId)
                .child(FIREBASE_CHILD_KEY_STUDENT_PHOTOS);

        // Get intent to see whether we are updating an old student or adding a new student
        mCurrentStudent = getIntent().getParcelableExtra(StudentProfile.STUDENT_EXTRA_KEY);

        if (mCurrentStudent != null) {
            setTitle(getString(R.string.edit_profile));
            isEditStudent = true;
            fillInCurrentStudentInfo();
        } else {
            setTitle(getString(R.string.add_student));
        }

        // If the user has taken a photo, maintain the picture across configuration changes
        if (mViewModel.studentPicBitmap != null) {
            studentHasPhoto = true;
            mAddPhotoView.setImageBitmap(mViewModel.studentPicBitmap);
        }

        // Set up the spinner
        setUpSpinner();

        // Set touch listeners
        setTouchListeners();

        // Set Add Photo OnClickListener to allow user to choose a photo or take one
        setUpAddPhoto();

        // Check for student photo
        checkIfThereIsStudentPhoto();
    }

    @Override
    protected void onResume() {
        mFullClassList.clear();
        super.onResume();

        // Initialize Firebase instances
        mClassesDatabaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                SchoolClass schoolClass = dataSnapshot.getValue(SchoolClass.class);
                mFullClassList.add(schoolClass.getTitle());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void openDatePicker(View v) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this,
                new android.app.DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int month, int day) {
                        // Format the date like this: Jan 01, 2018
                        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
                        String dateString = dateFormatSymbols.getMonths()[month] + " " + day + ", " + year;
                        mStudentBirthdateTextView.setText(dateString);

                        // Save the date as a long
                        try {
                            String dateStringForStorage = String.valueOf(day) + "/"
                                    + String.valueOf(month + 1) + "/"
                                    + String.valueOf(year);
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                            Date date = sdf.parse(dateStringForStorage);

                            mStudentBirthdate = date.getTime();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    // Method for filling in a current student's information into the input fields
    private void fillInCurrentStudentInfo() {
        // Extract info from Student object
        String name = WordUtils.capitalizeFully(mCurrentStudent.getName());
        int sex = mCurrentStudent.getSex();
        mStudentBirthdate = mCurrentStudent.getBirthdate();
        int grade = mCurrentStudent.getGrade();
        mChosenClassesList = (ArrayList<String>) mCurrentStudent.getClasses();
        String classes = TextUtils.join(", ", mChosenClassesList);
        String photoUrl = mCurrentStudent.getPhotoUrl();

        // Populate views with the current student's information
        mStudentNameEditText.setText(name);
        mStudentBirthdateTextView.setText(DateUtils.convertDateLongToString(mStudentBirthdate));
        mStudentGradeEditText.setText(String.valueOf(grade));
        mStudentClassesTextView.setText(classes);
        mStudentClassesTextView.setVisibility(View.VISIBLE);

        // Set student photo (if it exists)
        if (!TextUtils.isEmpty(photoUrl)) {
            Glide.with(mAddPhotoView.getContext())
                    .load(photoUrl)
                    .into(mAddPhotoView);
            studentHasPhoto = true;
        }

        // Set student sex spinner
        switch (sex) {
            case SEX_MALE_INT:
                mStudentSexSpinner.setSelection(SEX_MALE_INT);
                break;
            case SEX_FEMALE_INT:
                mStudentSexSpinner.setSelection(SEX_FEMALE_INT);
                break;
            default:
                throw new IllegalArgumentException("Invalid sex");
        }
    }

    // Set up OnClickListener for the AddPhoto imageview
    @SuppressLint("RestrictedApi")
    private void setUpAddPhoto() {
        mAddPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuBuilder menuBuilder = new MenuBuilder(EditStudentInfo.this);
                new MenuInflater(EditStudentInfo.this).inflate(R.menu.menu_photo, menuBuilder);
                MenuPopupHelper menuPopupHelper = new MenuPopupHelper(EditStudentInfo.this,
                        menuBuilder, mAddPhotoView);
                menuPopupHelper.setForceShowIcon(true);
                menuBuilder.setCallback(new MenuBuilder.Callback() {
                    // Create a menu for the user to choose between taking a new photo and
                    // choosing a photo from their photo gallery
                    @Override
                    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.photo_gallery_action:
                                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                                galleryIntent.setType("image/*");
                                startActivityForResult(galleryIntent, PICK_PHOTO_GALLERY);
                                break;
                            case R.id.camera_action:
                                launchCamera();
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid menu item selected");
                        }
                        return true;
                    }
                    @Override
                    public void onMenuModeChange(MenuBuilder menu) {

                    }
                });
                menuPopupHelper.show();
            }
        });
    }

    // Helper method to check if the student profile has a photo or not.
    // If so, hide the "+" and "add photo" views
    private void checkIfThereIsStudentPhoto() {
        if (studentHasPhoto) {
            mAddPhotoImageView.setVisibility(View.GONE);
            mAddPhotoLabelTextView.setVisibility(View.GONE);
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Check for camera on phone
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Temporary file to hold image
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, CAPTURE_PHOTO);
            }
        }
    }

    // Override onActivityResult to work with the photo that the user has captured/selected
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Pick photo
        if (requestCode == PICK_PHOTO_GALLERY && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(this, R.string.error_selecting_photo, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                mViewModel.studentPicBitmap = BitmapFactory.decodeStream(inputStream);
                mAddPhotoView.setImageBitmap(mViewModel.studentPicBitmap);
                studentHasPhoto = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            // Take new photo
        } else if (requestCode == CAPTURE_PHOTO && resultCode == Activity.RESULT_OK) {
            mViewModel.studentPicBitmap = BitmapFactory.decodeFile(mTempPhotoPath);
            mAddPhotoView.setImageBitmap(mViewModel.studentPicBitmap);
            studentHasPhoto = true;
        }
        checkIfThereIsStudentPhoto();
    }

    // Set listeners on the edit text views and spinner
    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListeners() {
        mStudentNameEditText.addTextChangedListener(mTextListener);
        mStudentSexSpinner.setOnTouchListener(mTouchListener);
        mStudentBirthdateRelativeLayout.setOnTouchListener(mTouchListener);
        mStudentGradeEditText.addTextChangedListener(mTextListener);
        mAddPhotoView.setOnTouchListener(mTouchListener);
        mStudentAddClassView.setOnTouchListener(mTouchListener);
        mStudentRemoveClassView.setOnTouchListener(mTouchListener);
    }

    // Helper method for setting up the spinner for choosing a student's sex
    private void setUpSpinner() {
        // Create adapter for student sex spinner
        ArrayAdapter sexSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, R.layout.sex_spinner_item);

        // Specify dropdown layout style
        sexSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply adapter to spinner
        mStudentSexSpinner.setAdapter(sexSpinnerAdapter);

        // Set an onitemseledctedlistener onto the spinner
        mStudentSexSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                switch (selection) {
                    case SEX_MALE:
                        mStudentSex = SEX_MALE_INT;
                        break;
                    case SEX_FEMALE:
                        mStudentSex = SEX_FEMALE_INT;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid Student Sex");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Male is already set as default
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the edit student menu
        getMenuInflater().inflate(R.menu.edit_student_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                // Save student's information
                saveStudent();
                return true;
            case android.R.id.home:
                // Check if changes have been made
                if (!mChangesMade) {
                    if (isEditStudent) {
                        onBackPressed();
                    } else {
                        NavUtils.navigateUpFromSameTask(this);
                    }
                    return true;
                } else {
                    showUnsavedChangesDialog();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    // Helper method for saving student information to database
    private void saveStudent() {
        // Check that every required field has been filled in with valid parameters
        if (!checkUserInputValidity()) {
            return;
        }

        // Insert the new student info into the database
        if (isEditStudent) {
            updateStudentOnFirebaseDatabase();
        } else {
            // Make a unique id for the student
            String studentId = UUID.randomUUID().toString();
            saveNewStudentToFirebaseDatabase(studentId);
        }
    }

    // Save the student photo to the Firebase Storage when the Student info is saved
    private void saveStudentPhotoToFirebaseStorage(final String studentId) {
        // Show progress bar
        mSavingProgressBar.setVisibility(View.VISIBLE);

        // Save photo to firebase storage
        final StorageReference photoRef = mStudentPhotosStorageReference.child(studentId);
        photoRef.putBytes(BitmapUtils.bitmapToByteArray(mViewModel.studentPicBitmap)).addOnSuccessListener(
                new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        // On success, get the URL link, which will be saved in the student database
                        mFirebaseStoragePhotoUri = uri;

                        String studentPhoto = mFirebaseStoragePhotoUri.toString();
                        String studentName = mStudentNameEditText.getText().toString().toLowerCase().trim();
                        int studentSex = mStudentSex;
                        long studentBirthdate = mStudentBirthdate;
                        int studentGrade = Integer.parseInt(mStudentGradeEditText.getText().toString());

                        Student newStudent = new Student(studentName, studentSex, studentBirthdate,
                                studentGrade, mChosenClassesList, studentPhoto, studentId);

                        mStudentsDatabaseReference.child(studentId).setValue(newStudent);

                        finish();
                    }
                });
            }
        });
    }

    // Method to save a new student to the Firebase Database
    private void saveNewStudentToFirebaseDatabase(String studentId) {
        // Extract Student information from the edit text views
        String studentName = mStudentNameEditText.getText().toString().toLowerCase().trim();
        int studentSex = mStudentSex;
        long studentBirthdate = mStudentBirthdate;
        int studentGrade = Integer.parseInt(mStudentGradeEditText.getText().toString());

        if (mViewModel.studentPicBitmap != null) {
            // Save photo to Firebase Storage using AsyncTask
            saveStudentPhotoToFirebaseStorage(studentId);
        } else {
            Student newStudent = new Student(studentName, studentSex, studentBirthdate, studentGrade,
                    mChosenClassesList, null, studentId);

            mStudentsDatabaseReference.child(studentId).setValue(newStudent);
        }

        // Close activity
        finish();
    }

    // Update existing student's information on the Firebase Database
    private void updateStudentOnFirebaseDatabase() {
        String studentName = mStudentNameEditText.getText().toString().toLowerCase().trim();
        int studentSex = mStudentSex;
        long studentBirthdate = mStudentBirthdate;
        int studentGrade = Integer.parseInt(mStudentGradeEditText.getText().toString());
        String studentId = mCurrentStudent.getStudentId();

        if (mViewModel.studentPicBitmap != null) {
            saveStudentPhotoToFirebaseStorage(studentId);
        } else {
            // Check if the student already has a photo saved
            String photoUrl;
            if (studentHasPhoto) {
                // if has photo, resave the photo to the student database
                photoUrl = mCurrentStudent.getPhotoUrl();
            } else {
                photoUrl = null;
            }

            mStudentsDatabaseReference.child(studentId)
                    .setValue(new Student(studentName, studentSex, studentBirthdate, studentGrade,
                            mChosenClassesList, photoUrl, studentId));

            // Close activity
            finish();
        }
    }

    // Helper method to check that the user's input for each field is valid
    private boolean checkUserInputValidity() {
        // Extract Student information from the edit text views
        String studentName = mStudentNameEditText.getText().toString().trim();
        String studentGradeString = mStudentGradeEditText.getText().toString();

        // Check for valid student name
        if (TextUtils.isEmpty(studentName)) {
            Toast.makeText(this, R.string.please_enter_valid_name, Toast.LENGTH_SHORT).show();
            mStudentNameEditText.requestFocus();
            return false;
        }

        // Check for valid student birthdate
        if (mStudentBirthdate == 0) {
            Toast.makeText(this, R.string.please_enter_valid_birthdate,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (DateUtils.isChosenDateAfterToday(mStudentBirthdate)) {
            Toast.makeText(this, R.string.please_enter_valid_birthdate,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check for valid student grade
        if (TextUtils.isEmpty(studentGradeString)) {
            Toast.makeText(this, R.string.please_enter_valid_grade, Toast.LENGTH_SHORT).show();
            mStudentGradeEditText.requestFocus();
            return false;
        } else {
            int studentGrade = Integer.parseInt(studentGradeString);
            if (studentGrade <= 0) {
                Toast.makeText(this, R.string.please_enter_valid_grade, Toast.LENGTH_SHORT).show();
                mStudentGradeEditText.requestFocus();
                return false;
            }
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (!mChangesMade) {
            super.onBackPressed();
            return;
        }

        // If there are any unsaved changes, show dialog to warn the user
        showUnsavedChangesDialog();
    }

    // Inform the user that there are unsaved changes to the student's information
    private void showUnsavedChangesDialog() {
        // Create an AlertDialog.Builder and set the message and click listeners for the positive
        // and negative buttons.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.you_have_unsaved_messages_dialog);

        // Leave the page if user clicks "Leave"
        builder.setPositiveButton(R.string.leave, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        // Stay on the page if user clicks "Stay"
        builder.setNegativeButton(R.string.stay, new DialogInterface.OnClickListener() {
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

    // Method to open Class Picker
    public void openClassPicker(View v) {
        // Attach data to fragment
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(FULL_CLASS_LIST_KEY, mFullClassList);
        mClassPickerFragment.setArguments(bundle);
        // Show the Class Picker DialogFragment.
        mClassPickerFragment.show(mFragmentManager, getString(R.string.add_classes));
    }

    // Method to open the Create Classes Activity
    public void openCreateClassesActivity(View v) {
        Intent intent = new Intent(this, CreateClassesActivity.class);
        startActivity(intent);
    }

    // Put the selected class title into the list of classes
    @Override
    public void onTitleSelected(String classTitle, String key) {
        if (key.equals(ClassPickerFragment.PICKER_KEY)) {
            addTitleToChosenClassList(classTitle);
        } else if (key.equals(ClassPickerFragment.REMOVER_KEY)) {
            removeTitleFromChosenClassList(classTitle);
        }
    }

    // Helper method to remove class title from Chosen Class List
    private void removeTitleFromChosenClassList(String classTitle) {
        for (Iterator<String> iterator = mChosenClassesList.iterator(); iterator.hasNext(); ) {
            String a = iterator.next();
            if (a.equals(classTitle)) {
                iterator.remove();
            }
        }
        mStudentClassesTextView.setText(TextUtils.join(", ", mChosenClassesList));
    }

    // Helper method to add class title to Chosen Class List
    private void addTitleToChosenClassList(String classTitle) {
        if (!mChosenClassesList.contains(classTitle)) {
            mChosenClassesList.add(classTitle);
            String classList = TextUtils.join(", ", mChosenClassesList);
            mStudentClassesTextView.setVisibility(View.VISIBLE);
            mStudentClassesTextView.setText(classList);
        }
    }

    // Method for opening Class Remover
    public void openClassRemover(View v) {
        // Attach data to fragment
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(CHOSEN_CLASS_LIST_KEY, mChosenClassesList);
        mClassPickerFragment.setArguments(bundle);
        // Show the Class Picker DialogFragment
        mClassPickerFragment.show(mFragmentManager, getString(R.string.remove_classes));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mStudentBirthdate != 0) {
            outState.putLong(BIRTHDATE_INSTANCE_STATE_KEY, mStudentBirthdate);
        }
        if (mChosenClassesList.size() > 0) {
            outState.putStringArrayList(CLASS_LIST_INSTANCE_STATE_KEY, mChosenClassesList);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BIRTHDATE_INSTANCE_STATE_KEY)) {
                mStudentBirthdate = savedInstanceState.getLong(BIRTHDATE_INSTANCE_STATE_KEY);
                mStudentBirthdateTextView.setText(DateUtils.convertDateLongToString(mStudentBirthdate));
            }
            if (savedInstanceState.containsKey(CLASS_LIST_INSTANCE_STATE_KEY)) {
                mChosenClassesList = savedInstanceState.getStringArrayList(CLASS_LIST_INSTANCE_STATE_KEY);
                String classString = TextUtils.join(", ", mChosenClassesList);
                mStudentClassesTextView.setText(classString);
                mStudentClassesTextView.setVisibility(View.VISIBLE);
            }
        }
    }
}
