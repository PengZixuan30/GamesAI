package io.github.pengzixuan30.gamesai.help;

import com.mojang.brigadier.context.CommandContext;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GamesAIHelp {

    public static int executeAskHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        String raw = ctx.getInput();
        GamesAIConfig config = GamesAI.getConfig();
        String version = FabricLoader.getInstance()
            .getModContainer("games_ai")
            .orElseThrow()
            .getMetadata()
            .getVersion()
            .getFriendlyString();

        // 欢迎信息
        source.sendFeedback(() -> Text.literal(config.getPrefix()
                + Text.translatable("help.games_ai.basic", version).getString()),
                    false);

        if (!raw.contains(" -m") && !raw.contains(" --model")) {
            // /ask <content>
            source.sendFeedback(() -> Text.literal(config.getPrefix()
                            + Text.translatable("help.games_ai.command.basic").getString())
                            .append(Text.literal("/ask <content>")
                                    .formatted(Formatting.GRAY)
                                    .styled(style -> style
                                            .withClickEvent(new ClickEvent.SuggestCommand(
                                                    "/ask "
                                            ))
                                    ))
                            .append(Text.literal(Text.translatable("help.games_ai.command.ask").getString())),
                    false);
        }

        // /ask -m <model> <content>
        source.sendFeedback(() -> Text.literal(config.getPrefix()
                        + Text.translatable("help.games_ai.command.basic").getString())
                        .append(Text.literal("/ask -m <model> <content>")
                        .formatted(Formatting.GRAY)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.SuggestCommand(
                                        "/ask -m "
                                ))
                        ))
                .append(Text.literal(Text.translatable("help.games_ai.command.ask").getString())),
            false);

        // 可用模型列表
        source.sendFeedback(() -> Text.literal(config.getPrefix()
                + Text.translatable("help.games_ai.ai.model",
                    String.join(", ", config.getAllAi().keySet())).getString()),
            false);

        return 1;
    }

    public static int executeGamesAIHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        String raw = ctx.getInput();
        GamesAIConfig config = GamesAI.getConfig();
        String version = FabricLoader.getInstance()
                .getModContainer("games_ai")
                .orElseThrow()
                .getMetadata()
                .getVersion()
                .getFriendlyString();

        // 欢迎信息
        source.sendFeedback(() -> Text.literal(config.getPrefix()
                    + Text.translatable("help.games_ai.basic", version).getString()),
            false);

        // 仅在 /gamesai（无子命令）或 /gamesai history 下显示 history 帮助
        if (!raw.contains("debug") && !raw.contains("help")) {
            // /gamesai history clear
            source.sendFeedback(() -> Text.literal(config.getPrefix()
                            + Text.translatable("help.games_ai.command.basic").getString())
                            .append(Text.literal("/gamesai history clear")
                                    .formatted(Formatting.GRAY)
                                    .styled(style -> style
                                            .withClickEvent(new ClickEvent.SuggestCommand(
                                                    "/gamesai history clear "
                                            )))
                            )
                            .append(Text.literal(" — " + Text.translatable("help.games_ai.history.clear").getString())),
                    false);

            if (source.hasPermissionLevel(4)) {
                // /gamesai history clearall
                source.sendFeedback(() -> Text.literal(config.getPrefix()
                                + Text.translatable("help.games_ai.command.basic").getString())
                                .append(Text.literal("/gamesai history clearall")
                                        .formatted(Formatting.GRAY)
                                        .styled(style -> style
                                                .withClickEvent(new ClickEvent.SuggestCommand(
                                                        "/gamesai history clearall "
                                                )))
                                )
                                .append(Text.literal(" — " + Text.translatable("help.games_ai.history.clearall").getString())),
                        false);
            }
        }

        return 1;
    }
}
