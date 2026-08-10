package top.chenray.qlogin;

import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录管理器 - 管理玩家登录状态、超时、IP 封禁 (Mojang 映射版)
 */
public class LoginManager {

    private static final Logger LOGGER = LoginMod.LOGGER;
    private static LoginManager instance;

    /** 玩家登录状态 */
    private final Map<UUID, LoginState> playerStates = new ConcurrentHashMap<>();

    /** 玩家加入时间（用于超时检测） */
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    /** 玩家登录位置（用于传送回） */
    private final Map<UUID, double[]> loginPositions = new ConcurrentHashMap<>();

    /** IP 封禁记录 <IP, 封禁到期时间戳> */
    private final Map<String, Long> bannedIps = new ConcurrentHashMap<>();

    /** IP 失败计数 <IP, 失败次数> */
    private final Map<String, Integer> ipFailCounts = new ConcurrentHashMap<>();

    /** 玩家IP映射 */
    private final Map<UUID, String> playerIps = new ConcurrentHashMap<>();

    /** 未登录玩家的操作屏蔽计数（用于取消事件时检查） */
    private final Map<UUID, Boolean> frozenPlayers = new ConcurrentHashMap<>();

    private LoginManager() {}

    public static synchronized LoginManager getInstance() {
        if (instance == null) {
            instance = new LoginManager();
        }
        return instance;
    }

    // ==================== 状态管理 ====================

    /**
     * 玩家加入服务器
     */
    public void onPlayerJoin(ServerPlayer player) {
        UUID uuid = player.getUUID();
        String ip = getPlayerIp(player);

        playerIps.put(uuid, ip);
        joinTimes.put(uuid, Instant.now().toEpochMilli());
        frozenPlayers.put(uuid, true);

        if (DatabaseManager.getInstance().isPlayerRegistered(uuid)) {
            playerStates.put(uuid, LoginState.NOT_LOGGED_IN);
            LOGGER.info("玩家 {} 已注册，等待登录", player.getScoreboardName());
        } else {
            playerStates.put(uuid, LoginState.UNREGISTERED);
            LOGGER.info("玩家 {} 未注册，提示注册", player.getScoreboardName());
        }

        // 检查 IP 是否被封禁（内存封禁）
        if (isIpBanned(ip)) {
            player.connection.disconnect(Component.literal(TextUtils.t("ban.ip_kick")));
            return;
        }

        // 发送欢迎消息
        LoginState state = playerStates.get(uuid);
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
     * 玩家离开服务器
     */
    public void onPlayerDisconnect(ServerPlayer player) {
        UUID uuid = player.getUUID();
        playerStates.remove(uuid);
        joinTimes.remove(uuid);
        loginPositions.remove(uuid);
        frozenPlayers.remove(uuid);
        playerIps.remove(uuid);
    }

    /**
     * 获取玩家登录状态
     */
    public LoginState getState(UUID uuid) {
        return playerStates.getOrDefault(uuid, LoginState.UNREGISTERED);
    }

    /**
     * 设置玩家已登录
     */
    public void setLoggedIn(UUID uuid) {
        playerStates.put(uuid, LoginState.LOGGED_IN);
        frozenPlayers.remove(uuid);
        loginPositions.remove(uuid);
    }

    /**
     * 设置玩家已登出
     */
    public void setLoggedOut(UUID uuid) {
        playerStates.put(uuid, LoginState.NOT_LOGGED_IN);
        frozenPlayers.put(uuid, true);
    }

    /**
     * 设置玩家为未注册状态（管理员注销时）
     */
    public void setUnregistered(UUID uuid) {
        playerStates.put(uuid, LoginState.UNREGISTERED);
        frozenPlayers.put(uuid, true);
    }

    /**
     * 判断玩家是否已登录
     */
    public boolean isLoggedIn(UUID uuid) {
        return playerStates.get(uuid) == LoginState.LOGGED_IN;
    }

    /**
     * 判断玩家是否被冻结（禁止操作）
     */
    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.getOrDefault(uuid, false);
    }

