package top.chenray.qlogin.command;

import top.chenray.qlogin.LoginManager;
import top.chenray.qlogin.LoginState;
import top.chenray.qlogin.config.ModConfig;
import top.chenray.qlogin.database.DatabaseManager;
import top.chenray.qlogin.util.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

/**
 * /changepassword <鏃у瘑鐮? <鏂板瘑鐮? - 淇敼瀵嗙爜鍛戒护
 */
public class ChangePasswordCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("changepassword")
            .then(CommandManager.argument("oldPassword", StringArgumentType.word())
                .then(CommandManager.argument("newPassword", StringArgumentType.word())
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        ServerPlayerEntity player = source.getPlayer();
                        if (player == null) {
                            source.sendError(new LiteralText("搂c姝ゅ懡浠ゅ彧鑳界敱鐜╁鎵ц"));
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

    private static int executeChangePassword(ServerPlayerEntity player, String oldPassword, String newPassword) {
        LoginManager loginManager = LoginManager.getInstance();
        LoginState state = loginManager.getState(player.getUuid());
        DatabaseManager db = DatabaseManager.getInstance();

        // 蹇呴』宸茬櫥褰?
        if (state != LoginState.LOGGED_IN) {
            player.sendMessage(new LiteralText("搂c璇峰厛鐧诲綍鍚庡啀淇敼瀵嗙爜"), false);
            return 0;
        }

        // 楠岃瘉鏃у瘑鐮?
        if (!db.verifyPassword(player.getUuid(), oldPassword)) {
            player.sendMessage(new LiteralText("搂c鏃у瘑鐮侀敊璇?), false);
            return 0;
        }

        // 楠岃瘉鏂板瘑鐮侀暱搴?
        ModConfig config = ModConfig.getInstance();
        if (newPassword.length() < config.getPasswordMinLength() || newPassword.length() > config.getPasswordMaxLength()) {
            player.sendMessage(new LiteralText("搂c鏂板瘑鐮侀暱搴﹀繀椤诲湪 " + config.getPasswordMinLength() + "-" + config.getPasswordMaxLength() + " 涓瓧绗︿箣闂?, false);
            return 0;
        }

        // 鏂版棫瀵嗙爜涓嶈兘鐩稿悓
        if (oldPassword.equals(newPassword)) {
            player.sendMessage(new LiteralText("搂c鏂板瘑鐮佷笉鑳戒笌鏃у瘑鐮佺浉鍚?), false);
            return 0;
        }

        // 鎵ц淇敼
        player.sendMessage(new LiteralText("搂7姝ｅ湪淇敼瀵嗙爜..."), false);
        if (db.changePassword(player.getUuid(), newPassword)) {
            TextUtils.sendSuccess(player, "password.change_success");
            LOGGER.info("鐜╁ {} 宸蹭慨鏀瑰瘑鐮?, player.getName().getString());
            return 1;
        } else {
            player.sendMessage(new LiteralText("搂c瀵嗙爜淇敼澶辫触锛岃绋嶅悗閲嶈瘯"), false);
            return 0;
        }
    }

    private static final org.slf4j.Logger LOGGER = top.chenray.qlogin.LoginMod.LOGGER;
}