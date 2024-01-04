package com.example.imagelabellingapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class EditProjectActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private EditText projName, inputLabel;
    private ListView labelList;
    private ArrayAdapter<String> adapter;
    private long projectId; // Project ID to identify the project being edited
    private ArrayList<String> labelArr;
    private String projectName;
    private Button labelAdd, saveBtn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_project);


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
        // Change the color of the back arrow
        changeBackArrowColor(toolbar, R.color.white);

        dbHelper = new DBHelper(this);

        // Initialize views and variables
        projName = findViewById(R.id.projName);
        labelList = findViewById(R.id.labelList);
        inputLabel = findViewById(R.id.inputLabel);
        saveBtn = findViewById(R.id.saveBtn);
        labelAdd = findViewById(R.id.labelAdd);
        labelArr = new ArrayList<>();


        // Initialize the adapter and set it to the labelList
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
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

        // adds on click listener for 'add' button
        labelAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // getting text from edittext
                String labelName = inputLabel.getText().toString().trim();

                // checks to see if item is not empty
                if (!labelName.isEmpty()) {
                    // adds item to list
                    labelArr.add(labelName);
                    // Notifies adapter that data in list is updated, updates list view
                    adapter.add(labelName);
                    // Clear input after adding
                    inputLabel.setText("");

                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), labelName + " was added", Snackbar.LENGTH_LONG);
                    View view = snack.getView();
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                    params.gravity = Gravity.TOP;
                    view.setLayoutParams(params);
                    snack.show();
                    // Clear input after adding
                   // inputLabel.setText("");

                }
            }
        });

        // Set onItemClickListener for labelList (similar to createProject)
        labelList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int item, long l) {
                final String selectedLabel = labelArr.get(item);

                // Creating options for the user
                CharSequence[] options = {"Edit", "Delete"};

                new AlertDialog.Builder(EditProjectActivity.this)
                        .setTitle("Select an option for " + selectedLabel)
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: // Edit
                                        showEditLabelDialog(selectedLabel, item);
                                        break;
                                    case 1: // Delete
                                        removeLabel(item);
                                        break;
                                }
                            }
                        }).create().show();

                return false;
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the updated project name
                projectName = projName.getText().toString().trim();

                if (!projectName.isEmpty()) {
                    // Update the project name in the database
                    dbHelper.updateProject(projectId, projectName);

                    // Process labels (Add new labels to the project)
                    for (String labelName : labelArr) {
                        insertLabel(projectId, labelName);
                    }
                    // Clear labelArr after updating labels in the database
                    labelArr.clear();
                    adapter.notifyDataSetChanged();

                    Intent intent = new Intent(EditProjectActivity.this, MainActivity2.class);
                    intent.putExtra("projectId", projectId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);

                    finish();
                }
            }
        });

    }

    // method to call the tintDrawable method to change the color of the back arrow to white
    private void changeBackArrowColor(Toolbar toolbar, int colorRes) {
        // Get the up button drawable
        Drawable upArrow = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_close_clear_cancel);

        // Tint the drawable
        if (upArrow != null) {
            upArrow = tintDrawable(upArrow, colorRes);
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }
    }

    // method to set the tint of the 'x' button in top left corner to white
    private Drawable tintDrawable(Drawable drawable, int colorRes) {
        Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, colorRes));
        return wrappedDrawable;
    }

    // Helper method to load project details for editing
    private void loadProjectDetails(long projectId) {
        // Retrieve project details from the database using dbHelper
        String projectName = dbHelper.getProjectName(projectId);
        ArrayList<String> labels = (ArrayList<String>) dbHelper.getLabelsForProject(projectId);

        // Set project name and labels to the views
        projName.setText(projectName);
        adapter.clear();
        adapter.addAll(labels);
    }

    // Method to remove a label
    private void removeLabel(int position) {
        labelArr.remove(position);
        adapter.notifyDataSetChanged();
    }

    // Method to show a dialog for editing a label
    private void showEditLabelDialog(final String label, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Label");

        // Set up the input
        final EditText input = new EditText(this);
        // Pre-fill the current label
        input.setText(label);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String editedLabel = input.getText().toString().trim();
                if (!editedLabel.isEmpty()) {
                    labelArr.set(position, editedLabel);
                    adapter.notifyDataSetChanged();
                    dialog.dismiss();
                } else {
                    // Show an error message if the edited label is empty
                    Toast.makeText(EditProjectActivity.this, "Label cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    // Helper method to insert a project and return the project ID
    private long insertProject(String projectName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_PROJECT_NAME, projectName);
        // The projectId variable is assigned the value returned by db.insert
        long projectId = db.insert(DBHelper.TABLE_PROJECTS, null, values);

        // Return the projectId
        return projectId;
    }

    // Helper method to insert a label with the corresponding project ID
    private void insertLabel(long projectId, String labelName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_PROJECT_ID, projectId);
        values.put(DBHelper.COLUMN_LABEL_NAME, labelName);

        db.insert(DBHelper.TABLE_LABELS, null, values);
    }

}
