package utils;

import model.ServerUser;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Manages user database on the server side.
 * Users are uniquely identified by their public key, not username.
 * @author Max Staneker, Mia Schienagel
 * @version 0.2
 */
public class UserDatabase {
    private static final String USER_DATA_DIR = "data";
    private static final String USER_DATABASE_FILE = "userDatabase.properties";
    private static final Map<String, ServerUser> userCache = new HashMap<>(); // Key = publicKeyId
    private static final Map<String, String> usernameToKeyId = new HashMap<>(); // Username -> publicKeyId
    
    static {
        loadAllUsers();
    }
    
    /**
     * Generate a unique ID based on public key
     */
    private static String generatePublicKeyId(BigInteger n, BigInteger e) {
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
    
    /**
     * Register a new user with their public key
     * Returns true if user was registered, false if public key already exists
     */
    public static boolean registerUser(String username, BigInteger publicKeyN, BigInteger publicKeyE) {
        String publicKeyId = generatePublicKeyId(publicKeyN, publicKeyE);
        
        // Check if this public key already exists
        if (userCache.containsKey(publicKeyId)) {
            System.out.println("[SERVER] Public key already exists for user: " + userCache.get(publicKeyId).getUsername());
            return false;
        }
        
        ServerUser user = new ServerUser(username, publicKeyN, publicKeyE);
        userCache.put(publicKeyId, user);
        usernameToKeyId.put(username, publicKeyId);
        saveUserToFile(user);
        System.out.println("[SERVER] User registered with ID " + publicKeyId + ": " + username);
        return true;
    }
    
    /**
     * Get a user by public key
     */
    public static ServerUser getUserByPublicKey(BigInteger publicKeyN, BigInteger publicKeyE) {
        String publicKeyId = generatePublicKeyId(publicKeyN, publicKeyE);
        return userCache.get(publicKeyId);
    }
    
    /**
     * Get a user by username (for display purposes)
     */
    public static ServerUser getUserByUsername(String username) {
        String publicKeyId = usernameToKeyId.get(username);
        if (publicKeyId != null) {
            return userCache.get(publicKeyId);
        }
        return null;
    }
    
    /**
     * Check if a public key exists
     */
    public static boolean publicKeyExists(BigInteger publicKeyN, BigInteger publicKeyE) {
        String publicKeyId = generatePublicKeyId(publicKeyN, publicKeyE);
        return userCache.containsKey(publicKeyId);
    }
    
    /**
     * Check if username is already taken
     */
    public static boolean usernameExists(String username) {
        return usernameToKeyId.containsKey(username);
    }
    
    /**
     * Get a user by username
     */
    public static ServerUser getUser(String username) {
        return getUserByUsername(username);
    }
    
    /**
     * Check if a user exists
     */
    public static boolean userExists(String username) {
        return usernameToKeyId.containsKey(username);
    }
    
    /**
     * Get public key for a user by username
     */
    public static BigInteger[] getPublicKey(String username) {
        ServerUser user = getUserByUsername(username);
        if (user != null) {
            return new BigInteger[]{user.getPublicKeyN(), user.getPublicKeyE()};
        }
        return null;
    }
    
    /**
     * Save user to file
     */
    private static void saveUserToFile(ServerUser user) {
        try {
            // Create directory if it doesn't exist
            Path userDataDir = Paths.get(USER_DATA_DIR);
            if (!Files.exists(userDataDir)) {
                Files.createDirectories(userDataDir);
            }
            
            Properties props = loadDatabaseFile();
            String userPrefix = "user." + user.getPublicKeyId() + ".";
            props.setProperty(userPrefix + "username", user.getUsername());
            props.setProperty(userPrefix + "publicKeyN", user.getPublicKeyN().toString(16));
            props.setProperty(userPrefix + "publicKeyE", user.getPublicKeyE().toString(16));
            props.setProperty(userPrefix + "registrationTime", String.valueOf(user.getRegistrationTime()));
            
            File databaseFile = new File(userDataDir.toFile(), USER_DATABASE_FILE);
            try (FileOutputStream fos = new FileOutputStream(databaseFile)) {
                props.store(fos, "User Database - Updated on " + new java.util.Date());
            }
            
        } catch (IOException e) {
            System.err.println("[SERVER] Error saving user to database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load all users from file into cache
     */
    private static void loadAllUsers() {
        Properties props = loadDatabaseFile();
        if (props.isEmpty()) {
            System.out.println("[SERVER] No existing user database found.");
            return;
        }
        
        Map<String, Map<String, String>> userDataMap = new HashMap<>();
        
        // Parse properties to extract user data
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("user.")) {
                String[] parts = key.split("\\.", 3);
                if (parts.length == 3) {
                    String publicKeyId = parts[1];
                    String property = parts[2];
                    
                    userDataMap.computeIfAbsent(publicKeyId, k -> new HashMap<>()).put(property, props.getProperty(key));
                }
            }
        }
        
        // Create ServerUser objects from parsed data
        for (Map.Entry<String, Map<String, String>> entry : userDataMap.entrySet()) {
            String publicKeyId = entry.getKey();
            Map<String, String> userData = entry.getValue();
            
            try {
                String username = userData.get("username");
                BigInteger publicKeyN = new BigInteger(userData.get("publicKeyN"), 16);
                BigInteger publicKeyE = new BigInteger(userData.get("publicKeyE"), 16);
                
                ServerUser user = new ServerUser(username, publicKeyN, publicKeyE);
                userCache.put(publicKeyId, user);
                usernameToKeyId.put(username, publicKeyId);
                
            } catch (Exception e) {
                System.err.println("[SERVER] Error loading user with ID " + publicKeyId + ": " + e.getMessage());
            }
        }
        
        System.out.println("[SERVER] Loaded " + userCache.size() + " users from database.");
    }
    
    /**
     * Load database file or create empty properties
     */
    private static Properties loadDatabaseFile() {
        Properties props = new Properties();
        File databaseFile = new File(USER_DATA_DIR, USER_DATABASE_FILE);
        
        if (databaseFile.exists()) {
            try (FileInputStream fis = new FileInputStream(databaseFile)) {
                props.load(fis);
            } catch (IOException e) {
                System.err.println("[SERVER] Error loading user database: " + e.getMessage());
            }
        }
        
        return props;
    }
    
    /**
     * Get all registered usernames used for viewing who is currently online
     */
    public static String[] getAllUsernames() {
        return usernameToKeyId.keySet().toArray(new String[0]);
    }
    
    /**
     * Get all public key IDs (for debugging)
     */
    public static String[] getAllPublicKeyIds() {
        return userCache.keySet().toArray(new String[0]);
    }
}
