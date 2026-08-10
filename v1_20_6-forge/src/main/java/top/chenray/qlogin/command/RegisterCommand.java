package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
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

        // 检查是否已登录
        if (state == LoginState.LOGGED_IN) {
            TextUtils.sendMsg(player, "login.already");
            return 0;
        }

        // 检查是否已注册
        if (db.isPlayerRegistered(player.getUUID())) {
            TextUtils.sendMsg(player, "register.exists");
            return 0;
        }

        // 验证密码长度
        ModConfig config = ModConfig.getInstance();
        if (password.length() < config.getPasswordMinLength() || password.length() > config.getPasswordMaxLength()) {
            TextUtils.sendMsg(player, "register.password_length",
                config.getPasswordMinLength(), config.getPasswordMaxLength());
            return 0;
        }

        // 验证两次密码一致
        if (!password.equals(confirmPassword)) {
            TextUtils.sendMsg(player, "register.password_mismatch");
            return 0;
        }

        // 执行注册
        String ip = loginManager.getPlayerIp(player);

        if (db.registerPlayer(player.getUUID(), player.getDisplayName().getString(), password, ip)) {
            loginManager.setLoggedIn(player.getUUID());
            loginManager.resetLoginFails(player.getUUID());
            TextUtils.sendMsg(player, "register.success", player.getDisplayName().getString());
            TextUtils.sendTitle(player, "注册成功", "欢迎加入服务器！");
            LOGGER.info("Player {} registered", player.getDisplayName().getString());
            return 1;
        } else {
            TextUtils.sendMsg(player, "register.fail");
            return 0;
        }
    }
}
