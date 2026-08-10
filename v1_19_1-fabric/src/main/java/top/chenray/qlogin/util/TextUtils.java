package top.chenray.qlogin.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 文本工具类 - 提供多语言彩色格式化输出
 */
public class TextUtils {

    /**
     * 翻译快捷方法
     */
    public static String t(String key, Object... args) {
        return LanguageManager.tr(key, args);
    }

    /**
     * 获取带前缀的消息文本
     */
    public static MutableText prefixed(Text text) {
        return Text.literal(LanguageManager.tr("prefix")).append(text);
    }

    /**
     * 发送系统消息给玩家
     */
    public static void sendMessage(ServerPlayerEntity player, Text message) {
        player.sendMessage(message);
    }

    /**
     * 发送翻译消息（带前缀）
     */
    public static void sendMsg(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(prefixed(Text.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 发送错误消息
     */
    public static void sendError(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(prefixed(Text.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 发送成功消息
     */
    public static void sendSuccess(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(prefixed(Text.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 发送警告消息
     */
    public static void sendWarning(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(prefixed(Text.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 发送 ActionBar 消息
     */
    public static void sendActionBar(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(Text.literal(LanguageManager.tr(key, args)), true);
    }

    /**
     * 发送标题消息（大标题 + 子标题）
     */
    public static void sendTitle(ServerPlayerEntity player, String title, String subtitle) {
        TitleHelper.getInstance().sendTitle(player, title, subtitle);
    }

    // ==================== 静态 Text 工厂方法（用于向 ServerCommandSource 发送消息） ====================

    /**
     * 创建绿色成功文本
     */
    public static MutableText success(String text) {
        return Text.literal("§a" + text);
    }

    /**
     * 创建红色错误文本
     */
    public static MutableText error(String text) {
        return Text.literal("§c" + text);
    }

    /**
     * 创建黄色警告文本
     */
    public static MutableText warning(String text) {
        return Text.literal("§e" + text);
    }

    /**
     * 创建蓝色信息文本
     */
    public static MutableText info(String text) {
        return Text.literal("§b" + text);
    }
}
