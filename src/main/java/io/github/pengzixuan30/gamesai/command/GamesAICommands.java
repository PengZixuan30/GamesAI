package io.github.pengzixuan30.gamesai.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import io.github.pengzixuan30.gamesai.openai.GamesAIRequestAI;
import io.github.pengzixuan30.gamesai.GamesAI;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.*;

public class GamesAICommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("ask")
                .then(literal("-m")
                    .then(argument("model", StringArgumentType.word())
                        .then(argument("content", StringArgumentType.greedyString())
                            .executes(GamesAICommands::executeModelAsk)
                        )
                    )
                )
                .then(literal("--model")
                    .then(argument("model", StringArgumentType.word())
                        .then(argument("content", StringArgumentType.greedyString())
                            .executes(GamesAICommands::executeModelAsk)
                        )
                    )
                )
                .then(argument("content", StringArgumentType.greedyString())
                    .executes(GamesAICommands::executeAsk)
                )
        );
    }

    private static int executeAsk(CommandContext<ServerCommandSource> ctx) {
        String content = StringArgumentType.getString(ctx, "content");
        ServerCommandSource source = ctx.getSource();
        String playerName = source.getName();

        source.sendFeedback(() -> Text.translatable("command.games_ai.ask.thinking"), false);

        CompletableFuture.supplyAsync(() -> askAi(playerName, content))
            .exceptionally(ex -> {
                GamesAI.LOGGER.error("Async AI request failed", ex);
                return Text.translatable("command.games_ai.ask.exception", ex.getMessage()).getString();
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

        source.sendFeedback(() -> Text.translatable("command.games_ai.ask.thinking_model", model), false);

        CompletableFuture.supplyAsync(() -> askModelAi(playerName, model, content))
            .exceptionally(ex -> {
                GamesAI.LOGGER.error("Async AI request failed", ex);
                return Text.translatable("command.games_ai.ask.exception", ex.getMessage()).getString();
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

    public static String askAi(String playerName, String content) {
        String model = GamesAI.getConfig().getDefaultAi();
        return GamesAIRequestAI.askAi(playerName, model, content);
    }

    public static String askModelAi(String playerName, String model, String content) {
        return GamesAIRequestAI.askAi(playerName, model, content);
    }
}
