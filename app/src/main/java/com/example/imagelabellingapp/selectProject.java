package com.example.imagelabellingapp;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class selectProject extends AppCompatActivity {

    private ListView projectListView;
    SelectAdapter adapter;
    private DBHelper dbHelper;
    private EditText searchEditText;
    private ImageButton searchButton, refreshButton, helpButton;

    private BroadcastReceiver refreshProjectListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // refresh the project list
            refreshProjectList();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_project);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        refreshButton = findViewById(R.id.refreshButton);
        helpButton = findViewById(R.id.helpButton);

        // Set click listener for the search button
        searchButton.setOnClickListener(view -> searchProjects());
        // Initialize the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);

        // Enable the home button (back arrow)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set the click listener for the back arrow
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the back arrow click
                onBackPressed();
            }
        });
        changeBackArrowColor(toolbar, R.color.white);

        // set OnClickListener for the refresh button
        refreshButton.setOnClickListener(v -> {
            // Clear the search EditText
            searchEditText.setText("");
            // Refresh the project list
            refreshProjectList();
        });

        // set onClickListener for help button
        helpButton.setOnClickListener(v -> showHelpPopup("Enter a keyword or full project\nname you would like to search.\n\nClick on magnifying glass icon\nto display search results."));


        dbHelper = new DBHelper(this);
        projectListView = findViewById(R.id.projectListView);

        // Retrieve existing project names from the projects table
        ArrayList<String> projectList = getExistingProjects();

        // Retrieve existing project names from the projects table
        ArrayList<String> descriptList = getExistingProjDesc();

        // Instantiate the custom adapter with the retrieved lists
        adapter = new SelectAdapter(this, projectList, descriptList);

        // Set the custom adapter to your ListView
        projectListView.setAdapter(adapter);
        // Set item click listener for the ListView
        projectListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProjectName = adapter.getItem(position);
            int selectedProjectId = getProjectId(selectedProjectName);
            // Start MainActivity2 and pass the projectId
            Intent intent = new Intent(selectProject.this, MainActivity2.class);
            intent.putExtra("projectId", (long) selectedProjectId);
            startActivity(intent);
        });

        // long click on item in the listView to delete project
        projectListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedProjectName = adapter.getItem(position);
            showDeleteConfirmationDialog(selectedProjectName);
            return true;
        });


        // used to register the BroadcastReceiver to listen for the "refresh_project_list" broadcast
        registerReceiver(refreshProjectListReceiver, new IntentFilter("refresh_project_list"));

    }
    @Override
    protected void onDestroy() {
        // used to unregister the BroadcastReceiver when the activity is destroyed
        unregisterReceiver(refreshProjectListReceiver);
        super.onDestroy();
    }

    // Helper method to retrieve existing project names from the projects table
    private ArrayList<String> getExistingProjects() {
        ArrayList<String> projects = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to retrieve project names
        String query = "SELECT " + DBHelper.COLUMN_PROJECT_NAME + " FROM " + DBHelper.TABLE_PROJECTS;

        Cursor cursor = db.rawQuery(query, null);

        try {
            if (cursor.moveToFirst()) {
                do {
                    String projectName = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PROJECT_NAME));
                    projects.add(projectName);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return projects;
    }
    // Helper method to retrieve existing project descriptions from the projects table
    private ArrayList<String> getExistingProjDesc() {
        ArrayList<String> descriptions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to retrieve project names
        String query = "SELECT " + DBHelper.COLUMN_DESCRIPTION + " FROM " + DBHelper.TABLE_PROJECTS;

        Cursor cursor = db.rawQuery(query, null);

        try {
            if (cursor.moveToFirst()) {
                do {
                    String projectDesc = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_DESCRIPTION));
                    descriptions.add(projectDesc);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return descriptions;
    }

    // Helper method to get the project ID based on the project name
    private int getProjectId(String projectName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to retrieve project ID based on project name
        String query = "SELECT " + DBHelper.COLUMN_ID + " FROM " + DBHelper.TABLE_PROJECTS +
                " WHERE " + DBHelper.COLUMN_PROJECT_NAME + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{projectName});

        int projectId = -1;

        try {
            if (cursor.moveToFirst()) {
                projectId = cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_ID));
            }
        } finally {
            cursor.close();
        }

        return projectId;
    }

    private void showDeleteConfirmationDialog(String projectName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Project");
        builder.setMessage("Are you sure you want to delete the project '" + projectName + "'?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Call a method to delete the project from the database and update the UI
            deleteProject(projectName);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteProject(String projectName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // get project ID based on the project name
        int projectId = getProjectId(projectName);

        // Get a list of all image paths for the project
        List<String> imagePaths = dbHelper.getImagePathsForProject(projectId);

        // Delete all images associated with the project
        for (String imagePath : imagePaths) {
            deleteImageFile(imagePath);
        }
        // delete related labels
        dbHelper.deleteLabelsForProject(projectId);
        // delete related images
        dbHelper.deleteImagesForProject(projectId);
        // delete project from projects table
        db.delete(DBHelper.TABLE_PROJECTS, DBHelper.COLUMN_PROJECT_NAME + " = ?", new String[]{projectName});
        // update the UI by refreshing the project list
        refreshProjectList();
    }

    private void refreshProjectList() {
        // Retrieve existing project names from the projects table
        ArrayList<String> projectList = getExistingProjects();
        ArrayList<String> descriptionList = getExistingProjDesc();
        // Update the ArrayAdapter and notify the ListView
        adapter.clear();
        adapter.updateData(projectList, descriptionList);
        adapter.notifyDataSetChanged();
    }

    private void changeBackArrowColor(Toolbar toolbar, int colorRes) {
        // Get the up button drawable (system default back arrow)
        final Drawable upArrow = toolbar.getNavigationIcon();

        // Tint the drawable
        if (upArrow != null) {
            upArrow.setColorFilter(ContextCompat.getColor(this, colorRes), PorterDuff.Mode.SRC_ATOP);
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }
    }


    // Method to perform the search and update the project list
    private void searchProjects() {
        String searchTerm = searchEditText.getText().toString().trim();

        if (!searchTerm.isEmpty()) {
            // Retrieve filtered project names from the projects table
            ArrayList<String> filteredProjects = getFilteredProjects(searchTerm);

            // Update the ArrayAdapter and notify the ListView
            adapter.clear();
            adapter.addAll(filteredProjects);
            adapter.notifyDataSetChanged();
        } else {
            // If the search term is empty, refresh the project list
            refreshProjectList();
        }
    }

    // Method to retrieve filtered project names and descriptions from the projects table
    private ArrayList<String> getFilteredProjects(String searchTerm) {
        ArrayList<String> filteredProjects = new ArrayList<>();
        ArrayList<String> filteredDescriptions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to retrieve project names and descriptions that contain the search term
        String query = "SELECT " + DBHelper.COLUMN_PROJECT_NAME + ", " + DBHelper.COLUMN_DESCRIPTION + " FROM " + DBHelper.TABLE_PROJECTS +
                " WHERE " + DBHelper.COLUMN_PROJECT_NAME + " LIKE ?";

        Cursor cursor = db.rawQuery(query, new String[]{"%" + searchTerm + "%"});

        try {
            if (cursor.moveToFirst()) {
                do {
                    String projectName = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PROJECT_NAME));
                    String projectDesc = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_DESCRIPTION));
                    filteredProjects.add(projectName);
                    filteredDescriptions.add(projectDesc);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        // Update the adapter with filtered project names and descriptions
        adapter.updateData(filteredProjects, filteredDescriptions);

        return filteredProjects;
    }

    private void showHelpPopup(String message){
        // creates a custom dialog
        Dialog helpDialog = new Dialog(this);
        helpDialog.setContentView(R.layout.popup_layout); // Create a layout for your popup

        // sets the message in the popup
        TextView popupMessage = helpDialog.findViewById(R.id.popupMessage);
        popupMessage.setText(message);

        // sets click listener for the close button in the popup
        ImageView closeButton = helpDialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> helpDialog.dismiss());

        // show the popup
        helpDialog.show();
    }


    private void deleteImageFile(String imagePath) {
        // Create a File object representing the image file
        File imageFile = new File(imagePath);

        // Check if the file exists before attempting to delete it
        if (imageFile.exists()) {
            // Delete the file
            boolean deleted = imageFile.delete();

            // Check if the file deletion was successful
            if (deleted) {
                // Log a message or perform any additional actions upon successful deletion
                Log.d("DeleteImage", "Image file deleted successfully: " + imagePath);
            } else {
                // Log an error message or handle the case where deletion failed
                Log.e("DeleteImage", "Failed to delete image file: " + imagePath);
            }
        } else {
            // Log a message or handle the case where the file does not exist
            Log.d("DeleteImage", "Image file does not exist: " + imagePath);
        }
    }

}
