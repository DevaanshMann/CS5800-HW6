package chatapp;

import java.time.Instant;
import java.util.*;

public class ChatServer {

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Set<String>> blockMap = new HashMap<>();

    public void registerUser(User usr){
        users.put(usr.getName(), usr);
        blockMap.putIfAbsent(usr.getName(), new HashSet<>());
        System.out.println("[SERVER] Registered user: " + usr.getName());
    }

    public void unregisterUser(User usr){
        users.remove(usr.getName());
        blockMap.remove(usr.getName());
        System.out.println("[SERVER] Unregistered user: " + usr.getName());
    }

    public void blockUser(String blockerName, String blockeeName){
        blockMap.putIfAbsent(blockerName, new HashSet<>());
        blockMap.get(blockerName).add(blockeeName);
        System.out.println("[SERVER] " + blockerName + " has blocked " + blockerName);
    }

//     NEED TO ADD?
//    public void unblockUser(String blockerName, String blockeeName){}

    public boolean isBlocked(String receiverName, String senderName){
        Set<String> blockedSenders = blockMap.getOrDefault(receiverName, Collections.emptySet());
        return blockedSenders.contains(senderName);
    }

    public void sendMessage(User sender, List<String> recipientNames, String content){
        Instant ts = Instant.now();
        Message msg = new Message(sender.getName(), recipientNames, ts, content);

        sender.getHistory().recordSentMsg(msg);

        for(String recName : recipientNames){
            User rec = users.get(recName);
            if(rec == null) continue;

            if(isBlocked(recName, sender.getName())) {
                System.out.println("[SERVER] Message from " + sender.getName() + " to " + recName + " BLOCKED.");
                continue;
            }
            rec.receiveMessageInternal(msg);
        }
        System.out.println("[SERVER] Delivered: " + msg);
    }

    public void undoLastMessage(User sender){
        Message last = sender.getHistory().getLastSentMessage();
        if(last == null) {
            System.out.println("[SERVER] Nothing to undo for: " + sender.getName());
            return;
        }

        MessageMemento snap = sender.getHistory().undoLastMemento();
        System.out.println("[SERVER] Undo request by " + sender.getName() + " for message \"" + snap.getContentSnapshot() + "\" at " + snap.getTimestamp());

        for(String recName : last.getRecipients()) {
            User rec = users.get(recName);
            if(rec == null) continue;

            rec.getHistory().removeMessage(last);

            Message sysNotice = new Message("SYSTEM", Collections.singletonList(recName), java.time.Instant.now(), "(Message retracted by " + sender.getName() + " from " + rec.getName() + ")");
            rec.receiveMessageInternal(sysNotice);
        }
        sender.getHistory().removeMessage(last);
        System.out.println("[SERVER] Message undone for " + sender.getName());
    }

    public void printUserHistory(User usr){
        System.out.println("---- Chat history for " + usr.getName() + " ----");
        for(Message msg : usr.getHistory().getAllMessages()){
            System.out.println(msg);
        }
        System.out.println("-----------------------------------------------------------");
    }
}
