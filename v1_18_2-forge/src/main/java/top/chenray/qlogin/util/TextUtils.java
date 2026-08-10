package top.chenray.qlogin.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 文本工具类 - 提供多语言彩色格式化输出
 * Forge 1.18.2 使用 Mojang 官方映射:
 *   TextComponent       ← Fabric 中 Text
 *   Component.literal()  ← Fabric 中 Text.literal()
 *   MutableComponent     ← Fabric 中 MutableText
 */
public class TextUtils {

    /**
     * 创建文本组件
     */
    public static TextComponent literal(String text) {
        return new TextComponent(text);
    }

    /**
     * 创建文本组件 Supplier（用于 CommandSourceStack.sendSuccess）
     */
    public static Supplier<TextComponent> literalSupplier(String text) {
        return () -> new TextComponent(text);
    }

    /**
     * 翻译快捷方法
     */
    public static String t(String key, Object... args) {
        return LanguageManager.tr(key, args);
    }

    /**
     * 创建带参数的文本组件
     */
    public static TextComponent translatable(String key, Object... args) {
        return TextUtils.literal(LanguageManager.tr(key, args));
    }

    /**
     * 获取带前缀的消息文本
     */
    public static MutableComponent prefixed(Component text) {
        return literal(LanguageManager.tr("prefix")).append(text);
    }

    /**
     * 发送系统消息给玩家
     */
    public static void sendMessage(ServerPlayer player, Component message, UUID uuid) {
        player.sendMessage(message, uuid);
    }

    /**
     * 发送翻译消息（带前缀）
     */
    public static void sendMsg(ServerPlayer player, UUID uuid, String key, Object... args) {
        player.sendMessage(prefixed(literal(LanguageManager.tr(key, args))), uuid);
    }

    /**
     * 发送错误消息
     */
    public static void sendError(ServerPlayer player, UUID uuid, String key, Object... args) {
        player.sendMessage(prefixed(literal(LanguageManager.tr(key, args))), uuid);
    }

    /**
     * 发送成功消息
     */
    public static void sendSuccess(ServerPlayer player, UUID uuid, String key, Object... args) {
        player.sendMessage(prefixed(literal(LanguageManager.tr(key, args))), uuid);
    }

    /**
     * 发送警告消息
     */
    public static void sendWarning(ServerPlayer player, UUID uuid, String key, Object... args) {
        player.sendMessage(prefixed(literal(LanguageManager.tr(key, args))), uuid);
    }

    /**
     * 发送 ActionBar 消息
     * Forge 1.18.2: ServerPlayer 的 displayClientMessage 第二个参数 true = actionbar
     */
    public static void sendActionBar(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(literal(LanguageManager.tr(key, args)), true);
    }

    /**
     * 发送标题消息（大标题 + 子标题）
     * Forge 1.18.2 原生支持发送标题包
     */
    public static void sendTitle(ServerPlayer player, String title, String subtitle) {
        player.displayClientMessage(literal("§6" + title + " §e" + subtitle), false);
    }

    // ==================== 静态 Component 工厂方法 ====================

    /**
     * 创建绿色成功文本
     */
    public static MutableComponent success(String text) {
        return literal("§a" + text);
    }

    /**
     * 创建红色错误文本
     */
    public static MutableComponent error(String text) {
        return literal("§c" + text);
    }

    /**
     * 创建黄色警告文本
     */
    public static MutableComponent warning(String text) {
        return literal("§e" + text);
    }

    /**
     * 创建蓝色信息文本
     */
    public static MutableComponent info(String text) {
        return literal("§b" + text);
    }
}
