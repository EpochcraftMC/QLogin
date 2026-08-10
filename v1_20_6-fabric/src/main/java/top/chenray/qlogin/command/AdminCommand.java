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
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 管理员命令 - /loginmod
 * 子命令: reload, unregister, resetpassword, info
 * 全部带 Tab 补全支持
 */
public class AdminCommand {

    /** Tab 补全: 所有已注册玩家名（支持离线的玩家） */
    private static final SuggestionProvider<ServerCommandSource> REGISTERED_PLAYERS =
        (context, builder) -> suggestRegisteredPlayers(builder);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        var loginmod = CommandManager.literal("loginmod")
            .requires(source -> source.hasPermissionLevel(4)); // OP 权限等级 4

        // /loginmod reload - 重载配置
        loginmod.then(CommandManager.literal("reload")
            .executes(AdminCommand::executeReload)
        );

        // /loginmod unregister <玩家名> - 强制注销（支持 Tab 补全）
        loginmod.then(CommandManager.literal("unregister")
            .then(CommandManager.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .executes(AdminCommand::executeUnregister)
            )
        );

        // /loginmod resetpassword <玩家名> <新密码> - 重置密码（支持 Tab 补全）
        loginmod.then(CommandManager.literal("resetpassword")
            .then(CommandManager.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .then(CommandManager.argument("newPassword", StringArgumentType.word())
                    .executes(AdminCommand::executeResetPassword)
                )
            )
        );

        // /loginmod info <玩家名> - 查看玩家信息（支持 Tab 补全）
        loginmod.then(CommandManager.literal("info")
            .then(CommandManager.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .executes(AdminCommand::executeInfo)
            )
        );

        dispatcher.register(loginmod);
    }

    // ==================== Tab 补全 ====================

    /**
     * 从数据库和在线玩家中获取所有已知玩家名，用于 Tab 补全
     */
    private static CompletableFuture<Suggestions> suggestRegisteredPlayers(SuggestionsBuilder builder) {
        // 1. 添加在线玩家名
        try {
            var server = top.chenray.qlogin.LoginMod.getServer();
            if (server != null) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    String name = player.getName().getString();
                    if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                        builder.suggest(name);
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. 从数据库添加所有已注册玩家名
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

    /**
     * 通过用户名查找在线玩家或数据库记录
     */
    private static ServerPlayerEntity findPlayerByUsername(String username) {
        var server = top.chenray.qlogin.LoginMod.getServer();
        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getName().getString().equalsIgnoreCase(username)) {
                    return player;
                }
            }
        }
        return null;
    }

    // ==================== 命令执行 ====================

    /**
     * /loginmod reload - 重载配置
     */
    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (ModConfig.reload()) {
            LanguageManager.reload();
            source.sendMessage(TextUtils.prefixed(Text.literal(LanguageManager.tr("admin.reload"))));
            LOGGER.info("Admin {} reloaded config", source.getName());
            return 1;
        } else {
            source.sendMessage(TextUtils.prefixed(Text.literal(LanguageManager.tr("admin.reload_fail"))));
            return 0;
        }
    }

    /**
     * /loginmod unregister <玩家> - 强制注销（支持离线玩家）
     */
    private static int executeUnregister(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity target = findPlayerByUsername(targetName);

        DatabaseManager db = DatabaseManager.getInstance();
        boolean success;

        if (target != null) {
            // 在线玩家 - 用 UUID 删除
            success = db.unregisterPlayerByUuid(target.getUuid());
            if (success) {
                LoginManager.getInstance().setUnregistered(target.getUuid());
                target.networkHandler.disconnect(Text.literal("§e你的账号已被管理员强制注销，请重新注册"));
            }
        } else {
            // 离线玩家 - 用用户名删除
            success = db.unregisterPlayer(targetName);
        }

        if (success) {
            source.sendMessage(TextUtils.success("已强制注销玩家 §e" + targetName));
            LOGGER.info("管理员 {} 强制注销了玩家 {}", source.getName(), targetName);
            return 1;
        } else {
            source.sendMessage(TextUtils.error("未找到玩家 §e" + targetName + "§c 的注册信息"));
            return 0;
        }
    }

    /**
     * /loginmod resetpassword <玩家> <新密码> - 重置密码（支持离线玩家）
     */
    private static int executeResetPassword(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");
        String newPassword = StringArgumentType.getString(context, "newPassword");
        ServerPlayerEntity target = findPlayerByUsername(targetName);

        // 验证密码长度
        ModConfig config = ModConfig.getInstance();
        if (newPassword.length() < config.getPasswordMinLength() || newPassword.length() > config.getPasswordMaxLength()) {
            source.sendMessage(TextUtils.error("密码长度必须在 " + config.getPasswordMinLength() + "-" + config.getPasswordMaxLength() + " 个字符之间"));
            return 0;
        }

        DatabaseManager db = DatabaseManager.getInstance();
        String uuid = null;

        // 查找玩家 UUID
        if (target != null) {
            uuid = target.getUuid().toString();
        } else {
            // 从数据库查找 UUID
            Map<String, Object> info = db.getPlayerInfo(targetName);
            if (info != null) {
                uuid = (String) info.get("uuid");
            }
        }

        if (uuid == null) {
            source.sendMessage(TextUtils.error("玩家 §e" + targetName + "§c 尚未注册"));
            return 0;
        }

        if (db.changePassword(java.util.UUID.fromString(uuid), newPassword)) {
            source.sendMessage(TextUtils.success("已重置玩家 §e" + targetName + "§a 的密码"));

            if (target != null) {
                target.sendMessage(TextUtils.warning("管理员 §e" + source.getName() + "§e 已重置你的密码"));
                target.sendMessage(TextUtils.info("新密码: §e" + newPassword + "§b，请尽快修改"));
            }

            LOGGER.info("管理员 {} 重置了玩家 {} 的密码", source.getName(), targetName);
            return 1;
        } else {
            source.sendMessage(TextUtils.error("密码重置失败"));
            return 0;
        }
    }

    /**
     * /loginmod info <玩家> - 查看信息（支持离线玩家）
     */
    private static int executeInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");

        Map<String, Object> info = DatabaseManager.getInstance().getPlayerInfo(targetName);
        if (info == null) {
            source.sendMessage(TextUtils.error("玩家 §e" + targetName + "§c 尚未注册"));
            return 0;
        }

        source.sendMessage(Text.literal("§7用户名: §e" + info.get("username")));
        source.sendMessage(Text.literal("§7UUID: §f" + info.get("uuid")));
        source.sendMessage(Text.literal("§7注册时间: §b" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date((Long) info.get("register_time")))));
        source.sendMessage(Text.literal("§7最后登录: §b" + (info.get("last_login") != null ?
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date((Long) info.get("last_login"))) : "无")));
        source.sendMessage(Text.literal("§7登录失败: §c" + info.get("login_fail_count")));

        @SuppressWarnings("unchecked")
        var ipHistory = (java.util.List<String>) new com.google.gson.Gson().fromJson(
            (String) info.get("ip_history"), java.util.List.class);
        if (ipHistory != null && !ipHistory.isEmpty()) {
            source.sendMessage(Text.literal("§7IP历史: §f" + String.join("§7, §f", ipHistory)));
        }

        return 1;
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}
