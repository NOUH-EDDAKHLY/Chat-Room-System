package com.example.chatroom.provider;

import javax.xml.ws.Endpoint;

public class Provider {
    public static final String ADDRESS = "http://localhost:9000/chat-room";

    public static void main(String[] args) {
        Endpoint.publish(ADDRESS, new ChatRoom());
        System.out.println("Chat Room SOAP provider running at " + ADDRESS);
        System.out.println("WSDL available at " + ADDRESS + "?wsdl");
    }
}
