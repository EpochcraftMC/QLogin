package top.chenray.qlogin;

import top.chenray.qlogin.command.*;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.handler.PlayerHandler;
import top.chenray.qlogin.util.LanguageManager;
import top.chenray.qlogin.util.TitleHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * 鐧诲綍绯荤粺 Mod 涓诲叆鍙?- Minecraft 1.21 鐗堟湰
 * 1.21 涓?TitleS2CPacket API 鏈夊彉鍖栵紝浣跨敤鏂扮増鏋勯€犳柟寮? */
public class LoginMod implements ModInitializer {

    public static final String MOD_ID = "loginmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static MinecraftServer SERVER;

    public static MinecraftServer getServer() {
        return SERVER;
    }

    @Override
    public void onInitialize() {
        long startTime = System.currentTimeMillis();
        LOGGER.info("======================================");
        LOGGER.info("  鐧诲綍绯荤粺 LoginMod v{} (1.21.1)", "?");
        LOGGER.info("======================================");

        // 娉ㄥ唽鐗堟湰閫傞厤鍣?        TitleHelper.setInstance(new v1_21TitleHelper());

        // 鏈嶅姟鍣ㄥ惎鍔ㄦ椂鍒濆鍖?        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SERVER = server;
            Path configDir = server.getServerDirectory().resolve("config").resolve("loginmod");
            ModConfig.load(configDir);
            LanguageManager.init();
            DatabaseManager.init(configDir);
            DatabaseManager.getInstance().connect();
            LOGGER.info("鐧诲綍绯荤粺鍒濆鍖栧畬鎴?({}ms)", System.currentTimeMillis() - startTime);
        });

        // 娉ㄥ唽鍛戒护
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RegisterCommand.register(dispatcher, registryAccess, environment);
            LoginCommand.register(dispatcher, registryAccess, environment);
            LogoutCommand.register(dispatcher, registryAccess, environment);
            ChangePasswordCommand.register(dispatcher, registryAccess, environment);
            AdminCommand.register(dispatcher, registryAccess, environment);
            LOGGER.info("鐧诲綍绯荤粺鍛戒护宸叉敞鍐?);
        });

        // 娉ㄥ唽浜嬩欢澶勭悊鍣?        PlayerHandler.register();

        // Tick 浜嬩欢
        ServerTickEvents.START_SERVER_TICK.register(PlayerHandler::onServerTick);

        // 鏈嶅姟鍣ㄥ叧闂?        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("姝ｅ湪鍏抽棴鐧诲綍绯荤粺...");
            DatabaseManager.getInstance().close();
            LOGGER.info("鐧诲綍绯荤粺宸插叧闂?);
        });

        LOGGER.info("鐧诲綍绯荤粺 LoginMod 1.21.1 鍔犺浇瀹屾垚");
    }

    /** 1.21 Title 閫傞厤鍣?*/
    @SuppressWarnings("unchecked")
    private static class v1_21TitleHelper implements TitleHelper {
        @Override
        public void sendTitle(ServerPlayer player, String title, String subtitle) {
            try {
                var handler = player.connection;
                Class<?> animPkt = Class.forName("net.minecraft.network.protocol.game.ClientboundSetTitleAnimationPacket");
                Class<?> subtitlePkt = Class.forName("net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket");
                Class<?> titlePkt = Class.forName("net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket");
                handler.send((net.minecraft.network.protocol.Packet<?>) animPkt.getConstructor(int.class, int.class, int.class)
                    .newInstance(10, 60, 20));
                handler.send((net.minecraft.network.protocol.Packet<?>) subtitlePkt.getConstructor(Component.class)
                    .newInstance(Component.literal("搂e" + subtitle)));
                handler.send((net.minecraft.network.protocol.Packet<?>) titlePkt.getConstructor(Component.class)
                    .newInstance(Component.literal("搂6" + title)));
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("搂6" + title + " 搂e" + subtitle), true);
            }
        }
    }
}