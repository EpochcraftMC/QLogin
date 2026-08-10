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
 * /register <密码> <确认密码> - 注册命令 (1.12.2 Forge)
 * 1.12.2 使用 CommandBase / ICommand 接口，而非 Brigadier
 */
public class CommandRegister extends CommandBase {

    @Override
    public String getName() {
        return "register";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/register <password> <confirmPassword>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 所有玩家可用
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true; // 所有玩家可用
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("reg");
        return aliases;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("此命令只能由玩家执行");
        }

        if (args.length < 2) {
            throw new CommandException("用法: /register <密码> <确认密码>");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        String password = args[0];
        String confirmPassword = args[1];

        executeRegister(player, password, confirmPassword);
    }

    private void executeRegister(EntityPlayerMP player, String password, String confirmPassword) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUniqueID());
        DatabaseManager db = DatabaseManager.getInstance();

        if (state == LoginState.LOGGED_IN) {
            TextUtils.sendMsg(player, "login.already");
            return;
        }

        if (db.isPlayerRegistered(player.getUniqueID())) {
            TextUtils.sendMsg(player, "register.exists");
            return;
        }

        ModConfig config = ModConfig.getInstance();
        if (password.length() < config.getPasswordMinLength() || password.length() > config.getPasswordMaxLength()) {
            TextUtils.sendMsg(player, "register.password_length", config.getPasswordMinLength(), config.getPasswordMaxLength());
            return;
        }

        if (!password.equals(confirmPassword)) {
            TextUtils.sendMsg(player, "register.password_mismatch");
            return;
        }

        String ip = loginManager.getPlayerIp(player);

        if (db.registerPlayer(player.getUniqueID(), player.getName(), password, ip)) {
            loginManager.setLoggedIn(player.getUniqueID());
            loginManager.resetLoginFails(player.getUniqueID());
            TextUtils.sendMsg(player, "register.success", player.getName());
            TextUtils.sendTitle(player, "注册成功", "欢迎加入服务器！");
            LoginMod.LOGGER.info("Player {} registered", player.getName());
        } else {
            TextUtils.sendMsg(player, "register.fail");
        }
    }

}
