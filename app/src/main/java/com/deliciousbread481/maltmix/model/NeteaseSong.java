package com.deliciousbread481.maltmix.model;

public class NeteaseSong {

    private String id;
    private String name;
    private String artist;
    private String album;
    private String coverUrl;
    private long duration;

    public NeteaseSong(String id, String name, String artist,
                       String album, String coverUrl, long duration) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.coverUrl = coverUrl;
        this.duration = duration;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getCoverUrl() { return coverUrl; }
    public long getDuration() { return duration; }
}