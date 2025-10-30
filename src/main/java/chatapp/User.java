package chatapp;

import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

public class User implements IterableByUser {

    private final String name;
    private final ChatServer server;
    private final ChatHistory history =  new ChatHistory();

    public User(String name, ChatServer server) {
        this.name = name;
        this.server = server;
    }

    public String getName() {
        return name;
    }

    public ChatHistory getHistory() {
        return history;
    }

    public void sendMessage(String recipient, String content) {
        server.sendMessage(this, Arrays.asList(recipient), content);
    }

    public void sendMessage(List<String> recipients, String content) {
        server.sendMessage(this, recipients, content);
    }

    public void receiveMessageInternal(Message msg) {
        server.undoLastMessage(this);
    }

    public void undoLastMessage() {
        server.undoLastMessage(this);
    }

    public void blockUser(String blockeeName){
        server.blockUser(this.name, blockeeName);
    }

    public void printHistory(){
        server.printUserHistory(this);
    }

    @Override
    public Iterator<Message> iterator(User userToUserWith){
        return history.iterator(userToUserWith);
    }
}
