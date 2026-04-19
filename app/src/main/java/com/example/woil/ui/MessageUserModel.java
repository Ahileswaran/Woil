package com.example.woil.ui;

public class MessageUserModel {
    private final String uid;
    private final String name;
    private final String role;
    private final String photoUrl;
    private final String lastMessage;
    private final String time;
    private final int unreadCount;
    private final boolean online;

    public MessageUserModel(String uid, String name, String role, String photoUrl,
                            String lastMessage, String time, int unreadCount, boolean online) {
        this.uid = uid;
        this.name = name;
        this.role = role;
        this.photoUrl = photoUrl;
        this.lastMessage = lastMessage;
        this.time = time;
        this.unreadCount = unreadCount;
        this.online = online;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getPhotoUrl() { return photoUrl; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getUnreadCount() { return unreadCount; }
    public boolean isOnline() { return online; }
}