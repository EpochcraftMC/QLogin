package top.chenray.qlogin.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * 文本工具类 - 提供多语言彩色格式化输出 (1.12.2 Forge)
 * 注意: 1.12.2 使用 ITextComponent / TextComponentString
 * 而不是 Fabric 的 Text.literal() / MutableText
 */
public class TextUtils {

    public static String t(String key, Object... args) {
        return LanguageManager.tr(key, args);
    }

    /**
     * 获取带前缀的消息文本
     */
    public static ITextComponent prefixed(ITextComponent text) {
        return new TextComponentString(LanguageManager.tr("prefix")).appendSibling(text);
    }

    /**
     * 发送翻译消息（带前缀）
     */
    public static void sendMsg(EntityPlayerMP player, String key, Object... args) {
        player.sendMessage(prefixed(new TextComponentString(LanguageManager.tr(key, args))));
    }

    /**
     * 发送错误消息
     */
    public static void sendError(EntityPlayerMP player, String key, Object... args) {
        player.sendMessage(prefixed(new TextComponentString("§c" + LanguageManager.tr(key, args))));
    }

    /**
     * 发送成功消息
     */
    public static void sendSuccess(EntityPlayerMP player, String key, Object... args) {
        player.sendMessage(prefixed(new TextComponentString("§a" + LanguageManager.tr(key, args))));
    }

    /**
     * 发送警告消息
     */
    public static void sendWarning(EntityPlayerMP player, String key, Object... args) {
        player.sendMessage(prefixed(new TextComponentString("§e" + LanguageManager.tr(key, args))));
    }

    /**
     * 发送 ActionBar 消息
     * 1.12.2 使用 SPacketChat 配合 ChatType.GAME_INFO
     */
    public static void sendActionBar(EntityPlayerMP player, String key, Object... args) {
        SPacketChat packet = new SPacketChat(
            new TextComponentString(LanguageManager.tr(key, args)),
            net.minecraft.util.text.ChatType.GAME_INFO
        );
        player.connection.sendPacket(packet);
    }

    /**
     * 发送标题消息
     */
    public static void sendTitle(EntityPlayerMP player, String title, String subtitle) {
        TitleHelper.getInstance().sendTitle(player, title, subtitle);
    }

    // ==================== 静态 ITextComponent 工厂方法 ====================

    public static ITextComponent success(String text) {
        return new TextComponentString("§a" + text);
    }

    public static ITextComponent error(String text) {
        return new TextComponentString("§c" + text);
    }

    public static ITextComponent warning(String text) {
        return new TextComponentString("§e" + text);
    }

    public static ITextComponent info(String text) {
        return new TextComponentString("§b" + text);
    }
}
