package top.chenray.qlogin.handler;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.util.TextUtils;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.slf4j.Logger;

/**
 * 鐜╁浜嬩欢澶勭悊鍣?- 澶勭悊鍔犲叆銆佺寮€銆佽涪鍑虹瓑浜嬩欢
 */
public class PlayerHandler {

    private static final Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;

    /**
     * 娉ㄥ唽鎵€鏈変簨浠剁洃鍚櫒
     */
    public static void register() {
        LoginManager loginManager = LoginManager.getInstance();

        // 鐜╁鍔犲叆浜嬩欢
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // 璁板綍鐧诲綍浣嶇疆
            loginManager.recordLoginPosition(player);

            // 澶勭悊鍔犲叆
            loginManager.onPlayerJoin(player);
        });

        // 鐜╁绂诲紑浜嬩欢
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            loginManager.onPlayerDisconnect(player);
        });

        // 鏂瑰潡鐮村潖浜嬩欢 - 闃绘鏈櫥褰曠帺瀹剁牬鍧忔柟鍧?
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!loginManager.isLoggedIn(serverPlayer.getUuid())) {
                    return false; // 鍙栨秷浜嬩欢
                }
            }
            return true;
        });

        // 鑱婂ぉ娑堟伅鎷︽埅 - 鏈櫥褰曠帺瀹朵笉鑳藉彂瑷€ (1.21 Fabric API)

        // 鏂瑰潡鏀剧疆/浜や簰浜嬩欢 - 闃绘鏈櫥褰曠帺瀹舵斁缃柟鍧楀拰浣跨敤鏂瑰潡
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!loginManager.isLoggedIn(serverPlayer.getUuid())) {
                    return net.minecraft.util.ActionResult.FAIL;
                }
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        // 鏀诲嚮瀹炰綋浜嬩欢 - 闃绘鏈櫥褰曠帺瀹舵敾鍑?
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!loginManager.isLoggedIn(serverPlayer.getUuid())) {
                    return net.minecraft.util.ActionResult.FAIL;
                }
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        // 浣跨敤瀹炰綋浜嬩欢 - 闃绘鏈櫥褰曠帺瀹朵笌瀹炰綋浜や簰
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
     * 鏈嶅姟绔?Tick 澶勭悊 - 妫€鏌ョ櫥褰曡秴鏃跺拰鍐荤粨鐜╁浣嶇疆
     */
    public static void onServerTick(MinecraftServer server) {
        LoginManager loginManager = LoginManager.getInstance();

        // 娓呯悊杩囨湡灏佺
        loginManager.cleanExpiredBans();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            LoginState state = loginManager.getState(player.getUuid());

            if (state == LoginState.LOGGED_IN) {
                continue;
            }

            // 妫€鏌ョ櫥褰曡秴鏃?
            if (loginManager.isLoginTimeout(player.getUuid())) {
                player.networkHandler.disconnect(new LiteralText(TextUtils.t("login.timeout_kick")));
                LOGGER.warn("Player {} login timeout, kicked", player.getName().getString());
                continue;
            }

            // 鍐荤粨鐜╁浣嶇疆 - 闃叉鏈櫥褰曠帺瀹剁Щ鍔?
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

            // 姣?10 绉掑彂閫佷竴娆℃彁绀?
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