package top.chenray.qlogin.database;

import top.chenray.qlogin.security.PasswordHasher;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;

/**
 * 数据库管理器 - SQLite
 */
public class DatabaseManager {

    private static final Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
    private static DatabaseManager instance;
    private static Path databaseDir;
    private Connection connection;

    private DatabaseManager() {}

    public static void init(Path dir) {
        databaseDir = dir;
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * 连接数据库
     */
    public void connect() {
        try {
            // 确保目录存在
            java.nio.file.Files.createDirectories(databaseDir);
            String url = "jdbc:sqlite:" + databaseDir.resolve("loginmod.db").toString();
            connection = DriverManager.getConnection(url);
            createTables();
            LOGGER.info("数据库已连接: {}", url);
        } catch (Exception e) {
            LOGGER.error("数据库连接失败", e);
        }
    }

    /**
     * 创建表
     */
    private void createTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                last_ip TEXT,
                last_login TIMESTAMP,
                registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * 关闭数据库
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("数据库已关闭");
            }
        } catch (SQLException e) {
            LOGGER.error("关闭数据库失败", e);
        }
    }

    /**
     * 检查玩家是否已注册
     */
    public boolean isPlayerRegistered(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.error("检查玩家注册状态失败", e);
            return false;
        }
    }

    /**
     * 注册玩家
     */
    public boolean registerPlayer(UUID uuid, String username, String password) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(password, salt);
        String sql = "INSERT OR REPLACE INTO players (uuid, username, password_hash, salt) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setString(3, hash);
            stmt.setString(4, salt);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.error("注册玩家失败", e);
            return false;
        }
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(UUID uuid, String password) {
        String sql = "SELECT password_hash, salt FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                return PasswordHasher.verify(password, salt, hash);
            }
        } catch (SQLException e) {
            LOGGER.error("验证密码失败", e);
        }
        return false;
    }

    /**
     * 修改密码
     */
    public boolean changePassword(UUID uuid, String oldPassword, String newPassword) {
        if (!verifyPassword(uuid, oldPassword)) {
            return false;
        }
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(newPassword, salt);
        String sql = "UPDATE players SET password_hash = ?, salt = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, hash);
            stmt.setString(2, salt);
            stmt.setString(3, uuid.toString());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.error("修改密码失败", e);
            return false;
        }
    }

    /**
     * 管理员重置密码
     */
    public boolean adminResetPassword(UUID uuid, String newPassword) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(newPassword, salt);
        String sql = "UPDATE players SET password_hash = ?, salt = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, hash);
            stmt.setString(2, salt);
            stmt.setString(3, uuid.toString());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.error("管理员重置密码失败", e);
            return false;
        }
    }

    /**
     * 更新登录信息（IP 和最后登录时间）
     */
    public void updateLoginInfo(UUID uuid, String ip) {
        String sql = "UPDATE players SET last_ip = ?, last_login = CURRENT_TIMESTAMP WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ip);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("更新登录信息失败", e);
        }
    }
}
