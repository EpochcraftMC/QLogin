package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * /logout - 鐧诲嚭鍛戒护
 */
public class LogoutCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("logout")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                ServerPlayer player = source.getPlayer();
                if (player == null) {
                    source.sendFailure(Component.literal("搂c姝ゅ懡浠ゅ彧鑳界敱鐜╁鎵ц"));
                    return 0;
                }
                return executeLogout(player);
            })
        );
    }

    private static int executeLogout(ServerPlayer player) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUUID());

        if (state != LoginState.LOGGED_IN) {
            player.sendSystemMessage(Component.literal("搂c浣犺繕娌℃湁鐧诲綍"));
            return 0;
        }

        loginManager.setLoggedOut(player.getUUID());
        TextUtils.sendWarning(player, "logout.success");
        player.sendSystemMessage(Component.literal("搂b浣跨敤 搂6/login <瀵嗙爜> 搂b閲嶆柊鐧诲綍"));
        LOGGER.info("鐜╁ {} 宸茬櫥鍑?, player.getName().getString());
        return 1;
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}