package com.example.travellog.utils;

import com.google.firebase.Timestamp;

public class TravelPost {
    private String title;
    private String description;
    private String ownerUid;
    private String ownerNickname;
    private Timestamp createdAt;

    public TravelPost() {}

    public TravelPost(String title, String description, String ownerUid, String ownerNickname,Timestamp createdAt) {
        this.title = title;
        this.description = description;
        this.ownerUid = ownerUid;
        this.ownerNickname = ownerNickname;
        this.createdAt = createdAt;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }
    public String getOwnerNickname() { return ownerNickname; }
    public void setOwnerNickname(String ownerNickname) { this.ownerNickname = ownerNickname; }
}
