package controller;

import jakarta.websocket.*;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import crypto.CryptoUtils;
import utils.UserDatabase;
import model.Message;
import controller.AuthenticationHandler;
/**
 * WebSocket endpoint for broadcasting chat messages between clients.
 *
 * The endpoint sends and receives JSON encoded {@link Message} objects using
 * Yasson for serialization.
 * @author Max Staneker, Mia Schienagel
 * @version 0.1.1
 */
@ServerEndpoint("/chat")
public class ChatWebSocket {

    private static final Set<Session> sessions = new CopyOnWriteArraySet<>(); // Active WebSocket sessions
    private static final Jsonb jsonb = JsonbBuilder.create();
    private static final AuthenticationHandler authHandler = new AuthenticationHandler();

    /**
     * Adds a newly opened session to the active session set.
     *
     * @param session the newly opened WebSocket session
     */
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        session.setMaxIdleTimeout(0); // 0 = keine Idle-Timeouts
        authHandler.initializeSession(session); // Initialize authentication for the session
        System.out.println("[SERVER] Client connected: " + session.getId());
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
        System.out.println("[SERVER] Raw JSON: " + messageJson);
        try {
            if (messageJson.equals("auth-request")) {
                String challenge = authHandler.generateChallenge(session);
                System.out.println("[SERVER] Challenge for session " + session.getId() + ": " + challenge);
                session.getBasicRemote().sendText("challenge:" + challenge);
                return;
            }
            
            if (messageJson.startsWith("check-username:")) {
                String username = messageJson.substring("check-username:".length());
                if (UserDatabase.userExists(username)) {
                    session.getBasicRemote().sendText("username-exists");
                    System.out.println("[SERVER] Username check - exists: " + username);
                } else {
                    session.getBasicRemote().sendText("username-available");
                    System.out.println("[SERVER] Username check - available: " + username);
                }
                return;
            }
            
            if (messageJson.startsWith("register:")) {
                // Format: register:username:publicKeyN:publicKeyE
                String[] parts = messageJson.split(":");
                if (parts.length == 4) {
                    String username = parts[1];
                    BigInteger publicKeyN = new BigInteger(parts[2], 16);
                    BigInteger publicKeyE = new BigInteger(parts[3], 16);
                    
                    if (UserDatabase.userExists(username)) {
                        session.getBasicRemote().sendText("register-failure:User already exists");
                        System.out.println("[SERVER] Registration failed - user already exists: " + username);
                    } else {
                        UserDatabase.registerUser(username, publicKeyN, publicKeyE);
                        session.getBasicRemote().sendText("register-success");
                        System.out.println("[SERVER] User registered successfully: " + username);
                    }
                } else {
                    session.getBasicRemote().sendText("register-failure:Invalid format");
                }
                return;
            }
            
            if (messageJson.startsWith("auth-response:")) {
                String[] parts = messageJson.split(":"); // Split by colon
                String signatureHex = parts[1];
                String username = parts[2];

                boolean valid = authHandler.verifySignature(session, signatureHex, username);
                if (valid) {
                    System.out.println("[SERVER] Authentication successful for user: " + username);
                    session.getBasicRemote().sendText("auth-success");
                } else {
                    System.out.println("[SERVER] Authentication failed for user: " + username);
                    session.getBasicRemote().sendText("auth-failure");
                }
                return;
            }
            /*if (messageJson.startsWith("new-user:")) { // currently not Implemented/Used
                System.out.println("[SERVER] New user registration request: " + messageJson); 
            }*/ // Not relevant, implementation has changed
            if (!authHandler.isAuthenticated(session)) {
                System.out.println("[SERVER] Unauthorized access attempt from session: " + session.getId());
                session.getBasicRemote().sendText("unauthorized");
                return;
            }
            // ------------------------------------------------------------- //
            // Direct Messaging Implementation, checking if the message is directed to a specific user
            if (messageJson.startsWith("init-chat:")) { 
                String chatPartner = messageJson.substring("init-chat:".length());
                System.out.println("[SERVER] Initializing chat with: " + chatPartner);
                
                if (UserDatabase.userExists(chatPartner)) { // Check if the user exists
                    session.getBasicRemote().sendText("chat-init-success:" + chatPartner);
                    System.out.println("[SERVER] Chat initialized with: " + chatPartner);
                } else {
                    session.getBasicRemote().sendText("chat-init-failure:User does not exist");
                    System.out.println("[SERVER] Chat initialization failed - user does not exist: " + chatPartner);
                }
                return;
            }
        
            if (!authHandler.isAuthenticated(session)) { // Check if the session is authenticated
                System.out.println("[SERVER] Unauthorized access attempt from session: " + session.getId() + " for direct messaging");
                session.getBasicRemote().sendText("unauthorized");
                return;
            }

            Message message = jsonb.fromJson(messageJson, Message.class); // Deserialize the JSON to a Message object
            System.out.println("[SERVER] Parsed: sender=" + message.getSender() + ", content=" + message.getContent() + ", recipient=" + message.getRecipient());

            sessions.add(session); // Ensure the session is added to the active sessions
            if (message.getRecipient() != null && !message.getRecipient().isEmpty()) {
                handleDirectMessage(message, session);
            } else {
                handleBroadcastMessage(message);
                }
            } 
        catch (IOException e) {
                System.err.println("[SERVER] Error processing message: " + e.getMessage());
                e.printStackTrace();
        }
    }


            // @Deprecated This part is for broadcasting messages to all connected clients, will be refactored in the future
            /*Message message = jsonb.fromJson(messageJson, Message.class); // Deserialize the JSON to a Message object
            System.out.println("[SERVER] Parsed: sender=" + message.getSender() + ", content=" + message.getContent());
            Message broadcast = new Message(
                    message.getSender(),
                    message.getContent(),
                    System.currentTimeMillis()
            );
            String json = jsonb.toJson(broadcast);
            System.out.println("[SERVER] SENDING JSON: " + json);
            for (Session s : sessions) {
                if (s.isOpen()) {
                    s.getBasicRemote().sendText(json);
                }
            }
            System.out.println("[SERVER] Broadcasting message: " + broadcast.getContent() + " from " + broadcast.getSender());
        } catch (IOException e) {
            e.printStackTrace();
        }/*
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
        authHandler.cleanup(session);
    }

    public static Set<Session> getSessions() {
        return sessions;
    }
    public static Jsonb getJsonb() {
        return jsonb;
    }
    
}
