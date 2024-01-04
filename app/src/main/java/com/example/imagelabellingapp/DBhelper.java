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
                COLUMN_IMAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_PROJECT_ID + " INTEGER," +
                COLUMN_IMAGE_PATH + " TEXT," +
                COLUMN_SELECTED_LABEL + " TEXT);";

        db.execSQL(createImagesTableQuery);
        db.execSQL(createProjectsTableQuery);
        db.execSQL(createLabelsTableQuery);
    }

    // used to upgrade the database if needed
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade the database if needed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGES);
        onCreate(db);
    }

    // used to update the project name in DB after making changes to it
    public void updateProject(long projectId, String newProjectName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROJECT_NAME, newProjectName);

        db.update(TABLE_PROJECTS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(projectId)});
        db.close();
    }

    // used to save new label name selected for a cropped image
    public void updateLabelForImage(long imageId, String selectedLabel) {

        Log.d("DBHelperBLYYYYYYYSYSGHUWVHSWHWBSDHIW", "ImageID==" + imageId + "selectedlabel==" + selectedLabel );
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_SELECTED_LABEL, selectedLabel);

        String selection = COLUMN_IMAGE_ID + " = ?";
        String[] selectionArgs = {String.valueOf(imageId)};

        db.update(TABLE_IMAGES, values, selection, selectionArgs);
    }

    // used to save the label name selected for a cropped image
    public void saveLabelForImage(long imageId, String selectedLabel) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(COLUMN_SELECTED_LABEL, selectedLabel);

            String selection = COLUMN_IMAGE_ID + " = ?";
            String[] selectionArgs = {String.valueOf(imageId)};

            int rowsUpdated = db.update(TABLE_IMAGES, values, selection, selectionArgs);
            db.close();

            Log.d("DBHelper", "Rows updated for ImageId " + imageId + ": " + rowsUpdated);

    }
    // used to insert a new label for a project
    public void insertLabel(long projectId, String labelName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROJECT_ID, projectId);
        values.put(COLUMN_LABEL_NAME, labelName);

        db.insert(TABLE_LABELS, null, values);
        db.close();
    }

    // used to delete labels associated to a project_id the user wants to delete
    public void deleteLabelsForProject(long projectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LABELS, COLUMN_PROJECT_ID + " = ?", new String[]{String.valueOf(projectId)});
    }

    // used to delete image_paths associated to a project_id the user wants to delete
    public void deleteImagesForProject(long projectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_IMAGES, COLUMN_PROJECT_ID + " = ?", new String[]{String.valueOf(projectId)});
    }

    // Method to insert a new image path into the images table
    public long insertImagePath(String imagePath, long projectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_PATH, imagePath);
        values.put(COLUMN_PROJECT_ID, projectId);
        return db.insert(TABLE_IMAGES, null, values);
    }

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
    public long getImageIdFromPath(String imagePath) {
        long imageId = -1;
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to retrieve imageId for the given imagePath
        String[] projection = {COLUMN_IMAGE_ID};
        String selection = COLUMN_IMAGE_PATH + " = ?";
        String[] selectionArgs = {imagePath};

        Cursor cursor = db.query(
                TABLE_IMAGES,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        // Process the cursor and retrieve the imageId
        if (cursor != null && cursor.moveToFirst()) {
            imageId = cursor.getLong(cursor.getColumnIndex(COLUMN_IMAGE_ID));
            cursor.close();
        }

        return imageId;
    }

    // method that retrieves a project name from the DB based on project_id
    public String getProjectName(long projectId) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the columns you want to retrieve
        String[] projection = {COLUMN_PROJECT_NAME};

        // Specify the selection criteria
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(projectId)};

        // Query the database
        Cursor cursor = db.query(
                TABLE_PROJECTS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        String projectName = null;

        // Process the cursor and retrieve the project name
        if (cursor != null && cursor.moveToFirst()) {
            projectName = cursor.getString(cursor.getColumnIndex(COLUMN_PROJECT_NAME));
            cursor.close();
        }

        return projectName;
    }






}

