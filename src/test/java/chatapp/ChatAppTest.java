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

    private static PrintStream ORIG_OUT;
    private static PrintStream ORIG_ERR;

    @BeforeAll
    static void muteConsole() {
        ORIG_OUT = System.out;
        ORIG_ERR = System.err;
        PrintStream devNull = new PrintStream(OutputStream.nullOutputStream());
        System.setOut(devNull);
        System.setErr(devNull);
    }

    @AfterAll
    static void restoreConsole() {
        System.setOut(ORIG_OUT);
        System.setErr(ORIG_ERR);
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

    // ------------------------------------------------------------
    // ChatServer.registerUser / unregisterUser
    // ------------------------------------------------------------

    @Test
    void registerUser_makesUserAvailableForDelivery() {
        // Arrange done in setUp()
        // Act
        devaansh.sendMessage("James", "hello after register");

        // Assert
        boolean jamesReceived = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("hello after register"));
        assertTrue(jamesReceived,
                "James should receive messages because he is registered on the server");
    }

    @Test
    void unregisterUser_stopsFutureDeliveryToThatUser() {
        // Arrange
        server.unregisterUser(james);

        // Act
        devaansh.sendMessage("James", "you shouldn't get this after unregister");

        // Assert
        boolean jamesReceived = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getContent().contains("you shouldn't get this after unregister"));

        assertFalse(jamesReceived,
                "Unregistered user should not receive new messages through mediator");
    }

    // ------------------------------------------------------------
    // ChatServer.blockUser + User.blockUser (these are linked)
    // ------------------------------------------------------------

    @Test
    void blockUser_preventsBlockedSenderFromDeliveringMessages() {
        // Arrange
        james.blockUser("Adrian"); // James blocks Adrian

        // Act
        adrian.sendMessage("James", "James did you get my last message?");

        // Assert
        boolean jamesSawAdrian = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Adrian"));
        boolean adrianThinksHeSent = adrian.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getContent().equals("James did you get my last message?"));

        assertFalse(jamesSawAdrian,
                "Blocked sender's message should NOT show in James's history");
        assertTrue(adrianThinksHeSent,
                "Adrian still records his own outgoing message in his history");
    }

    @Test
    void blockUser_doesNotAffectMessagesFromUnblockedSenders() {
        // Arrange
        james.blockUser("Adrian"); // James only blocked Adrian

        // Act
        devaansh.sendMessage("James", "Normal message from Devaansh");

        // Assert
        boolean jamesSawDevaansh = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("Normal message from Devaansh"));

        assertTrue(jamesSawDevaansh,
                "Blocking Adrian should NOT stop messages from Devaansh");
    }

    // ------------------------------------------------------------
    // ChatServer.sendMessage / User.sendMessage
    // ------------------------------------------------------------

    @Test
    void sendMessage_oneRecipient_deliversToRecipientAndSenderHistory() {
        // Act
        devaansh.sendMessage("James", "Hey James, are you coming to class today?");

        // Assert recipient
        boolean jamesReceived = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getRecipients().contains("James")
                        && m.getContent().equals("Hey James, are you coming to class today?"));

        // Assert sender recorded own send
        boolean senderRecorded = devaansh.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("Hey James, are you coming to class today?"));

        assertTrue(jamesReceived,
                "James should receive the message through the mediator");
        assertTrue(senderRecorded,
                "Sender should keep a copy of the sent message in their history");
    }

    @Test
    void sendMessage_multiRecipient_deliversToAllNonBlockedRecipients() {
        // Arrange
        james.blockUser("Adrian"); // James blocks Adrian
        // Now Adrian will try to message both James and Devaansh

        // Act
        adrian.sendMessage(List.of("James", "Devaansh"),
                "Group ping: meeting at 7?");

        // Assert:
        boolean jamesSawAdrian = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Adrian")
                        && m.getContent().equals("Group ping: meeting at 7?"));

        boolean devaanshSawAdrian = devaansh.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Adrian")
                        && m.getContent().equals("Group ping: meeting at 7?"));

        assertFalse(jamesSawAdrian,
                "James blocked Adrian, so James should NOT see Adrian's group message");
        assertTrue(devaanshSawAdrian,
                "Devaansh did NOT block Adrian, so he should see the message");
    }

    // ------------------------------------------------------------
    // ChatServer.undoLastMessage / User.undoLastMessage (Memento)
    // ------------------------------------------------------------

    @Test
    void undoLastMessage_removesMessageFromRecipientAndLeavesSystemNotice() {
        // Arrange: Devaansh sends to James
        devaansh.sendMessage("James", "I might be late today.");

        // Sanity precondition: James initially has that message
        assertTrue(
                james.getHistory().getAllMessages().stream().anyMatch(
                        m -> m.getSender().equals("Devaansh")
                                && m.getContent().equals("I might be late today.")
                ),
                "Precondition: James should see Devaansh's message before undo"
        );

        // Act
        devaansh.undoLastMessage();

        // Assert: James should NOT still have original, but SHOULD have SYSTEM notice
        boolean jamesStillHasOriginal = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("I might be late today."));

        boolean jamesHasSystemRetract = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("SYSTEM")
                        && m.getContent().contains("Message retracted by Devaansh"));

        assertFalse(jamesStillHasOriginal,
                "After undo, James should NOT see the original message from Devaansh");
        assertTrue(jamesHasSystemRetract,
                "After undo, James SHOULD see a SYSTEM retraction notice");
    }

    @Test
    void undoLastMessage_onlyUndoesMostRecentMessageNotOlderOnes() {
        // Arrange: Devaansh sends 2 messages to James
        devaansh.sendMessage("James", "First message");
        devaansh.sendMessage("James", "Second message");

        // Act: undo once (should remove "Second message", not "First message")
        devaansh.undoLastMessage();

        // Assert: "Second message" should be retracted but "First message" should remain visible
        boolean jamesHasFirst = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("First message"));

        boolean jamesHasSecond = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("Devaansh")
                        && m.getContent().equals("Second message"));

        boolean jamesHasSystemForSecond = james.getHistory().getAllMessages().stream()
                .anyMatch(m -> m.getSender().equals("SYSTEM")
                        && m.getContent().contains("Message retracted by Devaansh"));

        assertTrue(jamesHasFirst,
                "Undo of last message should NOT remove the earlier message");
        assertFalse(jamesHasSecond,
                "The most recent message ('Second message') should be removed after undo");
        assertTrue(jamesHasSystemForSecond,
                "A SYSTEM retraction notice should appear after undo");
    }

    // Edge case: undo with nothing sent should not blow up.
    @Test
    void undoLastMessage_whenNoMessagesDoesNotThrow() {
        assertDoesNotThrow(
                () -> james.undoLastMessage(),
                "Undoing with no sent messages should not throw an exception"
        );
    }

    // ------------------------------------------------------------
    // iterator(User) from User / ChatHistory (Iterator pattern)
    // ------------------------------------------------------------

    @Test
    void iterator_returnsOnlyConversationBetweenTwoSpecificUsers() {
        // Arrange: multiple messages between different pairs
        devaansh.sendMessage("James", "Msg A to James");
        devaansh.sendMessage("Adrian", "Msg B to Adrian");
        james.sendMessage("Devaansh", "Reply C to Devaansh");
        adrian.sendMessage("James", "Adrian to James (should maybe be blocked later?)");

        // Act: iterate over "what conversations involve James" from Devaansh's perspective
        Iterator<Message> it = devaansh.iterator(james);

        // Collect from iterator
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

        // Assert
        assertTrue(sawMsgAToJames,
                "Iterator should include messages sent from Devaansh to James");
        assertTrue(sawReplyCFromJames,
                "Iterator should include messages sent from James to Devaansh");
        assertFalse(sawMsgBToAdrian,
                "Iterator should NOT include messages unrelated to James");
    }

    @Test
    void iterator_isEmptyIfNoSharedMessages() {
        // Arrange:
        // James and Adrian talk.
        james.sendMessage("Adrian", "Yo Adrian");
        adrian.sendMessage("James", "Sup James");

        // But we will iterate from Devaansh's side ABOUT James.
        Iterator<Message> it = devaansh.iterator(james);

        // Act
        boolean hasAny = it.hasNext();

        // Assert
        assertFalse(hasAny,
                "If two users never exchanged messages with this user, iterator should be empty");
    }

    // ------------------------------------------------------------
    // printUserHistory / printMyHistory
    // (not super assertable without intercepting stdout, but we can assert no-throw
    // and that histories match expectation before/after calls)
    // ------------------------------------------------------------

    @Test
    void printMyHistory_doesNotThrowAndReflectsCurrentMessages() {
        // Arrange
        adrian.sendMessage("James", "hello james");
        adrian.sendMessage("Devaansh", "hello dev");

        // Act / Assert
        assertDoesNotThrow(
                () -> adrian.printHistory(),
                "printMyHistory should not throw"
        );

        // sanity check: Adrian should see both messages he sent in his own history
        long countVisible = adrian.getHistory().getAllMessages().stream()
                .filter(m -> m.getSender().equals("Adrian"))
                .count();
        assertEquals(2, countVisible,
                "Adrian's history should include the messages he sent");
    }

    @Test
    void printUserHistory_doesNotMutateHistory() {
        // Arrange
        devaansh.sendMessage("James", "Before print!");
        int beforeSize = james.getHistory().getAllMessages().size();

        // Act: print James' history through server
        assertDoesNotThrow(
                () -> server.printUserHistory(james),
                "printUserHistory should not throw or mutate anything"
        );

        int afterSize = james.getHistory().getAllMessages().size();

        // Assert
        assertEquals(beforeSize, afterSize,
                "Printing history should not change the stored history");
    }
}

