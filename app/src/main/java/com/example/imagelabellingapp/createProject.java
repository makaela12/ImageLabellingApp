package com.example.imagelabellingapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class createProject extends AppCompatActivity {
    private DBHelper dbHelper;
    TextView projTitle, labelTitle;

    EditText inputLabel, projName;
    Button saveBtn;

    ImageButton labelAdd, helpButton;
    ListView labelList;
    private String projectName;
    private ArrayList<String> labelArr;
    private ArrayAdapter<String> adapter;

    private RadioGroup radioGroup;
    private RadioButton radioFixed, radioCustom;
    private Spinner aspectRatioSpinner;
    private EditText editTextWidth, editTextHeight;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        dbHelper = new DBHelper(this);

        labelList = findViewById(R.id.labelList);
        labelAdd = findViewById(R.id.labelAdd);
        inputLabel = findViewById(R.id.inputLabel);
        projName = findViewById(R.id.projName);
        projTitle = findViewById(R.id.projTitle);
        labelTitle = findViewById(R.id.projTitle);
        labelArr = new ArrayList<>();
        saveBtn = findViewById(R.id.saveBtn);
        helpButton = findViewById(R.id.helpButton);
        radioGroup = findViewById(R.id.radioGroup);
        radioFixed = findViewById(R.id.radioFixed);
        radioCustom = findViewById(R.id.radioCustom);
        aspectRatioSpinner = findViewById(R.id.aspectRatioSpinner);
        editTextWidth = findViewById(R.id.editTextWidth);
        editTextHeight = findViewById(R.id.editTextHeight);

        // Initialize the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);

        // Enable the home button (back arrow)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set up the adapter for the aspect ratio spinner
        ArrayAdapter<CharSequence> aspectRatioAdapter = ArrayAdapter.createFromResource(this,
                R.array.aspect_ratio_options, android.R.layout.simple_spinner_item);
        aspectRatioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aspectRatioSpinner.setAdapter(aspectRatioAdapter);

        // Disable the spinner by default
        aspectRatioSpinner.setEnabled(false);


        // set onClickListener for help button
        helpButton.setOnClickListener(v -> showHelpPopup("Labels are used to define particular\nobjects in an image.\n\nTo Create a Label:\n-  Enter a name in the box titled \"Labels\"\n-  Click the '+' to add the label to your\n     project.\n\nClick and hold on a label to edit\nor delete."));

        // initializes adapter for list view
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labelArr); // Use the class-level variable
        // sets adapter for list view
        labelList.setAdapter(adapter);


        // adds on click listener for 'add' button
        labelAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Getting text from edittext
                String labelName = inputLabel.getText().toString().trim();

                // Checks if the label name is not empty
                if (!labelName.isEmpty()) {
                    // Checks if the label name already exists
                    if (!labelArr.contains(labelName)) {
                        // Adds item to the list
                        labelArr.add(labelName);
                        // Notifies the adapter that the data in the list is updated, updating the list view
                        adapter.notifyDataSetChanged();
                        // Snackbar for label added
                        Snackbar snack = Snackbar.make(findViewById(android.R.id.content), labelName + " was added", Snackbar.LENGTH_LONG);
                        View view = snack.getView();
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                        params.gravity = Gravity.TOP;
                        view.setLayoutParams(params);
                        snack.show();
                        // Clear input after adding
                        inputLabel.setText("");
                    } else {
                        Snackbar snack3 = Snackbar.make(findViewById(android.R.id.content), "Label name " + labelName + " already exists", Snackbar.LENGTH_LONG);
                        View view = snack3.getView();
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                        params.gravity = Gravity.TOP;
                        view.setLayoutParams(params);
                        snack3.show();
                        // Show an error message if the label name already exists
                        Toast.makeText(createProject.this, "Label name already exists", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Set listeners for radio buttons
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioFixed) {
                // Enable spinner and disable text boxes
                aspectRatioSpinner.setEnabled(true);
                editTextWidth.setEnabled(false);
                editTextHeight.setEnabled(false);
            } else if (checkedId == R.id.radioCustom) {
                // Disable spinner and enable text boxes
                aspectRatioSpinner.setEnabled(false);
                editTextWidth.setEnabled(true);
                editTextHeight.setEnabled(true);
            }
            else {
                // No radio button selected, disable the spinner and enable text boxes
                aspectRatioSpinner.setEnabled(false);
                editTextWidth.setEnabled(false);
                editTextHeight.setEnabled(false);
            }
        });

        // Set listener for spinner item selection
        aspectRatioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Update width and height text boxes with selected aspect ratio
                String selectedAspectRatio = (String) parentView.getItemAtPosition(position);
                String[] aspectRatioParts = selectedAspectRatio.split(" x ");
                editTextWidth.setText(aspectRatioParts[0]);
                editTextHeight.setText(aspectRatioParts[1]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing here
            }
        });

        // Allows user to delete a label from label list using the 'Long click' method
        labelList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int item, long l) {

                final String selectedLabel = labelArr.get(item);

                // Creating options for the user
                CharSequence[] options = {"Edit", "Delete"};

                new AlertDialog.Builder(createProject.this)
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

        // clicking save button will save new project to database, and will send user to the "select Image" page
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the project name entered by the user
                projectName = projName.getText().toString().trim();

                if (!projectName.isEmpty()) {
                    // Insert the project name into the projects table
                    long projectId = insertProject(projectName);

                    // Insert labels into the labels table with the corresponding project ID
                    if (projectId != -1) {
                        // Log the project insertion success
                        Log.d("createProject", "Project inserted successfully. Project ID: " + projectId);

                        for (String labelName : labelArr) {
                            insertLabel(projectId, labelName);
                        }

                        // Clear labelArr after adding labels to the database
                        labelArr.clear();
                        adapter.notifyDataSetChanged();
                        // Navigate to the "select Image" page with project ID
                        Intent intent = new Intent(createProject.this, MainActivity2.class);
                        // pass project_id to MainActivity2
                        intent.putExtra("projectId", projectId);
                        startActivity(intent);

                        finish();
                    }
                }else{
                // Log the project insertion failure
                    Snackbar snack2 = Snackbar.make(findViewById(android.R.id.content), "Please enter a project name", Snackbar.LENGTH_LONG);
                    View view = snack2.getView();
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                    params.gravity = Gravity.TOP;
                    view.setLayoutParams(params);
                    snack2.show();
                Log.e("createProject", "Failed to insert project into the database.");
            }
        }
        });
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
                    Toast.makeText(createProject.this, "Label cannot be empty", Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



}