    // ==================== 登录位置 ====================

    /**
     * 记录玩家登录时的位置
     */
    public void recordLoginPosition(ServerPlayer player) {
        loginPositions.put(player.getUUID(), new double[]{
            player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()
        });
    }

    /**
     * 获取玩家登录位置
     */
    public double[] getLoginPosition(UUID uuid) {
        return loginPositions.get(uuid);
    }

    // ==================== 超时检测 ====================

    /**
     * 检查玩家是否登录超时
     */
    public boolean isLoginTimeout(UUID uuid) {
        Long joinTime = joinTimes.get(uuid);
        if (joinTime == null) return false;
        LoginState state = playerStates.get(uuid);
        if (state == LoginState.LOGGED_IN) return false;
        int timeout = ModConfig.getInstance().getLoginTimeoutSeconds();
        return (Instant.now().toEpochMilli() - joinTime) > (timeout * 1000L);
    }

    /**
     * 获取剩余登录时间
     */
    public long getRemainingTime(UUID uuid) {
        Long joinTime = joinTimes.get(uuid);
        if (joinTime == null) return 0;
        int timeout = ModConfig.getInstance().getLoginTimeoutSeconds();
        long remaining = (timeout * 1000L) - (Instant.now().toEpochMilli() - joinTime);
        return Math.max(0, remaining / 1000);
    }

    // ==================== IP 封禁 ====================

    /**
     * 获取玩家 IP 地址
     */
    public String getPlayerIp(ServerPlayer player) {
        try {
            if (player.connection != null) {
                var addr = player.connection.getRemoteAddress();
                if (addr instanceof InetSocketAddress socketAddr) {
                    return socketAddr.getAddress().getHostAddress();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "0.0.0.0";
    }

    /**
     * 记录登录失败
     * @return true 如果触发封禁
     */
    public boolean recordLoginFail(UUID uuid) {
        String ip = playerIps.get(uuid);
        if (ip == null) return false;

        // 数据库失败计数
        int dbFails = DatabaseManager.getInstance().incrementFailCount(uuid);

        // 内存 IP 失败计数
        int ipFails = ipFailCounts.getOrDefault(ip, 0) + 1;
        ipFailCounts.put(ip, ipFails);

        int maxAttempts = ModConfig.getInstance().getMaxLoginAttempts();

        // 检查是否触发封禁
        if (ipFails >= maxAttempts) {
            banIp(ip, ModConfig.getInstance().getBanDurationSeconds());
            LOGGER.warn("IP {} 因登录失败次数过多已被临时封禁 {} 秒", ip, ModConfig.getInstance().getBanDurationSeconds());
            return true;
        }

        return false;
    }

    /**
     * 重置登录失败计数
     */
    public void resetLoginFails(UUID uuid) {
        String ip = playerIps.get(uuid);
        if (ip != null) {
            ipFailCounts.remove(ip);
        }
        DatabaseManager.getInstance().resetFailCount(uuid);
    }

    // ==================== IP 封禁管理 ====================

    /**
     * 封禁 IP
     */
    public void banIp(String ip, int durationSeconds) {
        long expiry = Instant.now().toEpochMilli() + (durationSeconds * 1000L);
        bannedIps.put(ip, expiry);
        LOGGER.info("IP {} 已被封禁 {} 秒", ip, durationSeconds);
    }

    /**
     * 检查 IP 是否被封禁
     */
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
     * 获取 IP 失败计数
     */
    public int getIpFailCount(String ip) {
        return ipFailCounts.getOrDefault(ip, 0);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取在线玩家
     */
    public ServerPlayer getPlayer(UUID uuid) {
        MinecraftServer server = LoginMod.getServer();
        if (server != null) {
            return server.getPlayerList().getPlayer(uuid);
        }
        return null;
    }

    /**
     * 获取玩家名（通过 UUID）
     */
    public String getPlayerName(UUID uuid) {
        MinecraftServer server = LoginMod.getServer();
        if (server != null) {
            var player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                return player.getScoreboardName();
            }
        }
        return null;
    }
}
