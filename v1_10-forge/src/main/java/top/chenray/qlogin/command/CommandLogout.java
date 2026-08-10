package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginMod;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.util.TextUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/**
 * /logout - 登出命令 (1.12.2 Forge)
 */
public class CommandLogout extends CommandBase {

    @Override
    public String getName() {
        return "logout";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/logout";
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
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("此命令只能由玩家执行");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUniqueID());

        if (state != LoginState.LOGGED_IN) {
            player.sendMessage(new TextComponentString("§c你还没有登录"));
            return;
        }

        loginManager.setLoggedOut(player.getUniqueID());
        TextUtils.sendWarning(player, "logout.success");
        player.sendMessage(new TextComponentString("§b使用 §6/login <密码> §b重新登录"));
        LoginMod.LOGGER.info("玩家 {} 已登出", player.getName());
    }

}
