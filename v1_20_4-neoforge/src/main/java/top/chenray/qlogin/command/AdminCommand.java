package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.LanguageManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * /qlogin <操作> - 管理员命令
 */
public class AdminCommand {

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 需要 op 权限
        dispatcher.register(Commands.literal("qlogin")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("reload")
                .executes(context -> executeReload(context.getSource()))
            )
            .then(Commands.literal("resetpassword")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("newPassword", StringArgumentType.word())
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            String newPassword = StringArgumentType.getString(context, "newPassword");
                            return executeResetPassword(context.getSource(), target, newPassword);
                        })
                    )
                )
            )
            .then(Commands.literal("unregister")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        return executeUnregister(context.getSource(), target);
                    })
                )
            )
        );
    }

    private static int executeReload(CommandSourceStack source) {
        ModConfig.load(source.getServer().getServerDirectory().toPath().resolve("config").resolve("loginmod"));
        LanguageManager.reload();
        TextUtils.sendMsg(source, "admin.reload");
        LOGGER.info("QLogin config reloaded by {}", source.getTextName());
        return 1;
    }

    private static int executeResetPassword(CommandSourceStack source, ServerPlayer target, String newPassword) {
        DatabaseManager db = DatabaseManager.getInstance();
        UUID targetUuid = target.getUUID();

        if (!db.isPlayerRegistered(targetUuid)) {
            source.sendFailure(Component.literal("§c该玩家尚未注册"));
            return 0;
        }

        if (db.adminResetPassword(targetUuid, newPassword)) {
            TextUtils.sendMsg(source, "admin.reset_password", target.getName().getString());
            LOGGER.info("Password reset for {} by {}", target.getName().getString(), source.getTextName());
            return 1;
        } else {
            source.sendFailure(Component.literal("§c重置密码失败"));
            return 0;
        }
    }

    private static int executeUnregister(CommandSourceStack source, ServerPlayer target) {
        DatabaseManager db = DatabaseManager.getInstance();
        UUID targetUuid = target.getUUID();

        if (!db.isPlayerRegistered(targetUuid)) {
            source.sendFailure(Component.literal("§c该玩家尚未注册"));
            return 0;
        }

        // 从数据库删除
        boolean deleted = db.adminResetPassword(targetUuid, "");
        if (deleted) {
            LoginManager.getInstance().setLoggedOut(targetUuid);
            TextUtils.sendMsg(source, "admin.unregister", target.getName().getString());
            LOGGER.info("Player {} unregistered by {}", target.getName().getString(), source.getTextName());
            return 1;
        } else {
            source.sendFailure(Component.literal("§c注销失败"));
            return 0;
        }
    }
}
