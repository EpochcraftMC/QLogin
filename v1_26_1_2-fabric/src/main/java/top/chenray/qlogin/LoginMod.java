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
 * 登录系统 Mod 主入口 - Minecraft 1.21 版本
 * 1.21 中 TitleS2CPacket API 有变化，使用新版构造方式
 */
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
        LOGGER.info("  登录系统 LoginMod v{} (1.21.1)", "?");
        LOGGER.info("======================================");

        // 注册版本适配器
        TitleHelper.setInstance(new v1_21TitleHelper());

        // 服务器启动时初始化
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SERVER = server;
            Path configDir = server.getServerDirectory().resolve("config").resolve("loginmod");
            ModConfig.load(configDir);
            LanguageManager.init();
            DatabaseManager.init(configDir);
            DatabaseManager.getInstance().connect();
            LOGGER.info("登录系统初始化完成 ({}ms)", System.currentTimeMillis() - startTime);
        });

        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RegisterCommand.register(dispatcher, registryAccess, environment);
            LoginCommand.register(dispatcher, registryAccess, environment);
            LogoutCommand.register(dispatcher, registryAccess, environment);
            ChangePasswordCommand.register(dispatcher, registryAccess, environment);
            AdminCommand.register(dispatcher, registryAccess, environment);
            LOGGER.info("登录系统命令已注册");
        });

        // 注册事件处理器
        PlayerHandler.register();

        // Tick 事件
        ServerTickEvents.START_SERVER_TICK.register(PlayerHandler::onServerTick);

        // 服务器关闭
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("正在关闭登录系统...");
            DatabaseManager.getInstance().close();
            LOGGER.info("登录系统已关闭");
        });

        LOGGER.info("登录系统 LoginMod 1.21.1 加载完成");
    }

    /** 1.21 Title 适配器 */
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
                    .newInstance(Component.literal("§e" + subtitle)));
                handler.send((net.minecraft.network.protocol.Packet<?>) titlePkt.getConstructor(Component.class)
                    .newInstance(Component.literal("§6" + title)));
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("§6" + title + " §e" + subtitle), true);
            }
        }
    }
}
