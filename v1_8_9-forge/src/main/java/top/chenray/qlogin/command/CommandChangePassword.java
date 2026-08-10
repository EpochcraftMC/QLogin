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

/**
 * /changepassword <旧密码> <新密码> - 修改密码命令 (1.12.2 Forge)
 */
public class CommandChangePassword extends CommandBase {

    @Override
    public String getName() {
        return "changepassword";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/changepassword <oldPassword> <newPassword>";
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

        if (args.length < 2) {
            throw new CommandException("用法: /changepassword <旧密码> <新密码>");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        String oldPassword = args[0];
        String newPassword = args[1];

        executeChangePassword(player, oldPassword, newPassword);
    }

    private void executeChangePassword(EntityPlayerMP player, String oldPassword, String newPassword) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUniqueID());
        DatabaseManager db = DatabaseManager.getInstance();

        if (state != LoginState.LOGGED_IN) {
            player.sendMessage(new TextComponentString("§c请先登录后再修改密码"));
            return;
        }

        if (!db.verifyPassword(player.getUniqueID(), oldPassword)) {
            player.sendMessage(new TextComponentString("§c旧密码错误"));
            return;
        }

        ModConfig config = ModConfig.getInstance();
        if (newPassword.length() < config.getPasswordMinLength() || newPassword.length() > config.getPasswordMaxLength()) {
            player.sendMessage(new TextComponentString("§c新密码长度必须在 " + config.getPasswordMinLength() + "-" + config.getPasswordMaxLength() + " 个字符之间"));
            return;
        }

        if (oldPassword.equals(newPassword)) {
            player.sendMessage(new TextComponentString("§c新密码不能与旧密码相同"));
            return;
        }

        player.sendMessage(new TextComponentString("§7正在修改密码..."));
        if (db.changePassword(player.getUniqueID(), newPassword)) {
            TextUtils.sendSuccess(player, "password.change_success");
            LoginMod.LOGGER.info("玩家 {} 已修改密码", player.getName());
        } else {
            player.sendMessage(new TextComponentString("§c密码修改失败，请稍后重试"));
        }
    }

}
