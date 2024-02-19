package com.example.imagelabellingapp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportProjectActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private long projectId;
    private ImageButton helpButton;

    private Button cancelButton, exportButton;
    private ListView listViewFiles;
    private TextView textViewZipFileName, textViewImageCount;
    private String projectName;
    private List<String> imagePaths;
    private List<String> labelNames;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_project);

        dbHelper = new DBHelper(this);

        // Initialize UI components
        textViewImageCount = findViewById(R.id.textViewImageCount);
        textViewZipFileName = findViewById(R.id.textViewZipFileName);
       // listViewFiles = findViewById(R.id.listViewFiles);
        helpButton = findViewById(R.id.helpButton);
        cancelButton = findViewById(R.id.cancelButton);
        exportButton = findViewById(R.id.exportButton);

        Intent intent = getIntent();
        projectId = intent.getLongExtra("projectId", -1);
        Log.d("Export Proj", "onCreate: projectid =" + projectId);

        // Retrieve project details
        projectName = dbHelper.getProjectName(projectId);
        imagePaths = dbHelper.getImagePathsForProject(projectId);

        // Set image count
        int imageCount = imagePaths.size();
        textViewImageCount.setText("Number of Images: " + imageCount);

        // Set zip file name
        String zipFileName = projectName + ".zip";
        textViewZipFileName.setText("Zip File Name: " + zipFileName);



        // Initialize ListView
        ListView listViewLabels = findViewById(R.id.viewLabels);
        ListView listViewImages = findViewById(R.id.viewImages);

// Assuming you have a method getImageFileNamesForProject in your DBHelper
     //   List<String> labelFileNames = dbHelper.getImageFileNamesForProject(projectId);
     //   List<String> imageFileNames = dbHelper.getImageFileNamesForProject(projectId);

// Create an ArrayAdapter for labels and images
       // ArrayAdapter<String> labelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labelFileNames);
     //   ArrayAdapter<String> imageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, imageFileNames);

// Set adapters to ListViews


      //  labelAdapter.notifyDataSetChanged();


      //  Log.d("ExportProject", "onCreate: labelFilename : " + labelFileNames +"imageFileNames" + imageFileNames);


        // Initialize the Toolbar
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Toolbar toolbar = findViewById(R.id.toolBar);
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
        changeExitIconColour(toolbar, R.color.white);

        // set onClickListener for help button
        helpButton.setOnClickListener(v -> showHelpPopup("To edit the project name,\n click on the current name.\n\nTo edit or delete a label, click\nand hold down on the label." +
                "\n\nTo add a new label, enter the\nlabel name and click the '+' icon."));


        // Handle button clicks
        cancelButton.setOnClickListener(v -> {
            // Handle cancel button click
            finish();  // Close the activity
        });

        exportButton.setOnClickListener(v -> {
            // Handle export button click
            exportProject();  // Replace with your export logic
        });

    }

    private void exportProject() {
        // Get project details
        projectName = dbHelper.getProjectName(projectId);
        imagePaths = dbHelper.getImagePathsForProject(projectId);
        labelNames = dbHelper.getLabelsForProject(projectId);


        // Create a zip file
        File exportZipFile = new File(getExternalFilesDir(null), projectName + ".zip");

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportZipFile))) {
            // Create 'labels' folder
            zipOutputStream.putNextEntry(new ZipEntry("labels/"));
            zipOutputStream.closeEntry();

            // Create 'images' folder
            zipOutputStream.putNextEntry(new ZipEntry("images/"));
            zipOutputStream.closeEntry();

            // Create 'classes.txt' file
            zipOutputStream.putNextEntry(new ZipEntry("classes.txt"));

            for (String label : labelNames) {
                zipOutputStream.write((label + "\n").getBytes());
            }
            zipOutputStream.closeEntry();


            // Process each image
            for (String imagePath : imagePaths) {
                // Retrieve bounding box information from the database
                float[] boundingBox = dbHelper.getBoundingBoxForExport(dbHelper.getImageIdFromPath(imagePath));
                long i_id = dbHelper.getImageIdFromPath(imagePath);
                String label = dbHelper.getLabelNameForBoundingBox(i_id,boundingBox[0],boundingBox[1],boundingBox[2],boundingBox[3]);

                // Write YOLO format label file
                zipOutputStream.putNextEntry(new ZipEntry("labels/" + getFileNameWithoutExtension(imagePath)));
                String labelData = String.format("%d %.6f %.6f %.6f %.6f",
                        labelNames.indexOf(label), // Object class index
                        (boundingBox[0] + boundingBox[2]) / 2, // x-center
                        (boundingBox[1] + boundingBox[3]) / 2, // y-center
                        (boundingBox[2] - boundingBox[0]),    // width
                        (boundingBox[3] - boundingBox[1]));   // height
                zipOutputStream.write(labelData.getBytes());
               // zipOutputStream.closeEntry();

                // Copy image file
                zipOutputStream.putNextEntry(new ZipEntry("images/" + getFileNameWithoutExtension(imagePath)));
                zipOutputStream.write(getFileBytes(new File(imagePath)));
                zipOutputStream.closeEntry();
            }

            // Show a success message
            runOnUiThread(() -> {
                Toast.makeText(this, "Project exported successfully", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Error exporting project", Toast.LENGTH_SHORT).show();
            });
        } finally {
           // finish();
        }
        // Create a URI for the exported ZIP file
        Uri uri = FileProvider.getUriForFile(this, "com.example.imagelabellingapp.fileprovider", exportZipFile);

        // Create a sharing intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/zip");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

        // Check if there is any application that can handle the sharing intent
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            // Start the sharing activity
            startActivity(Intent.createChooser(shareIntent, "Share via..."));
        } else {
            // Display a message that no app can handle the sharing intent
            Toast.makeText(this, "No app can handle the sharing intent", Toast.LENGTH_SHORT).show();
        }

      /*  // asks user to share .zip file for now.
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(this, "com.example.imagelabellingapp.fileprovider", exportZipFile);
        shareIntent.setType("application/zip");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share via..."));*/

    }
    // Helper function to get file name without extension
    private String getFileNameWithoutExtension(String filePath) {
        File file = new File(filePath);
        String fileName = file.getName();
        int pos = fileName.lastIndexOf(".");
        if (pos > 0 && pos < fileName.length() - 1) {
            return fileName.substring(0, pos);
        } else {
            return fileName;
        }
    }

    // Helper function to get file bytes
    private byte[] getFileBytes(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fileInputStream.read(bytes);
            fileInputStream.close();
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

        private void showHelpPopup (String message){
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

        // method to call the tintDrawable method to change the color of the 'x' icon to white
        private void changeExitIconColour (Toolbar toolbar,int colorRes){
            // Get the up button drawable
            Drawable upArrow = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_close_clear_cancel);

            // tint the drawable
            if (upArrow != null) {
                upArrow = tintDrawable(upArrow, colorRes);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
        }

        // method to set the tint of the 'x' button in top left corner to white
        private Drawable tintDrawable (Drawable drawable,int colorRes){
            Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, colorRes));
            return wrappedDrawable;
        }





}