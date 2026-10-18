import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class ChatRoomClient {
    private static final String ENDPOINT = "http://localhost:9000/chat-room";
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String CHAT_NS = "http://example.com/chatroom";

    private final Scanner scanner = new Scanner(System.in);
    private String token;
    private String username;
    private String roomId;

    public static void main(String[] args) {
        new ChatRoomClient().run();
    }

    private void run() {
        System.out.println("Chat Room SOAP client");
        System.out.println("Server: " + ENDPOINT);
        while (true) {
            System.out.println("\n1. Register\n2. Login\n3. Create room\n4. List rooms");
            System.out.println("5. Join room\n6. List users\n7. Broadcast message");
            System.out.println("8. Private message\n9. List messages\n10. Leave room");
            System.out.println("11. Logout\n0. Exit");
            String choice = ask("Choose an option: ");
            try {
                switch (choice) {
                    case "1" -> register();
                    case "2" -> login();
                    case "3" -> createRoom();
                    case "4" -> listRooms();
                    case "5" -> joinRoom();
                    case "6" -> listUsers();
                    case "7" -> broadcast();
                    case "8" -> privateMessage();
                    case "9" -> listMessages();
                    case "10" -> leaveRoom();
                    case "11" -> logout();
                    case "0" -> {
                        System.out.println("Client closed.");
                        return;
                    }
                    default -> System.out.println("Please choose a number from the menu.");
                }
            } catch (Exception exception) {
                System.out.println("Request failed: " + exception.getMessage());
            }
        }
    }

    private void register() throws Exception {
        String user = ask("Username: ");
        String password = ask("Password: ");
        if (user.isBlank() && password.isBlank()) {
            System.out.println("Username and password cannot be empty or blank.");
            return;
        }
        if (user.isBlank()) {
            System.out.println("Username cannot be empty or blank.");
            return;
        }
        if (password.isBlank()) {
            System.out.println("Password cannot be empty or blank.");
            return;
        }
        show(request("register", tag("username", user) + tag("password", password)));
    }

    private void login() throws Exception {
        String user = ask("Username: ");
        String password = ask("Password: ");
        if (user.isBlank() && password.isBlank()) {
            System.out.println("Username and password cannot be empty or blank.");
            return;
        }
        if (user.isBlank()) {
            System.out.println("Username cannot be empty or blank.");
            return;
        }
        if (password.isBlank()) {
            System.out.println("Password cannot be empty or blank.");
            return;
        }
        Result result = request("login", tag("username", user) + tag("password", password));
        show(result);
        if (result.success && result.value != null) {
            username = user;
            token = result.value;
            roomId = null;
        }
    }

    private void createRoom() throws Exception {
        if (!loggedIn()) return;
        Result result = request("createRoom", tag("token", token)
                + tag("name", ask("Room name: "))
                + tag("description", ask("Description: ")));
        show(result);
        if (result.value != null) System.out.println("Room ID: " + result.value);
    }

    private void listRooms() throws Exception {
        if (loggedIn()) show(request("listRooms", tag("token", token)));
    }

    private void joinRoom() throws Exception {
        if (!loggedIn()) return;
        String selected = ask("Room ID: ");
        Result result = request("joinRoom", tag("token", token) + tag("roomId", selected));
        show(result);
        if (result.success) roomId = selected;
    }

    private void listUsers() throws Exception {
        if (inRoom()) show(request("listUsers", tag("token", token) + tag("roomId", roomId)));
    }

    private void broadcast() throws Exception {
        if (inRoom()) show(request("broadcast", tag("token", token)
                + tag("roomId", roomId) + tag("text", ask("Message: "))));
    }

    private void privateMessage() throws Exception {
        if (inRoom()) show(request("privateMessage", tag("token", token)
                + tag("roomId", roomId)
                + tag("recipient", ask("Recipient username: "))
                + tag("text", ask("Message: "))));
    }

    private void listMessages() throws Exception {
        if (inRoom()) show(request("listMessages",
                tag("token", token) + tag("roomId", roomId)));
    }

    private void leaveRoom() throws Exception {
        if (!inRoom()) return;
        Result result = request("leaveRoom", tag("token", token) + tag("roomId", roomId));
        show(result);
        if (result.success) roomId = null;
    }

    private void logout() throws Exception {
        if (!loggedIn()) return;
        Result result = request("logout", tag("token", token));
        show(result);
        if (result.success) {
            token = null;
            username = null;
            roomId = null;
        }
    }

    private boolean loggedIn() {
        if (token == null) System.out.println("Please log in first.");
        return token != null;
    }

    private boolean inRoom() {
        if (!loggedIn()) return false;
        if (roomId == null) System.out.println("Please join a room first.");
        return roomId != null;
    }

    private Result request(String operation, String parameters) throws Exception {
        String xml = "<soap:Envelope xmlns:soap=\"" + SOAP_NS
                + "\" xmlns:c=\"" + CHAT_NS + "\"><soap:Body><c:" + operation
                + ">" + parameters + "</c:" + operation
                + "></soap:Body></soap:Envelope>";
        HttpURLConnection connection = (HttpURLConnection) URI.create(ENDPOINT).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        connection.setDoOutput(true);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(xml.getBytes(StandardCharsets.UTF_8));
        }
        try (InputStream input = connection.getResponseCode() >= 400
                ? connection.getErrorStream() : connection.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(input);
            String fault = text(document, "Fault", SOAP_NS);
            if (fault != null) throw new IllegalStateException(fault);
            return new Result("true".equalsIgnoreCase(text(document, "success", CHAT_NS)),
                    text(document, "message", CHAT_NS),
                    firstNonNull(text(document, "value", CHAT_NS), text(document, "token", CHAT_NS)));
        } finally {
            connection.disconnect();
        }
    }

    private static String text(Document document, String name, String namespace) {
        NodeList nodes = document.getElementsByTagNameNS(namespace, name);
        if (nodes.getLength() == 0) {
            nodes = document.getElementsByTagName(name);
        }
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private static String tag(String name, String value) {
        return "<" + name + ">" + escape(value) + "</" + name + ">";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void show(Result result) {
        System.out.println((result.success ? "SUCCESS: " : "FAILED: ") + result.message);
        if (result.value != null) System.out.println("Value: " + result.value);
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private record Result(boolean success, String message, String value) {
    }
}
