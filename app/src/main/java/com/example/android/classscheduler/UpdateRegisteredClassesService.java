package com.example.android.classscheduler;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.widget.Toast;

import com.example.android.classscheduler.model.SchoolClass;
import com.example.android.classscheduler.model.Student;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import static com.example.android.classscheduler.CreateClassesActivity.CLASS_TITLE_INTENT_KEY;
import static com.example.android.classscheduler.EditStudentInfo.FIREBASE_CHILD_KEY_CLASSES;

/**
 * Created by jonathanbarrera on 6/25/18.
 * Service for removing any deleted classes from a student's list of classes
 */

public class UpdateRegisteredClassesService extends JobIntentService {

    // Unique job ID for this service.
    static final int JOB_ID = 1000;

    // Convenience method for enqueuing work in to this service.
    static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, UpdateRegisteredClassesService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // Get Userid
        SharedPreferences sharedPreferences = getSharedPreferences(MainMenu.SHARED_PREFS, MODE_PRIVATE);
        String userId = sharedPreferences.getString(MainMenu.USER_ID_SHARED_PREF_KEY, null);

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference databaseReference = firebaseDatabase.getReference()
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_USERS)
                .child(userId)
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_STUDENTS);

        final String classTitle = intent.getStringExtra(CLASS_TITLE_INTENT_KEY);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();

                for (DataSnapshot child : children) {
                    Student student = child.getValue(Student.class);
                    List<String> studentClasses = student.getClasses();
                    if (studentClasses != null && studentClasses.contains(classTitle)) {
                        studentClasses.remove(classTitle);
                        databaseReference.child(student.getStudentId()).child(FIREBASE_CHILD_KEY_CLASSES)
                                .setValue(studentClasses);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
