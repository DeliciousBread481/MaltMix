package com.deliciousbread481.maltmix.model;

import java.util.Objects;

public class Song {
    private String id;
    private String title;
    private String artist;
    private String coverUrl;
    private String playUrl;
    private String source;

    public Song(String id, String title, String artist,
                String coverUrl, String playUrl, String source) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.playUrl = playUrl;
        this.source = source;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getCoverUrl() { return coverUrl; }
    public String getPlayUrl() { return playUrl; }
    public String getSource() { return source; }

    public void setPlayUrl(String playUrl) { this.playUrl = playUrl; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return id.equals(song.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}