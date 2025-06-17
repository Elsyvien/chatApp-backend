package controller;

/*
 * This class handles user authentication for WebSocket connections.
 * It manages session challenges and provides methods for challenge generation and signature verification.
 * @author Max Staneker, Mia Schienagel
 * @version 0.1.0
 */

import jakarta.websocket.Session;
import crypto.CryptoUtils;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthenticationHandler {
    private final CryptoUtils cryptoUtils = new CryptoUtils();
    private final Map<Session, String> challenges;
    private final Map<Session, Boolean> authenticatedSessions;

    public AuthenticationHandler() {
        this.challenges = new ConcurrentHashMap<>();
        this.authenticatedSessions = new ConcurrentHashMap<>();
    }

    public void initializeSession(Session session) {
        challenges.put(session, null);
        authenticatedSessions.put(session, false);
    }

    public String generateChallenge(Session session) {
        String challenge = cryptoUtils.generateChallenge();
        challenges.put(session, challenge);
        return challenge;
    }

    public boolean verifySignature(Session session, String signatureHex, String username) {
        String challenge = challenges.get(session);
        if (challenge == null) return false;

        // TODO: Load real public key from DB for the user:
        BigInteger n = new BigInteger("your_public_n_here");
        BigInteger e = new BigInteger("65537");
        BigInteger signature = new BigInteger(signatureHex, 16);

        boolean valid = cryptoUtils.verifySignature(challenge, signature, n, e);
        authenticatedSessions.put(session, valid);
        return valid;
    }

    public boolean isAuthenticated(Session session) {
        return authenticatedSessions.getOrDefault(session, false);
    }

    public void cleanup(Session session) {
        challenges.remove(session);
        authenticatedSessions.remove(session);
    }
}
