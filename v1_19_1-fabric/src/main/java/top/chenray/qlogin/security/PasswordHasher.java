package top.chenray.qlogin.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 密码加密工具 - 使用 SHA-256 + Salt
 * 格式: salt$hash (均为十六进制字符串)
 */
public class PasswordHasher {

    private static final int SALT_LENGTH = 32; // 32 bytes = 256 bits
    private static final String ALGORITHM = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成随机盐值（十六进制字符串）
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * 使用 SHA-256 对密码加盐哈希
     * @param password 明文密码
     * @param salt     盐值（十六进制）
     * @return 哈希后的十六进制字符串
     */
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
     * 生成完整的密码哈希存储值 (salt$hash)
     */
    public static String createPasswordHash(String password) {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        return salt + "$" + hash;
    }

    /**
     * 验证密码
     * @param password        明文密码
     * @param storedHashValue 数据库中存储的 salt$hash 值
     * @return 密码是否正确
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
