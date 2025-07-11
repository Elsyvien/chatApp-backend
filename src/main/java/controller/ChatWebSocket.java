package controller;

import jakarta.websocket.*;
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
import model.ServerUser;
import utils.MessageHandler;

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
        // Broadcast updated online users list
        MessageHandler.broadcastOnlineUsers(sessions);
        System.out.println("[SERVER] Active sessions: " + sessions.size());
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
                    
                    // Register user session immediately after successful authentication
                    MessageHandler.registerUserSession(username, session);
                    
                    // Send current online users list directly to the newly authenticated user 
                    // (now includes the new user since they're registered)
                    try {
                        Thread.sleep(500); // Short delay to ensure session is fully registered
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("[SERVER] Interrupted during sleep: " + e.getMessage());
                    }
                    MessageHandler.sendOnlineUsersToSession(session);
                    
                    // Then broadcast updated online users list to all other clients
                    MessageHandler.broadcastOnlineUsers(sessions);
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
            // Try to parse as JSON message first to check for special commands in content
            try {
                Message message = jsonb.fromJson(messageJson, Message.class);
                
                // Handle public key requests
                if (message.getContent() != null && message.getContent().startsWith("get-public-key:")) {
                    String requestedUsername = message.getContent().substring("get-public-key:".length());
                    System.out.println("[SERVER] Public key request for: " + requestedUsername + " from: " + message.getSender());
                    
                    ServerUser requestedUser = UserDatabase.getUser(requestedUsername);
                    if (requestedUser != null) {
                        String response = "public-key:" + requestedUsername + ":" + 
                            requestedUser.getPublicKeyN().toString(16) + ":" + 
                            requestedUser.getPublicKeyE().toString(16);
                        session.getBasicRemote().sendText(response);
                        System.out.println("[SERVER] Public key sent for: " + requestedUsername);
                    } else {
                        session.getBasicRemote().sendText("public-key-not-found:" + requestedUsername);
                        System.out.println("[SERVER] Public key not found for: " + requestedUsername);
                    }
                    return;
                }
                
                // Check if this is a chat initialization message sent as JSON
                if (message.getContent() != null && message.getContent().startsWith("init-chat:")) {
                    String chatPartner = message.getContent().substring("init-chat:".length());
                    System.out.println("[SERVER] Chat initialization request for: " + chatPartner + " from: " + message.getSender());
                    
                    // Register the sender's session for direct messaging
                    MessageHandler.registerUserSession(message.getSender(), session);
                    
                    if (UserDatabase.userExists(chatPartner)) {
                        session.getBasicRemote().sendText("chat-init-success:" + chatPartner);
                        System.out.println("[SERVER] Chat initialization successful for: " + chatPartner);
                        
                        // Automatically send public key of chat partner
                        ServerUser chatPartnerUser = UserDatabase.getUser(chatPartner);
                        if (chatPartnerUser != null) {
                            String keyResponse = "public-key:" + chatPartner + ":" + 
                                chatPartnerUser.getPublicKeyN().toString(16) + ":" + 
                                chatPartnerUser.getPublicKeyE().toString(16);
                            session.getBasicRemote().sendText(keyResponse);
                            System.out.println("[SERVER] Auto-sent public key for chat partner: " + chatPartner);
                        }
                    } else {
                        session.getBasicRemote().sendText("chat-init-failure:User not found");
                        System.out.println("[SERVER] Chat initialization failed - user not found: " + chatPartner);
                    }
                    return;
                }
                
                // Regular message handling
                System.out.println("[SERVER] Parsed: sender=" + message.getSender() + 
                                 ", content=" + message.getContent() + 
                                 ", recipient=" + message.getRecipient());
                
                // Register user session for direct messaging
                MessageHandler.registerUserSession(message.getSender(), session);
                
                // Route message based on type
                if (message.getRecipient() != null && !message.getRecipient().isEmpty()) {
                    MessageHandler.handleDirectMessage(message, session);
                } else {
                    MessageHandler.handleBroadcastMessage(message, sessions);
                }
                
            } catch (Exception jsonException) {
                // If JSON parsing fails, check for direct init-chat string
                if (messageJson.startsWith("init-chat:")) {
                    String chatPartner = messageJson.substring("init-chat:".length());
                    System.out.println("[SERVER] Direct chat initialization request for: " + chatPartner);
                    
                    if (UserDatabase.userExists(chatPartner)) {
                        session.getBasicRemote().sendText("chat-init-success:" + chatPartner);
                        System.out.println("[SERVER] Chat initialization successful for: " + chatPartner);
                    } else {
                        session.getBasicRemote().sendText("chat-init-failure:User not found");
                        System.out.println("[SERVER] Chat initialization failed - user not found: " + chatPartner);
                    }
                    return;
                } else {
                    System.err.println("[SERVER] Failed to parse message as JSON: " + messageJson);
                    jsonException.printStackTrace();
                }
            }
            
        } catch (IOException e) {
            System.err.println("[SERVER] Error processing message: " + e.getMessage());
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
        MessageHandler.removeUserSession(session);
        authHandler.cleanup(session);
        
        // Broadcast updated online users list
        MessageHandler.broadcastOnlineUsers(sessions);
        
        System.out.println("[SERVER] Client disconnected: " + session.getId());
    }

    public static Set<Session> getSessions() {
        return sessions;
    }
    public static Jsonb getJsonb() {
        return jsonb;
    }
    
}
