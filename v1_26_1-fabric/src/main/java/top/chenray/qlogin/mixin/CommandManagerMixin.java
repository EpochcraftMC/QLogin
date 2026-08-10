package top.chenray.qlogin.mixin;

import top.chenray.qlogin.LoginManager;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin - 拦截命令执行 (1.21)
 * 与 1.20.x 兼容 - Commands API 未变
 */
@Mixin(Commands.class)
public class CommandManagerMixin {

    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void onExecute(ParseResults<CommandSourceStack> parse, String command, CallbackInfo ci) {
        var source = parse.getContext().getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            LoginManager loginManager = LoginManager.getInstance();

            if (loginManager.isLoggedIn(player.getUUID())) return;

            String cmdText = command.startsWith("/") ? command.substring(1) : command;
            String cmdName = cmdText.split(" ")[0].toLowerCase();

            // 白名单 - 仅允许登录/注册相关命令
            if (cmdName.equals("register") || cmdName.equals("reg") ||
                cmdName.equals("login") || cmdName.equals("l") || cmdName.equals("log")) {
                return;
            }

            player.sendSystemMessage(Component.literal("§7[§b登录系统§7] §c✘ 请先登录后再执行命令！"));
            ci.cancel();
        }
    }
}
