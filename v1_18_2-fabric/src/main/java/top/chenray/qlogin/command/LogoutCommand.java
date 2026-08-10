package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

/**
 * /logout - 登出命令
 */
public class LogoutCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("logout")
            .executes(context -> {
                ServerCommandSource source = context.getSource();
                ServerPlayerEntity player = source.getPlayer();
                if (player == null) {
                    source.sendError(new LiteralText("§c此命令只能由玩家执行"));
                    return 0;
                }
                return executeLogout(player);
            })
        );
    }

    private static int executeLogout(ServerPlayerEntity player) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUuid());

        if (state != LoginState.LOGGED_IN) {
            player.sendMessage(new LiteralText("§c你还没有登录"));
            return 0;
        }

        loginManager.setLoggedOut(player.getUuid());
        TextUtils.sendWarning(player, "logout.success");
        player.sendMessage(new LiteralText("§b使用 §6/login <密码> §b重新登录"));
        LOGGER.info("玩家 {} 已登出", player.getName().getString());
        return 1;
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}
