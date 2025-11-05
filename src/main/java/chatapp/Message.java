package chatapp;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class Message {
    private final String sender;
    private final List<String> recipients;
    private final Instant timestamp;
    private final String content;

    public Message(String sender, List<String> recipients, Instant timestamp, String content) {
        this.sender = sender;
        this.recipients = recipients;
        this.timestamp = timestamp;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }
    public List<String> getRecipients() {
        return recipients;
    }
    public Instant getTimestamp() {
        return timestamp;
    }
    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        String recip = recipients.stream().collect(Collectors.joining(", "));
        return "[" + timestamp + "] " + sender + " -> [" + recip + "]: " + content;
    }
}
