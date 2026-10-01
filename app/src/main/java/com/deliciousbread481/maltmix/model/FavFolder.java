package com.deliciousbread481.maltmix.model;

public class FavFolder {

    private final long id;
    private final String title;
    private final int mediaCount;

    public FavFolder(long id, String title, int mediaCount) {
        this.id = id;
        this.title = title;
        this.mediaCount = mediaCount;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public int getMediaCount() { return mediaCount; }

    @Override
    public String toString() {
        return title + " (" + mediaCount + ")";
    }
}