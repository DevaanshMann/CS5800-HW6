package chatapp;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class SearchMessagesByUser implements Iterator<Message> {

    private final List<Message> allHistory;
    private final String otherUserName;
    private int currentIndex = 0;
    private Message nextMatch = null;

    public SearchMessagesByUser(List<Message> allHistory, String otherUserName) {
        this.allHistory = allHistory;
        this.otherUserName = otherUserName;
        advanceToNextMatch();
    }

    public void advanceToNextMatch() {
        nextMatch = null;

        while (currentIndex < allHistory.size()) {
            Message candidate = allHistory.get(currentIndex);
            boolean involvesOtherUser = candidate.getSender().equals(otherUserName) || candidate.getRecipients().contains(otherUserName);
            if (involvesOtherUser) {
                nextMatch = candidate;
                break;
            }
            currentIndex++;
        }
    }
    @Override
    public boolean hasNext() {
        return nextMatch != null;
    }

    @Override
    public Message next() {
        if(nextMatch == null) {
            throw new NoSuchElementException();
        }
        Message result = nextMatch;
        currentIndex++;
        advanceToNextMatch();
        return result;
    }
}
