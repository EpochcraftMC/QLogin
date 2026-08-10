package top.chenray.qlogin.command;

import com.google.gson.Gson;
import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginMod;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.LanguageManager;
import top.chenray.qlogin.util.TextUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * /loginmod - 管理员命令 (1.12.2 Forge)
 * 子命令: reload, unregister, resetpassword, info
 * 注意: 1.12.2 不支持 Brigadier TabCompleteProvider，使用 CommandBase.getTabCompletions 替代
 */
public class CommandAdmin extends CommandBase {

    @Override
    public String getName() {
        return "loginmod";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/loginmod <reload|unregister|resetpassword|info> [player] [newPassword]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4; // OP 权限等级 4
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new CommandException("用法: /loginmod <reload|unregister|resetpassword|info> [player] [newPassword]");
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "reload":
                executeReload(sender);
                break;
            case "unregister":
                if (args.length < 2) {
                    throw new CommandException("用法: /loginmod unregister <player>");
                }
                executeUnregister(server, sender, args[1]);
                break;
            case "resetpassword":
                if (args.length < 3) {
                    throw new CommandException("用法: /loginmod resetpassword <player> <newPassword>");
                }
                executeResetPassword(server, sender, args[1], args[2]);
                break;
            case "info":
                if (args.length < 2) {
                    throw new CommandException("用法: /loginmod info <player>");
                }
                executeInfo(sender, args[1]);
                break;
            default:
                throw new CommandException("未知子命令: " + subCmd + ". 可用: reload, unregister, resetpassword, info");
        }
    }

    // ==================== Tab 补全 ====================

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "reload", "unregister", "resetpassword", "info");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("unregister") ||
                args[0].equalsIgnoreCase("resetpassword") || args[0].equalsIgnoreCase("info"))) {
            List<String> suggestions = new ArrayList<>();

            // 在线玩家名
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    suggestions.add(player.getName());
                }
            }

            // 从数据库查询已注册玩家
            try {
                java.sql.Connection conn = DatabaseManager.getInstance().getConnection();
                if (conn != null) {
                    String sql = "SELECT DISTINCT username FROM players WHERE username LIKE ?";
                    try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, args[1] + "%");
                        ResultSet rs = pstmt.executeQuery();
                        while (rs.next()) {
                            String name = rs.getString("username");
                            if (!suggestions.contains(name)) {
                                suggestions.add(name);
                            }
                        }
                    }
                }
            } catch (SQLException ignored) {}

            return getListOfStringsMatchingLastWord(args, suggestions);
        }

        return Collections.emptyList();
    }

    // ==================== 命令执行 ====================

    private void executeReload(ICommandSender sender) {
        if (ModConfig.reload()) {
            LanguageManager.reload();
            sender.sendMessage(TextUtils.prefixed(new TextComponentString(LanguageManager.tr("admin.reload"))));
            LoginMod.LOGGER.info("Admin {} reloaded config", sender.getName());
        } else {
            sender.sendMessage(TextUtils.prefixed(new TextComponentString(LanguageManager.tr("admin.reload_fail"))));
        }
    }

    private EntityPlayerMP findPlayerByUsername(MinecraftServer server, String username) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player.getName().equalsIgnoreCase(username)) {
                return player;
            }
        }
        return null;
    }

    private void executeUnregister(MinecraftServer server, ICommandSender sender, String targetName) {
        EntityPlayerMP target = findPlayerByUsername(server, targetName);
        DatabaseManager db = DatabaseManager.getInstance();
        boolean success;

        if (target != null) {
            success = db.unregisterPlayerByUuid(target.getUniqueID());
            if (success) {
                LoginManager.getInstance().setUnregistered(target.getUniqueID());
                target.connection.disconnect(new TextComponentString("§e你的账号已被管理员强制注销，请重新注册"));
            }
        } else {
            success = db.unregisterPlayer(targetName);
        }

        if (success) {
            sender.sendMessage(TextUtils.success("已强制注销玩家 §e" + targetName));
            LoginMod.LOGGER.info("管理员 {} 强制注销了玩家 {}", sender.getName(), targetName);
        } else {
            sender.sendMessage(TextUtils.error("未找到玩家 §e" + targetName + "§c 的注册信息"));
        }
    }

    private void executeResetPassword(MinecraftServer server, ICommandSender sender, String targetName, String newPassword) {
        EntityPlayerMP target = findPlayerByUsername(server, targetName);

        ModConfig config = ModConfig.getInstance();
        if (newPassword.length() < config.getPasswordMinLength() || newPassword.length() > config.getPasswordMaxLength()) {
            sender.sendMessage(TextUtils.error("密码长度必须在 " + config.getPasswordMinLength() + "-" + config.getPasswordMaxLength() + " 个字符之间"));
            return;
        }

        DatabaseManager db = DatabaseManager.getInstance();
        String uuid = null;

        if (target != null) {
            uuid = target.getUniqueID().toString();
        } else {
            Map<String, Object> info = db.getPlayerInfo(targetName);
            if (info != null) {
                uuid = (String) info.get("uuid");
            }
        }

        if (uuid == null) {
            sender.sendMessage(TextUtils.error("玩家 §e" + targetName + "§c 尚未注册"));
            return;
        }

        if (db.changePassword(UUID.fromString(uuid), newPassword)) {
            sender.sendMessage(TextUtils.success("已重置玩家 §e" + targetName + "§a 的密码"));

            if (target != null) {
                target.sendMessage(TextUtils.warning("管理员 §e" + sender.getName() + "§e 已重置你的密码"));
                target.sendMessage(TextUtils.info("新密码: §e" + newPassword + "§b，请尽快修改"));
            }

            LoginMod.LOGGER.info("管理员 {} 重置了玩家 {} 的密码", sender.getName(), targetName);
        } else {
            sender.sendMessage(TextUtils.error("密码重置失败"));
        }
    }

    private void executeInfo(ICommandSender sender, String targetName) {
        Map<String, Object> info = DatabaseManager.getInstance().getPlayerInfo(targetName);
        if (info == null) {
            sender.sendMessage(TextUtils.error("玩家 §e" + targetName + "§c 尚未注册"));
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        sender.sendMessage(new TextComponentString("§7用户名: §e" + info.get("username")));
        sender.sendMessage(new TextComponentString("§7UUID: §f" + info.get("uuid")));
        sender.sendMessage(new TextComponentString("§7注册时间: §b" + sdf.format(new Date((Long) info.get("register_time")))));
        sender.sendMessage(new TextComponentString("§7最后登录: §b" + (info.get("last_login") != null ?
                sdf.format(new Date((Long) info.get("last_login"))) : "无")));
        sender.sendMessage(new TextComponentString("§7登录失败: §c" + info.get("login_fail_count")));

        @SuppressWarnings("unchecked")
        List<String> ipHistory = (List<String>) new Gson().fromJson(
                (String) info.get("ip_history"), List.class);
        if (ipHistory != null && !ipHistory.isEmpty()) {
            sender.sendMessage(new TextComponentString("§7IP历史: §f" + String.join("§7, §f", ipHistory)));
        }
    }

}
