package io.github.pengzixuan30.gamesai.command;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;
import io.github.pengzixuan30.gamesai.help.GamesAIHelp;
import io.github.pengzixuan30.gamesai.openai.GamesAIRequestAI;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class GamesAICommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("ask")
                .requires(source -> source.isExecutedByPlayer() || source.getEntity() == null)
                .then(literal("-m")
                    .then(argument("model", StringArgumentType.word())
                        .then(argument("content", StringArgumentType.greedyString())
                            .executes(GamesAICommands::executeModelAsk)
                        )
                        .executes(GamesAIHelp::executeAskHelp)
                    )
                    .executes(GamesAIHelp::executeAskHelp)
                )
                .then(literal("--model")
                    .then(argument("model", StringArgumentType.word())
                        .then(argument("content", StringArgumentType.greedyString())
                            .executes(GamesAICommands::executeModelAsk)
                        )
                        .executes(GamesAIHelp::executeAskHelp)
                    )
                    .executes(GamesAIHelp::executeAskHelp)
                )
                .then(argument("content", StringArgumentType.greedyString())
                    .executes(GamesAICommands::executeAsk)
                )
                .executes(GamesAIHelp::executeAskHelp)
        );
        dispatcher.register(
                literal("gamesai")
                        .then(literal("history")
                                .then(literal("clear")
                                        .executes(ctx -> {
                                            GamesAI.clearHistory(ctx.getSource().getName());
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal(GamesAI.getConfig().getPrefix()
                                                        + GamesAITranslations.tr("command.games_ai.history.clear")),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                                .then(literal("clearall")
                                        .requires(source -> source.hasPermissionLevel(4))
                                        .executes(ctx -> {
                                            GamesAI.clearAllHistory();
                                            GamesAI.LOGGER.info("Clear all history");
                                            ctx.getSource().getServer().getPlayerManager().broadcast(
                                                    Text.literal(GamesAI.getConfig().getPrefix()
                                                        + GamesAITranslations.tr("command.games_ai.history.clearall")),
                                                    false);
                                            return 1;
                                        })
                                )
                                .executes(GamesAIHelp::executeGamesAIHelp)
                        )
                        .then(literal("debug")
                                .executes(ctx -> {
                                    GamesAI.toggleDebugMode();
                                    String status = GamesAI.isDebugMode() ? "Enabled" : "Disabled";
                                    GamesAI.LOGGER.info("Debug mode is {}", status);
                                    ctx.getSource().getServer().getPlayerManager().broadcast(
                                            Text.literal(GamesAI.getConfig().getPrefix()
                                                    + GamesAITranslations.tr("command.games_ai.debug.toggle", status)),
                                            false
                                    );
                                    return 1;
                                })
                        )
                        .then(literal("help")
                                .executes(GamesAIHelp::executeGamesAIHelp)
                        )
                        .then(literal("reload")
                                .requires(source -> source.hasPermissionLevel(4))
                                .executes(ctx -> {
                                    GamesAITranslations.reloadTranslations();
                                    String lang = GamesAI.getConfig().getLang();
                                    GamesAI.LOGGER.info("Reload languages: {}", lang);
                                    ctx.getSource().getServer().getPlayerManager().broadcast(
                                            Text.literal(GamesAI.getConfig().getPrefix()
                                                    + GamesAITranslations.tr("command.games_ai.reload", lang)),
                                            false
                                    );
                                    return 1;
                                })
                        )
                        .then(literal("config")
                                .requires(source -> source.hasPermissionLevel(4))
                                .then(literal("lang")
                                        .then(literal("en_us")
                                                .executes(ctx -> {
                                                    GamesAI.getConfig().setLang("en_us");
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .executes(GamesAIHelp::executeGamesAIHelp)
        );
    }

    private static int executeAsk(CommandContext<ServerCommandSource> ctx) {
        String content = StringArgumentType.getString(ctx, "content");
        ServerCommandSource source = ctx.getSource();
        String playerName = source.getName();
        String model = GamesAI.getConfig().getDefaultAi();

        source.sendFeedback(() -> Text.literal(GamesAITranslations.tr("command.games_ai.ask.thinking", GamesAI.getConfig().getAllAi().get(model).getAiName())), false);

        CompletableFuture.supplyAsync(() -> GamesAIRequestAI.askAi(playerName, model, content))
            .exceptionally(ex -> {
                GamesAI.LOGGER.error("Async AI request failed", ex);
                return GamesAITranslations.tr("command.games_ai.ask.exception", ex.getMessage());
            })
            .thenAccept(result -> {
                try {
                    source.getServer().execute(() -> {
                        try {
                            source.sendFeedback(() -> Text.literal(result), false);
                        } catch (Exception e) {
                            GamesAI.LOGGER.error("Failed to send feedback", e);
                        }
                    });
                } catch (Exception e) {
                    GamesAI.LOGGER.error("Failed to schedule feedback on server thread", e);
                }
            });

        return 1;
    }

    private static int executeModelAsk(CommandContext<ServerCommandSource> ctx) {
        String model = StringArgumentType.getString(ctx, "model");
        String content = StringArgumentType.getString(ctx, "content");
        ServerCommandSource source = ctx.getSource();
        String playerName = source.getName();

        source.sendFeedback(() -> Text.literal(GamesAITranslations.tr("command.games_ai.ask.thinking_model", GamesAI.getConfig().getAllAi().get(model).getAiName(), model)), false);

        CompletableFuture.supplyAsync(() -> GamesAIRequestAI.askAi(playerName, model, content))
            .exceptionally(ex -> {
                GamesAI.LOGGER.error("Async AI request failed", ex);
                return GamesAITranslations.tr("command.games_ai.ask.exception", ex.getMessage());
            })
            .thenAccept(result -> {
                try {
                    source.getServer().execute(() -> {
                        try {
                            source.sendFeedback(() -> Text.literal(result), false);
                        } catch (Exception e) {
                            GamesAI.LOGGER.error("Failed to send feedback", e);
                        }
                    });
                } catch (Exception e) {
                    GamesAI.LOGGER.error("Failed to schedule feedback on server thread", e);
                }
            });

        return 1;
    }
}
