package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginMod;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.TextUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;

/**
 * /login <密码> 和 /l <密码> - 登录命令 (1.12.2 Forge)
 */
public class CommandLogin extends CommandBase {

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/login <password>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("l");
        aliases.add("log");
        return aliases;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("此命令只能由玩家执行");
        }

        if (args.length < 1) {
            throw new CommandException("用法: /login <密码>");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        String password = args[0];

        executeLogin(player, password);
    }

    private void executeLogin(EntityPlayerMP player, String password) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUniqueID());
        DatabaseManager db = DatabaseManager.getInstance();

        if (state == LoginState.LOGGED_IN) {
            TextUtils.sendWarning(player, "login.already");
            return;
        }

        if (!db.isPlayerRegistered(player.getUniqueID())) {
            TextUtils.sendMsg(player, "register.exists");
            return;
        }

        if (db.verifyPassword(player.getUniqueID(), password)) {
            loginManager.setLoggedIn(player.getUniqueID());
            loginManager.resetLoginFails(player.getUniqueID());

            String ip = loginManager.getPlayerIp(player);
            db.updateLoginInfo(player.getUniqueID(), ip);

            TextUtils.sendSuccess(player, "login.success", player.getName());
            TextUtils.sendTitle(player, "登录成功", "欢迎回来！");

            LoginMod.LOGGER.info("Player {} logged in", player.getName());
        } else {
            boolean banned = loginManager.recordLoginFail(player.getUniqueID());
            int maxAttempts = ModConfig.getInstance().getMaxLoginAttempts();

            if (banned) {
                TextUtils.sendMsg(player, "ban.too_many_attempts");
                player.connection.disconnect(new TextComponentString(TextUtils.t("ban.too_many_attempts")));
            } else {
                TextUtils.sendMsg(player, "login.fail", maxAttempts);
            }
        }
    }

}
