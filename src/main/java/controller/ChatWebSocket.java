package controller;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import model.Message;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket endpoint for broadcasting chat messages between clients.
 *
 * The endpoint sends and receives JSON encoded {@link Message} objects using
 * Yasson for serialization.
 * @author Max Staneker, Mia Schienagel
 */
@ServerEndpoint("/chat")
public class ChatWebSocket {

    /**
     * Active WebSocket sessions connected to this endpoint.
     */
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();

    private static final Jsonb jsonb = JsonbBuilder.create();


    /**
     * Adds a newly opened session to the active session set.
     *
     * @param session the newly opened WebSocket session
     */
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
    }

    /**
     * Receives a JSON representation of a {@link Message} from a client and
     * broadcasts it to all connected sessions. The server ensures a timestamp is
     * set before sending the message to every open WebSocket session.
     *
     * @param messageJson the incoming message in JSON format
     * @param session     the WebSocket session that sent the message
     */
    @OnMessage
    public void onMessage(String messageJson, Session session) {
        try {
            Message message = jsonb.fromJson(messageJson, Message.class);
            Message broadcast = new Message(message.getSender(), message.getContent(), System.currentTimeMillis());
            String json = jsonb.toJson(broadcast);
            for (Session s : sessions) {
                if (s.isOpen()) {
                    s.getBasicRemote().sendText(json);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the session from the active session set once the connection is
     * closed.
     *
     * @param session the WebSocket session that was closed
     */
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }
}
