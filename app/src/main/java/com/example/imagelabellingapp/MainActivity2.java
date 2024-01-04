package com.example.imagelabellingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
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
    DBHelper dbHelper;
    private File imageFile;
    private long projectId;
    private BroadcastReceiver newImageSavedReceiver;
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
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 10);
            }
        });

        takeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 12);
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

        // After initializing imageAdapter
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
            // continue with the cropping function
            startCropActivity(uri);
            // if the user wants to capture an image with the camera
        } else if (requestCode == 12 && resultCode == RESULT_OK) {
            // get the captured image bitmap from the extras
            bitmap = (Bitmap) data.getExtras().get("data");
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
                    imageId = saveCroppedImageToFile(resultUri, projectId, selectedLabel);
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

    // Helper method to get the image URI from the bitmap
    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private long saveCroppedImageToFile(Uri resultUri, long projectId, String selectedLabel) {
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
               long imageId = dbHelper.insertImagePath(imagePath, projectId);

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
                openImageDetailsActivity(selectedImagePath);
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
        // Refresh the project list in selectProject activity
        refreshProjectListInSelectProject();
    }

    private void refreshProjectListInSelectProject() {
        // Send a broadcast to notify selectProject activity to refresh the project list
        sendBroadcast(new Intent("refresh_project_list"));
    }



}