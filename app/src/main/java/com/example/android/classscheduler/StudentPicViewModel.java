package com.example.android.classscheduler;

import android.arch.lifecycle.ViewModel;
import android.graphics.Bitmap;

/**
 * Created by jonathanbarrera on 6/25/18.
 * A view model to hold a bitmap object (in order to save student photos between configuration changes
 */

public class StudentPicViewModel extends ViewModel {

    // Tracks the Bitmap for a student's pic
    public Bitmap studentPicBitmap;
}
