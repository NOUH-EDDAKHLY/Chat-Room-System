package com.example.chatroom.provider;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebService(
        serviceName = "ChatRoomService",
        portName = "ChatRoomPort",
        targetNamespace = "http://example.com/chatroom")
public class ChatRoom {
    private final Map<String, String> users = new HashMap<>();
    private final Map<String, String> sessions = new HashMap<>();
    private final Map<String, Room> rooms = new HashMap<>();

    @WebMethod
    public synchronized RegisterResult register(
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {
        String validationMessage = validateCredentials(username, password);
        if (validationMessage != null) {
            return new RegisterResult(false, validationMessage);
        }
        if (users.containsKey(username)) {
            return new RegisterResult(false, "Username is already registered.");
        }
        users.put(username, password);
        return new RegisterResult(true, "Account created.");
    }

    @WebMethod
    public synchronized LoginResult login(
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {
        String validationMessage = validateCredentials(username, password);
        if (validationMessage != null) {
            return new LoginResult(false, validationMessage, null);
        }
        String savedPassword = users.get(username);
        if (savedPassword == null || !savedPassword.equals(password)) {
            return new LoginResult(false, "Invalid username or password.", null);
        }
        String token = randomToken();
        sessions.put(token, username);
        return new LoginResult(true, "Login successful.", token);
    }

    @WebMethod
    public synchronized OperationResult createRoom(
            @WebParam(name = "token") String token,
            @WebParam(name = "name") String name,
            @WebParam(name = "description") String description) {
        if (authenticated(token) == null) return failure("Authentication required.");
        if (blank(name) || blank(description)) {
            return failure("Room name and description are required.");
        }
        if (rooms.values().stream().anyMatch(room -> room.name.equals(name))) {
            return failure("Room name must be unique.");
        }
        String id = UUID.randomUUID().toString();
        rooms.put(id, new Room(id, name, description));
        return success("Room created.", id);
    }

    @WebMethod
    public synchronized OperationResult listRooms(@WebParam(name = "token") String token) {
        if (authenticated(token) == null) return failure("Authentication required.");
        StringBuilder message = new StringBuilder();
        rooms.values().forEach(room -> message
                .append(room.id).append(" | ")
                .append(room.name).append(" | ")
                .append(room.description).append(" | members: ")
                .append(room.members.size()).append('\n'));
        return success(message.length() == 0 ? "No rooms found." : message.toString(), null);
    }

    @WebMethod
    public synchronized OperationResult joinRoom(
            @WebParam(name = "token") String token,
            @WebParam(name = "roomId") String roomId) {
        String username = authenticated(token);
        Room room = rooms.get(roomId);
        if (username == null) return failure("Authentication required.");
        if (room == null) return failure("Room not found.");
        room.members.add(username);
        return success("Joined room.", null);
    }

    @WebMethod
    public synchronized OperationResult listUsers(
            @WebParam(name = "token") String token,
            @WebParam(name = "roomId") String roomId) {
        String username = authenticated(token);
        Room room = rooms.get(roomId);
        if (username == null) return failure("Authentication required.");
        if (room == null || !room.members.contains(username)) {
            return failure("You must join the room first.");
        }
        return success(room.members.isEmpty() ? "No users in room."
                : String.join(", ", room.members), null);
    }

    @WebMethod
    public synchronized OperationResult listMessages(
            @WebParam(name = "token") String token,
            @WebParam(name = "roomId") String roomId) {
        String username = authenticated(token);
        Room room = rooms.get(roomId);
        if (username == null) return failure("Authentication required.");
        if (room == null || !room.members.contains(username)) {
            return failure("You must join the room first.");
        }
        String messages = room.messages.stream()
                .filter(message -> visibleTo(message, username))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No messages found.");
        return success(messages, null);
    }

    @WebMethod
    public synchronized OperationResult broadcast(
            @WebParam(name = "token") String token,
            @WebParam(name = "roomId") String roomId,
            @WebParam(name = "text") String text) {
        String sender = authenticated(token);
        Room room = rooms.get(roomId);
        if (sender == null) return failure("Authentication required.");
        if (room == null || !room.members.contains(sender)) {
            return failure("You must join the room first.");
        }
        if (blank(text)) return failure("Message text is required.");
        room.messages.add(sender + ": " + text);
        return success("Message broadcast.", null);
    }

    @WebMethod
    public synchronized OperationResult privateMessage(
            @WebParam(name = "token") String token,
            @WebParam(name = "roomId") String roomId,
            @WebParam(name = "recipient") String recipient,
            @WebParam(name = "text") String text) {
        String sender = authenticated(token);
        Room room = rooms.get(roomId);
        if (sender == null) return failure("Authentication required.");
        if (room == null || !room.members.contains(sender)) {
            return failure("You must join the room first.");
        }
        if (blank(recipient) || !room.members.contains(recipient)) {
            return failure("Recipient must be a user in the joined room.");
        }
        if (blank(text)) return failure("Message text is required.");
        room.messages.add("[private] " + sender + " -> " + recipient + ": " + text);
        return success("Private message sent.", null);
    }

    @WebMethod
    public synchronized OperationResult leaveRoom(
            @WebParam(name = "token") String token,
            @WebParam(name = "roomId") String roomId) {
        String username = authenticated(token);
        Room room = rooms.get(roomId);
        if (username == null) return failure("Authentication required.");
        if (room == null || !room.members.remove(username)) {
            return failure("You are not a member of this room.");
        }
        return success("Left room.", null);
    }

    @WebMethod
    public synchronized OperationResult logout(@WebParam(name = "token") String token) {
        return sessions.remove(token) == null
                ? failure("Invalid or expired token.")
                : success("Logged out.", null);
    }

    private String authenticated(String token) {
        return token == null ? null : sessions.get(token);
    }

    private String randomToken() {
        return UUID.randomUUID().toString();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String validateCredentials(String username, String password) {
        boolean usernameEmpty = blank(username);
        boolean passwordEmpty = blank(password);

        if (usernameEmpty && passwordEmpty) {
            return "Username and password cannot be empty or blank.";
        }
        if (usernameEmpty) {
            return "Username cannot be empty or blank.";
        }
        if (passwordEmpty) {
            return "Password cannot be empty or blank.";
        }
        return null;
    }

    private static OperationResult success(String message, String value) {
        return new OperationResult(true, message, value);
    }

    private static OperationResult failure(String message) {
        return new OperationResult(false, message, null);
    }

    private static final class Room {
        private final String id;
        private final String name;
        private final String description;
        private final java.util.Set<String> members = new HashSet<>();
        private final List<String> messages = new ArrayList<>();

        private Room(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
    }

    private static boolean visibleTo(String message, String username) {
        if (!message.startsWith("[private] ")) return true;
        int arrow = message.indexOf(" -> ");
        int colon = message.indexOf(": ", arrow);
        if (arrow < 0 || colon < 0) return false;
        String sender = message.substring("[private] ".length(), arrow);
        String recipient = message.substring(arrow + " -> ".length(), colon);
        return username.equals(sender) || username.equals(recipient);
    }
}
