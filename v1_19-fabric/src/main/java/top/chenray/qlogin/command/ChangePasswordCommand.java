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
 * /changepassword <旧密码> <新密码> - 修改密码命令
 */
public class ChangePasswordCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("changepassword")
            .then(CommandManager.argument("oldPassword", StringArgumentType.word())
                .then(CommandManager.argument("newPassword", StringArgumentType.word())
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        ServerPlayerEntity player = source.getPlayer();
                        if (player == null) {
                            source.sendError(Text.literal("§c此命令只能由玩家执行"));
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

    private static int executeChangePassword(ServerPlayerEntity player, String oldPassword, String newPassword) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUuid());
        DatabaseManager db = DatabaseManager.getInstance();

        // 必须已登录
        if (state != LoginState.LOGGED_IN) {
            player.sendMessage(Text.literal("§c请先登录后再修改密码"));
            return 0;
        }

        // 验证旧密码
        if (!db.verifyPassword(player.getUuid(), oldPassword)) {
            player.sendMessage(Text.literal("§c旧密码错误"));
            return 0;
        }

        // 验证新密码长度
        ModConfig config = ModConfig.getInstance();
        if (newPassword.length() < config.getPasswordMinLength() || newPassword.length() > config.getPasswordMaxLength()) {
            player.sendMessage(Text.literal("§c新密码长度必须在 " + config.getPasswordMinLength() + "-" + config.getPasswordMaxLength() + " 个字符之间"));
            return 0;
        }

        // 新旧密码不能相同
        if (oldPassword.equals(newPassword)) {
            player.sendMessage(Text.literal("§c新密码不能与旧密码相同"));
            return 0;
        }

        // 执行修改
        player.sendMessage(Text.literal("§7正在修改密码..."));
        if (db.changePassword(player.getUuid(), newPassword)) {
            TextUtils.sendSuccess(player, "password.change_success");
            LOGGER.info("玩家 {} 已修改密码", player.getName().getString());
            return 1;
        } else {
            player.sendMessage(Text.literal("§c密码修改失败，请稍后重试"));
            return 0;
        }
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}
