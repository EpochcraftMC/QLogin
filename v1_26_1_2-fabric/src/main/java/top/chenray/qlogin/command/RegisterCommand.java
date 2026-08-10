package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.config.ModConfig;
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
 * /register <瀵嗙爜> <纭瀵嗙爜> - 娉ㄥ唽鍛戒护
 */
public class RegisterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("register")
            .then(Commands.argument("password", StringArgumentType.word())
                .then(Commands.argument("confirmPassword", StringArgumentType.word())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerPlayer player = source.getPlayer();
                        if (player == null) {
                            source.sendFailure(Component.literal("搂c姝ゅ懡浠ゅ彧鑳界敱鐜╁鎵ц"));
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

        // 妫€鏌ユ槸鍚﹀凡鐧诲綍
        if (state == LoginState.LOGGED_IN) {
            TextUtils.sendMsg(player, "login.already");
            return 0;
        }

        // 妫€鏌ユ槸鍚﹀凡娉ㄥ唽
        if (db.isPlayerRegistered(player.getUUID())) {
            TextUtils.sendMsg(player, "register.exists");
            return 0;
        }

        // 楠岃瘉瀵嗙爜闀垮害
        ModConfig config = ModConfig.getInstance();
        if (password.length() < config.getPasswordMinLength() || password.length() > config.getPasswordMaxLength()) {
            TextUtils.sendMsg(player, "register.password_length", config.getPasswordMinLength(), config.getPasswordMaxLength());
            return 0;
        }

        // 楠岃瘉涓ゆ瀵嗙爜涓€鑷?        if (!password.equals(confirmPassword)) {
            TextUtils.sendMsg(player, "register.password_mismatch");
            return 0;
        }

        // 鎵ц娉ㄥ唽
        String ip = loginManager.getPlayerIp(player);

        if (db.registerPlayer(player.getUUID(), player.getName().getString(), password, ip)) {
            loginManager.setLoggedIn(player.getUUID());
            loginManager.resetLoginFails(player.getUUID());
            TextUtils.sendMsg(player, "register.success", player.getName().getString());
            TextUtils.sendTitle(player, "娉ㄥ唽鎴愬姛", "娆㈣繋鍔犲叆鏈嶅姟鍣紒");
            LOGGER.info("Player {} registered", player.getName().getString());
            return 1;
        } else {
            TextUtils.sendMsg(player, "register.fail");
            return 0;
        }
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}