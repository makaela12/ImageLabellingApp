package com.example.imagelabellingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageAdapter extends ArrayAdapter<String> {
    private Context context;
    private int resource;
    private String selectedLabel;
    private DBHelper dbHelper;

    private Map<Long, String> selectedLabelsMap = new HashMap<>();

    public ImageAdapter(Context context, int resource, List<String> objects, DBHelper dbHelper) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.dbHelper = dbHelper;

    }

    public void setSelectedLabel(long imageId, String selectedLabel) {
        selectedLabelsMap.put(imageId, selectedLabel);
        Log.d("ImageAdapter", "Set selectedLabel for imageId " + imageId + ": " + selectedLabel);
        // Save selected labels to SharedPreferences when the selection changes
        saveSelectedLabels(context);
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

        // Get the label for the corresponding imageId from the database
        //String label = dbHelper.getCurrentLabelForImage(getImageIdFromPath(imagePath));
        // Get the count of bounding boxes for the corresponding imageId from the database
        long imageId = getImageIdFromPath(imagePath);
        int bboxCount = dbHelper.getBBoxCountForImage(imageId);

        // Load image into ImageView using Glide
        Glide.with(context)
                .load(new File(imagePath))
                .centerCrop()
                .into(imageView);

        Log.d("imageadapter AYOOOOOO", "Set selectedLabel for imageId " + imagePath + ": " + selectedLabel);

        // Set the label in the TextView

        String text = "No. of boxes: " + bboxCount;

       // textView.setText(String.valueOf(bboxCount));
        textView.setText(text);


        return view;
    }

    private long getImageIdFromPath(String imagePath) {
        // Implement a method to retrieve imageId from the database based on the imagePath
        // You might need to modify this based on how you store image paths and ids in the database
        long imageId = dbHelper.getImageIdFromPath(imagePath);
        Log.d("ImageAdapter", "ImageId for " + imagePath + ": " + imageId);
        return dbHelper.getImageIdFromPath(imagePath);
    }

    // Save selected labels to SharedPreferences
    public void saveSelectedLabels(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("SelectedLabels", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        for (Map.Entry<Long, String> entry : selectedLabelsMap.entrySet()) {
            editor.putString(String.valueOf(entry.getKey()), entry.getValue());
        }

        editor.apply();
    }



}

