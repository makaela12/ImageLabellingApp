package com.example.imagelabellingapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class projectDetails extends AppCompatActivity {

    private DBHelper dbHelper;
    private long projectId;
    private ListView labelList;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> labelArr;
    private EditText projName, inputLabel, descript, editTextWidth, editTextHeight;
    private TextView textViewLabelCount, textViewImageCount, textViewImgSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_details);
        projName = findViewById(R.id.projName);
        descript = findViewById(R.id.descriptionEditText);
        dbHelper = new DBHelper(this);
        textViewImgSize = findViewById(R.id.textViewImgSize);
        //editTextWidth = findViewById(R.id.editTextWidth);
        //editTextHeight = findViewById(R.id.editTextHeight);
        textViewImageCount = findViewById(R.id.textViewImageCount);
        textViewLabelCount = findViewById(R.id.textViewLabelCount);
        labelArr = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        labelList = findViewById(R.id.labelList);
        labelList.setAdapter(adapter);

        // Retrieve the project ID from the intent
        Intent intent = getIntent();
        if (intent.hasExtra("projectId")) {
            projectId = intent.getLongExtra("projectId", -1);
            if (projectId != -1) {
                // Load project details for editing
                loadProjectDetails(projectId);
            } else {
                // Handle invalid project ID
                Toast.makeText(this, "Invalid Project ID", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity if the project ID is invalid
            }
        }

    }

    // helper method to retreive project details from db
    private void loadProjectDetails(long projectId) {
        // retrieve project details from the database using dbHelper
        String projectName = dbHelper.getProjectName(projectId);
        String desc = dbHelper.getImageDesc(projectId);
        int imageCount = dbHelper.getImageCountForProject(projectId);
        int labelCount = dbHelper.getLabelCountForProject(projectId);
        int[] imageSize = dbHelper.getImageSizeForProject(projectId);
        ArrayList<String> labels = (ArrayList<String>) dbHelper.getLabelsForProject(projectId);

        // set project name and labels to the views
        projName.setText(projectName);
        descript.setText(desc);
        textViewImageCount.setText(String.valueOf(imageCount));
        textViewLabelCount.setText(String.valueOf(labelCount));
        textViewImgSize.setText(String.valueOf(imageSize[0]) + "x" + String.valueOf(imageSize[1]));
       // editTextWidth.setText(String.valueOf(imageSize[0]));
       // editTextHeight.setText(String.valueOf(imageSize[1]));
        adapter.clear();
        labelArr.clear();

        Set<String> uniqueLabels = new HashSet<>(labels);
        for (String label : uniqueLabels) {
            adapter.add(label);
            labelArr.add(label);
        }
        Log.d("EditProjectActivity", "Loaded project details. New labelArr: " + labelArr.toString());
    }
}