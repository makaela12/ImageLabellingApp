package com.example.imagelabellingapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;
public class ImageUtils {
        // This method converts a Uri to a Bitmap
        public static Bitmap getBitmapFromUri(Context context, Uri uri) {
            try {
                return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
                return null; // Handle errors as needed
            }
        }
}

