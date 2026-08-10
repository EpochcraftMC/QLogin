package top.chenray.qlogin.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 密码哈希工具 - 使用 SHA-256 + 随机盐值
 */
public class PasswordHasher {

    private static final int SALT_LENGTH = 16;
    private static final String ALGORITHM = "SHA-256";

    /**
     * 生成随机盐值
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * 使用 SHA-256 哈希密码
     *
     * @param password 明文密码
     * @param salt     盐值
     * @return 哈希后的十六进制字符串
     */
    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt.getBytes());
            byte[] hashed = md.digest(password.getBytes());
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 验证密码
     *
     * @param password      明文密码
     * @param salt          盐值
     * @param hashedPassword 存储的哈希值
     * @return 是否匹配
     */
    public static boolean verify(String password, String salt, String hashedPassword) {
        return hash(password, salt).equals(hashedPassword.toLowerCase());
    }
}
