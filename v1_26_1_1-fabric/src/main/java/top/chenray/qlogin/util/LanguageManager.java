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
 * 璇█绠＄悊鍣?- 鏀寔澶氳瑷€
 * 璇█鏂囦欢浣嶄簬 assets/qlogin/lang/{locale}.json
 */
public class LanguageManager {

    private static final Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Map<String, String> translations = new HashMap<>();
    private static String currentLocale = "zh_cn";

    /**
     * 鍒濆鍖栬瑷€绠＄悊鍣?     */
    public static void init() {
        currentLocale = ModConfig.getInstance().getLanguage();
        loadLocale(currentLocale);
    }

    /**
     * 閲嶆柊鍔犺浇璇█
     */
    public static void reload() {
        currentLocale = ModConfig.getInstance().getLanguage();
        translations.clear();
        loadLocale(currentLocale);
    }

    /**
     * 鍔犺浇鎸囧畾璇█鏂囦欢
     */
    private static void loadLocale(String locale) {
        String path = "/assets/qlogin/lang/" + locale + ".json";
        try (InputStreamReader reader = new InputStreamReader(
                LanguageManager.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            if (reader == null) {
                LOGGER.warn("鏈壘鍒拌瑷€鏂囦欢: {}, 浣跨敤 zh_cn", path);
                loadLocale("zh_cn");
                return;
            }
            Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
            if (loaded != null) {
                translations.putAll(loaded);
                LOGGER.info("宸插姞杞借瑷€: {} ({} 鏉?", locale, translations.size());
            }
        } catch (Exception e) {
            LOGGER.error("鍔犺浇璇█鏂囦欢澶辫触: {}", path, e);
        }

        // 纭繚鑷冲皯鏈変竴浠界炕璇?        if (translations.isEmpty()) {
            loadDefault();
        }
    }

    /**
     * 鑾峰彇缈昏瘧
     */
    public static String tr(String key, Object... args) {
        String text = translations.get(key);
        if (text == null) {
            return "搂c{" + key + "}搂r";
        }
        if (args.length > 0) {
            text = String.format(text, args);
        }
        return text;
    }

    /**
     * 鑾峰彇褰撳墠璇█
     */
    public static String getCurrentLocale() {
        return currentLocale;
    }

    private static void loadDefault() {
        translations.put("prefix", "搂7[搂bQLogin搂7]搂r ");
        translations.put("login.registered", "鎮ㄥ凡娉ㄥ唽锛屼娇鐢?搂6/login <瀵嗙爜> 搂b鐧诲綍");
        translations.put("login.unregistered", "璇峰厛娉ㄥ唽: 搂6/register <瀵嗙爜> <纭瀵嗙爜>");
        translations.put("login.success", "鐧诲綍鎴愬姛锛佹杩庡洖鏉?搂e%s");
        translations.put("login.fail", "瀵嗙爜閿欒锛岃閲嶈瘯");
        translations.put("login.already", "鎮ㄥ凡缁忕櫥褰曚簡");
        translations.put("login.timeout_kick", "搂c搂l鐧诲綍瓒呮椂锛乗n搂7璇烽噸鏂拌繛鎺ュ苟浣跨敤 /login 鐧诲綍");
        translations.put("register.success", "娉ㄥ唽鎴愬姛锛佹杩?搂e%s");
        translations.put("register.fail", "娉ㄥ唽澶辫触锛岃閲嶈瘯");
        translations.put("register.exists", "璇ヨ处鍙峰凡娉ㄥ唽锛岃浣跨敤 /login 鐧诲綍");
        translations.put("register.password_mismatch", "涓ゆ杈撳叆鐨勫瘑鐮佷笉涓€鑷?);
        translations.put("register.password_length", "瀵嗙爜闀垮害椤诲湪 %d-%d 涓瓧绗︿箣闂?);
        translations.put("logout.success", "鎮ㄥ凡鎴愬姛鐧诲嚭");
        translations.put("logout.not_logged", "鎮ㄨ繕娌℃湁鐧诲綍");
        translations.put("password.change_success", "瀵嗙爜淇敼鎴愬姛");
        translations.put("password.wrong_old", "鏃у瘑鐮侀敊璇?);
        translations.put("password.same", "鏂板瘑鐮佷笉鑳戒笌鏃у瘑鐮佺浉鍚?);
        translations.put("command.blocked", "搂c鉁?璇峰厛鐧诲綍鍚庡啀鎵ц鍛戒护锛?);
        translations.put("chat.blocked", "搂c鉁?璇峰厛鐧诲綍鍚庡啀鍙戦€佽亰澶╂秷鎭紒");
        translations.put("interact.blocked", "搂c鈿?璇峰厛鐧诲綍鍚庡啀涓庢柟鍧椾氦浜掞紒");
        translations.put("interact.entity_blocked", "搂c鈿?璇峰厛鐧诲綍鍚庡啀涓庡疄浣撲氦浜掞紒");
        translations.put("actionbar.register", "搂c鈿?搂e璇锋敞鍐岃处鍙?搂6/register <瀵嗙爜> <纭瀵嗙爜>");
        translations.put("actionbar.login", "搂c鈿?搂e璇风櫥褰曡处鍙?搂6/login <瀵嗙爜>");
        translations.put("actionbar.urgent", "搂c鈿?搂e璇风珛鍗?s锛伮?(搂c%d搂7)");
        translations.put("actionbar.register_urgent", "娉ㄥ唽");
        translations.put("actionbar.login_urgent", "鐧诲綍");
        translations.put("admin.reload", "閰嶇疆宸查噸鏂板姞杞?);
        translations.put("admin.unregister", "宸插己鍒舵敞閿€鐜╁ 搂e%s");
        translations.put("admin.unregister_not_found", "鏈壘鍒扮帺瀹?搂e%s搂c 鐨勬敞鍐屼俊鎭?);
        translations.put("admin.reset_password", "宸查噸缃帺瀹?搂e%s搂a 鐨勫瘑鐮?);
        translations.put("admin.reset_password_notify", "绠＄悊鍛?搂e%s搂e 宸查噸缃偍鐨勫瘑鐮?);
        translations.put("admin.reset_password_new", "鏂板瘑鐮? 搂e%s搂b锛岃灏藉揩淇敼");
        translations.put("progress.verifying", "姝ｅ湪楠岃瘉...");
        translations.put("progress.registering", "姝ｅ湪娉ㄥ唽...");
        translations.put("progress.changing_password", "姝ｅ湪淇敼瀵嗙爜...");
        translations.put("welcome.title_register", "娆㈣繋鏉ュ埌鏈嶅姟鍣紒");
        translations.put("welcome.title_login", "娆㈣繋鍥炴潵锛?);
        translations.put("welcome.sub_register", "璇峰厛娉ㄥ唽璐﹀彿");
        translations.put("welcome.sub_login", "璇风櫥褰曡处鍙?);
        translations.put("kick.unregistered", "搂e鎮ㄧ殑璐﹀彿宸茶绠＄悊鍛樺己鍒舵敞閿€锛岃閲嶆柊娉ㄥ唽");
        translations.put("ban.ip_kick", "搂c鎮ㄧ殑 IP 宸茶涓存椂灏佺锛岃绋嶅悗鍐嶈瘯");
        translations.put("ban.too_many_attempts", "搂c瀵嗙爜閿欒娆℃暟杩囧锛孖P 宸茶涓存椂灏佺");
        translations.put("failcount.remaining", "鍓╀綑娆℃暟: 搂e%d");
    }
}