package top.chenray.qlogin.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import top.chenray.qlogin.LoginMod;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.security.PasswordHasher;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库管理器 - 使用 SQLite 存储玩家数据
 * 数据文件: config/loginmod/players.db
 */
public class DatabaseManager {

    private static final Logger LOGGER = LoginMod.LOGGER;
    private static final Gson GSON = new GsonBuilder().create();

    private static DatabaseManager instance;
    private Connection connection;
    private final Path dbPath;

    private DatabaseManager(Path configDir) {
        this.dbPath = configDir.resolve("players.db");
    }

    /**
     * 初始化数据库管理器
     */
    public static synchronized DatabaseManager init(Path configDir) {
        if (instance == null) {
            instance = new DatabaseManager(configDir);
        }
        return instance;
    }

    public static DatabaseManager getInstance() {
        return instance;
    }

    /**
     * 连接数据库并创建表
     */
    public void connect() {
        try {
            // 确保目录存在
            Files.createDirectories(dbPath.getParent());

            // 建立连接
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString());

            // 启用 WAL 模式提升性能
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
            }

            createTables();
            LOGGER.info("数据库已连接: {}", dbPath);
        } catch (Exception e) {
            LOGGER.error("数据库连接失败", e);
            throw new RuntimeException("无法连接到数据库", e);
        }
    }

    /**
     * 创建数据表
     */
    private void createTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                register_time BIGINT NOT NULL,
                last_login BIGINT,
                ip_history TEXT DEFAULT '[]',
                login_fail_count INTEGER DEFAULT 0,
                is_banned INTEGER DEFAULT 0,
                ban_reason TEXT,
                ban_expiry BIGINT
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }

        // 创建索引
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_username ON players(username)");
        }
    }

    /**
     * 检查玩家是否已注册
     */
    public boolean isPlayerRegistered(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.error("检查玩家注册状态失败", e);
            return false;
        }
    }

    /**
     * 注册新玩家
     */
    public boolean registerPlayer(UUID uuid, String username, String password, String ip) {
        String hash = PasswordHasher.createPasswordHash(password);
        String sql = """
            INSERT INTO players (uuid, username, password_hash, register_time, last_login, ip_history, login_fail_count)
            VALUES (?, ?, ?, ?, ?, ?, 0)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, username);
            pstmt.setString(3, hash);
            pstmt.setLong(4, Instant.now().toEpochMilli());
            pstmt.setLong(5, Instant.now().toEpochMilli());

            // IP 历史
            List<String> ips = new ArrayList<>();
            ips.add(ip);
            pstmt.setString(6, GSON.toJson(ips));

            pstmt.executeUpdate();
            LOGGER.info("玩家 {} ({}) 已注册", username, uuid);
            return true;
        } catch (SQLException e) {
            LOGGER.error("注册玩家失败", e);
            return false;
        }
    }

    /**
     * 验证玩家密码
     */
    public boolean verifyPassword(UUID uuid, String password) {
        String sql = "SELECT password_hash FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                return PasswordHasher.verifyPassword(password, storedHash);
            }
        } catch (SQLException e) {
            LOGGER.error("验证密码失败", e);
        }
        return false;
    }

    /**
     * 更新最后登录时间和 IP
     */
    public void updateLoginInfo(UUID uuid, String ip) {
        String sql = "SELECT ip_history FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String ipHistoryJson = rs.getString("ip_history");
                Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> ipHistory = GSON.fromJson(ipHistoryJson, listType);
                if (ipHistory == null) ipHistory = new ArrayList<>();

                if (!ipHistory.contains(ip)) {
                    ipHistory.add(ip);
                    if (ipHistory.size() > 10) {
                        ipHistory = ipHistory.subList(ipHistory.size() - 10, ipHistory.size());
                    }
                }

                String updateSql = "UPDATE players SET last_login = ?, ip_history = ?, login_fail_count = 0 WHERE uuid = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, Instant.now().toEpochMilli());
                    updateStmt.setString(2, GSON.toJson(ipHistory));
                    updateStmt.setString(3, uuid.toString());
                    updateStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOGGER.error("更新登录信息失败", e);
        }
    }

    /**
     * 修改密码
     */
    public boolean changePassword(UUID uuid, String newPassword) {
        String hash = PasswordHasher.createPasswordHash(newPassword);
        String sql = "UPDATE players SET password_hash = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, hash);
            pstmt.setString(2, uuid.toString());
            int updated = pstmt.executeUpdate();
            LOGGER.info("玩家 {} 密码已修改", uuid);
            return updated > 0;
        } catch (SQLException e) {
            LOGGER.error("修改密码失败", e);
            return false;
        }
    }

    /**
     * 增加登录失败计数
     */
    public int incrementFailCount(UUID uuid) {
        String sql = "UPDATE players SET login_fail_count = login_fail_count + 1 WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("增加失败计数失败", e);
        }

        // 获取最新计数
        String querySql = "SELECT login_fail_count FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(querySql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("login_fail_count");
            }
        } catch (SQLException e) {
            LOGGER.error("获取失败计数失败", e);
        }
        return 0;
    }

    /**
     * 重置失败计数
     */
    public void resetFailCount(UUID uuid) {
        String sql = "UPDATE players SET login_fail_count = 0 WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("重置失败计数失败", e);
        }
    }

    /**
     * 解除玩家注册（强制注销）
     */
    public boolean unregisterPlayer(String username) {
        String sql = "DELETE FROM players WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                LOGGER.info("玩家 {} 已被强制注销", username);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("强制注销玩家失败", e);
        }
        return false;
    }

    /**
     * 通过 UUID 解除注册
     */
    public boolean unregisterPlayerByUuid(UUID uuid) {
        String sql = "DELETE FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("强制注销玩家失败", e);
            return false;
        }
    }

    /**
     * 获取玩家信息（用于管理命令）
     */
    public Map<String, Object> getPlayerInfo(String username) {
        String sql = "SELECT * FROM players WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("uuid", rs.getString("uuid"));
                info.put("username", rs.getString("username"));
                info.put("register_time", rs.getLong("register_time"));
                info.put("last_login", rs.getLong("last_login"));
                info.put("login_fail_count", rs.getInt("login_fail_count"));
                info.put("ip_history", rs.getString("ip_history"));
                return info;
            }
        } catch (SQLException e) {
            LOGGER.error("获取玩家信息失败", e);
        }
        return null;
    }

    /**
     * 获取玩家名
     */
    public String getUsername(UUID uuid) {
        String sql = "SELECT username FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            LOGGER.error("获取玩家名失败", e);
        }
        return null;
    }

    /**
     * 检查玩家是否被封禁
     */
    public boolean isBanned(UUID uuid) {
        String sql = "SELECT is_banned, ban_expiry FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                if (rs.getInt("is_banned") == 1) {
                    long expiry = rs.getLong("ban_expiry");
                    if (expiry > 0 && Instant.now().toEpochMilli() < expiry) {
                        return true;
                    } else if (expiry > 0) {
                        // 封禁已过期，自动解封
                        unbanPlayer(uuid);
                        return false;
                    }
                    return true; // 永久封禁
                }
            }
        } catch (SQLException e) {
            LOGGER.error("检查封禁状态失败", e);
        }
        return false;
    }

    /**
     * 封禁玩家
     */
    public void banPlayer(UUID uuid, String reason, long durationSeconds) {
        long expiry = durationSeconds > 0 ? Instant.now().toEpochMilli() + (durationSeconds * 1000) : 0;
        String sql = "UPDATE players SET is_banned = 1, ban_reason = ?, ban_expiry = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reason);
            pstmt.setLong(2, expiry);
            pstmt.setString(3, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("封禁玩家失败", e);
        }
    }

    /**
     * 解封玩家
     */
    public void unbanPlayer(UUID uuid) {
        String sql = "UPDATE players SET is_banned = 0, ban_reason = NULL, ban_expiry = NULL WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("解封玩家失败", e);
        }
    }

    /**
     * 获取数据库连接（仅供内部查询使用）
     */
    public java.sql.Connection getConnection() {
        return connection;
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("数据库连接已关闭");
            }
        } catch (SQLException e) {
            LOGGER.error("关闭数据库连接失败", e);
        }
    }
}
