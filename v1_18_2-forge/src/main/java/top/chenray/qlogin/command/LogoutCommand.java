package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /logout - 登出命令
 */
public class LogoutCommand {

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("logout")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                ServerPlayer player = source.getPlayerOrException();
                return executeLogout(player);
            })
        );
    }

    private static int executeLogout(ServerPlayer player) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUUID());

        if (state != LoginState.LOGGED_IN) {
            player.sendMessage(TextUtils.literal("§c你还没有登录"), player.getUUID());
            return 0;
        }

        loginManager.setLoggedOut(player.getUUID());
        TextUtils.sendWarning(player, player.getUUID(), "logout.success");
        player.sendMessage(TextUtils.literal("§b使用 §6/login <密码> §b重新登录"), player.getUUID());
        LOGGER.info("玩家 {} 已登出", player.getDisplayName().getString());
        return 1;
    }
}
