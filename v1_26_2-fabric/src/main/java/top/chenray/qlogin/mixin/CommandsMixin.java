package top.chenray.qlogin.mixin;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 混入命令系统 - 拦截非登录状态下的命令执行 (Mojang 映射版)
 */
@Mixin(Commands.class)
public abstract class CommandsMixin {

    /**
     * 在命令执行前检查登录状态
     */
    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void onPerformCommand(ParseResults<CommandSourceStack> parse, String command, CallbackInfo ci) {
        var source = parse.getContext().getSource();

        // 只拦截玩家执行的命令
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 已登录玩家放行
        if (LoginManager.getInstance().isLoggedIn(player.getUUID())) {
            return;
        }

        // 登录/注册/修改密码命令放行
        String cmd = command.split(" ")[0].toLowerCase();
        if (cmd.equals("login") || cmd.equals("l") ||
            cmd.equals("register") || cmd.equals("changepassword") ||
            cmd.equals("logout")) {
            return;
        }

        // 管理员命令 /loginmod 放行
        if (cmd.equals("loginmod")) {
            return;
        }

        // 阻止命令执行
        source.sendFailure(Component.literal(TextUtils.t("command.blocked")));
        ci.cancel();
    }
}
