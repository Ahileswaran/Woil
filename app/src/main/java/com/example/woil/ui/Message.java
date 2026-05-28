package com.example.woil.ui;

public class Message {
    private final String id;
    private final String text;
    private final String time;
    private final boolean sentByMe;
    private final String senderUid;
    private final String senderPhotoUrl;
    private final long createdAtMillis;
    private final String type;

    public Message(String id,
                   String text,
                   String time,
                   boolean sentByMe,
                   String senderUid,
                   String senderPhotoUrl,
                   long createdAtMillis,
                   String type) {
        this.id = id;
        this.text = text;
        this.time = time;
        this.sentByMe = sentByMe;
        this.senderUid = senderUid;
        this.senderPhotoUrl = senderPhotoUrl;
        this.createdAtMillis = createdAtMillis;
        this.type = type == null ? "text" : type;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getTime() {
        return time;
    }

    public boolean isSentByMe() {
        return sentByMe;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public String getSenderPhotoUrl() {
        return senderPhotoUrl;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public String getType() {
        return type;
    }
}
