# Chat Room System Submission

## Part I

`part1/` contains:

- `ChatServer.java`
- `ChatClient.java`
- `design.txt`
- `architecture.png`

The Part I implementation uses Java standard networking and file I/O. It
implements account creation, which is one of the first five required
functionalities.

Compile:

```bash
javac part1/ChatServer.java part1/ChatClient.java
```

Run the server first, then run the client in another terminal:

```bash
java -cp part1 ChatServer
java -cp part1 ChatClient
```

## Part II

- `part2/server/` contains the Java SOAP/JAX-WS provider.
- `part2/client/` contains the separate SOAP client.

Run the server:

```bash
cd part2/server
./gradlew generateWsdl
./gradlew run
```

In another terminal, run the client:

```bash
cd part2/client
javac ChatRoomClient.java
java ChatRoomClient
```

The Part II service uses the endpoint:

```text
http://localhost:9000/chat-room
```
