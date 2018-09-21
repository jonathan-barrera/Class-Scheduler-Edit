package com.example.android.classscheduler.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonathanbarrera on 6/13/18.
 * Object to hold a SchoolClass's (as in School SchoolClass) information
 */

public class SchoolClass implements Parcelable {
    //TODO: fix onrestoreinstancestate for fragments

    // Variables to hold info
    private String mTitle;
    private String mSubject;
    private String mTeacher;
    private List<String> mSchedule;

    public SchoolClass() {}

    // Constructor
    public SchoolClass(String title, String subject, String teacher, List<String> schedule) {
        mTitle = title;
        mSubject = subject;
        mTeacher = teacher;
        mSchedule = schedule;
    }

    // Methods for retrieving information
    public String getTitle() { return mTitle; }
    public void setTitle(String title) { mTitle = title; }
    public String getSubject() { return mSubject; }
    public void setSubject(String subject) { mSubject = subject; }
    public String getTeacher() { return mTeacher; }
    public void setTeacher(String teacher) { mTeacher = teacher; }
    public List<String> getSchedule() { return mSchedule; }
    public void setSchedule(List<String> schedule) { mSchedule = schedule; }

    public static final Creator<SchoolClass> CREATOR = new Creator<SchoolClass>() {
        @Override
        public SchoolClass createFromParcel(Parcel in) {
            return new SchoolClass(in);
        }

        @Override
        public SchoolClass[] newArray(int size) {
            return new SchoolClass[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeString(mSubject);
        dest.writeString(mTeacher);
        dest.writeStringList(mSchedule);
    }

    protected SchoolClass(Parcel in) {
        mTitle = in.readString();
        mSubject = in.readString();
        mTeacher = in.readString();
        mSchedule = new ArrayList<>();
        in.readStringList(mSchedule);
    }
}
