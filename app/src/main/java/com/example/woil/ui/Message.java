package com.example.woil.ui;

public class Message {
    private final String text;
    private final String time;
    private final boolean isSentByMe;

    public Message(String text, String time, boolean isSentByMe) {
        this.text = text;
        this.time = time;
        this.isSentByMe = isSentByMe;
    }

    public String getText() {
        return text;
    }

    public String getTime() {
        return time;
    }

    public boolean isSentByMe() {
        return isSentByMe;
    }
}