package crypto;
/*
 * Provides Useful functionalities for cryptographic operations.
 * Generating and Verifying Signatures, Hashing, etc.
 * @author Max Staneker, Mia Schienagel
 * @version 0.1
 */
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtils {
    public String generateChallenge() {
        /*SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return new BigInteger(1, bytes).toString(16);*/

        SecureRandom random = new SecureRandom();
        // Erzeuge 1024 Bit = gleich groß wie n
        BigInteger bigChallenge = new BigInteger(1024, random);
        return bigChallenge.toString(16);
    }

    public boolean verifySignature(String challenge, BigInteger signature, BigInteger n, BigInteger e) {
        try {
            System.out.println("[DEBUG/Crypto] Original challenge: " + challenge);
            
            // Convert challenge hex string to bytes
            byte[] challengeBytes = hexStringToByteArray(challenge);
            System.out.println("[DEBUG/Crypto] Challenge bytes length: " + challengeBytes.length);
            
            // Hash the challenge bytes (same as client does)
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedChallenge = digest.digest(challengeBytes);
            BigInteger expected = new BigInteger(1, hashedChallenge);
            
            // Verify signature by decrypting it with public key
            BigInteger computed = signature.modPow(e, n);
            
            System.out.println("[DEBUG/Crypto] Expected: " + expected.toString(16));
            System.out.println("[DEBUG/Crypto] Computed: " + computed.toString(16));

            return expected.equals(computed);
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("[SERVER/Crypto] SHA-256 algorithm not found: " + ex.getMessage());
            return false;
        }
    }
    
    // Helper method to convert hex string to byte array
    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }
}
