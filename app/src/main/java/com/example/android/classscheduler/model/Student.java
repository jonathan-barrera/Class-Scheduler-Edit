package com.example.android.classscheduler.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonathanbarrera on 6/7/18.
 * Object to hold a Student's information
 */

public class Student implements Parcelable {

    // Variables to hold Student info
    private String mName;
    private int mSex;
    private long mBirthdate;
    private int mGrade;
    private List<String> mClasses;
    private String mPhotoUrl;
    private String mStudentId;

    public Student() {
    }

    // Constructor
    public Student(String name, int sex, long birthdate, int grade, List<String> classes, String photoUrl,
                   String studentId) {
        this.mName = name;
        this.mSex = sex;
        this.mBirthdate = birthdate;
        this.mGrade = grade;
        this.mClasses = classes;
        this.mPhotoUrl = photoUrl;
        this.mStudentId = studentId;
    }

    // Methods for retrieving info/setting
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getSex() { return mSex; }

    public void setSex(int sex) { this.mSex = sex; }

    public long getBirthdate() { return mBirthdate; }

    public void setBirthdate(long birthdate) { this.mBirthdate = birthdate; }

    public int getGrade() { return mGrade; }

    public void setGrade(int grade) {this.mGrade = grade; }

    public List<String> getClasses() { return mClasses; }

    public void setClasses(List<String> classes) { this.mClasses = classes; }

    public String getPhotoUrl() {
        return mPhotoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.mPhotoUrl = photoUrl;
    }

    public String getStudentId() { return mStudentId; }

    public void setStudentId(String studentId) { this.mStudentId = studentId; }

    public static final Creator<Student> CREATOR = new Creator<Student>() {
        @Override
        public Student createFromParcel(Parcel in) {
            return new Student(in);
        }

        @Override
        public Student[] newArray(int size) {
            return new Student[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeInt(mSex);
        dest.writeLong(mBirthdate);
        dest.writeInt(mGrade);
        dest.writeStringList(mClasses);
        dest.writeString(mPhotoUrl);
        dest.writeString(mStudentId);
    }

    protected Student(Parcel in) {
        mName = in.readString();
        mSex = in.readInt();
        mBirthdate = in.readLong();
        mGrade = in.readInt();
        mClasses = new ArrayList<>();
        in.readStringList(mClasses);
        mPhotoUrl = in.readString();
        mStudentId = in.readString();
    }
}
