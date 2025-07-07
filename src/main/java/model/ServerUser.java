package model;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents a user with their public key information on the server side.
 * Users are uniquely identified by their public key, not their username.
 * @author Max Staneker, Mia Schienagel
 * @version 0.2
 */
public class ServerUser {
    private final String username; // Display name only
    private final BigInteger publicKeyN;
    private final BigInteger publicKeyE;
    private final String publicKeyId; // Unique identifier based on public key
    private final long registrationTime;

    public ServerUser(String username, BigInteger publicKeyN, BigInteger publicKeyE) {
        this.username = username;
        this.publicKeyN = publicKeyN;
        this.publicKeyE = publicKeyE;
        this.publicKeyId = generatePublicKeyId(publicKeyN, publicKeyE);
        this.registrationTime = System.currentTimeMillis();
    }

    // Generate a unique ID based on the public key
    private String generatePublicKeyId(BigInteger n, BigInteger e) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = n.toString(16) + ":" + e.toString(16);
            byte[] hash = digest.digest(combined.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-256 not available", ex);
        }
    }

    public String getUsername() {
        return username;
    }

    public BigInteger getPublicKeyN() {
        return publicKeyN;
    }

    public BigInteger getPublicKeyE() {
        return publicKeyE;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public long getRegistrationTime() {
        return registrationTime;
    }

    @Override
    public String toString() {
        return "ServerUser{" +
                "username='" + username + '\'' +
                ", publicKeyId='" + publicKeyId + '\'' +
                ", publicKeyN=" + publicKeyN.toString(16) +
                ", publicKeyE=" + publicKeyE.toString(16) +
                ", registrationTime=" + registrationTime +
                '}';
    }
}
