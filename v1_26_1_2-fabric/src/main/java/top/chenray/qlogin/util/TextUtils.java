package top.chenray.qlogin.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * 鏂囨湰宸ュ叿绫?- 鎻愪緵澶氳瑷€褰╄壊鏍煎紡鍖栬緭鍑? */
public class TextUtils {

    /**
     * 缈昏瘧蹇嵎鏂规硶
     */
    public static String t(String key, Object... args) {
        return LanguageManager.tr(key, args);
    }

    /**
     * 鑾峰彇甯﹀墠缂€鐨勬秷鎭枃鏈?     */
    public static MutableComponent prefixed(Component text) {
        return Component.literal(LanguageManager.tr("prefix")).append(text);
    }

    /**
     * 鍙戦€佺郴缁熸秷鎭粰鐜╁
     */
    public static void sendMessage(ServerPlayer player, Component message) {
        player.sendSystemMessage(message);
    }

    /**
     * 鍙戦€佺炕璇戞秷鎭紙甯﹀墠缂€锛?     */
    public static void sendMsg(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(Component.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 鍙戦€侀敊璇秷鎭?     */
    public static void sendError(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(Component.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 鍙戦€佹垚鍔熸秷鎭?     */
    public static void sendSuccess(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(Component.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 鍙戦€佽鍛婃秷鎭?     */
    public static void sendWarning(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(Component.literal(LanguageManager.tr(key, args))));
    }

    /**
     * 鍙戦€?ActionBar 娑堟伅
     */
    public static void sendActionBar(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.literal(LanguageManager.tr(key, args)), true);
    }

    /**
     * 鍙戦€佹爣棰樻秷鎭紙澶ф爣棰?+ 瀛愭爣棰橈級
     */
    public static void sendTitle(ServerPlayer player, String title, String subtitle) {
        TitleHelper.getInstance().sendTitle(player, title, subtitle);
    }

    // ==================== 闈欐€?Component 宸ュ巶鏂规硶锛堢敤浜庡悜 CommandSourceStack 鍙戦€佹秷鎭級 ====================

    /**
     * 鍒涘缓缁胯壊鎴愬姛鏂囨湰
     */
    public static MutableComponent success(String text) {
        return Component.literal("搂a" + text);
    }

    /**
     * 鍒涘缓绾㈣壊閿欒鏂囨湰
     */
    public static MutableComponent error(String text) {
        return Component.literal("搂c" + text);
    }

    /**
     * 鍒涘缓榛勮壊璀﹀憡鏂囨湰
     */
    public static MutableComponent warning(String text) {
        return Component.literal("搂e" + text);
    }

    /**
     * 鍒涘缓钃濊壊淇℃伅鏂囨湰
     */
    public static MutableComponent info(String text) {
        return Component.literal("搂b" + text);
    }
}