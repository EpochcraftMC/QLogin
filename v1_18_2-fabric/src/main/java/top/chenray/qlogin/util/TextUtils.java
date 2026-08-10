package top.chenray.qlogin.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 鏂囨湰宸ュ叿绫?- 鎻愪緵澶氳瑷€褰╄壊鏍煎紡鍖栬緭鍑?
 */
public class TextUtils {

    /**
     * 缈昏瘧蹇嵎鏂规硶
     */
    public static String t(String key, Object... args) {
        return LanguageManager.tr(key, args);
    }

    /**
     * 鑾峰彇甯﹀墠缂€鐨勬秷鎭枃鏈?
     */
    public static MutableText prefixed(Text text) {
        return new LiteralText(LanguageManager.tr("prefix")).append(text);
    }

    /**
     * 鍙戦€佺郴缁熸秷鎭粰鐜╁
     */
    public static void sendMessage(ServerPlayerEntity player, Text message) {
        player.sendMessage(message, false);
    }

    /**
     * 鍙戦€佺炕璇戞秷鎭紙甯﹀墠缂€锛?
     */
    public static void sendMsg(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(prefixed(new LiteralText(LanguageManager.tr(key, args), false);
    }

    /**
     * 鍙戦€侀敊璇秷鎭?
     */
    public static void sendError(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(prefixed(new LiteralText(LanguageManager.tr(key, args), false);
    }

    /**
     * 鍙戦€佹垚鍔熸秷鎭?
     */
    public static void sendSuccess(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(prefixed(new LiteralText(LanguageManager.tr(key, args), false);
    }

    /**
     * 鍙戦€佽鍛婃秷鎭?
     */
    public static void sendWarning(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(prefixed(new LiteralText(LanguageManager.tr(key, args), false);
    }

    /**
     * 鍙戦€?ActionBar 娑堟伅
     */
    public static void sendActionBar(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(new LiteralText(LanguageManager.tr(key, args)), true);
    }

    /**
     * 鍙戦€佹爣棰樻秷鎭紙澶ф爣棰?+ 瀛愭爣棰橈級
     */
    public static void sendTitle(ServerPlayerEntity player, String title, String subtitle) {
        TitleHelper.getInstance().sendTitle(player, title, subtitle);
    }

    // ==================== 闈欐€?Text 宸ュ巶鏂规硶锛堢敤浜庡悜 ServerCommandSource 鍙戦€佹秷鎭級 ====================

    /**
     * 鍒涘缓缁胯壊鎴愬姛鏂囨湰
     */
    public static MutableText success(String text) {
        return new LiteralText("搂a" + text);
    }

    /**
     * 鍒涘缓绾㈣壊閿欒鏂囨湰
     */
    public static MutableText error(String text) {
        return new LiteralText("搂c" + text);
    }

    /**
     * 鍒涘缓榛勮壊璀﹀憡鏂囨湰
     */
    public static MutableText warning(String text) {
        return new LiteralText("搂e" + text);
    }

    /**
     * 鍒涘缓钃濊壊淇℃伅鏂囨湰
     */
    public static MutableText info(String text) {
        return new LiteralText("搂b" + text);
    }
}