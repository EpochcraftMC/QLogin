package top.chenray.qlogin;

import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.TextUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录管理器 - 管理玩家登录状态、超时、IP 封禁 (1.12.2 Forge)
 * 注意: 1.12.2 使用 EntityPlayerMP 代替 ServerPlayerEntity
 *       player.getUniqueID() 代替 player.getUuid()
 *       player.getName() 返回 String
 *       player.connection 代替 player.networkHandler
 */
public class LoginManager {

    private static final Logger LOGGER = LoginMod.LOGGER;
    private static LoginManager instance;

    /** 玩家登录状态 */
    private final Map<UUID, LoginState> playerStates = new ConcurrentHashMap<>();

    /** 玩家加入时间 */
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    /** 玩家登录位置（用于传送回） */
    private final Map<UUID, double[]> loginPositions = new ConcurrentHashMap<>();

    /** IP 封禁记录 */
    private final Map<String, Long> bannedIps = new ConcurrentHashMap<>();

    /** IP 失败计数 */
    private final Map<String, Integer> ipFailCounts = new ConcurrentHashMap<>();

    /** 玩家IP映射 */
    private final Map<UUID, String> playerIps = new ConcurrentHashMap<>();

    /** 未登录玩家的操作屏蔽计数 */
    private final Map<UUID, Boolean> frozenPlayers = new ConcurrentHashMap<>();

    private LoginManager() {}

    public static synchronized LoginManager getInstance() {
        if (instance == null) {
            instance = new LoginManager();
        }
        return instance;
    }

    // ==================== 状态管理 ====================

    public void onPlayerJoin(EntityPlayerMP player) {
        UUID uuid = player.getUniqueID();
        String ip = getPlayerIp(player);

        playerIps.put(uuid, ip);
        joinTimes.put(uuid, Instant.now().toEpochMilli());
        frozenPlayers.put(uuid, true);

        if (DatabaseManager.getInstance().isPlayerRegistered(uuid)) {
            playerStates.put(uuid, LoginState.NOT_LOGGED_IN);
            LOGGER.info("玩家 {} 已注册，等待登录", player.getName());
        } else {
            playerStates.put(uuid, LoginState.UNREGISTERED);
            LOGGER.info("玩家 {} 未注册，提示注册", player.getName());
        }

        if (isIpBanned(ip)) {
            player.connection.disconnect(new TextComponentString(TextUtils.t("ban.ip_kick")));
            return;
        }

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

    public void onPlayerDisconnect(EntityPlayerMP player) {
        UUID uuid = player.getUniqueID();
        playerStates.remove(uuid);
        joinTimes.remove(uuid);
        loginPositions.remove(uuid);
        frozenPlayers.remove(uuid);
        playerIps.remove(uuid);
    }

    public LoginState getState(UUID uuid) {
        return playerStates.getOrDefault(uuid, LoginState.UNREGISTERED);
    }

    public void setLoggedIn(UUID uuid) {
        playerStates.put(uuid, LoginState.LOGGED_IN);
        frozenPlayers.remove(uuid);
        loginPositions.remove(uuid);
    }

    public void setLoggedOut(UUID uuid) {
        playerStates.put(uuid, LoginState.NOT_LOGGED_IN);
        frozenPlayers.put(uuid, true);
    }

    public void setUnregistered(UUID uuid) {
        playerStates.put(uuid, LoginState.UNREGISTERED);
        frozenPlayers.put(uuid, true);
    }

    public boolean isLoggedIn(UUID uuid) {
        return playerStates.get(uuid) == LoginState.LOGGED_IN;
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.getOrDefault(uuid, false);
    }

    // ==================== 登录位置 ====================

    public void recordLoginPosition(EntityPlayerMP player) {
        loginPositions.put(player.getUniqueID(), new double[]{
            player.posX, player.posY, player.posZ, (double) player.rotationYaw, (double) player.rotationPitch
        });
    }

    public double[] getLoginPosition(UUID uuid) {
        return loginPositions.get(uuid);
    }

    // ==================== 超时检测 ====================

    public boolean isLoginTimeout(UUID uuid) {
        Long joinTime = joinTimes.get(uuid);
        if (joinTime == null) return false;
        LoginState state = playerStates.get(uuid);
        if (state == LoginState.LOGGED_IN) return false;
        int timeout = ModConfig.getInstance().getLoginTimeoutSeconds();
        return (Instant.now().toEpochMilli() - joinTime) > (timeout * 1000L);
    }

    public long getRemainingTime(UUID uuid) {
        Long joinTime = joinTimes.get(uuid);
        if (joinTime == null) return 0;
        int timeout = ModConfig.getInstance().getLoginTimeoutSeconds();
        long remaining = (timeout * 1000L) - (Instant.now().toEpochMilli() - joinTime);
        return Math.max(0, remaining / 1000);
    }

    // ==================== IP 操作 ====================

    public String getPlayerIp(EntityPlayerMP player) {
        try {
            if (player.connection != null && player.connection.netManager != null) {
                java.net.SocketAddress addr = player.connection.netManager.getRemoteAddress();
                if (addr instanceof InetSocketAddress) {
                    return ((InetSocketAddress) addr).getAddress().getHostAddress();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "0.0.0.0";
    }

    public boolean recordLoginFail(UUID uuid) {
        String ip = playerIps.get(uuid);
        if (ip == null) return false;

        int dbFails = DatabaseManager.getInstance().incrementFailCount(uuid);
        int ipFails = ipFailCounts.getOrDefault(ip, 0) + 1;
        ipFailCounts.put(ip, ipFails);

        int maxAttempts = ModConfig.getInstance().getMaxLoginAttempts();

        if (ipFails >= maxAttempts) {
            banIp(ip, ModConfig.getInstance().getBanDurationSeconds());
            LOGGER.warn("IP {} 因登录失败次数过多已被临时封禁 {} 秒", ip, ModConfig.getInstance().getBanDurationSeconds());
            return true;
        }

        return false;
    }

    public void resetLoginFails(UUID uuid) {
        String ip = playerIps.get(uuid);
        if (ip != null) {
            ipFailCounts.remove(ip);
        }
        DatabaseManager.getInstance().resetFailCount(uuid);
    }

    public void banIp(String ip, long durationSeconds) {
        long expiry = Instant.now().toEpochMilli() + (durationSeconds * 1000);
        bannedIps.put(ip, expiry);
    }

    public boolean isIpBanned(String ip) {
        Long expiry = bannedIps.get(ip);
        if (expiry == null) return false;
        if (Instant.now().toEpochMilli() > expiry) {
            bannedIps.remove(ip);
            return false;
        }
        return true;
    }

    public boolean isPlayerBanned(UUID uuid) {
        return DatabaseManager.getInstance().isBanned(uuid);
    }

    public void cleanExpiredBans() {
        long now = Instant.now().toEpochMilli();
        bannedIps.entrySet().removeIf(entry -> now > entry.getValue());
    }

    // ==================== 白名单命令 ====================

    public static boolean isWhitelistedCommand(String command) {
        String cmd = command.toLowerCase().trim();

        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }

        String commandName = cmd.split(" ")[0];

        return commandName.equals("register") || commandName.equals("reg") ||
               commandName.equals("login") || commandName.equals("l") || commandName.equals("log") ||
               commandName.equals("logout") || commandName.equals("changepassword") ||
               commandName.equals("loginmod");
    }
}
