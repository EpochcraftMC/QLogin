package top.chenray.qlogin;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * QLogin 登录系统 - Forge 1.20.1 版本
 *
 * 使用 Mojang 官方映射 (official mapping)
 *
 * ⚠ Forge 1.20.1 API 变更对照:
 *   - Component.literal()       ← Fabric 中 Text.literal()
 *   - Component.translatable()  ← Fabric 中 Text.translatable()
 *   - MutableComponent          ← Fabric 中 MutableText
 *   - ServerPlayer              ← Fabric 中 ServerPlayerEntity
 *   - player.getUUID()          ← Fabric 中 player.getUuid()
 *   - player.connection         ← Fabric 中 player.networkHandler
 *   - player.connection.disconnect() 同 Fabric
 *   - ChatFormatting            ← Fabric 中 Formatting
 *   - Brigadier 命令注册方式同 Fabric 1.21
 *   - SLF4J/LogUtils            ← Forge 自带日志
 */
@Mod(LoginMod.MOD_ID)
public class LoginMod {

    public static final String MOD_ID = "qlogin";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static MinecraftServer server;

    /**
     * 获取服务器实例
     */
    public static MinecraftServer getServer() {
        return server;
    }

    public LoginMod() {
        LOGGER.info("======================================");
        LOGGER.info("  QLogin 登录系统 v{} (1.20.1 Forge)", "1.0.0");
        LOGGER.info("======================================");

        // 注册事件总线
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onCommonSetup);

        // 注册 Forge 事件总线
        MinecraftForge.EVENT_BUS.register(this);
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
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        long startTime = System.currentTimeMillis();
        server = event.getServer();

        // 配置目录: config/loginmod/
        Path configDir = server.getServerDirectory().toPath().resolve("config").resolve("loginmod");

        // 加载配置
        top.chenray.qlogin.config.ModConfig.load(configDir);

        // 初始化语言管理器
        top.chenray.qlogin.util.LanguageManager.init();

        // 初始化数据库
        top.chenray.qlogin.database.DatabaseManager.init(configDir);
        top.chenray.qlogin.database.DatabaseManager.getInstance().connect();

        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(new top.chenray.qlogin.handler.PlayerHandler());

        LOGGER.info("QLogin 初始化完成 ({}ms)", System.currentTimeMillis() - startTime);
    }

    /**
     * 注册命令事件
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        top.chenray.qlogin.command.RegisterCommand.register(dispatcher);
        top.chenray.qlogin.command.LoginCommand.register(dispatcher);
        top.chenray.qlogin.command.LogoutCommand.register(dispatcher);
        top.chenray.qlogin.command.ChangePasswordCommand.register(dispatcher);
        top.chenray.qlogin.command.AdminCommand.register(dispatcher);

        LOGGER.info("QLogin 命令已注册");
        LOGGER.info("QLogin 登录系统加载完成 (1.20.1 Forge)");
    }

    /**
     * 服务器关闭事件 - 关闭数据库
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("正在关闭登录系统...");
        top.chenray.qlogin.database.DatabaseManager.getInstance().close();
        LOGGER.info("登录系统已关闭");
    }
}
