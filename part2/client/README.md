# Chat Room SOAP Client

This is a separate terminal client for the SOAP provider in `part2/server`.

Start the server first:

```bash
cd part2/server
./gradlew --console=plain run
```

In another terminal, compile and run this client:

```bash
cd part2/client
javac ChatRoomClient.java
java ChatRoomClient
```

The client sends SOAP `POST` requests to:

```text
http://localhost:9000/chat-room
```

It provides menu options for all ten service operations and keeps the login
token and joined room ID during the client session.
