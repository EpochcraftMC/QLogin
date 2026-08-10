package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /login <密码> 和 /l <密码> - 登录命令
 */
public class LoginCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        // /login <password>
        dispatcher.register(CommandManager.literal("login")
            .then(CommandManager.argument("password", StringArgumentType.word())
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayer();
                    if (player == null) {
                        source.sendError(Text.literal("§c此命令只能由玩家执行"));
                        return 0;
                    }
                    String password = StringArgumentType.getString(context, "password");
                    return executeLogin(player, password);
                })
            )
        );

        // /l <password> (别名)
        dispatcher.register(CommandManager.literal("l")
            .then(CommandManager.argument("password", StringArgumentType.word())
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayer();
                    if (player == null) {
                        source.sendError(Text.literal("§c此命令只能由玩家执行"));
                        return 0;
                    }
                    String password = StringArgumentType.getString(context, "password");
                    return executeLogin(player, password);
                })
            )
        );
    }

    private static int executeLogin(ServerPlayerEntity player, String password) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUuid());
        DatabaseManager db = DatabaseManager.getInstance();

        if (state == LoginState.LOGGED_IN) {
            TextUtils.sendWarning(player, "login.already");
            return 0;
        }

        if (!db.isPlayerRegistered(player.getUuid())) {
            TextUtils.sendMsg(player, "register.exists");
            return 0;
        }

        if (db.verifyPassword(player.getUuid(), password)) {
            loginManager.setLoggedIn(player.getUuid());
            loginManager.resetLoginFails(player.getUuid());

            String ip = loginManager.getPlayerIp(player);
            db.updateLoginInfo(player.getUuid(), ip);

            TextUtils.sendSuccess(player, "login.success", player.getName().getString());
            TextUtils.sendTitle(player, "登录成功", "欢迎回来！");

            LOGGER.info("Player {} logged in", player.getName().getString());
            return 1;
        } else {
            boolean banned = loginManager.recordLoginFail(player.getUuid());
            int maxAttempts = top.chenray.qlogin.config.ModConfig.getInstance().getMaxLoginAttempts();

            if (banned) {
                TextUtils.sendMsg(player, "ban.too_many_attempts");
                player.networkHandler.disconnect(Text.literal(TextUtils.t("ban.too_many_attempts")));
            } else {
                TextUtils.sendMsg(player, "login.fail", maxAttempts);
            }
            return 0;
        }
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}
