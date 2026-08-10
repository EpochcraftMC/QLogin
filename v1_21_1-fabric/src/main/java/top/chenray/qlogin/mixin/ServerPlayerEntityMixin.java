package top.chenray.qlogin.mixin;

import top.chenray.qlogin.LoginManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin - 未登录玩家无敌保护
 * 拦截所有伤害来源（怪物、掉落、火焰、玩家攻击等）
 */
@Mixin(LivingEntity.class)
public class ServerPlayerEntityMixin {

    /**
     * 拦截 damage - 未登录玩家免疫所有伤害
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity player) {
            if (!LoginManager.getInstance().isLoggedIn(player.getUuid())) {
                cir.setReturnValue(false);
            }
        }
    }
}
