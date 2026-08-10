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
 * Mixin - 鎷︽埅鍛戒护鎵ц (1.21)
 * 涓?1.20.x 鍏煎 - Commands API 鏈彉
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

            // 鐧藉悕鍗?- 浠呭厑璁哥櫥褰?娉ㄥ唽鐩稿叧鍛戒护
            if (cmdName.equals("register") || cmdName.equals("reg") ||
                cmdName.equals("login") || cmdName.equals("l") || cmdName.equals("log")) {
                return;
            }

            player.sendSystemMessage(Component.literal("搂7[搂b鐧诲綍绯荤粺搂7] 搂c鉁?璇峰厛鐧诲綍鍚庡啀鎵ц鍛戒护锛?));
            ci.cancel();
        }
    }
}