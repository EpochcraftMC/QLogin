package top.chenray.qlogin.mixin;

import top.chenray.qlogin.LoginManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin - 鎷︽埅鐜╁浜や簰琛屼负 (1.21)
 * 1.21 涓柟娉曠鍚嶄笌 1.20.x 鍏煎
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerInteractionManagerMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand,
                                  BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!LoginManager.getInstance().isLoggedIn(player.getUUID())) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("搂c鈿?璇峰厛鐧诲綍鍚庡啀涓庢柟鍧椾氦浜掞紒"), true);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    // interactEntity - 1.21 姝ゆ柟娉曠鍚嶆湁鍙樺寲锛岀敱 Fabric API 浜嬩欢澶勭悊

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand,
                                 CallbackInfoReturnable<InteractionResult> cir) {
        if (!LoginManager.getInstance().isLoggedIn(player.getUUID())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}