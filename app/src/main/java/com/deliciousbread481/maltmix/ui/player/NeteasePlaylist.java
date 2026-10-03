package com.deliciousbread481.maltmix.model;

public class NeteasePlaylist {

    private final long id;
    private final String name;
    private final int trackCount;
    private final String coverUrl;

    public NeteasePlaylist(long id, String name, int trackCount, String coverUrl) {
        this.id = id;
        this.name = name;
        this.trackCount = trackCount;
        this.coverUrl = coverUrl;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public int getTrackCount() { return trackCount; }
    public String getCoverUrl() { return coverUrl; }
}