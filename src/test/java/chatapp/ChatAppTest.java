package chatapp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ChatAppTest {

    private static PrintStream originalOut;
    private static PrintStream originalErr;

    @BeforeAll
    static void muteConsole() {
        originalOut = System.out;
        originalErr = System.err;
        PrintStream devNull = new PrintStream(OutputStream.nullOutputStream());
        System.setOut(devNull);
        System.setErr(devNull);
    }

    @AfterAll
    static void restoreConsole() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private ChatServer server;
    private User devaansh;
    private User james;
    private User adrian;

    @BeforeEach
    void setUp() {
        server = new ChatServer();

        devaansh = new User("Devaansh", server);
        james    = new User("James", server);
        adrian   = new User("Adrian", server);

        server.registerUser(devaansh);
        server.registerUser(james);
        server.registerUser(adrian);
    }

    @Test
    void registerUser_makesUserAvailableForDelivery() {
        devaansh.sendMessage("James", "hello after register");

        boolean jamesReceived = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("hello after register"));
        assertTrue(jamesReceived, "James should receive messages because he is registered on the server");
    }

    @Test
    void unregisterUser_stopsFutureDeliveryToThatUser() {
        server.unregisterUser(james);

        devaansh.sendMessage("James", "you shouldn't get this after unregister");

        boolean jamesReceived = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getContent().contains("you shouldn't get this after unregister"));

        assertFalse(jamesReceived, "Unregistered user should not receive new messages through mediator");
    }

    @Test
    void blockUser_preventsBlockedSenderFromDeliveringMessages() {
        james.blockUser("Adrian");

        adrian.sendMessage("James", "James did you get my last message?");

        boolean jamesSawAdrian = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Adrian"));
        boolean adrianThinksHeSent = adrian.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getContent().equals("James did you get my last message?"));

        assertFalse(jamesSawAdrian, "Blocked sender's message should NOT show in James's history");
        assertTrue(adrianThinksHeSent, "Adrian still records his own outgoing message in his history");
    }

    @Test
    void blockUser_doesNotAffectMessagesFromUnblockedSenders() {
        james.blockUser("Adrian");

        devaansh.sendMessage("James", "Normal message from Devaansh");

        boolean jamesSawDevaansh = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("Normal message from Devaansh"));

        assertTrue(jamesSawDevaansh, "Blocking Adrian should NOT stop messages from Devaansh");
    }

    @Test
    void sendMessage_oneRecipient_deliversToRecipientAndSenderHistory() {
        devaansh.sendMessage("James", "Hey James, are you coming to class today?");

        boolean jamesReceived = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getRecipients().contains("James")
                        && m.getContent().equals("Hey James, are you coming to class today?"));

        boolean senderRecorded = devaansh.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("Hey James, are you coming to class today?"));

        assertTrue(jamesReceived, "James should receive the message through the mediator");
        assertTrue(senderRecorded, "Sender should keep a copy of the sent message in their history");
    }

    @Test
    void sendMessage_multiRecipient_deliversToAllNonBlockedRecipients() {
        james.blockUser("Adrian");

        adrian.sendMessage(List.of("James", "Devaansh"),
                "Group ping: meeting at 7?");

        boolean jamesSawAdrian = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Adrian")
                        && m.getContent().equals("Group ping: meeting at 7?"));

        boolean devaanshSawAdrian = devaansh.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Adrian")
                        && m.getContent().equals("Group ping: meeting at 7?"));

        assertFalse(jamesSawAdrian, "James blocked Adrian, so James should NOT see Adrian's group message");
        assertTrue(devaanshSawAdrian, "Devaansh did NOT block Adrian, so he should see the message");
    }

    @Test
    void undoLastMessage_removesMessageFromRecipientAndLeavesSystemNotice() {
        devaansh.sendMessage("James", "I might be late today.");

        assertTrue(
                james.getHistory().getAllMessages().stream().anyMatch(
                        m -> m.getSender().equals("Devaansh")
                                && m.getContent().equals("I might be late today.")),
                        "Precondition: James should see Devaansh's message before undo"
        );

        devaansh.undoLastMessage();

        boolean jamesStillHasOriginal = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("I might be late today."));

        boolean jamesHasSystemRetract = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("SYSTEM")
                        && m.getContent().contains("Message retracted by Devaansh"));

        assertFalse(jamesStillHasOriginal, "After undo, James should NOT see the original message from Devaansh");
        assertTrue(jamesHasSystemRetract, "After undo, James SHOULD see a SYSTEM retraction notice");
    }

    @Test
    void undoLastMessage_onlyUndoesMostRecentMessageNotOlderOnes() {
        devaansh.sendMessage("James", "First message");
        devaansh.sendMessage("James", "Second message");

        devaansh.undoLastMessage();

        boolean jamesHasFirst = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("First message"));

        boolean jamesHasSecond = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("Second message"));

        boolean jamesHasSystemForSecond = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("SYSTEM")
                        && m.getContent().contains("Message retracted by Devaansh"));

        assertTrue(jamesHasFirst, "Undo of last message should NOT remove the earlier message");
        assertFalse(jamesHasSecond, "The most recent message ('Second message') should be removed after undo");
        assertTrue(jamesHasSystemForSecond, "A SYSTEM retraction notice should appear after undo");
    }

    @Test
    void undoLastMessage_whenNoMessagesDoesNotThrow() {
        assertDoesNotThrow(
                () -> james.undoLastMessage(), "Undoing with no sent messages should not throw an exception"
        );
    }

    @Test
    void iterator_returnsOnlyConversationBetweenTwoSpecificUsers() {
        devaansh.sendMessage("James", "Msg A to James");
        devaansh.sendMessage("Adrian", "Msg B to Adrian");
        james.sendMessage("Devaansh", "Reply C to Devaansh");
        adrian.sendMessage("James", "Adrian to James (should maybe be blocked later?)");

        Iterator<Message> it = devaansh.iterator(james);

        boolean sawMsgAToJames = false;
        boolean sawReplyCFromJames = false;
        boolean sawMsgBToAdrian = false;

        while (it.hasNext()) {
            Message m = it.next();
            if (m.getContent().equals("Msg A to James")) {
                sawMsgAToJames = true;
            }
            if (m.getContent().equals("Reply C to Devaansh")) {
                sawReplyCFromJames = true;
            }
            if (m.getContent().equals("Msg B to Adrian")) {
                sawMsgBToAdrian = true;
            }
        }

        assertTrue(sawMsgAToJames, "Iterator should include messages sent from Devaansh to James");
        assertTrue(sawReplyCFromJames, "Iterator should include messages sent from James to Devaansh");
        assertFalse(sawMsgBToAdrian, "Iterator should NOT include messages unrelated to James");
    }

    @Test
    void iterator_isEmptyIfNoSharedMessages() {
        james.sendMessage("Adrian", "Yo Adrian");
        adrian.sendMessage("James", "Sup James");

        Iterator<Message> it = devaansh.iterator(james);

        boolean hasAny = it.hasNext();

        assertFalse(hasAny, "If two users never exchanged messages with this user, iterator should be empty");
    }

    @Test
    void printMyHistory_doesNotThrowAndReflectsCurrentMessages() {
        adrian.sendMessage("James", "hello james");
        adrian.sendMessage("Devaansh", "hello dev");

        assertDoesNotThrow(
                () -> adrian.printHistory(), "printMyHistory should not throw"
        );

        long countVisible = adrian.getHistory().getAllMessages().stream()
                .filter(m -> m.getSender().equals("Adrian"))
                .count();
        assertEquals(2, countVisible, "Adrian's history should include the messages he sent");
    }

    @Test
    void printUserHistory_doesNotMutateHistory() {
        devaansh.sendMessage("James", "Before print!");
        int beforeSize = james.getHistory().getAllMessages().size();

        assertDoesNotThrow(
                () -> server.printUserHistory(james), "printUserHistory should not throw or mutate anything"
        );

        int afterSize = james.getHistory().getAllMessages().size();

        assertEquals(beforeSize, afterSize, "Printing history should not change the stored history");
    }
}

