package top.chenray.qlogin.util;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 鐗堟湰閫傞厤鎺ュ彛 - 鍚勭増鏈ā鍧楁彁渚涚嫭绔嬪疄鐜?
 * 澶勭悊璺ㄧ増鏈笉涓€鑷寸殑 Minecraft API锛堝 TitleS2CPacket锛?
 */
public interface TitleHelper {

    /** 鑾峰彇褰撳墠鐗堟湰鐨勯€傞厤鍣ㄥ疄渚?*/
    static TitleHelper getInstance() {
        return Holder.INSTANCE;
    }

    /** 璁剧疆閫傞厤鍣ㄥ疄渚嬶紙鐢卞悇鐗堟湰妯″潡鐨勫叆鍙ｈ皟鐢級 */
    static void setInstance(TitleHelper instance) {
        Holder.INSTANCE = instance;
    }

    /** 鍙戦€佹爣棰橈紙澶ф爣棰?+ 鍓爣棰橈級 */
    void sendTitle(ServerPlayerEntity player, String title, String subtitle);

    /** 鍐呴儴鎸佹湁绫?*/
    class Holder {
        private static TitleHelper INSTANCE = new DefaultTitleHelper();
    }

    /** 榛樿绌哄疄鐜帮紙鏃犵増鏈€傞厤鏃朵繚搴曪級 */
    class DefaultTitleHelper implements TitleHelper {
        @Override
        public void sendTitle(ServerPlayerEntity player, String title, String subtitle) {
            // fallback: 浣跨敤 ActionBar
            player.sendMessage(
                net.minecraft.text.Text.literal("搂6" + title + " 搂e" + subtitle),
                true
            );
        }
    }
}