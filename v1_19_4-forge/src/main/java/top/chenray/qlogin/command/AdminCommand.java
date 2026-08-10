package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.LanguageManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 管理员命令 - /loginmod
 * 子命令: reload, unregister, resetpassword, info
 * 全部带 Tab 补全支持
 */
public class AdminCommand {

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;

    /** Tab 补全: 所有已注册玩家名（支持离线的玩家） */
    private static final SuggestionProvider<CommandSourceStack> REGISTERED_PLAYERS =
        (context, builder) -> suggestRegisteredPlayers(builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var loginmod = Commands.literal("loginmod")
            .requires(source -> source.hasPermission(4)); // OP 权限等级 4

        // /loginmod reload - 重载配置
        loginmod.then(Commands.literal("reload")
            .executes(AdminCommand::executeReload)
        );

        // /loginmod unregister <玩家名> - 强制注销
        loginmod.then(Commands.literal("unregister")
            .then(Commands.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .executes(AdminCommand::executeUnregister)
            )
        );

        // /loginmod resetpassword <玩家名> <新密码> - 重置密码
        loginmod.then(Commands.literal("resetpassword")
            .then(Commands.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .then(Commands.argument("newPassword", StringArgumentType.word())
                    .executes(AdminCommand::executeResetPassword)
                )
            )
        );

        // /loginmod info <玩家名> - 查看玩家信息
        loginmod.then(Commands.literal("info")
            .then(Commands.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .executes(AdminCommand::executeInfo)
            )
        );

        dispatcher.register(loginmod);
    }

    // ==================== Tab 补全 ====================

    /**
     * 从数据库获取所有已注册玩家名用于 Tab 补全
     */
    private static CompletableFuture<Suggestions> suggestRegisteredPlayers(SuggestionsBuilder builder) {
        DatabaseManager db = DatabaseManager.getInstance();
        for (String name : db.getAllPlayerNames()) {
            if (name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    // ==================== 命令执行 ====================

    /**
     * /loginmod reload - 重新加载配置和语言
     */
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        boolean configOk = ModConfig.reload();
        LanguageManager.reload();

        if (configOk) {
            source.sendSystemMessage(Component.literal(TextUtils.t("admin.reload")));
            LOGGER.info("管理员 {} 重新加载了配置", source.getTextName());
        } else {
            source.sendSystemMessage(Component.literal(TextUtils.t("admin.reload_fail")));
        }

        return configOk ? 1 : 0;
    }

    /**
     * /loginmod unregister <玩家名> - 强制注销玩家
     */
    private static int executeUnregister(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String playerName = StringArgumentType.getString(context, "player");

        DatabaseManager db = DatabaseManager.getInstance();
        UUID uuid = db.findUuidByUsername(playerName);

        if (uuid == null) {
            source.sendSystemMessage(Component.literal(
                TextUtils.t("admin.unregister_not_found", playerName)));
            return 0;
        }

        // 如果玩家在线，踢出服务器
        ServerPlayer target = source.getServer().getPlayerList().getPlayer(uuid);
        if (target != null) {
            LoginManager.getInstance().setUnregistered(uuid);
            target.connection.disconnect(Component.literal(TextUtils.t("kick.unregistered")));
        }

        db.unregisterPlayer(uuid);
        source.sendSystemMessage(Component.literal(
            TextUtils.t("admin.unregister", playerName)));
        LOGGER.info("管理员 {} 强制注销了玩家 {}", source.getTextName(), playerName);
        return 1;
    }

    /**
     * /loginmod resetpassword <玩家名> <新密码> - 重置玩家密码
     */
    private static int executeResetPassword(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String playerName = StringArgumentType.getString(context, "player");
        String newPassword = StringArgumentType.getString(context, "newPassword");

        DatabaseManager db = DatabaseManager.getInstance();
        UUID uuid = db.findUuidByUsername(playerName);

        if (uuid == null) {
            source.sendSystemMessage(Component.literal(
                TextUtils.t("admin.unregister_not_found", playerName)));
            return 0;
        }

        // 验证新密码长度
        ModConfig config = ModConfig.getInstance();
        if (newPassword.length() < config.getPasswordMinLength() || newPassword.length() > config.getPasswordMaxLength()) {
            source.sendSystemMessage(Component.literal(
                "§c密码长度必须在 " + config.getPasswordMinLength() + "-" + config.getPasswordMaxLength() + " 个字符之间"));
            return 0;
        }

        db.changePassword(uuid, newPassword);

        // 通知在线玩家
        ServerPlayer target = source.getServer().getPlayerList().getPlayer(uuid);
        if (target != null) {
            target.sendSystemMessage(Component.literal(
                TextUtils.t("admin.reset_password_notify", source.getTextName())));
            target.sendSystemMessage(Component.literal(
                TextUtils.t("admin.reset_password_new", newPassword)));
        }

        source.sendSystemMessage(Component.literal(
            TextUtils.t("admin.reset_password", playerName)));
        LOGGER.info("管理员 {} 重置了玩家 {} 的密码", source.getTextName(), playerName);
        return 1;
    }

    /**
     * /loginmod info <玩家名> - 查看玩家详细信息
     */
    private static int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String playerName = StringArgumentType.getString(context, "player");

        DatabaseManager db = DatabaseManager.getInstance();
        UUID uuid = db.findUuidByUsername(playerName);

        if (uuid == null) {
            source.sendSystemMessage(Component.literal(
                TextUtils.t("admin.unregister_not_found", playerName)));
            return 0;
        }

        Map<String, String> info = db.getPlayerInfo(uuid);
        if (info.isEmpty()) {
            source.sendSystemMessage(Component.literal("§c无法获取玩家信息"));
            return 0;
        }

        source.sendSystemMessage(Component.literal("§7=== §bQLogin 玩家信息 §7==="));
        source.sendSystemMessage(Component.literal(
            TextUtils.t("info.uuid", info.get("uuid"))));
        source.sendSystemMessage(Component.literal(
            TextUtils.t("info.username", info.get("username"))));
        source.sendSystemMessage(Component.literal(
            TextUtils.t("info.register_time", info.get("register_time"))));
        source.sendSystemMessage(Component.literal(
            TextUtils.t("info.last_login", info.get("last_login"))));
        source.sendSystemMessage(Component.literal(
            TextUtils.t("info.login_fail", info.get("login_fail_count"))));

        // 格式化 IP 历史
        String ipHistory = info.get("ip_history");
        if (ipHistory != null) {
            source.sendSystemMessage(Component.literal(
                TextUtils.t("info.ip_history", ipHistory)));
        }

        return 1;
    }
}
