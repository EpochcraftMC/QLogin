package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * /changepassword <鏃у瘑鐮? <鏂板瘑鐮? - 淇敼瀵嗙爜鍛戒护
 */
public class ChangePasswordCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("changepassword")
            .then(Commands.argument("oldPassword", StringArgumentType.word())
                .then(Commands.argument("newPassword", StringArgumentType.word())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerPlayer player = source.getPlayer();
                        if (player == null) {
                            source.sendFailure(Component.literal("搂c姝ゅ懡浠ゅ彧鑳界敱鐜╁鎵ц"));
                            return 0;
                        }
                        String oldPassword = StringArgumentType.getString(context, "oldPassword");
                        String newPassword = StringArgumentType.getString(context, "newPassword");
                        return executeChangePassword(player, oldPassword, newPassword);
                    })
                )
            )
        );
    }

    private static int executeChangePassword(ServerPlayer player, String oldPassword, String newPassword) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUUID());
        DatabaseManager db = DatabaseManager.getInstance();

        // 蹇呴』宸茬櫥褰?        if (state != LoginState.LOGGED_IN) {
            player.sendSystemMessage(Component.literal("搂c璇峰厛鐧诲綍鍚庡啀淇敼瀵嗙爜"));
            return 0;
        }

        // 楠岃瘉鏃у瘑鐮?        if (!db.verifyPassword(player.getUUID(), oldPassword)) {
            player.sendSystemMessage(Component.literal("搂c鏃у瘑鐮侀敊璇?));
            return 0;
        }

        // 楠岃瘉鏂板瘑鐮侀暱搴?        ModConfig config = ModConfig.getInstance();
        if (newPassword.length() < config.getPasswordMinLength() || newPassword.length() > config.getPasswordMaxLength()) {
            player.sendSystemMessage(Component.literal("搂c鏂板瘑鐮侀暱搴﹀繀椤诲湪 " + config.getPasswordMinLength() + "-" + config.getPasswordMaxLength() + " 涓瓧绗︿箣闂?));
            return 0;
        }

        // 鏂版棫瀵嗙爜涓嶈兘鐩稿悓
        if (oldPassword.equals(newPassword)) {
            player.sendSystemMessage(Component.literal("搂c鏂板瘑鐮佷笉鑳戒笌鏃у瘑鐮佺浉鍚?));
            return 0;
        }

        // 鎵ц淇敼
        player.sendSystemMessage(Component.literal("搂7姝ｅ湪淇敼瀵嗙爜..."));
        if (db.changePassword(player.getUUID(), newPassword)) {
            TextUtils.sendSuccess(player, "password.change_success");
            LOGGER.info("鐜╁ {} 宸蹭慨鏀瑰瘑鐮?, player.getName().getString());
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("搂c瀵嗙爜淇敼澶辫触锛岃绋嶅悗閲嶈瘯"));
            return 0;
        }
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}