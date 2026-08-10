package top.chenray.qlogin;

import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.TextUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鐧诲綍绠＄悊鍣?- 绠＄悊鐜╁鐧诲綍鐘舵€併€佽秴鏃躲€両P 灏佺
 */
public class LoginManager {

    private static final Logger LOGGER = LoginMod.LOGGER;
    private static LoginManager instance;

    /** 鐜╁鐧诲綍鐘舵€?*/
    private final Map<UUID, LoginState> playerStates = new ConcurrentHashMap<>();

    /** 鐜╁鍔犲叆鏃堕棿锛堢敤浜庤秴鏃舵娴嬶級 */
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    /** 鐜╁鐧诲綍浣嶇疆锛堢敤浜庝紶閫佸洖锛?*/
    private final Map<UUID, double[]> loginPositions = new ConcurrentHashMap<>();

    /** IP 灏佺璁板綍 <IP, 灏佺鍒版湡鏃堕棿鎴? */
    private final Map<String, Long> bannedIps = new ConcurrentHashMap<>();

    /** IP 澶辫触璁℃暟 <IP, 澶辫触娆℃暟> */
    private final Map<String, Integer> ipFailCounts = new ConcurrentHashMap<>();

    /** 鐜╁IP鏄犲皠 */
    private final Map<UUID, String> playerIps = new ConcurrentHashMap<>();

    /** 鏈櫥褰曠帺瀹剁殑鎿嶄綔灞忚斀璁℃暟锛堢敤浜庡彇娑堜簨浠舵椂妫€鏌ワ級 */
    private final Map<UUID, Boolean> frozenPlayers = new ConcurrentHashMap<>();

    private LoginManager() {}

    public static synchronized LoginManager getInstance() {
        if (instance == null) {
            instance = new LoginManager();
        }
        return instance;
    }

    // ==================== 鐘舵€佺鐞?====================

    /**
     * 鐜╁鍔犲叆鏈嶅姟鍣?     */
    public void onPlayerJoin(ServerPlayer player) {
        UUID uuid = player.getUUID();
        String ip = getPlayerIp(player);

        playerIps.put(uuid, ip);
        joinTimes.put(uuid, Instant.now().toEpochMilli());
        frozenPlayers.put(uuid, true);

        if (DatabaseManager.getInstance().isPlayerRegistered(uuid)) {
            playerStates.put(uuid, LoginState.NOT_LOGGED_IN);
            LOGGER.info("鐜╁ {} 宸叉敞鍐岋紝绛夊緟鐧诲綍", player.getName().getString());
        } else {
            playerStates.put(uuid, LoginState.UNREGISTERED);
            LOGGER.info("鐜╁ {} 鏈敞鍐岋紝鎻愮ず娉ㄥ唽", player.getName().getString());
        }

        // 妫€鏌?IP 鏄惁琚皝绂侊紙鍐呭瓨灏佺锛?        if (isIpBanned(ip)) {
            player.connection.disconnect(Component.literal(TextUtils.t("ban.ip_kick")));
            return;
        }

