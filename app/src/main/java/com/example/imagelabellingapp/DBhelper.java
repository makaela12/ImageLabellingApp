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

    public static final String TABLE_BBOXES = "bboxes";

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

    public static final String COLUMN_ORIGINAL_IMAGE_PATH = "original_image_path";

    // New column for storing selected label in the images table
    public static final String COLUMN_SELECTED_LABEL = "selected_label";


    public static final String COLUMN_BBOX_ID = "bbox_id";
    public static final String COLUMN_BBOX_X_MIN = "bbox_x_min";
    public static final String COLUMN_BBOX_Y_MIN = "bbox_y_min";
    public static final String COLUMN_BBOX_X_MAX = "bbox_x_max";
    public static final String COLUMN_BBOX_Y_MAX = "bbox_y_max";

    // New table for storing file information
    public static final String TABLE_FILES = "files";
    public static final String COLUMN_FILE_ID = "file_id";
    public static final String COLUMN_FILENAME = "filename";

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
                COLUMN_ORIGINAL_IMAGE_PATH + " TEXT," +
                COLUMN_SELECTED_LABEL + " TEXT);";

        String createBBoxesTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_BBOXES + " (" +
                COLUMN_BBOX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_IMAGE_ID + " INTEGER," +
                COLUMN_LABEL_NAME + " INTEGER," +
                COLUMN_BBOX_X_MIN + " REAL," +
                COLUMN_BBOX_Y_MIN + " REAL," +
                COLUMN_BBOX_X_MAX + " REAL," +
                COLUMN_BBOX_Y_MAX + " REAL," +
                "FOREIGN KEY (" + COLUMN_IMAGE_ID + ") REFERENCES " + TABLE_IMAGES + "(" + COLUMN_IMAGE_ID + ")," +
                "FOREIGN KEY (" + COLUMN_LABEL_NAME + ") REFERENCES " + TABLE_LABELS + "(" + COLUMN_LABEL_NAME + "));";

        // Create Files table
        String createFilesTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_FILES + " (" +
                COLUMN_FILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_IMAGE_ID + " INTEGER," +
                COLUMN_FILENAME + " TEXT," +
                "FOREIGN KEY (" + COLUMN_IMAGE_ID + ") REFERENCES " + TABLE_IMAGES + "(" + COLUMN_IMAGE_ID + "));";

        db.execSQL(createImagesTableQuery);
        db.execSQL(createProjectsTableQuery);
        db.execSQL(createLabelsTableQuery);
        db.execSQL(createBBoxesTableQuery);
        db.execSQL(createFilesTableQuery);
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
        db.close();
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

    // used to delete labels associated to a project_id the user wants to delete
    public void deleteLabelsForProject(long projectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LABELS, COLUMN_PROJECT_ID + " = ?", new String[]{String.valueOf(projectId)});
    }

    // used to delete image_paths associated to a project_id the user wants to delete
    public void deleteImagesForProject(long projectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete associated bounding boxes from the "bboxes" table
        String deleteBBoxesQuery = "DELETE FROM " + TABLE_BBOXES + " WHERE " + COLUMN_IMAGE_ID + " IN " +
                "(SELECT " + COLUMN_IMAGE_ID + " FROM " + TABLE_IMAGES + " WHERE " + COLUMN_PROJECT_ID + " = ?)";
        String[] deleteBBoxesArgs = {String.valueOf(projectId)};
        db.execSQL(deleteBBoxesQuery, deleteBBoxesArgs);
        db.delete(TABLE_IMAGES, COLUMN_PROJECT_ID + " = ?", new String[]{String.valueOf(projectId)});
    }

    // Method to insert a new image path into the images table
    public long insertImagePath(String originalImagePath, String imagePath, long projectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ORIGINAL_IMAGE_PATH, originalImagePath);
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
        Log.d("DBHelper",  "******labels for project =" + labels);
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
    public long getImageIdFromOPath(String imagePath) {
        if (imagePath == null) {
            return -1;  // or handle the case where imagePath is null
        }
        long imageId = -1;
        SQLiteDatabase db = this.getReadableDatabase();

        Log.d("DBHelper", "getImageIdFromOPath: Image Path: " + imagePath);

        // Query to retrieve imageId for the given imagePath
        String[] projection = {COLUMN_IMAGE_ID};
        String selection = COLUMN_ORIGINAL_IMAGE_PATH + " = ?";
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

    public String getOriginalImagePath(String selectedImagePath) {

        String originalImagePath = null;
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {COLUMN_ORIGINAL_IMAGE_PATH};
        String selection = COLUMN_IMAGE_PATH + " = ?";
        String[] selectionArgs = {selectedImagePath};

        Cursor cursor = db.query(
                TABLE_IMAGES,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            originalImagePath = cursor.getString(cursor.getColumnIndex(COLUMN_ORIGINAL_IMAGE_PATH));
            cursor.close();
        }

        return originalImagePath;
    }
    // Method to get the count of images associated with a label in the images table
    public int getImageCountForLabel(long projectId, String labelName) {
        int count = 0;
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to retrieve the count of images associated with the label
        String query = "SELECT COUNT(*) FROM " + TABLE_IMAGES +
                " WHERE " + COLUMN_PROJECT_ID + " = ? AND " + COLUMN_SELECTED_LABEL + " = ?";
        String[] selectionArgs = {String.valueOf(projectId), labelName};

        Cursor cursor = db.rawQuery(query, selectionArgs);

        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }

        return count;
    }

    // New method to delete label and associated images
    public void deleteLabelAndImages(final long projectId, final String label) {
        performTransaction(new TransactionCallback() {
            @Override
            public void onTransaction(SQLiteDatabase db) {
                // Delete label
                db.delete(TABLE_LABELS, COLUMN_PROJECT_ID + " = ? AND " + COLUMN_LABEL_NAME + " = ?",
                        new String[]{String.valueOf(projectId), label});

                // Delete associated images
                db.delete(TABLE_IMAGES, COLUMN_PROJECT_ID + " = ? AND " + COLUMN_SELECTED_LABEL + " = ?",
                        new String[]{String.valueOf(projectId), label});
            }
        });
    }
    // Helper method to perform database transactions
    private void performTransaction(TransactionCallback callback) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            db.beginTransaction();
            callback.onTransaction(db);
            db.setTransactionSuccessful();
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    // Method to delete a bounding box from the database
    public void deleteBoundingBox(BoundingBox boundingBox) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            String tableName = TABLE_BBOXES;
            String idColumn = COLUMN_BBOX_ID;

            // get the ID of the bounding box you want to delete
            long boundingBoxId = getBoundingBoxId(db, tableName, idColumn, boundingBox);

            // Delete the bounding box from the database
            db.delete(tableName, idColumn + " = ?", new String[]{String.valueOf(boundingBoxId)});
        } finally {
            db.close(); // Close the database connection
        }
    }

    // Helper method to get the ID of a bounding box
    private long getBoundingBoxId(SQLiteDatabase db, String tableName, String idColumn, BoundingBox boundingBox) {
        String[] projection = {idColumn};
        String selection = COLUMN_BBOX_X_MIN + " = ? AND " +
                COLUMN_BBOX_Y_MIN + " = ? AND " +
                COLUMN_BBOX_X_MAX + " = ? AND " +
                COLUMN_BBOX_Y_MAX + " = ?";
        String[] selectionArgs = {
                String.valueOf(boundingBox.getLeft()),
                String.valueOf(boundingBox.getTop()),
                String.valueOf(boundingBox.getRight()),
                String.valueOf(boundingBox.getBottom())
        };

        Cursor cursor = db.query(
                tableName,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        long boundingBoxId = -1;

        try {
            if (cursor.moveToFirst()) {
                boundingBoxId = cursor.getLong(cursor.getColumnIndexOrThrow(idColumn));
            }
        } finally {
            cursor.close(); // Close the cursor
        }

        return boundingBoxId;
    }

    public int getBBoxCountForImage(long imageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;

        try {
            String query = "SELECT COUNT(*) FROM bboxes WHERE image_id = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(imageId)});

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
                cursor.close();
            }
        } finally {
            db.close();
        }

        return count;
    }

    // Interface for the callback used in database transactions
    private interface TransactionCallback {
        void onTransaction(SQLiteDatabase db);
    }
    public void insertLabel(long projectId, String labelName) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if the label already exists for the given project
        if (!labelExists(db, projectId, labelName)) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_PROJECT_ID, projectId);
            values.put(COLUMN_LABEL_NAME, labelName);

            db.insert(TABLE_LABELS, null, values);
        } else {
            Log.d("DBHelper", "Label already exists for project_id " + projectId + " and label_name " + labelName);
        }

        db.close();
    }

    private boolean labelExists(SQLiteDatabase db, long projectId, String labelName) {
        String selection = COLUMN_PROJECT_ID + " = ? AND " + COLUMN_LABEL_NAME + " = ?";
        String[] selectionArgs = {String.valueOf(projectId), labelName};

        Cursor cursor = db.query(
                TABLE_LABELS,
                null,  // Projection: null returns all columns
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        boolean labelExists = cursor != null && cursor.getCount() > 0;

        if (cursor != null) {
            cursor.close();
        }

        return labelExists;
    }
    public void updateCroppedImagePath(long imageId, String croppedImagePath) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_PATH, croppedImagePath);

        String selection = COLUMN_IMAGE_ID + " = ?";
        String[] selectionArgs = {String.valueOf(imageId)};

        db.update(TABLE_IMAGES, values, selection, selectionArgs);

        db.close();
    }

    public String getCurrentLabelForImage(long imageId) {
        String currentLabel = null;
        SQLiteDatabase db = getReadableDatabase();

        // Query to retrieve the label_name for the given imageId
        String[] projection = {COLUMN_SELECTED_LABEL};
        String selection = COLUMN_IMAGE_ID + " = ?";
        String[] selectionArgs = {String.valueOf(imageId)};

        Cursor cursor = db.query(
                TABLE_IMAGES,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        // Process the cursor and retrieve the label_name
        if (cursor != null && cursor.moveToFirst()) {
            currentLabel = cursor.getString(cursor.getColumnIndex(COLUMN_SELECTED_LABEL));
            cursor.close();
        }

        Log.d("DBHelper",  "******current label for image =" + currentLabel);
        return currentLabel;
    }


    // Method to update a label in the database when a user wants to edit the label name
    public void updateLabel(long projectId, String oldLabel, String newLabel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LABEL_NAME, newLabel);

        // Specify the WHERE clause to identify the label to be updated
        String whereClause = COLUMN_PROJECT_ID + " = ? AND " + COLUMN_LABEL_NAME + " = ?";
        String[] whereArgs = {String.valueOf(projectId), oldLabel};

        // Update the label in the database
        db.update(TABLE_LABELS, values, whereClause, whereArgs);
        db.close();
    }

    public void deleteImage(long projectId, long imageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Define the WHERE clause for the bboxes table
        String bboxSelection = COLUMN_IMAGE_ID + " = ?";
        // Define the arguments for the WHERE clause for the bboxes table
        String[] bboxSelectionArgs = {String.valueOf(imageId)};
        // Perform deletion in the bboxes table
        db.delete(TABLE_BBOXES, bboxSelection, bboxSelectionArgs);
        // define the WHERE clause
        String selection = COLUMN_PROJECT_ID + " = ? AND " + COLUMN_IMAGE_ID + " = ?";
        // define the arguments for the WHERE clause
        String[] selectionArgs = {String.valueOf(projectId), String.valueOf(imageId)};
        // perform the deletion
        db.delete(TABLE_IMAGES, selection, selectionArgs);
        // close the database
        db.close();
    }

    public void updateSelectedLabelInImages(long projectId, String oldLabel, String newLabel) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_SELECTED_LABEL, newLabel);

        // Specify the WHERE clause to identify the rows to be updated
        String whereClause = COLUMN_PROJECT_ID + " = ? AND " + COLUMN_SELECTED_LABEL + " = ?";
        String[] whereArgs = {String.valueOf(projectId), oldLabel};

        // Perform the update
        db.update(TABLE_IMAGES, values, whereClause, whereArgs);

        db.close();
    }


    public Cursor getBoundingBoxForImage(long imageId) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to retrieve bounding box coordinates for the given image ID
        String[] projection = {
                COLUMN_BBOX_X_MIN,
                COLUMN_BBOX_Y_MIN,
                COLUMN_BBOX_X_MAX,
                COLUMN_BBOX_Y_MAX
        };

        String selection = COLUMN_IMAGE_ID + " = ?";
        String[] selectionArgs = {String.valueOf(imageId)};

        return db.query(
                TABLE_BBOXES,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
    }

    // In your DBHelper class
    public float[] getBoundingBoxForExport(long imageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        float[] boundingBox = new float[4];

        // TODO: Query the database to retrieve bounding box values based on the imageId
        // Example query: "SELECT bbox_x_min, bbox_y_min, bbox_x_max, bbox_y_max FROM bboxes WHERE image_id = ?"
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_BBOX_X_MIN + ", " + COLUMN_BBOX_Y_MIN + ", " +
                COLUMN_BBOX_X_MAX + ", " + COLUMN_BBOX_Y_MAX +
                " FROM " + TABLE_BBOXES +
                " WHERE " + COLUMN_IMAGE_ID + " = ?", new String[]{String.valueOf(imageId)});

        if (cursor.moveToFirst()) {
            boundingBox[0] = cursor.getFloat(0); // x_min
            boundingBox[1] = cursor.getFloat(1); // y_min
            boundingBox[2] = cursor.getFloat(2); // x_max
            boundingBox[3] = cursor.getFloat(3); // y_max
        }

        cursor.close();
        return boundingBox;
    }

    // Method to update bounding box coordinates in the database
    public void updateBoundingBoxCoordinates(long imageId, float xMin, float yMin, float xMax, float yMax) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Put the new bounding box coordinates in the ContentValues
        values.put(COLUMN_BBOX_X_MIN, xMin);
        values.put(COLUMN_BBOX_Y_MIN, yMin);
        values.put(COLUMN_BBOX_X_MAX, xMax);
        values.put(COLUMN_BBOX_Y_MAX, yMax);

        // Update the database row for the specified imageId
        db.update(TABLE_BBOXES, values, "image_id=?", new String[]{String.valueOf(imageId)});

        // Close the database
        db.close();
    }

    // Add a method to get file names for a specific project
    public List<String> getImageFileNamesForProject(long projectId) {
        List<String> fileNames = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // Query to retrieve file names for a specific project
        String query = "SELECT " + COLUMN_FILENAME +
                " FROM " + TABLE_FILES +
                " INNER JOIN " + TABLE_IMAGES +
                " ON " + TABLE_FILES + "." + COLUMN_IMAGE_ID + " = " + TABLE_IMAGES + "." + COLUMN_IMAGE_ID +
                " WHERE " + TABLE_IMAGES + "." + COLUMN_PROJECT_ID + " = ?;";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(projectId)});

        // Iterate through the cursor and add file names to the list
        while (cursor.moveToNext()) {
            String fileName = cursor.getString(cursor.getColumnIndex(COLUMN_FILENAME));
            fileNames.add(fileName);
        }

        // Close the cursor and database
        cursor.close();
        db.close();

        return fileNames;
    }

    // Retrieve all bounding boxes associated with a specific image ID
    public List<BoundingBox> getBoundingBoxesForImage(long imageId) {
        List<BoundingBox> boundingBoxes = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        String[] columns = {
                COLUMN_BBOX_ID,
                COLUMN_BBOX_X_MIN,
                COLUMN_BBOX_Y_MIN,
                COLUMN_BBOX_X_MAX,
                COLUMN_BBOX_Y_MAX,
                COLUMN_LABEL_NAME
        };
        String selection = COLUMN_IMAGE_ID + " = ?";
        String[] selectionArgs = {String.valueOf(imageId)};

        Cursor cursor = db.query(TABLE_BBOXES, columns, selection, selectionArgs, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(COLUMN_BBOX_ID));
                float xMin = cursor.getFloat(cursor.getColumnIndex(COLUMN_BBOX_X_MIN));
                float yMin = cursor.getFloat(cursor.getColumnIndex(COLUMN_BBOX_Y_MIN));
                float xMax = cursor.getFloat(cursor.getColumnIndex(COLUMN_BBOX_X_MAX));
                float yMax = cursor.getFloat(cursor.getColumnIndex(COLUMN_BBOX_Y_MAX));
                String label = cursor.getString(cursor.getColumnIndex(COLUMN_LABEL_NAME));
                float[] coordinates = {xMin, yMin, xMax, yMax};

                BoundingBox boundingBox = new BoundingBox(coordinates,label,id);
                boundingBoxes.add(boundingBox);
            }

            cursor.close();
        }

        db.close();
        return boundingBoxes;
    }


    public long insertBoundingBox(long imageId, float xMin, float yMin, float xMax, float yMax, String label) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_IMAGE_ID, imageId);
        values.put(COLUMN_BBOX_X_MIN, xMin);
        values.put(COLUMN_BBOX_Y_MIN, yMin);
        values.put(COLUMN_BBOX_X_MAX, xMax);
        values.put(COLUMN_BBOX_Y_MAX, yMax);
        values.put(COLUMN_LABEL_NAME, label);

        long boundingBoxId = db.insert(TABLE_BBOXES, null, values);

        db.close(); // Close the database connection

        return boundingBoxId;
    }

    public void deleteBoundingBoxById(long boundingBoxId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_BBOX_ID + " = ?";
        String[] whereArgs = {String.valueOf(boundingBoxId)};
        db.delete(TABLE_BBOXES, whereClause, whereArgs);
        db.close();
    }

}



