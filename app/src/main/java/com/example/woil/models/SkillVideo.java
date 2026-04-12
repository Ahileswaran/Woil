package com.example.woil.models;

import android.net.Uri;
import java.io.Serializable;

public class SkillVideo implements Serializable {

    private String id;
    private String title;
    private String category;
    private String description;
    private String status;
    private String videoUriString;

    public SkillVideo() {
    }

    public SkillVideo(String id, String title, String category, String description, String status, String videoUriString) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.status = status;
        this.videoUriString = videoUriString;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getVideoUriString() {
        return videoUriString;
    }

    public Uri getVideoUri() {
        if (videoUriString == null || videoUriString.isEmpty()) return null;
        return Uri.parse(videoUriString);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setVideoUriString(String videoUriString) {
        this.videoUriString = videoUriString;
    }
}