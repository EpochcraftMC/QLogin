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
 * Mixin - 拦截玩家交互行为 (1.21)
 * 1.21 中方法签名与 1.20.x 兼容
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerInteractionManagerMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand,
                                  BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!LoginManager.getInstance().isLoggedIn(player.getUUID())) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c⚠ 请先登录后再与方块交互！"), true);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    // interactEntity - 1.21 此方法签名有变化，由 Fabric API 事件处理

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand,
                                 CallbackInfoReturnable<InteractionResult> cir) {
        if (!LoginManager.getInstance().isLoggedIn(player.getUUID())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
