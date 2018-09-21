package com.example.android.classscheduler.utils;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by jonathanbarrera on 6/3/18.
 * This class contains two helper methods for converting between byte[] and bitmap
 * and saving the image file
 */

public class BitmapUtils {

    // Helper method to convert Bitmap to Byte Array
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Choose quality of upload depending on image file size
        int photoSize = bitmap.getByteCount();
        int quality;
        if (photoSize >= 8000000) {
            quality = 25;
        } else if (photoSize >= 6000000 && photoSize < 8000000) {
            quality = 45;
        } else if (photoSize >= 4000000 && photoSize < 6000000) {
            quality = 60;
        } else if (photoSize >= 2000000 && photoSize < 4000000) {
            quality = 80;
        } else {
            quality = 100;
        }

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    // Helper method for creating a temporary file to hold the image
    public static File createTempImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalCacheDir();

        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

}
