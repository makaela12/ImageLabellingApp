package com.example.imagelabellingapp;// SelectProjectActivity.java

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class selectProject extends AppCompatActivity {

    private ListView projectListView;
    private Button addProjectButton;
    private ArrayAdapter<String> projectAdapter;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_project);

        dbHelper = new DBHelper(this);

        projectListView = findViewById(R.id.projectListView);
        addProjectButton = findViewById(R.id.addProjectButton);

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

        // Set click listener for the "Add Project" button
        addProjectButton.setOnClickListener(v -> {
            // TODO: Add logic to handle adding a new project (e.g., navigate to a new activity)
            Toast.makeText(selectProject.this, "Add Project Clicked", Toast.LENGTH_SHORT).show();
        });
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
}
