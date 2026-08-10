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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * 登录系统 Mod 主入口 - Minecraft 1.19.2 (Fabric)
 */
public class LoginMod implements ModInitializer {

    public static final String MOD_ID = "loginmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static MinecraftServer SERVER;

    public static MinecraftServer getServer() { return SERVER; }

    @Override
    public void onInitialize() {
        long startTime = System.currentTimeMillis();
        LOGGER.info("======================================");
        LOGGER.info("  登录系统 LoginMod v{} (1.19.2 Fabric)", "?");
        LOGGER.info("======================================");

        TitleHelper.setInstance(new v1_19_2TitleHelper());

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SERVER = server;
            Path configDir = server.getRunDirectory().toPath().resolve("config").resolve("loginmod");
            ModConfig.load(configDir);
            LanguageManager.init();
            DatabaseManager.init(configDir);
            DatabaseManager.getInstance().connect();
            LOGGER.info("登录系统初始化完成 ({}ms)", System.currentTimeMillis() - startTime);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RegisterCommand.register(dispatcher, registryAccess, environment);
            LoginCommand.register(dispatcher, registryAccess, environment);
            LogoutCommand.register(dispatcher, registryAccess, environment);
            ChangePasswordCommand.register(dispatcher, registryAccess, environment);
            AdminCommand.register(dispatcher, registryAccess, environment);
            LOGGER.info("登录系统命令已注册");
        });

        PlayerHandler.register();
        ServerTickEvents.START_SERVER_TICK.register(PlayerHandler::onServerTick);

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("正在关闭登录系统...");
            DatabaseManager.getInstance().close();
            LOGGER.info("登录系统已关闭");
        });

        LOGGER.info("登录系统 LoginMod 1.19.2 Fabric 加载完成");
    }

    /** 1.19.2 Title 适配器 */
    @SuppressWarnings("unchecked")
    private static class v1_19_2TitleHelper implements TitleHelper {
        @Override
        public void sendTitle(ServerPlayerEntity player, String title, String subtitle) {
            try {
                var handler = player.networkHandler;
                Class<?> animPkt = Class.forName("net.minecraft.network.packet.s2c.play.SetTitleAnimationS2CPacket");
                Class<?> subtitlePkt = Class.forName("net.minecraft.network.packet.s2c.play.SetSubtitleS2CPacket");
                Class<?> titlePkt = Class.forName("net.minecraft.network.packet.s2c.play.SetTitleS2CPacket");
                handler.sendPacket((net.minecraft.network.Packet<?>) animPkt.getConstructor(int.class, int.class, int.class)
                    .newInstance(10, 60, 20));
                handler.sendPacket((net.minecraft.network.Packet<?>) subtitlePkt.getConstructor(Text.class)
                    .newInstance(Text.literal("§e" + subtitle)));
                handler.sendPacket((net.minecraft.network.Packet<?>) titlePkt.getConstructor(Text.class)
                    .newInstance(Text.literal("§6" + title)));
            } catch (Exception e) {
                player.sendMessage(Text.literal("§6" + title + " §e" + subtitle), true);
            }
        }
    }
}
