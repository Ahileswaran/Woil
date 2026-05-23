
package com.example.woil.ui;

public class NotificationItem {
    private final String id;
    private final String title;
    private final String body;
    private final String timeText;
    private final String targetType;
    private final String targetId;
    public NotificationItem(String id, String title, String body, String timeText, String targetType, String targetId) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.timeText = timeText;
        this.targetType = targetType;
        this.targetId = targetId;
    }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getTimeText() { return timeText; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
}
