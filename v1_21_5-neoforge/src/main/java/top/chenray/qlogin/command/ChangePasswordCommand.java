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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("changepassword")
            .then(Commands.argument("oldPassword", StringArgumentType.word())
                .then(Commands.argument("newPassword", StringArgumentType.word())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerPlayer player = source.getPlayer();
                        if (player == null) {
                            source.sendFailure(Component.literal("§c此命令只能由玩家执行"));
                            return 0;
                        }
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

        if (state != LoginState.LOGGED_IN) {
            TextUtils.sendWarning(player, "login.already");
            return 0;
        }

        if (oldPassword.equals(newPassword)) {
            TextUtils.sendError(player, "changepassword.same");
            return 0;
        }

        int minLen = ModConfig.getInstance().getPasswordMinLength();
        int maxLen = ModConfig.getInstance().getPasswordMaxLength();

        if (newPassword.length() < minLen) {
            TextUtils.sendError(player, "register.password_too_short", minLen);
            return 0;
        }

        if (newPassword.length() > maxLen) {
            TextUtils.sendError(player, "register.password_too_long", maxLen);
            return 0;
        }

        if (db.changePassword(player.getUUID(), oldPassword, newPassword)) {
            TextUtils.sendSuccess(player, "changepassword.success");
            return 1;
        } else {
            TextUtils.sendError(player, "changepassword.wrong");
            return 0;
        }
    }
}
