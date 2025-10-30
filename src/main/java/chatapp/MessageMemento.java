package chatapp;

import java.time.Instant;

public class MessageMemento {
    private final String contentSnapshot;
    private final Instant timestamp;
    public MessageMemento(String contentSnapshot, Instant timestamp) {
        this.contentSnapshot = contentSnapshot;
        this.timestamp = timestamp;
    }
    public String getContentSnapshot() {
        return contentSnapshot;
    }
    public Instant getTimestamp() {
        return timestamp;
    }
}
