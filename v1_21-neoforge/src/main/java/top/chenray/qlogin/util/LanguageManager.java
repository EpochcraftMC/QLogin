package top.chenray.qlogin.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import top.chenray.qlogin.config.ModConfig;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 语言管理器 - 支持多语言
 * 语言文件位于 assets/qlogin/lang/{locale}.json
 */
public class LanguageManager {

    private static final Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Map<String, String> translations = new HashMap<>();
    private static String currentLocale = "zh_cn";

    /**
     * 初始化语言管理器
     */
    public static void init() {
        currentLocale = ModConfig.getInstance().getLanguage();
        loadLocale(currentLocale);
    }

    /**
     * 重新加载语言
     */
    public static void reload() {
        currentLocale = ModConfig.getInstance().getLanguage();
        translations.clear();
        loadLocale(currentLocale);
    }

    /**
     * 加载指定语言文件
     */
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

    /**
     * 获取翻译
     */
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

    /**
     * 获取当前语言
     */
    public static String getCurrentLocale() {
        return currentLocale;
    }

    private static void loadDefault() {
        translations.put("prefix", "§7[§bQLogin§7]§r ");
        translations.put("login.registered", "您已注册，使用 §6/login <密码> §b登录");
        translations.put("login.unregistered", "请先注册: §6/register <密码> <确认密码>");
        translations.put("login.success", "登录成功！欢迎回来 §e%s");
        translations.put("login.fail", "密码错误，请重试");
        translations.put("login.already", "您已经登录了");
        translations.put("login.timeout_kick", "§c§l登录超时！\n§7请重新连接并使用 /login 登录");
        translations.put("register.success", "注册成功！欢迎 §e%s");
        translations.put("register.fail", "注册失败，请重试");
        translations.put("register.exists", "您已经注册了，请使用 /login 登录");
        translations.put("register.password_mismatch", "两次输入的密码不一致");
        translations.put("register.password_too_short", "密码太短，至少 %d 位");
        translations.put("register.password_too_long", "密码太长，最多 %d 位");
        translations.put("logout.success", "已成功退出登录");
        translations.put("changepassword.success", "密码修改成功");
        translations.put("changepassword.wrong", "原密码错误");
        translations.put("changepassword.same", "新密码不能与旧密码相同");
        translations.put("admin.reload", "配置已重新加载");
        translations.put("admin.reset_password", "已重置 §e%s §b的密码");
        translations.put("admin.unregister", "已注销 §e%s §b的账号");
        translations.put("interact.blocked", "§c请先登录！");
        translations.put("interact.entity_blocked", "§c请先登录！");
        translations.put("chat.blocked", "§c请先登录后再发言！");
        translations.put("actionbar.register", "§b请注册 §7(/register) §e剩余时间: %s");
        translations.put("actionbar.login", "§b请登录 §7(/login) §e剩余时间: %s");
        translations.put("actionbar.register_urgent", "§c请尽快注册！");
        translations.put("actionbar.login_urgent", "§c请尽快登录！");
        translations.put("actionbar.urgent", "§c剩余 %d 秒！");
    }
}
