import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ChatServer {
    private static final int PORT = 8080; 
    private static final String USERS_FILE = "users.txt";
    private static final Object fileLock = new Object();
    private static final String ACCOUNT_CREATED = "OK Account created";
    private static final String INVALID_FORMAT = "ERR Invalid format";
    private static final String INVALID_USERNAME = "ERR Username cannot be empty or contain spaces";
    private static final String INVALID_PASSWORD = "ERR Password cannot be empty";
    private static final String USERNAME_EXISTS = "ERR Username already exists";
    private static final String UNKNOWN_COMMAND = "ERR Unknown command";

    public static void main(String[] args) {
        System.out.println("Starting Chat Server on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running. Waiting for clients...\n");

            while (true) {
                try (Socket clientConnection = serverSocket.accept()) {
                    handleClient(clientConnection);
                }
            }
        } catch (IOException e) {
            System.err.println("Server error.");
        }
    }

    private static void handleClient(Socket clientConnection) {
        try (BufferedReader requestReader = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
             BufferedWriter responseWriter = new BufferedWriter(new OutputStreamWriter(clientConnection.getOutputStream()))) {

            for (int attempt = 1; attempt <= 3; attempt++) {
                String request = requestReader.readLine();
                if (request == null) {
                    return;
                }

                String response;
                String[] requestFields = tokenizeRequest(request);

                if (requestFields.length != 3) {
                    response = INVALID_FORMAT;
                } else {
                    String command = requestFields[0];
                    String username = requestFields[1];
                    String password = requestFields[2];

                    if (command.equals("CREATE_ACCOUNT")) {
                        synchronized (fileLock) {
                            if (username.isEmpty() || username.contains(" ")) {
                                response = INVALID_USERNAME;
                            } else if (password.isEmpty()) {
                                response = INVALID_PASSWORD;
                            } else if (isUserExists(username)) {
                                response = USERNAME_EXISTS;
                            } else {
                                saveUser(username, password);
                                response = ACCOUNT_CREATED;
                            }
                        }
                    } else {
                        response = UNKNOWN_COMMAND;
                    }
                }

                responseWriter.write(response + "\n");
                responseWriter.flush();

                if (response.equals(ACCOUNT_CREATED) || attempt == 3) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client connection error.");
        }
    }

    private static String[] tokenizeRequest(String request) {
        List<String> fields = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(request, ";", true);
        StringBuilder currentField = new StringBuilder();

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals(";")) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(token);
            }
        }

        fields.add(currentField.toString());
        return fields.toArray(new String[0]);
    }

    private static boolean isUserExists(String username) throws IOException {
        File file = new File(USERS_FILE);
        if (!file.exists()) return false;
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String userLine;

            while ((userLine = br.readLine()) != null) {
                if (userLine.startsWith(username + ":")) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void saveUser(String username, String password) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            bw.write(username + ":" + password + "\n");
        }
    }
}