package com.example.woil;

import com.google.firebase.Timestamp;

public class JobModel {
    public String id;
    public String clientUid;
    public String title;
    public String category;
    public String locationText;
    public Double lat;
    public Double lng;
    public Timestamp startAt;
    public Timestamp endAt;
    public Double wageSuggested;
    public String wageSuggestedText;
    public String description;
    public String assignedUid;
    public String status;
    public Boolean allowOffers;
    public Timestamp createdAt;

    // empty constructor required for Firestore deserialization if used
    public JobModel() {}
}