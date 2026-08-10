package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.config.ModConfig;
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
 * /register <密码> <确认密码> - 注册命令
 */
public class RegisterCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("register")
            .then(CommandManager.argument("password", StringArgumentType.word())
                .then(CommandManager.argument("confirmPassword", StringArgumentType.word())
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        ServerPlayerEntity player = source.getPlayer();
                        if (player == null) {
                            source.sendError(Text.literal("§c此命令只能由玩家执行"));
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

    private static int executeRegister(ServerPlayerEntity player, String password, String confirmPassword) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUuid());
        DatabaseManager db = DatabaseManager.getInstance();

        // 检查是否已登录
        if (state == LoginState.LOGGED_IN) {
            TextUtils.sendMsg(player, "login.already");
            return 0;
        }

        // 检查是否已注册
        if (db.isPlayerRegistered(player.getUuid())) {
            TextUtils.sendMsg(player, "register.exists");
            return 0;
        }

        // 验证密码长度
        ModConfig config = ModConfig.getInstance();
        if (password.length() < config.getPasswordMinLength() || password.length() > config.getPasswordMaxLength()) {
            TextUtils.sendMsg(player, "register.password_length", config.getPasswordMinLength(), config.getPasswordMaxLength());
            return 0;
        }

        // 验证两次密码一致
        if (!password.equals(confirmPassword)) {
            TextUtils.sendMsg(player, "register.password_mismatch");
            return 0;
        }

        // 执行注册
        String ip = loginManager.getPlayerIp(player);

        if (db.registerPlayer(player.getUuid(), player.getName().getString(), password, ip)) {
            loginManager.setLoggedIn(player.getUuid());
            loginManager.resetLoginFails(player.getUuid());
            TextUtils.sendMsg(player, "register.success", player.getName().getString());
            TextUtils.sendTitle(player, "注册成功", "欢迎加入服务器！");
            LOGGER.info("Player {} registered", player.getName().getString());
            return 1;
        } else {
            TextUtils.sendMsg(player, "register.fail");
            return 0;
        }
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}
