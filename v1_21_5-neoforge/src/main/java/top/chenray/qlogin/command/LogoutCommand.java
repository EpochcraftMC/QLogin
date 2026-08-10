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
 * /logout - 退出登录命令
 */
public class LogoutCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("logout")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                ServerPlayer player = source.getPlayer();
                if (player == null) {
                    source.sendFailure(Component.literal("§c此命令只能由玩家执行"));
                    return 0;
                }

                LoginManager loginManager = LoginManager.getInstance();
                LoginState state = loginManager.getState(player.getUUID());

                if (state != LoginState.LOGGED_IN) {
                    TextUtils.sendWarning(player, "login.already");
                    return 0;
                }

                loginManager.setLoggedOut(player.getUUID());
                TextUtils.sendSuccess(player, "logout.success");

                return 1;
            })
        );
    }
}
