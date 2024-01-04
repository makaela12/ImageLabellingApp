package com.example.imagelabellingapp;// SelectProjectActivity.java

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class selectProject extends AppCompatActivity {

    private ListView projectListView;
    private ArrayAdapter<String> projectAdapter;
    private DBHelper dbHelper;

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

}
