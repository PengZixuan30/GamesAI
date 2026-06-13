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

        CompletableFuture.supplyAsync(() -> askAi(content))
            .thenAccept(result -> {
                source.getServer().execute(() -> {
                    source.sendFeedback(() -> Text.literal(result), false);
                });
            });

        return 1;
    }
    private static int executeModelAsk(CommandContext<ServerCommandSource> ctx) {
        String model = StringArgumentType.getString(ctx, "model");
        String content = StringArgumentType.getString(ctx, "content");
        ServerCommandSource source = ctx.getSource();

        CompletableFuture.supplyAsync(() -> askModelAi(model, content))
            .thenAccept(result -> {
                source.getServer().execute(() -> {
                    source.sendFeedback(() -> Text.literal(result), false);
                });
            });

        return 1;
    }

    public static String askAi(String content) {
        String model = GamesAI.getConfig().getDefaultAi();
        return GamesAIRequestAI.askAi(model, content);
    }

    public static String askModelAi(String model, String content) {
        return GamesAIRequestAI.askAi(model, content);
    }
}
