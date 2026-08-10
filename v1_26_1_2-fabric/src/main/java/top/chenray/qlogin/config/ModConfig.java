package top.chenray.qlogin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import top.chenray.qlogin.LoginMod;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 鐧诲綍绯荤粺閰嶇疆
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig instance;
    private static Path configPath;

    @SerializedName("login_timeout_seconds")
    private int loginTimeoutSeconds = 60;

    @SerializedName("max_login_attempts")
    private int maxLoginAttempts = 5;

    @SerializedName("ban_duration_seconds")
    private int banDurationSeconds = 300;

    @SerializedName("kick_on_timeout")
    private boolean kickOnTimeout = true;

    @SerializedName("allow_spectator_on_timeout")
    private boolean allowSpectatorOnTimeout = false;

    @SerializedName("password_min_length")
    private int passwordMinLength = 4;

    @SerializedName("password_max_length")
    private int passwordMaxLength = 32;

    @SerializedName("language")
    private String language = "zh_cn";

    public int getLoginTimeoutSeconds() {
        return loginTimeoutSeconds;
    }

    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public int getBanDurationSeconds() {
        return banDurationSeconds;
    }

    public boolean isKickOnTimeout() {
        return kickOnTimeout;
    }

    public boolean isAllowSpectatorOnTimeout() {
        return allowSpectatorOnTimeout;
    }

    public int getPasswordMinLength() {
        return passwordMinLength;
    }

    public int getPasswordMaxLength() {
        return passwordMaxLength;
    }

    public String getLanguage() {
        return language;
    }

    /**
     * 鍔犺浇閰嶇疆
     */
    public static ModConfig load(Path configDir) {
        configPath = configDir.resolve("loginmod.json");
        Logger logger = LoginMod.LOGGER;

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                instance = GSON.fromJson(json, ModConfig.class);
                logger.info("閰嶇疆宸插姞杞? {}", configPath);
            } catch (Exception e) {
                logger.error("鍔犺浇閰嶇疆澶辫触锛屼娇鐢ㄩ粯璁ら厤缃?, e);
                instance = new ModConfig();
            }
        } else {
            instance = new ModConfig();
            save();
            logger.info("宸插垱寤洪粯璁ら厤缃枃浠? {}", configPath);
        }

        return instance;
    }

    /**
     * 淇濆瓨閰嶇疆鍒版枃浠?     */
    public static void save() {
        if (configPath == null || instance == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(instance));
        } catch (IOException e) {
            LoginMod.LOGGER.error("淇濆瓨閰嶇疆澶辫触", e);
        }
    }

    /**
     * 閲嶆柊鍔犺浇閰嶇疆
     */
    public static boolean reload() {
        if (configPath != null && Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                instance = GSON.fromJson(json, ModConfig.class);
                LoginMod.LOGGER.info("閰嶇疆宸查噸鏂板姞杞?);
                return true;
            } catch (Exception e) {
                LoginMod.LOGGER.error("閲嶆柊鍔犺浇閰嶇疆澶辫触", e);
                return false;
            }
        }
        return false;
    }

    public static ModConfig getInstance() {
        return instance;
    }
}