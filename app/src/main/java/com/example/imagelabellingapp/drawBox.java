package com.example.imagelabellingapp;


import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class drawBox extends AppCompatActivity {
    ImageView imageView;
    private Bitmap bitmap;
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_box);
        imageView = findViewById(R.id.imageView);

        // Retrieve the bitmap or image URI from the intent
        bitmap = getIntent().getParcelableExtra("imageBitmap");
        imageUri = getIntent().getParcelableExtra("imageUri");

        if (bitmap != null) {
            // Handle the case when a Bitmap is passed
            imageView.setImageBitmap(bitmap);
        } else if (imageUri != null) {
            // Handle the case when an image Uri is passed
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
