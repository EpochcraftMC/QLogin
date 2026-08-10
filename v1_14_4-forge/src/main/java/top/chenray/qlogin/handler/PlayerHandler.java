package top.chenray.qlogin.handler;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.util.TextUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.apache.logging.log4j.Logger;

/**
 * 玩家事件处理器 - 处理加入、离开、Tick 等事件 (1.12.2 Forge)
 * 使用 Forge 的事件系统 (EventBus) 而非 Fabric API
 *
 * 注意: 1.12.2 Forge 事件:
 * - PlayerLoggedInEvent / PlayerLoggedOutEvent (玩家加入/离开)
 * - PlayerInteractEvent (玩家交互)
 * - ServerChatEvent (聊天)
 * - TickEvent.ServerTickEvent (服务器 Tick)
 * - LivingEvent.LivingUpdateEvent (实体更新)
 */
public class PlayerHandler {

    private static final Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;

    /**
     * 玩家加入服务器事件
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
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
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            LoginManager.getInstance().onPlayerDisconnect(player);
        }
    }

    /**
     * 服务器 Tick 事件 - 检查登录超时和冻结玩家位置
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != Phase.END) return;
        if (!ModConfig.getInstance().isKickOnTimeout()) return;

        LoginManager loginManager = LoginManager.getInstance();

        // 清理过期封禁
        loginManager.cleanExpiredBans();

        MinecraftServer server = top.chenray.qlogin.LoginMod.getServer();
        if (server == null) return;

        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            LoginState state = loginManager.getState(player.getUniqueID());

            if (state == LoginState.LOGGED_IN) {
                continue;
            }

            // 检查登录超时
            if (loginManager.isLoginTimeout(player.getUniqueID())) {
                player.connection.disconnect(new net.minecraft.util.text.TextComponentString(TextUtils.t("login.timeout_kick")));
                LOGGER.warn("Player {} login timeout, kicked", player.getName());
                continue;
            }

            // 冻结玩家位置 - 防止未登录玩家移动
            double[] loginPos = loginManager.getLoginPosition(player.getUniqueID());
            if (loginPos != null) {
                double dx = player.posX - loginPos[0];
                double dz = player.posZ - loginPos[2];

                if (Math.abs(dx) > 0.5 || Math.abs(dz) > 0.5) {
                    // 1.12.2 使用 setPositionAndUpdate 代替 teleport
                    player.connection.setPlayerLocation(
                        loginPos[0], loginPos[1], loginPos[2],
                        (float) loginPos[3], (float) loginPos[4]);
                }

                if (player.posY < -50) {
                    player.connection.setPlayerLocation(
                        loginPos[0], loginPos[1], loginPos[2],
                        (float) loginPos[3], (float) loginPos[4]);
                    player.setHealth(player.getMaxHealth());
                    player.getFoodStats().setFoodLevel(20);
                }
            }

            // 发送 ActionBar 提示
            long remaining = loginManager.getRemainingTime(player.getUniqueID());
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

    // ==================== 交互拦截 ====================

    /**
     * 右键点击方块事件
     */
    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
            if (!LoginManager.getInstance().isLoggedIn(player.getUniqueID())) {
                if (!event.getWorld().isRemote) {
                    player.sendMessage(new net.minecraft.util.text.TextComponentString(TextUtils.t("interact.blocked")));
                }
                event.setCanceled(true);
            }
        }
    }

    /**
     * 左键点击方块事件（用于阻止破坏方块）
     */
    @SubscribeEvent
    public void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
            if (!LoginManager.getInstance().isLoggedIn(player.getUniqueID())) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 右键使用物品事件
     */
    @SubscribeEvent
    public void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
            if (!LoginManager.getInstance().isLoggedIn(player.getUniqueID())) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 实体交互事件
     */
    @SubscribeEvent
    public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
            if (!LoginManager.getInstance().isLoggedIn(player.getUniqueID())) {
                if (!event.getWorld().isRemote) {
                    player.sendMessage(new net.minecraft.util.text.TextComponentString(TextUtils.t("interact.entity_blocked")));
                }
                event.setCanceled(true);
            }
        }
    }

    /**
     * 聊天事件 - 阻止未登录玩家发送聊天消息
     * 1.12.2 Forge 的 ServerChatEvent 可直接取消
     */
    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (event.getPlayer() instanceof EntityPlayerMP) {
            EntityPlayerMP player = event.getPlayer();
            if (!LoginManager.getInstance().isLoggedIn(player.getUniqueID())) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(TextUtils.t("chat.blocked")));
                event.setCanceled(true);
            }
        }
    }
}