        // 鍙戦€佹杩庢秷鎭?        LoginState state = playerStates.get(uuid);
        if (state == LoginState.UNREGISTERED) {
            TextUtils.sendMsg(player, "welcome.title_register");
            TextUtils.sendMsg(player, "login.unregistered");
            TextUtils.sendMsg(player, "register.password_length", ModConfig.getInstance().getPasswordMinLength(), ModConfig.getInstance().getPasswordMaxLength());
        } else {
            TextUtils.sendMsg(player, "welcome.title_login");
            TextUtils.sendMsg(player, "login.registered");
        }
    }

    /**
     * 鐜╁绂诲紑鏈嶅姟鍣?     */
    public void onPlayerDisconnect(ServerPlayer player) {
        UUID uuid = player.getUUID();
        playerStates.remove(uuid);
        joinTimes.remove(uuid);
        loginPositions.remove(uuid);
        frozenPlayers.remove(uuid);
        playerIps.remove(uuid);
    }

    /**
     * 鑾峰彇鐜╁鐧诲綍鐘舵€?     */
    public LoginState getState(UUID uuid) {
        return playerStates.getOrDefault(uuid, LoginState.UNREGISTERED);
    }

    /**
     * 璁剧疆鐜╁宸茬櫥褰?     */
    public void setLoggedIn(UUID uuid) {
        playerStates.put(uuid, LoginState.LOGGED_IN);
        frozenPlayers.remove(uuid);
        loginPositions.remove(uuid);
    }

    /**
     * 璁剧疆鐜╁宸茬櫥鍑?     */
    public void setLoggedOut(UUID uuid) {
        playerStates.put(uuid, LoginState.NOT_LOGGED_IN);
        frozenPlayers.put(uuid, true);
    }

    /**
     * 璁剧疆鐜╁涓烘湭娉ㄥ唽鐘舵€侊紙绠＄悊鍛樻敞閿€鏃讹級
     */
    public void setUnregistered(UUID uuid) {
        playerStates.put(uuid, LoginState.UNREGISTERED);
        frozenPlayers.put(uuid, true);
    }

    /**
     * 鍒ゆ柇鐜╁鏄惁宸茬櫥褰?     */
    public boolean isLoggedIn(UUID uuid) {
        return playerStates.get(uuid) == LoginState.LOGGED_IN;
    }

    /**
     * 鍒ゆ柇鐜╁鏄惁琚喕缁擄紙绂佹鎿嶄綔锛?     */
    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.getOrDefault(uuid, false);
    }

    // ==================== 鐧诲綍浣嶇疆 ====================

    /**
     * 璁板綍鐜╁鐧诲綍鏃剁殑浣嶇疆
     */
    public void recordLoginPosition(ServerPlayer player) {
        loginPositions.put(player.getUUID(), new double[]{
            player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()
        });
    }

    /**
     * 鑾峰彇鐜╁鐧诲綍浣嶇疆
     */
    public double[] getLoginPosition(UUID uuid) {
        return loginPositions.get(uuid);
    }

    // ==================== 瓒呮椂妫€娴?====================

    /**
     * 妫€鏌ョ帺瀹舵槸鍚︾櫥褰曡秴鏃?     */
    public boolean isLoginTimeout(UUID uuid) {
        Long joinTime = joinTimes.get(uuid);
        if (joinTime == null) return false;
        LoginState state = playerStates.get(uuid);
        if (state == LoginState.LOGGED_IN) return false;
        int timeout = ModConfig.getInstance().getLoginTimeoutSeconds();
        return (Instant.now().toEpochMilli() - joinTime) > (timeout * 1000L);
    }

    /**
     * 鑾峰彇鍓╀綑鐧诲綍鏃堕棿
     */
    public long getRemainingTime(UUID uuid) {
        Long joinTime = joinTimes.get(uuid);
        if (joinTime == null) return 0;
        int timeout = ModConfig.getInstance().getLoginTimeoutSeconds();
        long remaining = (timeout * 1000L) - (Instant.now().toEpochMilli() - joinTime);
        return Math.max(0, remaining / 1000);
    }

    // ==================== IP 灏佺 ====================

    /**
     * 鑾峰彇鐜╁ IP 鍦板潃
     */
    public String getPlayerIp(ServerPlayer player) {
        try {
            if (player.connection != null) {
                var addr = player.connection.getRemoteAddress();
                if (addr instanceof java.net.InetSocketAddress socketAddr) {
                    return socketAddr.getAddress().getHostAddress();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "0.0.0.0";
    }

    /**
     * 璁板綍鐧诲綍澶辫触
     * @return true 濡傛灉瑙﹀彂灏佺
     */
    public boolean recordLoginFail(UUID uuid) {
        String ip = playerIps.get(uuid);
        if (ip == null) return false;

        // 鏁版嵁搴撳け璐ヨ鏁?        int dbFails = DatabaseManager.getInstance().incrementFailCount(uuid);

        // 鍐呭瓨 IP 澶辫触璁℃暟
        int ipFails = ipFailCounts.getOrDefault(ip, 0) + 1;
        ipFailCounts.put(ip, ipFails);

        int maxAttempts = ModConfig.getInstance().getMaxLoginAttempts();

        // 妫€鏌ユ槸鍚﹁Е鍙戝皝绂?        if (ipFails >= maxAttempts) {
            banIp(ip, ModConfig.getInstance().getBanDurationSeconds());
            LOGGER.warn("IP {} 鍥犵櫥褰曞け璐ユ鏁拌繃澶氬凡琚复鏃跺皝绂?{} 绉?, ip, ModConfig.getInstance().getBanDurationSeconds());
            return true;
        }

        return false;
    }

    /**
     * 閲嶇疆鐧诲綍澶辫触璁℃暟
     */
    public void resetLoginFails(UUID uuid) {
        String ip = playerIps.get(uuid);
        if (ip != null) {
            ipFailCounts.remove(ip);
        }
        DatabaseManager.getInstance().resetFailCount(uuid);
    }

    /**
     * 灏佺 IP
     */
    public void banIp(String ip, long durationSeconds) {
        long expiry = Instant.now().toEpochMilli() + (durationSeconds * 1000);
        bannedIps.put(ip, expiry);
    }

    /**
     * 妫€鏌?IP 鏄惁琚皝绂?     */
    public boolean isIpBanned(String ip) {
        Long expiry = bannedIps.get(ip);
        if (expiry == null) return false;
        if (Instant.now().toEpochMilli() > expiry) {
            bannedIps.remove(ip);
            return false;
        }
        return true;
    }

    /**
     * 妫€鏌ョ帺瀹?IP 鏄惁琚皝绂?     */
    public boolean isPlayerBanned(UUID uuid) {
        return DatabaseManager.getInstance().isBanned(uuid);
    }

    /**
     * 娓呯悊杩囨湡灏佺
     */
    public void cleanExpiredBans() {
        long now = Instant.now().toEpochMilli();
        bannedIps.entrySet().removeIf(entry -> now > entry.getValue());
    }

    // ==================== 鐧藉悕鍗曞懡浠?====================

    /**
     * 妫€鏌ュ懡浠ゆ槸鍚﹀湪鐧藉悕鍗曞唴锛堟湭鐧诲綍鏃跺彲浠ユ墽琛岋級
     */
    public static boolean isWhitelistedCommand(String command) {
        String cmd = command.toLowerCase().trim();

        // 绉婚櫎寮€澶寸殑 /
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }

        // 鑾峰彇鍛戒护鍚嶏紙绗竴涓崟璇嶏級
        String commandName = cmd.split(" ")[0];

        return switch (commandName) {
            case "register", "reg", "login", "l", "log" -> true;
            default -> false;
        };
    }
}