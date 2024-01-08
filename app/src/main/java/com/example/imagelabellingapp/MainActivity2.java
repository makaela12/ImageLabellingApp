package com.example.imagelabellingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity2 extends AppCompatActivity {

    Button selectButton, takeButton;
    Bitmap bitmap;
    private ListView imageListView;
    private ImageAdapter imageAdapter;

    private ArrayAdapter<String> deleteAdapter;

    private
    DBHelper dbHelper;
    private long projectId;
    private BroadcastReceiver newImageSavedReceiver;
    private BroadcastReceiver labelChangedReceiver;
    private static final int REQUEST_IMAGE_DETAILS = 203;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        dbHelper = new DBHelper(this);

        // Check if there is saved instance state
        if (savedInstanceState != null) {
            // Restore necessary data
            projectId = savedInstanceState.getLong("projectId");
            // Restore any other data you might need
        } else {
            // Handle the normal creation of the activity
            Intent intent = getIntent();
            if (intent.hasExtra("projectId")) {
                projectId = intent.getLongExtra("projectId", -1);
            }
        }

        // permission to let the user access the camera
        getPermission();

        selectButton = findViewById(R.id.selectButton);
        takeButton = findViewById(R.id.takeButton);
        imageListView = findViewById(R.id.imageListView);


        // Initialize and set up the image ListView
        setupImageListView();

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (labelNamesExistForProject()) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 10);
                } else {
                    showLabelErrorDialog();
                }
            }
        });

        takeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (labelNamesExistForProject()) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, 12);
                } else {
                    showLabelErrorDialog();
                }
            }
        });
        // Initialize the BroadcastReceiver
        newImageSavedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Refresh the data in the imageAdapter
                setupImageListView();
            }
        };

        // Register the BroadcastReceiver to listen for the "new_image_saved" broadcast
        registerReceiver(newImageSavedReceiver, new IntentFilter("new_image_saved"));

        // Initialize the BroadcastReceiver for label changes
        labelChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long changedImageId = intent.getLongExtra("imageId", -1);
                String newLabel = intent.getStringExtra("newLabel");

                // Update the label in the imageAdapter
                imageAdapter.setSelectedLabel(changedImageId, newLabel);
                imageAdapter.notifyDataSetChanged();
            }
        };

        // Register the BroadcastReceiver to listen for the "label_changed" broadcast
        registerReceiver(labelChangedReceiver, new IntentFilter("label_changed"));


        // Retrieve saved labels from SharedPreferences
        Map<Long, String> savedLabels = getSavedLabels();
        // Set the saved labels in the adapter
        imageAdapter.setSelectedLabelsMap(savedLabels);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the home button (back button)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Inflate the menu
        toolbar.inflateMenu(R.menu.menu_main);

        // Set up menu item click listener
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                // handles edit project action
                case R.id.menu_edit_project:
                    if (projectId != -1) {
                        Intent editProjectIntent = new Intent(MainActivity2.this, EditProjectActivity.class);
                        editProjectIntent.putExtra("projectId", projectId);
                        startActivity(editProjectIntent);
                        //startActivityForResult(editProjectIntent, REQUEST_CODE);
                    } else {
                        Log.e("MainActivity2", "Invalid projectId: " + projectId);
                    }
                    return true;
                case R.id.menu_export_project:
                    // handles export project action
                    // TODO: Implement the logic for exporting the project
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        });
    }
    // Check if label names exist for the current project
    private boolean labelNamesExistForProject() {
        // Retrieve label names for the current project using dbHelper
        List<String> labelNames = dbHelper.getLabelsForProject(projectId);

        // Return true if at least one label name exists
        return labelNames != null && !labelNames.isEmpty();
    }
    private void showLabelErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage("Labels must be defined before adding images.\n\nDo you want to create labels now?");
        builder.setPositiveButton("Create Labels", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Start EditProjectActivity for label creation
                Intent intent = new Intent(MainActivity2.this, EditProjectActivity.class);
                intent.putExtra("projectId", projectId);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing (stay on MainActivity2)
                dialog.dismiss();
            }
        });
        builder.show();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("MainActivity2", "onCreateOptionsMenu called");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        Log.d("MainActivity2", "Menu items count: " + menu.size());
        return true;
    }

    private Map<Long, String> getSavedLabels() {
        Map<Long, String> savedLabels = new HashMap<>();
        SharedPreferences preferences = getSharedPreferences("SelectedLabels", Context.MODE_PRIVATE);

        // Iterate through the preferences and add to the map
        Map<String, ?> allEntries = preferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            long imageId = Long.parseLong(entry.getKey());
            String label = (String) entry.getValue();
            savedLabels.put(imageId, label);
        }

        return savedLabels;
    }

    @Override
    protected void onDestroy() {
        // Unregister the BroadcastReceiver when the activity is destroyed
        unregisterReceiver(newImageSavedReceiver);
        unregisterReceiver(labelChangedReceiver);
        super.onDestroy();
    }

    void getPermission() {
        // Asks the user for permission to access the camera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity2.this, new String[]{android.Manifest.permission.CAMERA}, 11);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Allows access to the user's camera to take a photo if the user gave access permission
        if (requestCode == 11) {
            if (grantResults.length > 0) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Declare variables at the beginning of onActivityResult
    long imageId; // Initialize with a default value
    String selectedLabel;
    String originalImagePath;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainActivity2", "onActivityResult called with requestCode: " + requestCode);

        if (data != null && data.getExtras() != null) {
            Bundle extras = data.getExtras();
            for (String key : extras.keySet()) {
                Log.d("MainActivity2", "Extra key: " + key + ", Extra value: " + extras.get(key));
            }
        }
        Uri resultUri = null;


        // if the user wants to select an image from their photo gallery
        if (requestCode == 10 && resultCode == RESULT_OK) {
            // retrieve the URI of the selected image
            Uri uri = data.getData();
            // Get the originalImagePath for gallery-selected images
            originalImagePath = getOriginalImagePathFromUri(uri);
            // continue with the cropping function
            startCropActivity(uri);
            // if the user wants to capture an image with the camera
        } else if (requestCode == 12 && resultCode == RESULT_OK) {
            // get the captured image bitmap from the extras
            bitmap = (Bitmap) data.getExtras().get("data");
            // Get the originalImagePath for camera-captured images
            originalImagePath = getOriginalImagePathFromBitmap(bitmap);
            // continue with cropping function
            startCropActivity(getImageUri(this, bitmap));

        }
        // After the image is cropped
        else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            // retrieve the result of the image cropping activity
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            // check if the cropping was successful
            if (result != null) {
                if (resultCode == RESULT_OK) {
                    // gets the URI of the cropped image
                    resultUri = result.getUri();
                    // Save the cropped image to a file and get the imageId
                    imageId = saveCroppedImageToFile(resultUri, projectId, selectedLabel, originalImagePath);
                    // Start ImageDetailsActivity
                    openImageDetailsActivity(resultUri.toString());

                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                    if (error != null) {
                        error.printStackTrace();
                    }
                }
            } else {
                // Handle the case where CropImage result is null
                Log.e("MainActivity2", "CropImage result is null");
            }
        }
            // Handle the result from ImageDetailsActivity
            if (requestCode == REQUEST_IMAGE_DETAILS && resultCode == RESULT_OK) {
                // Handle the result data, which contains the selected label
                String selectedLabel = data.getStringExtra("selectedLabel");
                long imageId = data.getLongExtra("imageId", -1);

                // Pass the selected label to the ImageAdapter
                Log.d("MainActivity2", "ImageId: " + imageId + ", SelectedLabel: " + selectedLabel);
                imageAdapter.setSelectedLabel(imageId, selectedLabel);
                imageAdapter.notifyDataSetChanged();

            }
        super.onActivityResult(requestCode, resultCode, data);
    }


    // Method to get the original image path from URI for gallery-selected images
    private String getOriginalImagePathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String originalImagePath = cursor.getString(columnIndex);
            cursor.close();
            return originalImagePath;
        }
        return null;
    }

    // Method to get the original image path from Bitmap for camera-captured images
    private String getOriginalImagePathFromBitmap(Bitmap bitmap) {
        // Save the original image to a temporary file
        try {
            File cacheDir = getCacheDir();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = "original_" + timestamp + ".jpg";
            File imageFile = new File(cacheDir, filename);

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper method to get the image URI from the bitmap
    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private long saveCroppedImageToFile(Uri resultUri, long projectId, String selectedLabel, String originalImagePath) {
        // Ensure that projectId is not -1
        if (projectId != -1) {
            try {
                Bitmap croppedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);

                // Save the cropped image to a file in the cache directory
                File cacheDir = getCacheDir();
                String timestamp = String.valueOf(System.currentTimeMillis());
                String filename = "cropped_" + timestamp + ".jpg";
                File imageFile = new File(cacheDir, filename);

                FileOutputStream fos = new FileOutputStream(imageFile);
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();

                // Set imagePath to the absolute path of the saved image
                String imagePath = imageFile.getAbsolutePath();
                // Save the image path to the images table and get the imageId
               long imageId = dbHelper.insertImagePath(originalImagePath, imagePath, projectId);

                Log.e("CORRECT IMAGEID?????", "Invalid projectId: " + imageId);

                // Save the label associated with the imageId
                dbHelper.saveLabelForImage(imageId, selectedLabel);

                // Notify that a new image has been saved
                sendBroadcast(new Intent("new_image_saved"));

                // Update the ListView
                updateListView();

                return imageId;

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Handle the case where projectId is -1
            Log.e("MainActivity2", "Invalid projectId: " + projectId);
        }
        return -1;
    }

    private void updateListView() {
            // Retrieve list of image paths for a specific project using getImagePathsForProject method from dbHelper instance
            ArrayList<String> updatedImagePaths = dbHelper.getImagePathsForProject(projectId);

            // Retrieve list of labels for a specific project using getLabelsForProject method from dbHelper instance
            List<String> labels = dbHelper.getLabelsForProject(projectId);

            // If the adapter is not set, initialize it
            if (imageAdapter == null) {
                imageAdapter = new ImageAdapter(this, R.layout.image_list_item, updatedImagePaths, dbHelper);
                imageListView.setAdapter(imageAdapter);
            } else {
                // Clear the existing data in the adapter
                imageAdapter.clear();
                // Add all the new image paths
                imageAdapter.addAll(updatedImagePaths);
            }
            // Notify the adapter that the data has changed
            imageAdapter.notifyDataSetChanged();
        }


    private void startCropActivity(Uri sourceUri) {
        CropImage.activity(sourceUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    private void setupImageListView() {
        runOnUiThread(() -> {
            // Retrieve list of image paths for a specific project using getImagePathsForProject method from dbHelper instance
            ArrayList<String> imagePaths = dbHelper.getImagePathsForProject(projectId);

            // Retrieve list of labels for a specific project using getLabelsForProject method from dbHelper instance
            List<String> labels = dbHelper.getLabelsForProject(projectId);

            // If the adapter is not set, initialize it
            if (imageAdapter == null) {
                imageAdapter = new ImageAdapter(this, R.layout.image_list_item, imagePaths, dbHelper);
                imageListView.setAdapter(imageAdapter);
            } else {
                // Clear the existing data in the adapter
                imageAdapter.clear();
                // Add all the new image paths
                imageAdapter.addAll(imagePaths);
            }

            // Notify the adapter that the data has changed
            imageAdapter.notifyDataSetChanged();

            // Set click listener for the ListView items
            imageListView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedImagePath = imageAdapter.getItem(position);
                String originalImagePath = getOriginalImagePath(selectedImagePath);
                openEditImage(selectedImagePath, originalImagePath);
            });

            // Set long-click listener for the ListView items to handle deletions
            imageListView.setOnItemLongClickListener((parent, view, position, id) -> {
                // Display a confirmation dialog
                showDeleteImageDialog(position);
                return true; // Consume the long-click event
            });
        });
    }

    private void openImageDetailsActivity(String imagePath) {
        Log.d("MainActivity2", "Opening ImageDetailsActivity with Image Path: " + imagePath);
        Intent intent = new Intent(MainActivity2.this, ImageDetailsActivity.class);
        intent.putExtra("imagePath", imagePath);
        intent.putExtra("projectId", projectId);
        intent.putExtra("imageId", imageId);
        intent.putExtra("selectedLabel", selectedLabel);
        startActivityForResult(intent, REQUEST_IMAGE_DETAILS);
    }

    private void openEditImage(String imagePath, String originalImagePath) {
        Log.d("MainActivity2", "Opening EditImage activity with Image Path: " + imagePath);
        Intent editImageIntent = new Intent(MainActivity2.this, EditImage.class);
        editImageIntent.putExtra("imagePath", imagePath);
        editImageIntent.putExtra("originalImagePath", originalImagePath);
        editImageIntent.putExtra("projectId", projectId);
        editImageIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  // Add this line
        startActivity(editImageIntent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("projectId", projectId);
        outState.putString("selectedLabel", selectedLabel);
        // Save any other data you want to persist
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Handle the back button click (navigate up)
                onBackPressed();
                navigateToSelectProject();
                return true;
            // Add other menu item cases if needed
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // used to refresh the project list in selectProject after making changes to project name
    @Override
    protected void onResume() {
        super.onResume();
        setupImageListView();
        // Refresh the project list in selectProject activity
        refreshProjectListInSelectProject();
        // Register a BroadcastReceiver to listen for data changes
        IntentFilter intentFilter = new IntentFilter("data_changed");
        BroadcastReceiver dataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Update the ListView when data changes
                updateListView();
            }
        };
        registerReceiver(dataChangedReceiver, intentFilter);
    }

    private void refreshProjectListInSelectProject() {
        // Send a broadcast to notify selectProject activity to refresh the project list
        sendBroadcast(new Intent("refresh_project_list"));
    }

    private void navigateToSelectProject() {
        // Navigate to the selectProject activity
        Intent intent = new Intent(MainActivity2.this, selectProject.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private String getOriginalImagePath(String selectedImagePath) {
        // Use your DBHelper to query the original image path from the database
        String originalImagePath = dbHelper.getOriginalImagePath(selectedImagePath);

        return originalImagePath;
    }

    private void showDeleteImageDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Image");
        builder.setMessage("Are you sure you want to delete this image?");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get the image path from the adapter based on the position
                String selectedImagePath = imageAdapter.getItem(position);
                // Retrieve the corresponding image ID from the database
                long imageID = dbHelper.getImageIdFromPath(selectedImagePath);
                // Delete the corresponding row in the images database
                dbHelper.deleteImage(projectId, imageID);

                // Remove the item from the adapter
                imageAdapter.remove(selectedImagePath);
                imageAdapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

}