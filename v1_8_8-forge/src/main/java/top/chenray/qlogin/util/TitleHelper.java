package top.chenray.qlogin.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * 标题工具 - 1.12.2 使用 SPacketTitle 发送大标题
 * 1.12.2 Forge 中 SPacketTitle 的构造方式:
 *   SPacketTitle(SPacketTitle.Type type, ITextComponent component, int fadeIn, int stay, int fadeOut)
 */
public class TitleHelper {

    private static TitleHelper instance;

    public static TitleHelper getInstance() {
        if (instance == null) {
            instance = new TitleHelper();
        }
        return instance;
    }

    public static void setInstance(TitleHelper helper) {
        instance = helper;
    }

    /**
     * 发送标题到玩家
     */
    public void sendTitle(EntityPlayerMP player, String title, String subtitle) {
        try {
            // 设置动画时间
            SPacketTitle timesPacket = new SPacketTitle(SPacketTitle.Type.TIMES,
                new TextComponentString(""), 10, 60, 20);
            player.connection.sendPacket(timesPacket);

            // 副标题
            if (subtitle != null && !subtitle.isEmpty()) {
                SPacketTitle subtitlePacket = new SPacketTitle(SPacketTitle.Type.SUBTITLE,
                    new TextComponentString("§e" + subtitle), 0, 0, 0);
                player.connection.sendPacket(subtitlePacket);
            }

            // 主标题
            SPacketTitle titlePacket = new SPacketTitle(SPacketTitle.Type.TITLE,
                new TextComponentString("§6" + title), 0, 0, 0);
            player.connection.sendPacket(titlePacket);
        } catch (Exception e) {
            // fallback: 使用聊天栏
            player.sendMessage(new TextComponentString("§6" + title + " §e" + subtitle));
        }
    }
}
