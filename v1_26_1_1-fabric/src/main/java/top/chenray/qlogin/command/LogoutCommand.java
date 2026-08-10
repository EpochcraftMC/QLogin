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
 * /logout - 登出命令
 */
public class LogoutCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("logout")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                ServerPlayer player = source.getPlayer();
                if (player == null) {
                    source.sendFailure(Component.literal("§c此命令只能由玩家执行"));
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
            player.sendSystemMessage(Component.literal("§c你还没有登录"));
            return 0;
        }

        loginManager.setLoggedOut(player.getUUID());
        TextUtils.sendWarning(player, "logout.success");
        player.sendSystemMessage(Component.literal("§b使用 §6/login <密码> §b重新登录"));
        LOGGER.info("玩家 {} 已登出", player.getName().getString());
        return 1;
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}
