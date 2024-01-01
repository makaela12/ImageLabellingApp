package com.example.imagelabellingapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class ImageAdapter extends ArrayAdapter<String> {
    private Context context;
    private int resource;

    public ImageAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(resource, parent, false);
        }

        // Get the ImageView and TextView from the layout
        ImageView imageView = view.findViewById(R.id.imageView);
        TextView textView = view.findViewById(android.R.id.text1);

        // Set your data to the views
        String imagePath = getItem(position);

        // Load image into ImageView using Glide
        Glide.with(context)
                .load(new File(imagePath)) // Assuming imagePath is the file path
                .centerCrop() // You can customize this based on your needs
                .into(imageView);
        // Load image into ImageView using Glide or other methods
        // For now, assuming imagePath is the file path
        // Glide.with(context).load(new File(imagePath)).into(imageView);
        // Set text to TextView
        textView.setText("Image " + (position + 1)); // Customize this based on your needs


        return view;
    }
}

