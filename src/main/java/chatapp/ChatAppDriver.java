package chatapp;

import java.util.Iterator;
import java.util.List;

public class ChatAppDriver {
    public static void main(String[] args) {

        ChatServer server = new ChatServer();

        User devaansh = new User("Devaansh", server);
        User james = new User("James", server);
        User adrian = new User("Adrian", server);

        server.registerUser(devaansh);
        server.registerUser(james);
        server.registerUser(adrian);

        System.out.println("\n 1. Normal messaging via Mediator");
        devaansh.sendMessage("James", "Hey James, are you coming to class today?");
        james.sendMessage("Devaansh", "Yeah, I'll be there at 7");
        adrian.sendMessage(List.of("devaansh", "james"), "Don't forget the project notes");

        devaansh.printHistory();
        james.printHistory();
        adrian.printHistory();

        System.out.println("\n 2. Block feature using Mediator");
        james.blockUser("Adrian");

        adrian.sendMessage("James", "James, did you get my last message>");
        james.printHistory();
        adrian.printHistory();

        System.out.println("\n 3. Undo last message (Memento)");
        devaansh.undoLastMessage();

        devaansh.printHistory();
        james.printHistory();

        System.out.println("\n 4. Iterator over history with a specific user");
        Iterator<Message> it = devaansh.iterator(james);
        System.out.println("Message in Devaansh's history involving James");
        while (it.hasNext()) {
            Message msg = it.next();
            System.out.println("->" + msg);
        }
        System.out.println("------DONE-----");
    }
}
