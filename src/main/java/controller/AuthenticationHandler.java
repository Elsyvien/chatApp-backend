package controller;

/*
 * This class handles user authentication for WebSocket connections.
 * Users are identified by their public key, not username.
 * @author Max Staneker, Mia Schienagel
 * @version 0.2.0
 */

import jakarta.websocket.Session;
import crypto.CryptoUtils;
import utils.UserDatabase;
import model.ServerUser;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthenticationHandler {
    private final CryptoUtils cryptoUtils = new CryptoUtils();
    private final Map<Session, String> challenges;
    private final Map<Session, String> authenticatedUsers; // Session -> publicKeyId
    private final Map<Session, String> usernames; // Session -> username (for display)

    public AuthenticationHandler() {
        this.challenges = new ConcurrentHashMap<>();
        this.authenticatedUsers = new ConcurrentHashMap<>();
        this.usernames = new ConcurrentHashMap<>();
    }

    public void initializeSession(Session session) {
        // Session starts unauthenticated
    }

    public String generateChallenge(Session session) {
        String challenge = cryptoUtils.generateChallenge();
        challenges.put(session, challenge);
        return challenge;
    }

    public boolean verifySignature(Session session, String signatureHex, String username) {
        String challenge = challenges.get(session);
        if (challenge == null) return false;

        // Get user by username (for backward compatibility)
        ServerUser user = UserDatabase.getUserByUsername(username);
        if (user == null) {
            System.out.println("[SERVER] User not found in database: " + username);
            return false;
        }
        
        BigInteger n = user.getPublicKeyN();
        BigInteger e = user.getPublicKeyE();
        BigInteger signature = new BigInteger(signatureHex, 16);

        boolean valid = cryptoUtils.verifySignature(challenge, signature, n, e);
        
        if (valid) {
            authenticatedUsers.put(session, user.getPublicKeyId());
            usernames.put(session, user.getUsername());
            System.out.println("[SERVER] Authentication successful for user: " + username + " (ID: " + user.getPublicKeyId() + ")");
        } else {
            System.out.println("[SERVER] Authentication failed for user: " + username);
        }
        
        return valid;
    }

    public boolean isAuthenticated(Session session) {
        return authenticatedUsers.containsKey(session);
    }
    
    public String getAuthenticatedUserId(Session session) {
        return authenticatedUsers.get(session);
    }
    
    public String getAuthenticatedUsername(Session session) {
        return usernames.get(session);
    }

    public void cleanup(Session session) {
        challenges.remove(session);
        authenticatedUsers.remove(session);
        usernames.remove(session);
    }
}
