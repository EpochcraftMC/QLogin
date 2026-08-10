package top.chenray.qlogin;

import top.chenray.qlogin.command.*;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.handler.PlayerHandler;
import top.chenray.qlogin.util.LanguageManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * QLogin 主入口 - 1.12.2 版本 (兼容 Cleanroom / Forge)
 *
 * ⚠ 1.12.2 迁移注意事项:
 *   - 使用 stable_39 映射 (目前绝大多数 1.12.2 项目使用)
 *   - 使用 @Mod 注解替代 Fabric 的 ModInitializer
 *   - 使用 Forge 事件系统替代 Fabric API
 *   - 使用 ICommand 接口替代 Brigadier (1.12.2 尚无 Brigadier)
 *   - EntityPlayerMP 替代 ServerPlayerEntity
 *   - TextComponentString 替代 Text.literal()
 *   - player.getName() 返回 String (而非 Text)
 *   - player.getUniqueID() 替代 player.getUuid()
 *   - player.connection 替代 player.networkHandler
 *   - player.connection.setPlayerLocation() 替代 player.teleport()
 *   - SPacketTitle 发送标题 (含 Type.TIMES/SUBTITLE/TITLE)
 *   - Log4j (@@LogManager) 替代 SLF4J
 *   - Java 8 (无 HexFormat, 使用 DatatypeConverter)
 *   - Files.readAllBytes() + new String() 替代 Files.readString()
 *
 * 🧹 Cleanroom 兼容要点:
 *   - 不使用 FMLCorePlugin / IFMLLoadingPlugin — Cleanroom 内置 Mixin
 *   - Mixin 配置通过 @Mod(mixinConfigs = ...) 声明
 *   - 无需添加 mixin 依赖 (Cleanroom 已内置 Mixin 0.8.5+)
 *   - 所有 Forge 事件、命令、stable_39 映射完全兼容
 */
@Mod(
    modid = LoginMod.MOD_ID,
    name = "QLogin",
    version = "1.0.0",
    acceptableRemoteVersions = "*",
    serverSideOnly = true
)
public class LoginMod {

    public static final String MOD_ID = "qlogin";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /** Minecraft Server 实例 */
    private static MinecraftServer server;

    /**
     * 获取服务器实例
     */
    public static MinecraftServer getServer() {
        return server;
    }

    /**
     * 预初始化阶段 - 加载配置
     */
    @EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("======================================");
        LOGGER.info("  QLogin 登录系统 v{} (1.12.2 Forge)", "1.0.0");
        LOGGER.info("======================================");

        // 配置目录: config/loginmod/
        Path configDir = event.getModConfigurationDirectory().toPath().resolve("loginmod");

        // 加载配置
        ModConfig.load(configDir);

        // 初始化语言管理器
        LanguageManager.init();

        // 初始化数据库
        DatabaseManager.init(configDir);
        DatabaseManager.getInstance().connect();

        // 创建并注册玩家事件处理器
        PlayerHandler handler = new PlayerHandler();
        // Forge 事件总线: 交互事件、聊天事件、Tick 事件等
        MinecraftForge.EVENT_BUS.register(handler);
        // FML 事件总线: PlayerLoggedInEvent / PlayerLoggedOutEvent
        FMLCommonHandler.instance().bus().register(handler);

        LOGGER.info("QLogin 预初始化完成 ({}ms)", System.currentTimeMillis() - startTime);
    }

    /**
     * 服务器启动事件 - 注册命令和获取服务器实例
     */
    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        server = event.getServer();

        // 注册命令 (1.12.2 使用 ICommand 接口)
        event.registerServerCommand(new CommandRegister());
        event.registerServerCommand(new CommandLogin());
        event.registerServerCommand(new CommandLogout());
        event.registerServerCommand(new CommandChangePassword());
        event.registerServerCommand(new CommandAdmin());

        LOGGER.info("QLogin 命令已注册");
        LOGGER.info("QLogin 登录系统加载完成 (1.12.2 Forge, stable_39 映射)");
    }

    /**
     * 服务器关闭事件 - 关闭数据库
     */
    @EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        LOGGER.info("正在关闭登录系统...");
        DatabaseManager.getInstance().close();
        LOGGER.info("登录系统已关闭");
    }
}
