package model;

import jakarta.json.bind.annotation.JsonbProperty;

public class Message {
    @JsonbProperty("sender")
    private String sender;
    @JsonbProperty("content")
    private String content;
    @JsonbProperty("recipient")
    private String recipient; 
    @JsonbProperty("timestamp")
    private long timestamp;

    // Default constructor
    public Message() {
    }

    // Full constructor
    public Message(String sender, String content, long timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Constructor with User to send a message to
    public Message(String sender, String content, String recipient) {
        this.sender = sender;
        this.content = content;
        this.recipient = recipient;
        this.timestamp = System.currentTimeMillis();
    }


    public String getSender() {
        return sender;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getRecipient() {
        return recipient;
    }
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                (recipient != null ? ", recipient='" + recipient + '\'' : "") +
                '}';
    }
}
