package top.chenray.qlogin.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * 文本工具类 - 提供多语言彩色格式化输出 (Mojang 映射版)
 * NeoForge 1.21.1 使用 Mojang 官方映射
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
    public static MutableComponent prefixed(Component text) {
        return Component.literal(LanguageManager.tr("prefix")).append(text);
    }

    /**
     * 发送翻译消息（带前缀）给玩家
     */
    public static void sendMsg(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(Component.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 发送翻译消息（带前缀）给命令源
     */
    public static void sendMsg(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> prefixed(Component.literal(LanguageManager.tr(key, args))), false);
    }

    /**
     * 发送错误消息给玩家
     */
    public static void sendError(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(Component.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 发送错误消息给命令源
     */
    public static void sendFailure(CommandSourceStack source, Component message) {
        source.sendFailure(message);
    }

    /**
     * 发送成功消息给玩家
     */
    public static void sendSuccess(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(Component.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 发送警告消息给玩家
     */
    public static void sendWarning(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(Component.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 发送 ActionBar 消息给玩家
     */
    public static void sendActionBar(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.literal(LanguageManager.tr(key, args)), true);
    }

    /**
     * 发送标题消息（大标题 + 子标题）
     * NeoForge 1.21.1 直接使用 displayClientMessage 作为简化实现
     */
    public static void sendTitle(ServerPlayer player, String title, String subtitle) {
        player.displayClientMessage(Component.literal("§6" + title + " §e" + subtitle), false);
    }

    // ==================== 静态 Component 工厂方法 ====================

    public static MutableComponent success(String text) {
        return Component.literal("§a" + text);
    }

    public static MutableComponent error(String text) {
        return Component.literal("§c" + text);
    }

    public static MutableComponent warning(String text) {
        return Component.literal("§e" + text);
    }

    public static MutableComponent info(String text) {
        return Component.literal("§b" + text);
    }
}
