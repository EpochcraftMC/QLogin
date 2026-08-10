package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.LanguageManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 管理员命令 - /loginmod (Mojang 映射版)
 */
public class AdminCommand {

    /** Tab 补全: 所有已注册玩家名 */
    private static final SuggestionProvider<CommandSourceStack> REGISTERED_PLAYERS =
        (context, builder) -> suggestRegisteredPlayers(builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                 CommandBuildContext registryAccess,
                                 Commands.CommandSelection environment) {
        var loginmod = Commands.literal("loginmod")
            .requires(source -> source.permissions() instanceof net.minecraft.server.permissions.LevelBasedPermissionSet s
                && s.level().isEqualOrHigherThan(net.minecraft.server.permissions.PermissionLevel.OWNERS)); // OP 权限等级 4

        // /loginmod reload
        loginmod.then(Commands.literal("reload")
            .executes(AdminCommand::executeReload)
        );

        // /loginmod unregister <玩家名>
        loginmod.then(Commands.literal("unregister")
            .then(Commands.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .executes(AdminCommand::executeUnregister)
            )
        );

        // /loginmod resetpassword <玩家名> <新密码>
        loginmod.then(Commands.literal("resetpassword")
            .then(Commands.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .then(Commands.argument("newPassword", StringArgumentType.word())
                    .executes(AdminCommand::executeResetPassword)
                )
            )
        );

        // /loginmod info <玩家名>
        loginmod.then(Commands.literal("info")
            .then(Commands.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .executes(AdminCommand::executeInfo)
            )
        );

        dispatcher.register(loginmod);
    }

    // ==================== Tab 补全 ====================

    private static CompletableFuture<Suggestions> suggestRegisteredPlayers(SuggestionsBuilder builder) {
        try {
            var server = top.chenray.qlogin.LoginMod.getServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    String name = player.getScoreboardName();
                    if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                        builder.suggest(name);
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            var conn = DatabaseManager.getInstance().getConnection();
            if (conn != null) {
                String sql = "SELECT DISTINCT username FROM players WHERE username LIKE ?";
                try (var pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, builder.getRemainingLowerCase() + "%");
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        builder.suggest(rs.getString("username"));
                    }
                }
            }
        } catch (SQLException ignored) {}

        return builder.buildFuture();
    }

    private static ServerPlayer findPlayerByUsername(String username) {
        var server = top.chenray.qlogin.LoginMod.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getScoreboardName().equalsIgnoreCase(username)) {
                    return player;
                }
            }
        }
        return null;
    }

    // ==================== 命令执行 ====================

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (ModConfig.reload()) {
            LanguageManager.reload();
            source.sendSuccess(() -> TextUtils.prefixed(Component.literal(LanguageManager.tr("admin.reload"))), false);
            top.chenray.qlogin.LoginMod.LOGGER.info("Admin {} reloaded config", source.getTextName());
            return 1;
        } else {
            source.sendFailure(TextUtils.prefixed(Component.literal(LanguageManager.tr("admin.reload_fail"))));
            return 0;
        }
    }

    private static int executeUnregister(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");
        ServerPlayer target = findPlayerByUsername(targetName);

        DatabaseManager db = DatabaseManager.getInstance();
        boolean success;

        if (target != null) {
            success = db.unregisterPlayerByUuid(target.getUUID());
            if (success) {
                LoginManager.getInstance().setUnregistered(target.getUUID());
                target.connection.disconnect(Component.literal(TextUtils.t("kick.unregistered")));
            }
        } else {
            success = db.unregisterPlayer(targetName);
        }

        if (success) {
            source.sendSuccess(() -> TextUtils.success(TextUtils.t("admin.unregister", targetName)), false);
            top.chenray.qlogin.LoginMod.LOGGER.info("管理员 {} 强制注销了玩家 {}", source.getTextName(), targetName);
            return 1;
        } else {
            source.sendFailure(TextUtils.error(TextUtils.t("admin.unregister_not_found", targetName)));
            return 0;
        }
    }

    private static int executeResetPassword(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");
        String newPassword = StringArgumentType.getString(context, "newPassword");
        ServerPlayer target = findPlayerByUsername(targetName);

        ModConfig config = ModConfig.getInstance();
        if (newPassword.length() < config.getPasswordMinLength() || newPassword.length() > config.getPasswordMaxLength()) {
            source.sendFailure(TextUtils.error(TextUtils.t("register.password_length",
                config.getPasswordMinLength(), config.getPasswordMaxLength())));
            return 0;
        }

        DatabaseManager db = DatabaseManager.getInstance();
        String uuid = null;

        if (target != null) {
            uuid = target.getUUID().toString();
        } else {
            Map<String, Object> info = db.getPlayerInfo(targetName);
            if (info != null) {
                uuid = (String) info.get("uuid");
            }
        }

        if (uuid == null) {
            source.sendFailure(TextUtils.error(TextUtils.t("admin.unregister_not_found", targetName)));
            return 0;
        }

        if (db.changePassword(java.util.UUID.fromString(uuid), newPassword)) {
            source.sendSuccess(() -> TextUtils.success(TextUtils.t("admin.reset_password", targetName)), false);

            if (target != null) {
                target.sendSystemMessage(TextUtils.warning(TextUtils.t("admin.reset_password_notify", source.getTextName())));
                target.sendSystemMessage(TextUtils.info(TextUtils.t("admin.reset_password_new", newPassword)));
            }

            top.chenray.qlogin.LoginMod.LOGGER.info("管理员 {} 重置了玩家 {} 的密码", source.getTextName(), targetName);
            return 1;
        } else {
            source.sendFailure(TextUtils.error(TextUtils.t("password.change_fail")));
            return 0;
        }
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");

        Map<String, Object> info = DatabaseManager.getInstance().getPlayerInfo(targetName);
        if (info == null) {
            source.sendFailure(TextUtils.error(TextUtils.t("admin.unregister_not_found", targetName)));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(TextUtils.t("info.username", info.get("username"))), false);
        source.sendSuccess(() -> Component.literal(TextUtils.t("info.uuid", info.get("uuid"))), false);
        source.sendSuccess(() -> Component.literal(TextUtils.t("info.register_time",
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date((Long) info.get("register_time"))))), false);
        source.sendSuccess(() -> Component.literal(TextUtils.t("info.last_login",
            info.get("last_login") != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date((Long) info.get("last_login")))
                : TextUtils.t("info.last_login_none"))), false);
        source.sendSuccess(() -> Component.literal(TextUtils.t("info.login_fail", info.get("login_fail_count"))), false);
        source.sendSuccess(() -> Component.literal(TextUtils.t("info.ip_history", info.get("ip_history"))), false);

        return 1;
    }
}
