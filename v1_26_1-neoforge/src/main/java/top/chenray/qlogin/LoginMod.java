package top.chenray.qlogin;

import top.chenray.qlogin.command.*;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.handler.PlayerHandler;
import top.chenray.qlogin.util.LanguageManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * QLogin 登录系统 - NeoForge 1.21.1 版本
 *
 * NeoForge 与 Forge API 高度相似，主要区别:
 *   - @Mod 来自 net.neoforged.fml.common.Mod
 *   - @SubscribeEvent 来自 net.neoforged.bus.api.SubscribeEvent
 *   - NeoForge.EVENT_BUS 替代 MinecraftForge.EVENT_BUS
 *   - 使用 neoforge.mods.toml 替代 mods.toml
 *   - 使用 NeoGradle 构建
 */
@Mod(LoginMod.MOD_ID)
public class LoginMod {

    public static final String MOD_ID = "qlogin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static MinecraftServer server;

    public static MinecraftServer getServer() {
        return server;
    }

    public LoginMod(IEventBus modEventBus) {
        LOGGER.info("======================================");
        LOGGER.info("  QLogin 登录系统 v{} (1.21.1 NeoForge)", "1.0.0");
        LOGGER.info("======================================");

        // 注册 Mod 事件总线
        modEventBus.addListener(this::onCommonSetup);

        // 注册 NeoForge 事件总线
        var eventBus = NeoForge.EVENT_BUS;
        eventBus.addListener(this::onServerStarting);
        eventBus.addListener(this::onRegisterCommands);
        eventBus.addListener(this::onServerStopping);
    }

    /**
     * 通用初始化阶段
     */
    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("QLogin 通用初始化完成");
    }

    /**
     * 服务器启动事件 - 初始化配置、数据库、事件处理器
     */
    private void onServerStarting(ServerStartingEvent event) {
        long startTime = System.currentTimeMillis();
        server = event.getServer();

        // 配置目录: config/loginmod/
        Path configDir = server.getServerDirectory().toPath().resolve("config").resolve("loginmod");

        // 加载配置
        ModConfig.load(configDir);

        // 初始化语言管理器
        LanguageManager.init();

        // 初始化数据库
        DatabaseManager.init(configDir);
        DatabaseManager.getInstance().connect();

        // 注册事件处理器
        NeoForge.EVENT_BUS.register(new PlayerHandler());

        LOGGER.info("QLogin 初始化完成 ({}ms)", System.currentTimeMillis() - startTime);
    }

    /**
     * 注册命令事件
     */
    private void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        RegisterCommand.register(dispatcher);
        LoginCommand.register(dispatcher);
        LogoutCommand.register(dispatcher);
        ChangePasswordCommand.register(dispatcher);
        AdminCommand.register(dispatcher);

        LOGGER.info("QLogin 命令已注册");
        LOGGER.info("QLogin 登录系统加载完成 (1.21.1 NeoForge)");
    }

    /**
     * 服务器关闭事件 - 关闭数据库
     */
    private void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("正在关闭登录系统...");
        DatabaseManager.getInstance().close();
        LOGGER.info("登录系统已关闭");
    }
}
