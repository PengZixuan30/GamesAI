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
        source.sendFeedback(() -> Text.literal("")
                .append(Text.literal(config.getPrefix()))
                .append(Text.translatable("help.games_ai.basic", version)),
                    false);

        if (raw.equals("/ask")) {
            // /ask <content>
            source.sendFeedback(() -> Text.literal("")
                            .append(Text.literal(config.getPrefix()))
                            .append(Text.translatable("help.games_ai.command.basic"))
                            .append(Text.literal("/ask <content>")
                                    .formatted(Formatting.GRAY)
                                    .styled(style -> style
                                            .withClickEvent(new ClickEvent.SuggestCommand(
                                                    "/ask "
                                            ))
                                    ))
                            .append(Text.translatable("help.games_ai.command.ask")),
                    false);
        }

        // /ask -m <model> <content>
        source.sendFeedback(() -> Text.literal("")
                .append(Text.literal(config.getPrefix()))
                .append(Text.translatable("help.games_ai.command.basic"))
                .append(Text.literal("/ask -m <model> <content>")
                        .formatted(Formatting.GRAY)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.SuggestCommand(
                                        "/ask -m "
                                ))
                        ))
                .append(Text.translatable("help.games_ai.command.ask")),
            false);

        // 可用模型列表
        source.sendFeedback(() -> Text.literal("")
                .append(Text.literal(config.getPrefix()))
                .append(Text.translatable("help.games_ai.ai.model",
                    String.join(", ", config.getAllAi().keySet()))),
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
        source.sendFeedback(() -> Text.literal("")
                    .append(Text.literal(config.getPrefix()))
                    .append(Text.translatable("help.games_ai.basic", version)),
            false);

        if (raw.equals("/gamesai history") || raw.equals("/gamesai")) {
            // /gamesai history clear
            source.sendFeedback(() -> Text.literal("")
                            .append(Text.literal(config.getPrefix()))
                            .append(Text.translatable("help.games_ai.command.basic"))
                            .append(Text.literal("/gamesai history clear")
                                    .formatted(Formatting.GRAY)
                                    .styled(style -> style
                                            .withClickEvent(new ClickEvent.SuggestCommand(
                                                    "/gamesai history clear "
                                            )))
                            )
                            .append(Text.literal(" — "))
                            .append(Text.translatable("help.games_ai.history.clear")),
                    false);

            if (source.hasPermissionLevel(4)) {
                // /gamesai history clearall
                source.sendFeedback(() -> Text.literal("")
                                .append(Text.literal(config.getPrefix()))
                                .append(Text.translatable("help.games_ai.command.basic"))
                                .append(Text.literal("/gamesai history clearall")
                                        .formatted(Formatting.GRAY)
                                        .styled(style -> style
                                                .withClickEvent(new ClickEvent.SuggestCommand(
                                                        "/gamesai history clearall "
                                                )))
                                )
                                .append(Text.literal(" — "))
                                .append(Text.translatable("help.games_ai.history.clearall")),
                        false);
            }
        }

        return 1;
    }
}
