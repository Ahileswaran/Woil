package com.example.woil.ui;

public class JobApplicationModel {
    private final String matchId;
    private final String jobId;
    private final String workerUid;
    private final String workerName;
    private final String workerPhotoUrl;
    private final String workerRole;
    private final String workerLocationText;
    private final double workerRating;
    private final long completedJobs;
    private final String status;
    private final String timeText;
    private final String category;
    private final double distanceKm;
    private final long etaMinutes;
    private final String clientUid;
    private final String jobTitle;

    public JobApplicationModel(String matchId, String jobId, String workerUid, String workerName, String workerPhotoUrl,
                               String workerRole, String workerLocationText, double workerRating, long completedJobs,
                               String status, String timeText, String category, double distanceKm, long etaMinutes,
                               String clientUid, String jobTitle) {
        this.matchId = matchId;
        this.jobId = jobId;
        this.workerUid = workerUid;
        this.workerName = workerName;
        this.workerPhotoUrl = workerPhotoUrl;
        this.workerRole = workerRole;
        this.workerLocationText = workerLocationText;
        this.workerRating = workerRating;
        this.completedJobs = completedJobs;
        this.status = status;
        this.timeText = timeText;
        this.category = category;
        this.distanceKm = distanceKm;
        this.etaMinutes = etaMinutes;
        this.clientUid = clientUid;
        this.jobTitle = jobTitle;
    }
    public String getMatchId() { return matchId; }
    public String getJobId() { return jobId; }
    public String getWorkerUid() { return workerUid; }
    public String getWorkerName() { return workerName; }
    public String getWorkerPhotoUrl() { return workerPhotoUrl; }
    public String getWorkerRole() { return workerRole; }
    public String getWorkerLocationText() { return workerLocationText; }
    public double getWorkerRating() { return workerRating; }
    public long getCompletedJobs() { return completedJobs; }
    public String getStatus() { return status; }
    public String getTimeText() { return timeText; }
    public String getCategory() { return category; }
    public double getDistanceKm() { return distanceKm; }
    public long getEtaMinutes() { return etaMinutes; }
    public String getClientUid() { return clientUid; }
    public String getJobTitle() { return jobTitle; }
}