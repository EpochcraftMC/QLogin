package top.chenray.qlogin.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import top.chenray.qlogin.LoginMod;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 语言管理器 - 支持多语言 (1.12.2 Forge)
 * 语言文件位于 assets/qlogin/lang/{locale}.json
 * 注意: 1.12.2 使用 Log4j 而非 SLF4J
 */
public class LanguageManager {

    private static final Logger LOGGER = LoginMod.LOGGER;
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Map<String, String> translations = new HashMap<>();
    private static String currentLocale = "zh_cn";

    public static void init() {
        currentLocale = top.chenray.qlogin.config.ModConfig.getInstance().getLanguage();
        loadLocale(currentLocale);
    }

    public static void reload() {
        currentLocale = top.chenray.qlogin.config.ModConfig.getInstance().getLanguage();
        translations.clear();
        loadLocale(currentLocale);
    }

    private static void loadLocale(String locale) {
        String path = "/assets/qlogin/lang/" + locale + ".json";
        try (InputStreamReader reader = new InputStreamReader(
                LanguageManager.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            if (reader == null) {
                LOGGER.warn("未找到语言文件: {}, 使用 zh_cn", path);
                loadLocale("zh_cn");
                return;
            }
            Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
            if (loaded != null) {
                translations.putAll(loaded);
                LOGGER.info("已加载语言: {} ({} 条)", locale, translations.size());
            }
        } catch (Exception e) {
            LOGGER.error("加载语言文件失败: {}", path, e);
        }

        if (translations.isEmpty()) {
            loadDefault();
        }
    }

    public static String tr(String key, Object... args) {
        String text = translations.get(key);
        if (text == null) {
            return "§c{" + key + "}§r";
        }
        if (args.length > 0) {
            text = String.format(text, args);
        }
        return text;
    }

    public static String getCurrentLocale() {
        return currentLocale;
    }

    private static void loadDefault() {
        translations.put("prefix", "§7[§bQLogin§7]§r ");
        translations.put("login.registered", "您已注册，使用 §6/login <密码> §b登录");
        translations.put("login.unregistered", "请先注册: §6/register <密码> <确认密码>");
        translations.put("login.success", "§a✔ 登录成功！欢迎回来 §e%s");
        translations.put("login.fail", "§c✘ 密码错误，请重试 (剩余次数: §e%d§c)");
        translations.put("login.already", "§e⚠ 您已经登录了");
        translations.put("login.timeout_kick", "§c§l登录超时！\n§7请重新连接并使用 /login 登录");
        translations.put("register.success", "§a✔ 注册成功！欢迎 §e%s");
        translations.put("register.fail", "§c✘ 注册失败，请重试");
        translations.put("register.exists", "§c✘ 该账号已注册，请使用 /login 登录");
        translations.put("register.password_mismatch", "§c✘ 两次输入的密码不一致");
        translations.put("register.password_length", "§c✘ 密码长度须在 %d-%d 个字符之间");
        translations.put("logout.success", "§e⚠ 您已成功登出");
        translations.put("logout.not_logged", "§c✘ 您还没有登录");
        translations.put("password.change_success", "§a✔ 密码修改成功");
        translations.put("password.wrong_old", "§c✘ 旧密码错误");
        translations.put("password.same", "§c✘ 新密码不能与旧密码相同");
        translations.put("command.blocked", "§c✘ 请先登录后再执行命令！");
        translations.put("chat.blocked", "§c✘ 请先登录后再发送聊天消息！");
        translations.put("interact.blocked", "§c⚠ 请先登录后再与方块交互！");
        translations.put("interact.entity_blocked", "§c⚠ 请先登录后再与实体交互！");
        translations.put("actionbar.register", "§c⚠ §e请注册账号 §6/register <密码> <确认密码>");
        translations.put("actionbar.login", "§c⚠ §e请登录账号 §6/login <密码>");
        translations.put("actionbar.urgent", "§c⚠ §e请立即%s！§7(§c%d§7)");
        translations.put("actionbar.register_urgent", "注册");
        translations.put("actionbar.login_urgent", "登录");
        translations.put("admin.reload", "§a✔ 配置已重新加载");
        translations.put("admin.reload_fail", "§c✘ 配置重载失败，请检查配置文件格式");
        translations.put("admin.unregister", "§a✔ 已强制注销玩家 §e%s");
        translations.put("admin.unregister_not_found", "§c✘ 未找到玩家 §e%s§c 的注册信息");
        translations.put("admin.reset_password", "§a✔ 已重置玩家 §e%s§a 的密码");
        translations.put("admin.reset_password_notify", "§e⚠ 管理员 §e%s§e 已重置您的密码");
        translations.put("admin.reset_password_new", "§bℹ 新密码: §e%s§b，请尽快修改");
        translations.put("progress.verifying", "§7[§bQLogin§7] §b正在验证...");
        translations.put("progress.registering", "§7[§bQLogin§7] §b正在注册...");
        translations.put("progress.changing_password", "§7[§bQLogin§7] §b正在修改密码...");
        translations.put("welcome.title_register", "欢迎来到服务器！");
        translations.put("welcome.sub_register", "请先注册账号");
        translations.put("welcome.title_login", "欢迎回来！");
        translations.put("welcome.sub_login", "请登录账号");
        translations.put("kick.unregistered", "§e您的账号已被管理员强制注销，请重新注册");
        translations.put("ban.ip_kick", "§c您的 IP 已被临时封禁，请稍后再试");
        translations.put("ban.too_many_attempts", "§c密码错误次数过多，IP 已被临时封禁");
        translations.put("failcount.remaining", "剩余次数: §e%d");
        translations.put("info.uuid", "§7UUID: §f%s");
        translations.put("info.username", "§7用户名: §e%s");
        translations.put("info.register_time", "§7注册时间: §b%s");
        translations.put("info.last_login", "§7最后登录: §b%s");
        translations.put("info.last_login_none", "无");
        translations.put("info.login_fail", "§7登录失败: §c%d");
        translations.put("info.ip_history", "§7IP历史: §f%s");
    }
}
