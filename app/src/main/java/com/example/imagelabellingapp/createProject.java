package com.example.imagelabellingapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class createProject extends AppCompatActivity {
    TextView projTitle, labelTitle;

    EditText inputLabel, projName;
    Button labelAdd, saveBtn;
    ListView labelList;

    ArrayList<String> labelArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        labelList = findViewById(R.id.labelList);
        labelAdd = findViewById(R.id.labelAdd);
        inputLabel = findViewById(R.id.inputLabel);
        projName = findViewById(R.id.projName);
        projTitle = findViewById(R.id.projTitle);
        labelTitle = findViewById(R.id.projTitle);
        labelArr = new ArrayList<>();
        saveBtn = findViewById(R.id.saveBtn);

        // initializes adapter for list view
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labelArr);
        // sets adapter for list view
        labelList.setAdapter(adapter);

        // adds on click listener for 'add' button
        labelAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // getting text from edit text
                String item = inputLabel.getText().toString();

                // checks to see if item is not empty
                if (!item.isEmpty()){
                    // adds item to list
                    labelArr.add(item);
                    // notifies adapter that data in list is updated, updates list view
                    adapter.notifyDataSetChanged();
                    //Toast.makeText(createProject.this, item + " was added.", Toast.LENGTH_SHORT).show();
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), item + " was added", Snackbar.LENGTH_LONG);
                    View view = snack.getView();
                    FrameLayout.LayoutParams params =(FrameLayout.LayoutParams)view.getLayoutParams();
                    params.gravity = Gravity.TOP;
                    view.setLayoutParams(params);
                    snack.show();
                }
            }
        });

        // Allows user to delete a label from label list using the 'Long click' method
        labelList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int item, long l) {

                new AlertDialog.Builder(createProject.this)
                        .setTitle("Do you want to remove " + labelArr.get(item) + " from list?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                               labelArr.remove(item);
                               adapter.notifyDataSetChanged();
                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss(); // if user does not want to delete this label, dismiss action
                            }
                        }).create().show();
                return false;
            }
        });
    // clicking save button will save new project to database, and will send user to the "select Image" page
    saveBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(createProject.this,MainActivity2.class);
            startActivity(intent);
        }
    });





    }
}