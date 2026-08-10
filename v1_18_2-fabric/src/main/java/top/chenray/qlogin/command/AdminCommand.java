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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 绠＄悊鍛樺懡浠?- /loginmod
 * 瀛愬懡浠? reload, unregister, resetpassword, info
 * 鍏ㄩ儴甯?Tab 琛ュ叏鏀寔
 */
public class AdminCommand {

    /** Tab 琛ュ叏: 鎵€鏈夊凡娉ㄥ唽鐜╁鍚嶏紙鏀寔绂荤嚎鐨勭帺瀹讹級 */
    private static final SuggestionProvider<ServerCommandSource> REGISTERED_PLAYERS =
        (context, builder) -> suggestRegisteredPlayers(builder);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandManager.RegistrationEnvironment environment) {
        var loginmod = CommandManager.literal("loginmod")
            .requires(source -> source.hasPermissionLevel(4)); // OP 鏉冮檺绛夌骇 4

        // /loginmod reload - 閲嶈浇閰嶇疆
        loginmod.then(CommandManager.literal("reload")
            .executes(AdminCommand::executeReload)
        );

        // /loginmod unregister <鐜╁鍚? - 寮哄埗娉ㄩ攢锛堟敮鎸?Tab 琛ュ叏锛?
        loginmod.then(CommandManager.literal("unregister")
            .then(CommandManager.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .executes(AdminCommand::executeUnregister)
            )
        );

        // /loginmod resetpassword <鐜╁鍚? <鏂板瘑鐮? - 閲嶇疆瀵嗙爜锛堟敮鎸?Tab 琛ュ叏锛?
        loginmod.then(CommandManager.literal("resetpassword")
            .then(CommandManager.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .then(CommandManager.argument("newPassword", StringArgumentType.word())
                    .executes(AdminCommand::executeResetPassword)
                )
            )
        );

        // /loginmod info <鐜╁鍚? - 鏌ョ湅鐜╁淇℃伅锛堟敮鎸?Tab 琛ュ叏锛?
        loginmod.then(CommandManager.literal("info")
            .then(CommandManager.argument("player", StringArgumentType.string())
                .suggests(REGISTERED_PLAYERS)
                .executes(AdminCommand::executeInfo)
            )
        );

        dispatcher.register(loginmod);
    }

    // ==================== Tab 琛ュ叏 ====================

    /**
     * 浠庢暟鎹簱鍜屽湪绾跨帺瀹朵腑鑾峰彇鎵€鏈夊凡鐭ョ帺瀹跺悕锛岀敤浜?Tab 琛ュ叏
     */
    private static CompletableFuture<Suggestions> suggestRegisteredPlayers(SuggestionsBuilder builder) {
        // 1. 娣诲姞鍦ㄧ嚎鐜╁鍚?
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

        // 2. 浠庢暟鎹簱娣诲姞鎵€鏈夊凡娉ㄥ唽鐜╁鍚?
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
     * 閫氳繃鐢ㄦ埛鍚嶆煡鎵惧湪绾跨帺瀹舵垨鏁版嵁搴撹褰?
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

    // ==================== 鍛戒护鎵ц ====================

    /**
     * /loginmod reload - 閲嶈浇閰嶇疆
     */
    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (ModConfig.reload()) {
            LanguageManager.reload();
            source.sendFeedback(TextUtils.prefixed(new LiteralText(LanguageManager.tr("admin.reload"))), false);
            LOGGER.info("Admin {} reloaded config", source.getName());
            return 1;
        } else {
            source.sendFeedback(TextUtils.prefixed(new LiteralText(LanguageManager.tr("admin.reload_fail"))), false);
            return 0;
        }
    }

    /**
     * /loginmod unregister <鐜╁> - 寮哄埗娉ㄩ攢锛堟敮鎸佺绾跨帺瀹讹級
     */
    private static int executeUnregister(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity target = findPlayerByUsername(targetName);

        DatabaseManager db = DatabaseManager.getInstance();
        boolean success;

        if (target != null) {
            // 鍦ㄧ嚎鐜╁ - 鐢?UUID 鍒犻櫎
            success = db.unregisterPlayerByUuid(target.getUuid());
            if (success) {
                LoginManager.getInstance().setUnregistered(target.getUuid());
                target.networkHandler.disconnect(new LiteralText("搂e浣犵殑璐﹀彿宸茶绠＄悊鍛樺己鍒舵敞閿€锛岃閲嶆柊娉ㄥ唽"));
            }
        } else {
            // 绂荤嚎鐜╁ - 鐢ㄧ敤鎴峰悕鍒犻櫎
            success = db.unregisterPlayer(targetName);
        }

        if (success) {
            source.sendFeedback(TextUtils.success("宸插己鍒舵敞閿€鐜╁ 搂e" + targetName), false);
            LOGGER.info("绠＄悊鍛?{} 寮哄埗娉ㄩ攢浜嗙帺瀹?{}", source.getName(), targetName);
            return 1;
        } else {
            source.sendFeedback(TextUtils.error("鏈壘鍒扮帺瀹?搂e" + targetName + "搂c 鐨勬敞鍐屼俊鎭?), false);
            return 0;
        }
    }

    /**
     * /loginmod resetpassword <鐜╁> <鏂板瘑鐮? - 閲嶇疆瀵嗙爜锛堟敮鎸佺绾跨帺瀹讹級
     */
    private static int executeResetPassword(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");
        String newPassword = StringArgumentType.getString(context, "newPassword");
        ServerPlayerEntity target = findPlayerByUsername(targetName);

        // 楠岃瘉瀵嗙爜闀垮害
        ModConfig config = ModConfig.getInstance();
        if (newPassword.length() < config.getPasswordMinLength() || newPassword.length() > config.getPasswordMaxLength()) {
            source.sendFeedback(TextUtils.error("瀵嗙爜闀垮害蹇呴』鍦?" + config.getPasswordMinLength() + "-" + config.getPasswordMaxLength() + " 涓瓧绗︿箣闂?), false);
            return 0;
        }

        DatabaseManager db = DatabaseManager.getInstance();
        String uuid = null;

        // 鏌ユ壘鐜╁ UUID
        if (target != null) {
            uuid = target.getUuid().toString();
        } else {
            // 浠庢暟鎹簱鏌ユ壘 UUID
            Map<String, Object> info = db.getPlayerInfo(targetName);
            if (info != null) {
                uuid = (String) info.get("uuid");
            }
        }

        if (uuid == null) {
            source.sendFeedback(TextUtils.error("鐜╁ 搂e" + targetName + "搂c 灏氭湭娉ㄥ唽"), false);
            return 0;
        }

        if (db.changePassword(java.util.UUID.fromString(uuid), newPassword)) {
            source.sendFeedback(TextUtils.success("宸查噸缃帺瀹?搂e" + targetName + "搂a 鐨勫瘑鐮?), false);

            if (target != null) {
                target.sendMessage(TextUtils.warning("绠＄悊鍛?搂e" + source.getName() + "搂e 宸查噸缃綘鐨勫瘑鐮?, false);
                target.sendMessage(TextUtils.info("鏂板瘑鐮? 搂e" + newPassword + "搂b锛岃灏藉揩淇敼", false);
            }

            LOGGER.info("绠＄悊鍛?{} 閲嶇疆浜嗙帺瀹?{} 鐨勫瘑鐮?, source.getName(), targetName);
            return 1;
        } else {
            source.sendFeedback(TextUtils.error("瀵嗙爜閲嶇疆澶辫触"), false);
            return 0;
        }
    }

    /**
     * /loginmod info <鐜╁> - 鏌ョ湅淇℃伅锛堟敮鎸佺绾跨帺瀹讹級
     */
    private static int executeInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");

        Map<String, Object> info = DatabaseManager.getInstance().getPlayerInfo(targetName);
        if (info == null) {
            source.sendFeedback(TextUtils.error("鐜╁ 搂e" + targetName + "搂c 灏氭湭娉ㄥ唽"), false);
            return 0;
        }

        source.sendFeedback(new LiteralText("搂7鐢ㄦ埛鍚? 搂e" + info.get("username")), false);
        source.sendFeedback(new LiteralText("搂7UUID: 搂f" + info.get("uuid")), false);
        source.sendFeedback(new LiteralText("搂7娉ㄥ唽鏃堕棿: 搂b" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date((Long) info.get("register_time")))), false);
        source.sendFeedback(new LiteralText("搂7鏈€鍚庣櫥褰? 搂b" + (info.get("last_login") != null ?
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date((Long) info.get("last_login"))) : "鏃?)), false);
        source.sendFeedback(new LiteralText("搂7鐧诲綍澶辫触: 搂c" + info.get("login_fail_count")), false);

        @SuppressWarnings("unchecked")
        var ipHistory = (java.util.List<String>) new com.google.gson.Gson().fromJson(
            (String) info.get("ip_history"), java.util.List.class);
        if (ipHistory != null && !ipHistory.isEmpty()) {
            source.sendFeedback(new LiteralText("搂7IP鍘嗗彶: 搂f" + String.join("搂7, 搂f", ipHistory)), false);
        }

        return 1;
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}