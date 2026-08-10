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
 * /changepassword <旧密码> <新密码> - 修改密码命令
 */
public class ChangePasswordCommand {

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("changepassword")
            .then(Commands.argument("oldPassword", StringArgumentType.word())
                .then(Commands.argument("newPassword", StringArgumentType.word())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerPlayer player = source.getPlayerOrException();
                        String oldPassword = StringArgumentType.getString(context, "oldPassword");
                        String newPassword = StringArgumentType.getString(context, "newPassword");
                        return executeChangePassword(player, oldPassword, newPassword);
                    })
                )
            )
        );
    }

    private static int executeChangePassword(ServerPlayer player, String oldPassword, String newPassword) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUUID());
        DatabaseManager db = DatabaseManager.getInstance();

        // 必须已登录
        if (state != LoginState.LOGGED_IN) {
            player.sendMessage(TextUtils.literal("§c请先登录后再修改密码"), player.getUUID());
            return 0;
        }

        // 验证旧密码
        if (!db.verifyPassword(player.getUUID(), oldPassword)) {
            player.sendMessage(TextUtils.literal("§c旧密码错误"), player.getUUID());
            return 0;
        }

        // 验证新密码长度
        ModConfig config = ModConfig.getInstance();
        if (newPassword.length() < config.getPasswordMinLength() || newPassword.length() > config.getPasswordMaxLength()) {
            player.sendMessage(TextUtils.literal(
                "§c新密码长度必须在 " + config.getPasswordMinLength() + "-" + config.getPasswordMaxLength() + " 个字符之间"), player.getUUID());
            return 0;
        }

        // 新旧密码不能相同
        if (oldPassword.equals(newPassword)) {
            player.sendMessage(TextUtils.literal("§c新密码不能与旧密码相同"), player.getUUID());
            return 0;
        }

        // 执行修改
        player.sendMessage(TextUtils.literal("§7正在修改密码..."), player.getUUID());
        if (db.changePassword(player.getUUID(), newPassword)) {
            TextUtils.sendSuccess(player, player.getUUID(), "password.change_success");
            LOGGER.info("玩家 {} 已修改密码", player.getDisplayName().getString());
            return 1;
        } else {
            player.sendMessage(TextUtils.literal("§c密码修改失败，请稍后重试"), player.getUUID());
            return 0;
        }
    }
}
