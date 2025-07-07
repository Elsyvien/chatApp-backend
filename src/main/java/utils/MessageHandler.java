package utils;

import jakarta.websocket.Session;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import model.Message;
import utils.UserDatabase;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/*
 * Handles message processing and broadcasting in the chat application.
 * This class is responsible for receiving messages from clients,
 * processing them, and broadcasting them to all connected clients or specific users.
 * @author Max Staneker, Mia Schienagel
 * @version 0.2
 */

public class MessageHandler {

    private static final Jsonb jsonb = JsonbBuilder.create();
    private static final Map<String, Session> userSessions = new ConcurrentHashMap<>();

    /**
     * Handlles direct messages to a specific user.
     * @param message the message to be sent
     * @param senderSession the session of the user sending the message
     * @throws IOException if an error occurs while sending the message
     */
    public static void handleDirectMessage(Message message, Session senderSession) throws IOException {
        String recipient = message.getRecipient();
        Session recipientSession = userSessions.get(recipient);

        // Check if recipient is in Database
        if (recipientSession == null) {
            if (!UserDatabase.userExists(recipient)) {
                senderSession.getBasicRemote().sendText("message-failed:User does not exist");
                System.out.println("[MESSAGE HANDLER] Direct message failed - user does not exist: " + recipient);
                return;
            }
        }
            
            // Create timestamped message
            Message directMessage = new Message(
                message.getSender(),
                message.getContent(),
                message.getRecipient()
            );
        
        String json = jsonb.toJson(directMessage);
        System.out.println("[MESSAGE HANDLER] SENDING DIRECT MESSAGE JSON: " + json);
        
        // Send to recipient if online
        if (recipientSession != null && recipientSession.isOpen()) {
            recipientSession.getBasicRemote().sendText(json);
            System.out.println("[MESSAGE HANDLER] Direct message sent to " + recipient + " from " + message.getSender());
            
            // Send delivery confirmation to sender
            senderSession.getBasicRemote().sendText("message-delivered:" + recipient);
        } else {
            // Recipient is offline
            System.out.println("[MESSAGE HANDLER] Recipient " + recipient + " is offline. Message not delivered.");
            senderSession.getBasicRemote().sendText("message-failed:Recipient offline");
            
            // TODO: Store message for later delivery (optional)
            // storeOfflineMessage(directMessage);
        }
    }
    
    /**
     * Handles broadcast messages to all connected users
     * @param message the message to broadcast
     * @param sessions all active sessions
     * @throws IOException if broadcasting fails
     */
    public static void handleBroadcastMessage(Message message, Set<Session> sessions) throws IOException {
        Message broadcast = new Message(
            message.getSender(),
            message.getContent(),
            System.currentTimeMillis()
        );
        
        String json = jsonb.toJson(broadcast);
        System.out.println("[MESSAGE HANDLER] SENDING BROADCAST JSON: " + json);
        
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    System.err.println("[MESSAGE HANDLER] Failed to send message to session: " + session.getId());
                }
            }
        }
        System.out.println("[MESSAGE_HANDLER] Broadcasting message: " + broadcast.getContent() + " from " + broadcast.getSender());
    }
    
    /**
     * Registers a user session for direct messaging
     * @param username the username
     * @param session the user's session
     */
    public static void registerUserSession(String username, Session session) {
        userSessions.put(username, session);
        System.out.println("[MESSAGE HANDLER] User session registered: " + username);
    }
    
    /**
     * Removes a user session
     * @param session the session to remove
     */
    public static void removeUserSession(Session session) {
        userSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        System.out.println("[MESSAGE HANDLER] User session removed for session: " + session.getId());
    }
    
    /**
     * Sends list of online users to all connected clients
     * @param sessions all active sessions
     */
    public static void broadcastOnlineUsers(Set<Session> sessions) {
        try {
            Set<String> onlineUsers =  userSessions.keySet();
            String userListJson = jsonb.toJson(onlineUsers);
            String message = "online-users:" + userListJson;
            
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            }
            System.out.println("[MESSAGE HANDLER] Online users list broadcasted: " + onlineUsers);
        } catch (IOException e) {
            System.err.println("[MESSAGE HANDLER] Failed to broadcast online users: " + e.getMessage());
        }
    }
    
    /**
     * Gets a user's session by username
     * @param username the username
     * @return the session or null if not found
     */
    public static Session getUserSession(String username) {
        return userSessions.get(username);
    }
    
    /**
     * Checks if a user is currently online
     * @param username the username to check
     * @return true if user is online, false otherwise
     */
    public static boolean isUserOnline(String username) {
        Session session = userSessions.get(username);
        return session != null && session.isOpen();
    }
}
