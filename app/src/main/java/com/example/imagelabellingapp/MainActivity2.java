package com.example.imagelabellingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity2 extends AppCompatActivity {

    Button selectButton, takeButton;
    Bitmap bitmap;
    private ListView imageListView;
    private ImageAdapter imageAdapter; // Use ImageAdapter instead of ArrayAdapter
    DBHelper dbHelper;
    private File imageFile;
    private long projectId;
    private BroadcastReceiver newImageSavedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        dbHelper = new DBHelper(this);

        Intent intent = getIntent();
        if (intent.hasExtra("projectId")) {
            projectId = intent.getLongExtra("projectId", -1);
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
    }

    @Override
    protected void onDestroy() {
        // Unregister the BroadcastReceiver when the activity is destroyed
        unregisterReceiver(newImageSavedReceiver);
        super.onDestroy();
    }

    void getPermission(){
        // Asks the user for permission to access the camera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity2.this, new String[]{android.Manifest.permission.CAMERA}, 11);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Allows access to the user's camera to take a photo if the user gave access permission
        if (requestCode == 11){
            if (grantResults.length>0){
                if(grantResults[0]!= PackageManager.PERMISSION_GRANTED){
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
            // Convert Bitmap to File
            File imageFile = bitmapToFile(bitmap);
            // continue with cropping function
            startCropActivity(getImageUri(this, bitmap));

        }
        // After the image is cropped
        else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            // retrieve the result of the image cropping activity
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            // check if the cropping was successful
            if (resultCode == RESULT_OK) {
                // gets the URI of the cropped image
                Uri resultUri = result.getUri();
                // Save the cropped image to a file
                saveCroppedImageToFile(resultUri, projectId);
                // Start ImageDetailsActivity
                openImageDetailsActivity(resultUri.toString());

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                error.printStackTrace();
            }
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

    private void saveCroppedImageToFile(Uri resultUri, long projectId) {
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

                // Save the image path to the images table
                long imageId = dbHelper.insertImagePath(imageFile.getAbsolutePath(), projectId);

                // Set imagePath to the absolute path of the saved image
                String imagePath = imageFile.getAbsolutePath();

                // TODO: Save additional metadata (e.g., selected label) to a database or file here

                // Notify that a new image has been saved
                sendBroadcast(new Intent("new_image_saved"));

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Handle the case where projectId is -1
            // You can show an error message or take appropriate action
            // For now, we'll log an error
            Log.e("MainActivity2", "Invalid projectId: " + projectId);
        }
    }
// OLDDDDDD *******
  /*  private void saveCroppedImageToFile(Uri resultUri, long projectId) {
        // Ensure that projectId is not -1
        if (projectId != -1) {
            try {
                Bitmap croppedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);

                // Save the cropped image to a file
                File filesDir = getFilesDir();

                String timestamp = String.valueOf(System.currentTimeMillis());
                String filename = "cropped_" + timestamp + ".jpg";
                File imageFile = new File(filesDir, filename);

                // old *****8
               // imageFile = new File(filesDir, "images.jpg"); *** just changed to see if file name inconsistinsys were causing glide to not find the file
               //File imageFile = new File(resultUri.getPath());


                FileOutputStream fos = new FileOutputStream(imageFile);
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();

                // Save the image path to the images table
                long imageId = dbHelper.insertImagePath(imageFile.getAbsolutePath(), projectId);

                // Set imagePath to the absolute path of the saved image
                String imagePath = imageFile.getAbsolutePath();

                // TODO: Save additional metadata (e.g., selected label) to a database or file here

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Handle the case where projectId is -1
            // You can show an error message or take appropriate action
            // For now, we'll log an error
            Log.e("MainActivity2", "Invalid projectId: " + projectId);
        }
    }*/

    private void startCropActivity(Uri sourceUri) {
        CropImage.activity(sourceUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }
    private File bitmapToFile(Bitmap bitmap) {
        // Obtain directory path for app's files
        File filesDir = getFilesDir();

        // Generate a unique filename using a timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = "image_" + timestamp + ".jpg";

        // Create a new file object representing the destination file where the bitmap will be saved
        File imageFile = new File(filesDir, filename);

        try {
            // Write the bitmap data to the file
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Return the file object representing the file where the bitmap has been saved
        return imageFile;
    }


    private void setupImageListView() {
        runOnUiThread(() -> {
            // Retrieve list of image paths for a specific project using getImagePathsForProject method from dbHelper instance
            ArrayList<String> imagePaths = dbHelper.getImagePathsForProject(projectId);

            // If the adapter is not set, initialize it
            if (imageAdapter == null) {
                imageAdapter = new ImageAdapter(this, R.layout.image_list_item, imagePaths);
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
        startActivity(intent);
    }
}

// ******** OLD MAINACTIVITY2 *********
/*
    Button selectButton, takeButton;
    Bitmap bitmap;
    private ListView imageListView;
    private ArrayAdapter<String> imageAdapter;
    DBHelper dbHelper;
    private File imageFile;
    private long projectId;
    private BroadcastReceiver newImageSavedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        dbHelper = new DBHelper(this);

        Intent intent = getIntent();
        if (intent.hasExtra("projectId")) {
            projectId = intent.getIntExtra("projectId", -1);
        }

        // permission to let user access camera
        getPermission();

        selectButton = findViewById(R.id.selectButton);
        takeButton = findViewById(R.id.takeButton);
        imageListView = findViewById(R.id.imageListView);

        // Initialize and set up the image ListView
        setupImageListView();

        // Use dbHelper to get image paths
        ArrayList<String> imagePaths = dbHelper.getImagePathsForProject(projectId);

        // ****** old code ****
        // Initialize the ArrayAdapter in your onCreate or setup method
       // imageAdapter = new ArrayAdapter<String>(this, R.layout.image_list_item, imagePaths);

        // ***** new code from fixes ****

        // Initialize the ArrayAdapter in your onCreate or setup method
        imageAdapter = new ArrayAdapter<>(this, R.layout.image_list_item, imagePaths);

        // Initialize the custom ArrayAdapter
        ImageAdapter imageAdapter = new ImageAdapter(this, R.layout.image_list_item, imagePaths);
        // Set the adapter to your ListView
        imageListView.setAdapter(imageAdapter);
        // *****************************

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
    }

    @Override
    protected void onDestroy() {
        // Unregister the BroadcastReceiver when the activity is destroyed
        unregisterReceiver(newImageSavedReceiver);

        super.onDestroy();
    }

    void getPermission(){
        // Asks user for permission to access camera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity2.this, new String[]{android.Manifest.permission.CAMERA}, 11);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Allows access to users camera to take photo if user gave access permission
        if (requestCode == 11){
            if (grantResults.length>0){
                if(grantResults[0]!= PackageManager.PERMISSION_GRANTED){
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
   // private long projectId;

   @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
           // Convert Bitmap to File
           File imageFile = bitmapToFile(bitmap);
           // continue with cropping function
           startCropActivity(getImageUri(this, bitmap));

       }
       // After the image is cropped
       else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
           // retrieve the result of the image cropping activity
           CropImage.ActivityResult result = CropImage.getActivityResult(data);
           // check if the cropping was successful
           if (resultCode == RESULT_OK) {
               // gets the URI of the cropped image
               Uri resultUri = result.getUri();
               // Save the cropped image to a file
               saveCroppedImageToFile(resultUri, projectId);
               // Start ImageDetailsActivity
               openImageDetailsActivity(resultUri.toString());

           } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
               Exception error = result.getError();
               error.printStackTrace();
           }
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

    private void saveCroppedImageToFile(Uri resultUri, long projectId) {
        // Ensure that projectId is not -1
        if (projectId != -1) {
            try {
                Bitmap croppedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);

                // Save the cropped image to a file
                File filesDir = getFilesDir();
                imageFile = new File(filesDir, "images.jpg");

                FileOutputStream fos = new FileOutputStream(imageFile);
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();

                // Save the image path to the images table
                long imageId = dbHelper.insertImagePath(imageFile.getAbsolutePath(), projectId);

                // Set imagePath to the absolute path of the saved image
                String imagePath = imageFile.getAbsolutePath();

                // TODO: Save additional metadata (e.g., selected label) to a database or file here

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Handle the case where projectId is -1
            // You can show an error message or take appropriate action
            // For now, we'll log an error
            Log.e("MainActivity2", "Invalid projectId: " + projectId);
        }
    }

    private void startCropActivity(Uri sourceUri) {
        CropImage.activity(sourceUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }
    private File bitmapToFile(Bitmap bitmap) {
        // Obtain directory path for app's files
        File filesDir = getFilesDir();

        // Generate a unique filename using a timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = "image_" + timestamp + ".jpg";

        // Create a new file object representing the destination file where the bitmap will be saved
        File imageFile = new File(filesDir, filename);

        try {
            // Write the bitmap data to the file
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Return the file object representing the file where the bitmap has been saved
        return imageFile;
    }

    private void setupImageListView() {
            // Retrieve list of image paths for a specific project using getImagePathsForProject method from dbHelper instance
            ArrayList<String> imagePaths = dbHelper.getImagePathsForProject(projectId);

            // If the adapter is not set, initialize it
            if (imageAdapter == null) {
                imageAdapter = new ArrayAdapter<>(this, R.layout.image_list_item, imagePaths);
                imageListView.setAdapter(imageAdapter);
            } else {
                // Clear the existing data in the adapter
                imageAdapter.clear();

                // Add all the new image paths
                imageAdapter.addAll(imagePaths);
            }
                // Notify the adapter that the data has changed
                imageAdapter.notifyDataSetChanged();

    }
    private void openImageDetailsActivity(String imagePath) {
        Intent intent = new Intent(MainActivity2.this, ImageDetailsActivity.class);
        intent.putExtra("imagePath", imagePath);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

}

*/

