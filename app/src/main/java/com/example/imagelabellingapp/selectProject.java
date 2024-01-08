package com.example.imagelabellingapp;// SelectProjectActivity.java

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class selectProject extends AppCompatActivity {

    private ListView projectListView;
    private ArrayAdapter<String> projectAdapter;
    private DBHelper dbHelper;
    private EditText searchEditText;
    private ImageButton searchButton;

    private BroadcastReceiver refreshProjectListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Refresh the project list
            refreshProjectList();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_project);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);

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


        dbHelper = new DBHelper(this);
        projectListView = findViewById(R.id.projectListView);

        // Retrieve existing project names from the projects table
        ArrayList<String> projectList = getExistingProjects();

        // ArrayAdapter to populate the ListView
        projectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, projectList);
        projectListView.setAdapter(projectAdapter);

        // Set item click listener for the ListView
        projectListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProjectName = projectAdapter.getItem(position);
            int selectedProjectId = getProjectId(selectedProjectName);
            // Start MainActivity2 and pass the projectId
            Intent intent = new Intent(selectProject.this, MainActivity2.class);
            intent.putExtra("projectId", (long) selectedProjectId);
            startActivity(intent);
        });

        projectListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedProjectName = projectAdapter.getItem(position);
            showDeleteConfirmationDialog(selectedProjectName);
            return true; // to consume the long click event
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
                    // error shows warning, not really sure why but it still runs fine.. might need to fix later
                    String projectName = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PROJECT_NAME));
                    projects.add(projectName);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return projects;
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
        // Update the ArrayAdapter and notify the ListView
        projectAdapter.clear();
        projectAdapter.addAll(projectList);
        projectAdapter.notifyDataSetChanged();
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
            projectAdapter.clear();
            projectAdapter.addAll(filteredProjects);
            projectAdapter.notifyDataSetChanged();
        } else {
            // If the search term is empty, refresh the project list
            refreshProjectList();
        }
    }

    // Method to retrieve filtered project names from the projects table
    private ArrayList<String> getFilteredProjects(String searchTerm) {
        ArrayList<String> filteredProjects = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to retrieve project names that contain the search term
        String query = "SELECT " + DBHelper.COLUMN_PROJECT_NAME + " FROM " + DBHelper.TABLE_PROJECTS +
                " WHERE " + DBHelper.COLUMN_PROJECT_NAME + " LIKE ?";

        Cursor cursor = db.rawQuery(query, new String[]{"%" + searchTerm + "%"});

        try {
            if (cursor.moveToFirst()) {
                do {
                    String projectName = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PROJECT_NAME));
                    filteredProjects.add(projectName);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return filteredProjects;
    }

}
