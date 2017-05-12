package com.example.livechatapp;

/**
 * Created by nq57153 on 12/29/2016.
 */

public class Message {

    private String username;
    private long sendTime;
    private String chatMessage;
    private String messageType = "null";
    private boolean isComplete = false; //Ensures that even if it is somehow unmodified, the app doesn't crash.

    public Message() {}

    public Message(String username, long sendTime, String chatMessage, String messageType, boolean isComplete) {
        this.username = username;
        this.sendTime = sendTime;
        this.chatMessage = chatMessage;
        this.messageType = messageType;
        this.isComplete = isComplete;
    }

    //The following three methods are for getting the three values.
    public String getUsername() {
        return username;
    }

    public long getSendTime() {
        return sendTime;
    }

    public String getChatMessage() {
        return chatMessage;
    }

    public String getMessageType() {
        return messageType;
    }

    public boolean getIsComplete() {
        return isComplete;
    }

}
