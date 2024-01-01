package com.example.imagelabellingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ImageLabelDB";
    private static final int DATABASE_VERSION = 2;

    // Table names
    public static final String TABLE_PROJECTS = "projects";
    public static final String TABLE_LABELS = "labels";

    // Common columns
    public static final String COLUMN_ID = "id";

    // Projects table columns
    public static final String COLUMN_PROJECT_NAME = "project_name";

    // Labels table columns
    public static final String COLUMN_PROJECT_ID = "project_id";
    public static final String COLUMN_LABEL_NAME = "label_name";


    // New table for storing cropped images
    public static final String TABLE_IMAGES = "images";
    public static final String COLUMN_IMAGE_ID = "image_id";
    public static final String COLUMN_IMAGE_PATH = "image_path";

    // New column for storing selected label in the images table
    public static final String COLUMN_SELECTED_LABEL = "selected_label";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("DBHelper", "onCreate called");
        // Create projects table
        String createProjectsTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_PROJECTS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_PROJECT_NAME + " TEXT);";

        // Create labels table
        String createLabelsTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_LABELS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_PROJECT_ID + " INTEGER," +
                COLUMN_LABEL_NAME + " TEXT," +
                "FOREIGN KEY (" + COLUMN_PROJECT_ID + ") REFERENCES " + TABLE_PROJECTS + "(" + COLUMN_ID + "));";

        // Create Images table
        String createImagesTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_IMAGES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_PROJECT_ID + " INTEGER," +
                COLUMN_IMAGE_PATH + " TEXT," +
                COLUMN_SELECTED_LABEL + " TEXT);";

        db.execSQL(createImagesTableQuery);
        db.execSQL(createProjectsTableQuery);
        db.execSQL(createLabelsTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade the database if needed
        // You can modify the table schema or recreate tables here
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGES);
        onCreate(db);
    }
    // Method to insert a new image path into the images table
    public long insertImagePath(String imagePath, long projectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_PATH, imagePath);
        values.put(COLUMN_PROJECT_ID, projectId);
        return db.insert(TABLE_IMAGES, null, values);
    }

    public void saveImageDetails(String imagePath, long projectId, String selectedLabel) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_PATH, imagePath);
        values.put(COLUMN_PROJECT_ID, projectId);
        values.put(COLUMN_SELECTED_LABEL, selectedLabel);

        db.insert(TABLE_IMAGES, null, values);
        db.close();
    }

    /*
    public void saveImageDetails(String imagePath, String selectedLabel) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            // Start a transaction
            db.beginTransaction();

            // Insert image details into the database
            ContentValues values = new ContentValues();
            values.put(COLUMN_IMAGE_PATH, imagePath);
            values.put(COLUMN_SELECTED_LABEL, selectedLabel);
            // Add other columns as needed

            db.insert(TABLE_IMAGES, null, values);

            // Set the transaction as successful
            db.setTransactionSuccessful();
        } finally {
            // End the transaction
            db.endTransaction();
        }
    }*/

    // Method to get image paths for a specific project
    public ArrayList<String> getImagePathsForProject(long projectId) {
        ArrayList<String> imagePaths = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to retrieve image paths for the given project ID
        String[] projection = {COLUMN_IMAGE_PATH};
        String selection = COLUMN_PROJECT_ID + " = ?";
        String[] selectionArgs = {String.valueOf(projectId)};

        Cursor cursor = db.query(
                TABLE_IMAGES,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        // Process the cursor and populate a list of image paths
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String imagePath = cursor.getString(cursor.getColumnIndex(COLUMN_IMAGE_PATH));
                imagePaths.add(imagePath);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return imagePaths;
    }

    public List<String> getLabelsForProject(long projectId) {
        List<String> labels = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to retrieve labels for the given project ID
        String[] projection = {COLUMN_LABEL_NAME};
        String selection = COLUMN_PROJECT_ID + " = ?";
        String[] selectionArgs = {String.valueOf(projectId)};

        Cursor cursor = db.query(
                TABLE_LABELS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        // Process the cursor and populate a list of labels
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String labelName = cursor.getString(cursor.getColumnIndex(COLUMN_LABEL_NAME));
                labels.add(labelName);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return labels;
    }
}

