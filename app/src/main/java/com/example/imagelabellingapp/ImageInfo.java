package com.example.imagelabellingapp;

public class ImageInfo {
    private long imageId;
    private String imagePath;

    public ImageInfo(long imageId, String imagePath) {
        this.imageId = imageId;
        this.imagePath = imagePath;
    }

    public long getImageId() {
        return imageId;
    }

    public String getImagePath() {
        return imagePath;
    }
}
