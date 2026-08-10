package top.chenray.qlogin.mixin;

import top.chenray.qlogin.LoginManager;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin - 拦截命令执行 (1.12.2 Forge)
 * 使用稳定映射 stable_39，类名对应 MCP 名称
 *
 * 拦截 CommandHandler.executeCommand 方法来阻止未登录玩家使用非白名单命令
 */
@Mixin(CommandHandler.class)
public class CommandHandlerMixin {

    /**
     * 在命令执行前检查玩家登录状态
     * 注意: 1.12.2 的 executeCommand 签名:
     *   executeCommand(ICommandSender sender, String rawCommand)
     */
    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true)
    private void onExecuteCommand(ICommandSender sender, String rawCommand, CallbackInfo ci) {
        // 只拦截玩家执行的命令
        if (!(sender instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;

        // 已登录玩家放行
        if (LoginManager.getInstance().isLoggedIn(player.getUniqueID())) {
            return;
        }

        // 提取命令名
        String cmd = rawCommand;
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        String cmdName = cmd.split(" ")[0].toLowerCase();

        // 白名单命令放行
        if (cmdName.equals("register") || cmdName.equals("reg") ||
            cmdName.equals("login") || cmdName.equals("l") || cmdName.equals("log") ||
            cmdName.equals("logout") || cmdName.equals("changepassword") ||
            cmdName.equals("loginmod")) {
            return;
        }

        // 阻止命令执行
        player.sendMessage(new TextComponentString("§7[§b登录系统§7] §c✘ 请先登录后再执行命令！"));
        ci.cancel();
    }
}
