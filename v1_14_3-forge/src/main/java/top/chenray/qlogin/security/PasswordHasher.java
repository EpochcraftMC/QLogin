package top.chenray.qlogin.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 密码加密工具 - 使用 SHA-256 + Salt (1.12.2 Forge)
 * 格式: salt$hash (均为十六进制字符串)
 * 注意: 1.12.2 使用 Java 8，没有 HexFormat，改用 DatatypeConverter
 */
public class PasswordHasher {

    private static final int SALT_LENGTH = 32;
    private static final String ALGORITHM = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_CHARS[v >>> 4];
            hex[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hex);
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(hexToBytes(salt));
            byte[] hashed = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static String createPasswordHash(String password) {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        return salt + "$" + hash;
    }

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
