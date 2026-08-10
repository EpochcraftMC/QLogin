package top.chenray.qlogin.mixin;

import top.chenray.qlogin.LoginManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin - 鏈櫥褰曠帺瀹舵棤鏁屼繚鎶? * 鎷︽埅鎵€鏈変激瀹虫潵婧愶紙鎬墿銆佹帀钀姐€佺伀鐒般€佺帺瀹舵敾鍑荤瓑锛? */
@Mixin(LivingEntity.class)
public class ServerPlayerEntityMixin {

    /**
     * 鎷︽埅 hurtServer - 鏈櫥褰曠帺瀹跺厤鐤墍鏈変激瀹?     */
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player) {
            if (!LoginManager.getInstance().isLoggedIn(player.getUUID())) {
                cir.setReturnValue(false);
            }
        }
    }
}