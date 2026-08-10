package top.chenray.qlogin.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 瀵嗙爜鍔犲瘑宸ュ叿 - 浣跨敤 SHA-256 + Salt
 * 鏍煎紡: salt$hash (鍧囦负鍗佸叚杩涘埗瀛楃涓?
 */
public class PasswordHasher {

    private static final int SALT_LENGTH = 32; // 32 bytes = 256 bits
    private static final String ALGORITHM = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 鐢熸垚闅忔満鐩愬€硷紙鍗佸叚杩涘埗瀛楃涓诧級
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * 浣跨敤 SHA-256 瀵瑰瘑鐮佸姞鐩愬搱甯?     * @param password 鏄庢枃瀵嗙爜
     * @param salt     鐩愬€硷紙鍗佸叚杩涘埗锛?     * @return 鍝堝笇鍚庣殑鍗佸叚杩涘埗瀛楃涓?     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(HexFormat.of().parseHex(salt));
            byte[] hashed = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 鐢熸垚瀹屾暣鐨勫瘑鐮佸搱甯屽瓨鍌ㄥ€?(salt$hash)
     */
    public static String createPasswordHash(String password) {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        return salt + "$" + hash;
    }

    /**
     * 楠岃瘉瀵嗙爜
     * @param password        鏄庢枃瀵嗙爜
     * @param storedHashValue 鏁版嵁搴撲腑瀛樺偍鐨?salt$hash 鍊?     * @return 瀵嗙爜鏄惁姝ｇ‘
     */
    public static boolean verifyPassword(String password, String storedHashValue) {
        if (storedHashValue == null || !storedHashValue.contains("$")) {
            return false;
        }
        String[] parts = storedHashValue.split("\\$", 2);
        if (parts.length != 2) {
            return false;
        }
        String salt = parts[0];
        String expectedHash = parts[1];
        String actualHash = hashPassword(password, salt);
        return MessageDigest.isEqual(
            expectedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            actualHash.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}