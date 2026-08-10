package top.chenray.qlogin.util;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 版本适配接口 - 各版本模块提供独立实现
 * 处理跨版本不一致的 Minecraft API（如 TitleS2CPacket）
 */
public interface TitleHelper {

    /** 获取当前版本的适配器实例 */
    static TitleHelper getInstance() {
        return Holder.INSTANCE;
    }

    /** 设置适配器实例（由各版本模块的入口调用） */
    static void setInstance(TitleHelper instance) {
        Holder.INSTANCE = instance;
    }

    /** 发送标题（大标题 + 副标题） */
    void sendTitle(ServerPlayerEntity player, String title, String subtitle);

    /** 内部持有类 */
    class Holder {
        private static TitleHelper INSTANCE = new DefaultTitleHelper();
    }

    /** 默认空实现（无版本适配时保底） */
    class DefaultTitleHelper implements TitleHelper {
        @Override
        public void sendTitle(ServerPlayerEntity player, String title, String subtitle) {
            // fallback: 使用 ActionBar
            player.sendMessage(
                net.minecraft.text.Text.literal("§6" + title + " §e" + subtitle),
                true
            );
        }
    }
}
