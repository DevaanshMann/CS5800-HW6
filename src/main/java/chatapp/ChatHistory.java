package chatapp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChatHistory implements IterableByUser {

    private final List<Message> history = new ArrayList<>();
    private final List<MessageMemento> sentMementos = new ArrayList<>();
    private final List<Message> sentMsg = new ArrayList<>();

    public void addHistory(Message msg) {
        history.add(msg);
    }

    public void recordSentMsg(Message msg) {
        sentMsg.add(msg);
        sentMementos.add(new MessageMemento(msg.getContent(),msg.getTimestamp()));
        history.add(msg);
    }

    public Message getLastSentMessage() {
        if (sentMsg.isEmpty()) {
            return null;
        }
        return sentMsg.getLast();
    }

    public MessageMemento undoLastMemento(){
        if (sentMsg.isEmpty()) {
            return null;
        }
        return sentMementos.remove(sentMsg.size() - 1);
    }

    public void removeMessage(Message msg){
        history.remove(msg);
        sentMsg.remove(msg);
    }

    public List<Message> getAllMessages() {
        return history;
    }

    @Override
    public Iterator<Message> iterator(User userToSearchWith) {
        return new SearchMessagesByUser(history, userToSearchWith.getName());
    }
}
