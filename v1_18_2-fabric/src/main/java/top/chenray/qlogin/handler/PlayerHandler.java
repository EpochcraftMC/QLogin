package top.chenray.qlogin.handler;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.util.TextUtils;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;

/**
 * 玩家事件处理器 - 处理加入、离开、踢出等事件
 */
public class PlayerHandler {

    private static final Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;

    /**
     * 注册所有事件监听器
     */
    public static void register() {
        LoginManager loginManager = LoginManager.getInstance();

        // 玩家加入事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // 记录登录位置
            loginManager.recordLoginPosition(player);

            // 处理加入
            loginManager.onPlayerJoin(player);
        });

        // 玩家离开事件
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            loginManager.onPlayerDisconnect(player);
        });

        // 方块破坏事件 - 阻止未登录玩家破坏方块
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!loginManager.isLoggedIn(serverPlayer.getUuid())) {
                    return false; // 取消事件
                }
            }
            return true;
        });

        // 聊天消息拦截 - 未登录玩家不能发言 (1.21 Fabric API)
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!loginManager.isLoggedIn(sender.getUuid())) {
                sender.sendMessage(Text.literal("§7[§b登录系统§7] §c✘ 请先登录后再发言！"));
                return false; // 取消消息
            }
            return true;
        });

        // 方块放置/交互事件 - 阻止未登录玩家放置方块和使用方块
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!loginManager.isLoggedIn(serverPlayer.getUuid())) {
                    return net.minecraft.util.ActionResult.FAIL;
                }
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        // 攻击实体事件 - 阻止未登录玩家攻击
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!loginManager.isLoggedIn(serverPlayer.getUuid())) {
                    return net.minecraft.util.ActionResult.FAIL;
                }
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        // 使用实体事件 - 阻止未登录玩家与实体交互
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!loginManager.isLoggedIn(serverPlayer.getUuid())) {
                    return net.minecraft.util.ActionResult.FAIL;
                }
            }
            return net.minecraft.util.ActionResult.PASS;
        });
    }

    /**
     * 服务端 Tick 处理 - 检查登录超时和冻结玩家位置
     */
    public static void onServerTick(MinecraftServer server) {
        LoginManager loginManager = LoginManager.getInstance();

        // 清理过期封禁
        loginManager.cleanExpiredBans();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            LoginState state = loginManager.getState(player.getUuid());

            if (state == LoginState.LOGGED_IN) {
                continue;
            }

            // 检查登录超时
            if (loginManager.isLoginTimeout(player.getUuid())) {
                player.networkHandler.disconnect(Text.literal(TextUtils.t("login.timeout_kick")));
                LOGGER.warn("Player {} login timeout, kicked", player.getName().getString());
                continue;
            }

            // 冻结玩家位置 - 防止未登录玩家移动
            double[] loginPos = loginManager.getLoginPosition(player.getUuid());
            if (loginPos != null) {
                double dx = player.getX() - loginPos[0];
                double dz = player.getZ() - loginPos[2];

                if (Math.abs(dx) > 0.5 || Math.abs(dz) > 0.5) {
                    player.teleport(server.getOverworld(),
                        loginPos[0], loginPos[1], loginPos[2],
                        (float) loginPos[3], (float) loginPos[4]);
                }

                if (player.getY() < -50) {
                    player.teleport(server.getOverworld(),
                        loginPos[0], loginPos[1], loginPos[2],
                        (float) loginPos[3], (float) loginPos[4]);
                    player.setHealth(player.getMaxHealth());
                    player.getHungerManager().setFoodLevel(20);
                }
            }

            // 每 10 秒发送一次提示
            long remaining = loginManager.getRemainingTime(player.getUuid());
            if (remaining > 0 && remaining % 10 == 0) {
                if (state == LoginState.UNREGISTERED) {
                    TextUtils.sendActionBar(player, "actionbar.register", String.valueOf(remaining));
                } else {
                    TextUtils.sendActionBar(player, "actionbar.login", String.valueOf(remaining));
                }
            } else if (remaining <= 5 && remaining > 0) {
                if (state == LoginState.UNREGISTERED) {
                    TextUtils.sendActionBar(player, "actionbar.urgent", TextUtils.t("actionbar.register_urgent"), remaining);
                } else {
                    TextUtils.sendActionBar(player, "actionbar.urgent", TextUtils.t("actionbar.login_urgent"), remaining);
                }
            }
        }
    }
}
