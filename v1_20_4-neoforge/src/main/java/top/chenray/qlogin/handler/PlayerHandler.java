package top.chenray.qlogin.handler;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.util.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Phase;
import net.neoforged.fml.LogicalSide;
import org.slf4j.Logger;

/**
 * 玩家事件处理器 - 使用 NeoForge 事件系统
 *
 * NeoForge 1.21.1 事件对照:
 * - PlayerEvent.PlayerLoggedInEvent  → 玩家加入
 * - PlayerEvent.PlayerLoggedOutEvent → 玩家离开
 * - ServerChatEvent                 → 聊天事件
 * - PlayerInteractEvent             → 交互事件
 * - ServerTickEvent                 → 服务器 Tick
 * - LivingEvent.LivingTickEvent     → 实体 Tick
 */
public class PlayerHandler {

    private static final Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;

    /**
     * 玩家加入服务器事件
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LoginManager loginManager = LoginManager.getInstance();

            // 记录登录位置
            loginManager.recordLoginPosition(player);

            // 处理加入
            loginManager.onPlayerJoin(player);
        }
    }

    /**
     * 玩家离开服务器事件
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LoginManager.getInstance().onPlayerDisconnect(player);
        }
    }

    /**
     * 服务器 Tick 事件 - 检查登录超时和冻结玩家位置
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.getPhase() != Phase.END) return;

        LoginManager loginManager = LoginManager.getInstance();

        // 清理过期封禁
        loginManager.cleanExpiredBans();

        MinecraftServer server = top.chenray.qlogin.LoginMod.getServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            LoginState state = loginManager.getState(player.getUUID());

            if (state == LoginState.LOGGED_IN) {
                continue;
            }

            // 检查登录超时
            if (loginManager.isLoginTimeout(player.getUUID())) {
                player.connection.disconnect(Component.literal(TextUtils.t("login.timeout_kick")));
                LOGGER.warn("Player {} login timeout, kicked", player.getDisplayName().getString());
                continue;
            }

            // 冻结玩家位置 - 防止未登录玩家移动
            double[] loginPos = loginManager.getLoginPosition(player.getUUID());
            if (loginPos != null) {
                double dx = player.getX() - loginPos[0];
                double dz = player.getZ() - loginPos[2];

                if (Math.abs(dx) > 0.5 || Math.abs(dz) > 0.5) {
                    player.teleportTo(
                        loginPos[0], loginPos[1], loginPos[2]);
                    player.setYRot((float) loginPos[3]);
                    player.setXRot((float) loginPos[4]);
                }

                if (player.getY() < -50) {
                    player.teleportTo(
                        loginPos[0], loginPos[1], loginPos[2]);
                    player.setYRot((float) loginPos[3]);
                    player.setXRot((float) loginPos[4]);
                    player.setHealth(player.getMaxHealth());
                    player.getFoodData().setFoodLevel(20);
                }
            }

            // 发送 ActionBar 提示
            long remaining = loginManager.getRemainingTime(player.getUUID());
            if (remaining > 0 && remaining % 10 == 0) {
                if (state == LoginState.UNREGISTERED) {
                    TextUtils.sendActionBar(player, "actionbar.register", String.valueOf(remaining));
                } else {
                    TextUtils.sendActionBar(player, "actionbar.login", String.valueOf(remaining));
                }
            } else if (remaining <= 5 && remaining > 0) {
                if (state == LoginState.UNREGISTERED) {
                    TextUtils.sendActionBar(player, "actionbar.urgent",
                        TextUtils.t("actionbar.register_urgent"), remaining);
                } else {
                    TextUtils.sendActionBar(player, "actionbar.urgent",
                        TextUtils.t("actionbar.login_urgent"), remaining);
                }
            }
        }
    }

    // ==================== 交互拦截 ====================

    /**
     * 右键点击方块事件 - 阻止未登录玩家交互
     */
    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!LoginManager.getInstance().isLoggedIn(player.getUUID())) {
                if (event.getSide() == LogicalSide.SERVER) {
                    player.sendSystemMessage(Component.literal(TextUtils.t("interact.blocked")));
                }
                event.setCanceled(true);
            }
        }
    }

    /**
     * 左键点击方块事件
     */
    @SubscribeEvent
    public void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!LoginManager.getInstance().isLoggedIn(player.getUUID())) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 右键点击实体事件
     */
    @SubscribeEvent
    public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!LoginManager.getInstance().isLoggedIn(player.getUUID())) {
                if (event.getSide() == LogicalSide.SERVER) {
                    player.sendSystemMessage(Component.literal(TextUtils.t("interact.entity_blocked")));
                }
                event.setCanceled(true);
            }
        }
    }

    /**
     * 聊天事件 - 阻止未登录玩家聊天
     */
    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!LoginManager.getInstance().isLoggedIn(player.getUUID())) {
            player.sendSystemMessage(Component.literal(TextUtils.t("chat.blocked")));
            event.setCanceled(true);
        }
    }

    /**
     * 玩家 Tick 事件 - 阻止未登录玩家移动（备用）
     */
    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LoginManager loginManager = LoginManager.getInstance();
            LoginState state = loginManager.getState(player.getUUID());

            if (state == LoginState.LOGGED_IN) return;

            double[] loginPos = loginManager.getLoginPosition(player.getUUID());
            if (loginPos != null && loginManager.isPlayerFrozen(player.getUUID())) {
                player.teleportTo(loginPos[0], loginPos[1], loginPos[2]);
                player.setYRot((float) loginPos[3]);
                player.setXRot((float) loginPos[4]);
            }
        }
    }
}
