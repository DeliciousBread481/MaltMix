package com.deliciousbread481.maltmix.model;

public class FavVideo {

    private final String bvid;
    private final long cid;
    private final String title;
    private final String cover;
    private final String upperName;
    private final int duration;

    public FavVideo(String bvid, long cid, String title,
                    String cover, String upperName, int duration) {
        this.bvid = bvid;
        this.cid = cid;
        this.title = title;
        this.cover = cover;
        this.upperName = upperName;
        this.duration = duration;
    }

    public String getBvid() { return bvid; }
    public long getCid() { return cid; }
    public String getTitle() { return title; }
    public String getCover() { return cover; }
    public String getUpperName() { return upperName; }
    public int getDuration() { return duration; }
}