package top.chenray.qlogin.command;

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
                    return 1;
                })
            )
            .then(CommandManager.literal("resetpassword")
                .then(CommandManager.argument("player", StringArgumentType.word())
                    .then(CommandManager.argument("newPassword", StringArgumentType.word())
                        .executes(context -> {
                            String targetName = StringArgumentType.getString(context, "player");
                            String newPw = StringArgumentType.getString(context, "newPassword");
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
                            if (target == null) { source.sendError(Text.literal("§c未找到在线玩家: " + targetName)); return 0; }
                            if (!DatabaseManager.getInstance().isPlayerRegistered(target.getUuid())) {
                                source.sendError(Text.literal("§c该玩家尚未注册")); return 0;
                            }
                            if (DatabaseManager.getInstance().adminResetPassword(target.getUuid(), newPw)) {
                                TextUtils.sendMsg(target, "admin.reset_password", target.getName().getString());
                                return 1;
                            }
                            source.sendError(Text.literal("§c重置密码失败"));
                            return 0;
                        })
                    )
                )
            )
        );
    }
}
