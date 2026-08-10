package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.LanguageManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * /qlogin <操作> - 管理员命令
 */
public class AdminCommand {

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("qlogin")
            .requires(source -> source.hasPermissionLevel(3))
            .then(CommandManager.literal("reload")
                .executes(context -> {
                    ModConfig.load(context.getSource().getServer().getRunDirectory().toPath().resolve("config").resolve("loginmod"));
                    LanguageManager.reload();
                    TextUtils.sendMsg(context.getSource().getPlayer(), "admin.reload");
                    LOGGER.info("QLogin config reloaded");
                    return 1;
                })
            )
            .then(CommandManager.literal("resetpassword")
                .then(CommandManager.argument("player", StringArgumentType.word())
                    .then(CommandManager.argument("newPassword", StringArgumentType.word())
                        .executes(context -> {
                            String targetName = StringArgumentType.getString(context, "player");
                            String newPassword = StringArgumentType.getString(context, "newPassword");
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
                            if (target == null) {
                                source.sendError(Text.literal("§c未找到在线玩家: " + targetName));
                                return 0;
                            }
                            DatabaseManager db = DatabaseManager.getInstance();
                            if (!db.isPlayerRegistered(target.getUuid())) {
                                source.sendError(Text.literal("§c该玩家尚未注册"));
                                return 0;
                            }
                            if (db.adminResetPassword(target.getUuid(), newPassword)) {
                                TextUtils.sendMsg(target, "admin.reset_password", target.getName().getString());
                                LOGGER.info("Password reset for {} by {}", target.getName().getString(), source.getName());
                                return 1;
                            } else {
                                source.sendError(Text.literal("§c重置密码失败"));
                                return 0;
                            }
                        })
                    )
                )
            )
        );
    }
}
