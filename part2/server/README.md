# Chat Room SOAP Service

This project follows the professor's Java-first JAX-WS calculator architecture:

```text
Java service -> JAX-WS Endpoint -> WSDL -> client
```

## First step

```bash
./gradlew classes
./gradlew generateWsdl
./gradlew run
```

The provider will be available at:

```text
http://localhost:9000/chat-room
```

The runtime WSDL will be available at:

```text
http://localhost:9000/chat-room?wsdl
```

The service supports listing messages in a joined room. The result includes
all broadcasts and only private messages sent by or addressed to the logged-in
user.

The service stores data in memory, so restarting the provider removes users,
tokens, rooms, memberships, and messages.

This is an educational implementation. User passwords and session data are
stored only in memory, and passwords are not intended to be stored this way
in a production system.
