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
 * 登录系统配置
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
     * 加载配置
     */
    public static ModConfig load(Path configDir) {
        configPath = configDir.resolve("loginmod.json");
        Logger logger = LoginMod.LOGGER;

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                instance = GSON.fromJson(json, ModConfig.class);
                logger.info("配置已加载: {}", configPath);
            } catch (Exception e) {
                logger.error("加载配置失败，使用默认配置", e);
                instance = new ModConfig();
            }
        } else {
            instance = new ModConfig();
            save();
            logger.info("已创建默认配置文件: {}", configPath);
        }

        return instance;
    }

    /**
     * 获取配置实例
     */
    public static ModConfig getInstance() {
        return instance;
    }

    /**
     * 保存配置
     */
    public static void save() {
        if (configPath == null || instance == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(instance));
        } catch (IOException e) {
            LoginMod.LOGGER.error("保存配置失败", e);
        }
    }
}
