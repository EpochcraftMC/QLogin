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
 * 鏁版嵁搴撶鐞嗗櫒 - 浣跨敤 SQLite 瀛樺偍鐜╁鏁版嵁
 * 鏁版嵁鏂囦欢: config/loginmod/players.db
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
     * 鍒濆鍖栨暟鎹簱绠＄悊鍣?     */
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
     * 杩炴帴鏁版嵁搴撳苟鍒涘缓琛?     */
    public void connect() {
        try {
            // 纭繚鐩綍瀛樺湪
            Files.createDirectories(dbPath.getParent());

            // 寤虹珛杩炴帴
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString());

            // 鍚敤 WAL 妯″紡鎻愬崌鎬ц兘
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
            }

            createTables();
            LOGGER.info("鏁版嵁搴撳凡杩炴帴: {}", dbPath);
        } catch (Exception e) {
            LOGGER.error("鏁版嵁搴撹繛鎺ュけ璐?, e);
            throw new RuntimeException("鏃犳硶杩炴帴鍒版暟鎹簱", e);
        }
    }

    /**
     * 鍒涘缓鏁版嵁琛?     */
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

        // 鍒涘缓绱㈠紩
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_username ON players(username)");
        }
    }

    /**
     * 妫€鏌ョ帺瀹舵槸鍚﹀凡娉ㄥ唽
     */
    public boolean isPlayerRegistered(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.error("妫€鏌ョ帺瀹舵敞鍐岀姸鎬佸け璐?, e);
            return false;
        }
    }

    /**
     * 娉ㄥ唽鏂扮帺瀹?     */
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

            // IP 鍘嗗彶
            List<String> ips = new ArrayList<>();
            ips.add(ip);
            pstmt.setString(6, GSON.toJson(ips));

            pstmt.executeUpdate();
            LOGGER.info("鐜╁ {} ({}) 宸叉敞鍐?, username, uuid);
            return true;
        } catch (SQLException e) {
            LOGGER.error("娉ㄥ唽鐜╁澶辫触", e);
            return false;
        }
    }

    /**
     * 楠岃瘉鐜╁瀵嗙爜
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
            LOGGER.error("楠岃瘉瀵嗙爜澶辫触", e);
        }
        return false;
    }

    /**
     * 鏇存柊鏈€鍚庣櫥褰曟椂闂村拰 IP
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
            LOGGER.error("鏇存柊鐧诲綍淇℃伅澶辫触", e);
        }
    }

    /**
     * 淇敼瀵嗙爜
     */
    public boolean changePassword(UUID uuid, String newPassword) {
        String hash = PasswordHasher.createPasswordHash(newPassword);
        String sql = "UPDATE players SET password_hash = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, hash);
            pstmt.setString(2, uuid.toString());
            int updated = pstmt.executeUpdate();
            LOGGER.info("鐜╁ {} 瀵嗙爜宸蹭慨鏀?, uuid);
            return updated > 0;
        } catch (SQLException e) {
            LOGGER.error("淇敼瀵嗙爜澶辫触", e);
            return false;
        }
    }

    /**
     * 澧炲姞鐧诲綍澶辫触璁℃暟
     */
    public int incrementFailCount(UUID uuid) {
        String sql = "UPDATE players SET login_fail_count = login_fail_count + 1 WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("澧炲姞澶辫触璁℃暟澶辫触", e);
        }

        // 鑾峰彇鏈€鏂拌鏁?        String querySql = "SELECT login_fail_count FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(querySql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("login_fail_count");
            }
        } catch (SQLException e) {
            LOGGER.error("鑾峰彇澶辫触璁℃暟澶辫触", e);
        }
        return 0;
    }

    /**
     * 閲嶇疆澶辫触璁℃暟
     */
    public void resetFailCount(UUID uuid) {
        String sql = "UPDATE players SET login_fail_count = 0 WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("閲嶇疆澶辫触璁℃暟澶辫触", e);
        }
    }

    /**
     * 瑙ｉ櫎鐜╁娉ㄥ唽锛堝己鍒舵敞閿€锛?     */
    public boolean unregisterPlayer(String username) {
        String sql = "DELETE FROM players WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                LOGGER.info("鐜╁ {} 宸茶寮哄埗娉ㄩ攢", username);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("寮哄埗娉ㄩ攢鐜╁澶辫触", e);
        }
        return false;
    }

    /**
     * 閫氳繃 UUID 瑙ｉ櫎娉ㄥ唽
     */
    public boolean unregisterPlayerByUuid(UUID uuid) {
        String sql = "DELETE FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("寮哄埗娉ㄩ攢鐜╁澶辫触", e);
            return false;
        }
    }

    /**
     * 鑾峰彇鐜╁淇℃伅锛堢敤浜庣鐞嗗懡浠わ級
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
            LOGGER.error("鑾峰彇鐜╁淇℃伅澶辫触", e);
        }
        return null;
    }

    /**
     * 鑾峰彇鐜╁鍚?     */
    public String getUsername(UUID uuid) {
        String sql = "SELECT username FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            LOGGER.error("鑾峰彇鐜╁鍚嶅け璐?, e);
        }
        return null;
    }

    /**
     * 妫€鏌ョ帺瀹舵槸鍚﹁灏佺
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
                        // 灏佺宸茶繃鏈燂紝鑷姩瑙ｅ皝
                        unbanPlayer(uuid);
                        return false;
                    }
                    return true; // 姘镐箙灏佺
                }
            }
        } catch (SQLException e) {
            LOGGER.error("妫€鏌ュ皝绂佺姸鎬佸け璐?, e);
        }
        return false;
    }

    /**
     * 灏佺鐜╁
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
            LOGGER.error("灏佺鐜╁澶辫触", e);
        }
    }

    /**
     * 瑙ｅ皝鐜╁
     */
    public void unbanPlayer(UUID uuid) {
        String sql = "UPDATE players SET is_banned = 0, ban_reason = NULL, ban_expiry = NULL WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("瑙ｅ皝鐜╁澶辫触", e);
        }
    }

    /**
     * 鑾峰彇鏁版嵁搴撹繛鎺ワ紙浠呬緵鍐呴儴鏌ヨ浣跨敤锛?     */
    public java.sql.Connection getConnection() {
        return connection;
    }

    /**
     * 鍏抽棴鏁版嵁搴撹繛鎺?     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("鏁版嵁搴撹繛鎺ュ凡鍏抽棴");
            }
        } catch (SQLException e) {
            LOGGER.error("鍏抽棴鏁版嵁搴撹繛鎺ュけ璐?, e);
        }
    }
}