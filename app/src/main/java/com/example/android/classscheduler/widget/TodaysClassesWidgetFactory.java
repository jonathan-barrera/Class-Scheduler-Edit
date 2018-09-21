package com.example.android.classscheduler.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.android.classscheduler.EditStudentInfo;
import com.example.android.classscheduler.MainMenu;
import com.example.android.classscheduler.R;
import com.example.android.classscheduler.model.SchoolClass;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jonathanbarrera on 6/19/18.
 */

public class TodaysClassesWidgetFactory implements RemoteViewsService.RemoteViewsFactory, ValueEventListener {

    private Context mContext;
    private String mUserId;
    private List<String> mClassList;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;


    public TodaysClassesWidgetFactory(Context context, Intent intent) {
        // Get context
        mContext = context;

        // Initialize list
        mClassList = new ArrayList<>();

        // Initialize Firebase
        mFirebaseDatabase = FirebaseDatabase.getInstance();
    }

    @Override
    public void onCreate() {}

    @Override
    public void onDataSetChanged() {
        getItems();
    }

    private void getItems() {
        // Get UserId
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(MainMenu.SHARED_PREFS, Context.MODE_PRIVATE);
        mUserId = sharedPreferences.getString(MainMenu.USER_ID_SHARED_PREF_KEY, null);

        mDatabaseReference = mFirebaseDatabase.getReference()
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_USERS)
                .child(mUserId)
                .child(EditStudentInfo.FIREBASE_CHILD_KEY_CLASSES);

        mDatabaseReference.addValueEventListener(this);

        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Helper method for checking whether a classes schedule includes today
    private boolean checkIfTodaysDateIncluded(List<String> scheduleList) {
        for (int i = 0; i < scheduleList.size(); i++) {
            String schedule = scheduleList.get(i);
            String[] scheduleParts = schedule.split("/");
            int scheduleDayOfWeek = Integer.parseInt(scheduleParts[0]);
            int todayDayOfTheWeek = getDayOfTheWeekAsInteger();
            if (scheduleDayOfWeek == todayDayOfTheWeek) {
                return true;
            }
        }
        return false;
    }

    // Helper method for getting the day of the week as an integer
    private int getDayOfTheWeekAsInteger() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE");
        Date date = new Date();
        String dayOfWeek = simpleDateFormat.format(date);

        switch (dayOfWeek) {
            case "Sunday":
                return 1;
            case "Monday":
                return 2;
            case "Tuesday":
                return 3;
            case "Wednesday":
                return 4;
            case "Thursday":
                return 5;
            case "Friday":
                return 6;
            case "Saturday":
                return 7;
            default:
                throw new IllegalArgumentException("Illegal Day of the Week: " + dayOfWeek);
        }
    }

    @Override
    public void onDestroy() {
        mClassList.clear();
    }

    @Override
    public int getCount() {
        return mClassList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        // Inflate list item
        RemoteViews listItem = new RemoteViews(mContext.getPackageName(),
                R.layout.widget_list_item);

        // Populate list item
        listItem.setTextViewText(R.id.widget_list_item_text_view, mClassList.get(position));

        return listItem;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        Iterable<DataSnapshot> children = dataSnapshot.getChildren();

        for (DataSnapshot child : children) {
            SchoolClass schoolClass = child.getValue(SchoolClass.class);
            List<String> schedule = schoolClass.getSchedule();
            if (checkIfTodaysDateIncluded(schedule)) {
                mClassList.add(schoolClass.getTitle());

            }
        }

        synchronized (this) {
            this.notify();
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
    }
}
