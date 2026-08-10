package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /register <密码> <确认密码> - 注册命令
 */
public class RegisterCommand {

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("register")
            .then(Commands.argument("password", StringArgumentType.word())
                .then(Commands.argument("confirmPassword", StringArgumentType.word())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerPlayer player = source.getPlayer();
                        if (player == null) {
                            source.sendFailure(Component.literal("§c此命令只能由玩家执行"));
                            return 0;
                        }
                        String password = StringArgumentType.getString(context, "password");
                        String confirmPassword = StringArgumentType.getString(context, "confirmPassword");
                        return executeRegister(player, password, confirmPassword);
                    })
                )
            )
        );
    }

    private static int executeRegister(ServerPlayer player, String password, String confirmPassword) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUUID());
        DatabaseManager db = DatabaseManager.getInstance();

        if (state == LoginState.LOGGED_IN) {
            TextUtils.sendWarning(player, "login.already");
            return 0;
        }

        if (db.isPlayerRegistered(player.getUUID())) {
            TextUtils.sendMsg(player, "register.exists");
            return 0;
        }

        if (!password.equals(confirmPassword)) {
            TextUtils.sendError(player, "register.password_mismatch");
            return 0;
        }

        int minLen = ModConfig.getInstance().getPasswordMinLength();
        int maxLen = ModConfig.getInstance().getPasswordMaxLength();

        if (password.length() < minLen) {
            TextUtils.sendError(player, "register.password_too_short", minLen);
            return 0;
        }

        if (password.length() > maxLen) {
            TextUtils.sendError(player, "register.password_too_long", maxLen);
            return 0;
        }

        if (db.registerPlayer(player.getUUID(), player.getName().getString(), password)) {
            loginManager.setLoggedIn(player.getUUID());
            loginManager.resetLoginFails(player.getUUID());

            String ip = loginManager.getPlayerIp(player);
            db.updateLoginInfo(player.getUUID(), ip);

            TextUtils.sendSuccess(player, "register.success", player.getName().getString());
            TextUtils.sendTitle(player, "注册成功", "欢迎来到服务器！");

            LOGGER.info("Player {} registered", player.getName().getString());
            return 1;
        } else {
            TextUtils.sendError(player, "register.fail");
            return 0;
        }
    }
}
