import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        final int PORT = 8080;
        final String host = "localhost";
        final String ACCOUNT_CREATED = "OK Account created";
        Scanner input = new Scanner(System.in);

        boolean accountCreated = false;
        int attemptsMade = 0;

        try (Socket socket = new Socket(host, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                
            for (int attempt = 1; attempt <= 3 && !accountCreated; attempt++) {
                attemptsMade = attempt;
                System.out.println("\n---- Create New Account ----");
                System.out.println("Attempt " + attempt + " of 3");
                System.out.println("Rules: Username cannot be empty and must not contain space.");

                System.out.print("Enter username: ");
                String username = input.nextLine();
                
                System.out.print("Enter password: ");
                String password = input.nextLine();

                String request = "CREATE_ACCOUNT;" + username + ";" + password + "\n";
                writer.write(request);
                writer.flush();

                String response = reader.readLine();
                
                if (response == null) {
                    System.out.println("Server disconnected unexpectedly.");
                    break;
                }

                if (response.equals(ACCOUNT_CREATED)) {
                    System.out.println("Account created successfully!");
                    accountCreated = true;
                } else {
                    System.out.println(response);
                }
            }
        } catch (IOException e) {
            System.err.println("Error: Could not connect to server. Is it running?");
        }

        if (!accountCreated && attemptsMade == 3) {
            System.out.println("Maximum of 3 attempts reached.");
        }
        input.close();
    }
}       