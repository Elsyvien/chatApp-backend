package crypto;
/*
 * Provides Useful functionalities for cryptographic operations.
 * Generating and Verifying Signatures, Hashing, etc.
 * @author Max Staneker, Mia Schienagel
 * @version 0.1
 */
import java.math.BigInteger;
import java.security.SecureRandom;

public class CryptoUtils {
    public String generateChallenge() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return new BigInteger(1, bytes).toString(16);
    }

    public boolean verifySignature(String challenge, BigInteger signature, BigInteger n, BigInteger e) {
        BigInteger hash = new BigInteger(challenge.getBytes());
        return signature.modPow(e, n).equals(hash);
    }
}

