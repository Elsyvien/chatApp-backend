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
        BigInteger expected = new BigInteger(challenge, 16);
        BigInteger computed = signature.modPow(e, n);
        
        System.out.println("[DEBUG/Crypto] Expected: " + expected.toString(16));
        System.out.println("[DEBUG/Crypto] Computed: " + computed.toString(16));

        return expected.equals(computed);
    }
}
