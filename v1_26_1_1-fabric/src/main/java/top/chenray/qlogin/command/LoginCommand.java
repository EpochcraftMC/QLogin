package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * /login <瀵嗙爜> 鍜?/l <瀵嗙爜> - 鐧诲綍鍛戒护
 */
public class LoginCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        // /login <password>
        dispatcher.register(Commands.literal("login")
            .then(Commands.argument("password", StringArgumentType.word())
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerPlayer player = source.getPlayer();
                    if (player == null) {
                        source.sendFailure(Component.literal("搂c姝ゅ懡浠ゅ彧鑳界敱鐜╁鎵ц"));
                        return 0;
                    }
                    String password = StringArgumentType.getString(context, "password");
                    return executeLogin(player, password);
                })
            )
        );

        // /l <password> (鍒悕)
        dispatcher.register(Commands.literal("l")
            .then(Commands.argument("password", StringArgumentType.word())
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerPlayer player = source.getPlayer();
                    if (player == null) {
                        source.sendFailure(Component.literal("搂c姝ゅ懡浠ゅ彧鑳界敱鐜╁鎵ц"));
                        return 0;
                    }
                    String password = StringArgumentType.getString(context, "password");
                    return executeLogin(player, password);
                })
            )
        );
    }

    private static int executeLogin(ServerPlayer player, String password) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUUID());
        DatabaseManager db = DatabaseManager.getInstance();

        if (state == LoginState.LOGGED_IN) {
            TextUtils.sendWarning(player, "login.already");
            return 0;
        }

        if (!db.isPlayerRegistered(player.getUUID())) {
            TextUtils.sendMsg(player, "register.exists");
            return 0;
        }

        if (db.verifyPassword(player.getUUID(), password)) {
            loginManager.setLoggedIn(player.getUUID());
            loginManager.resetLoginFails(player.getUUID());

            String ip = loginManager.getPlayerIp(player);
            db.updateLoginInfo(player.getUUID(), ip);

            TextUtils.sendSuccess(player, "login.success", player.getName().getString());
            TextUtils.sendTitle(player, "鐧诲綍鎴愬姛", "娆㈣繋鍥炴潵锛?);

            LOGGER.info("Player {} logged in", player.getName().getString());
            return 1;
        } else {
            boolean banned = loginManager.recordLoginFail(player.getUUID());
            int maxAttempts = top.chenray.qlogin.config.ModConfig.getInstance().getMaxLoginAttempts();

            if (banned) {
                TextUtils.sendMsg(player, "ban.too_many_attempts");
                player.connection.disconnect(Component.literal(TextUtils.t("ban.too_many_attempts")));
            } else {
                TextUtils.sendMsg(player, "login.fail", maxAttempts);
            }
            return 0;
        }
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}