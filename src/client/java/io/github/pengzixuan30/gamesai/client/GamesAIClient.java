package io.github.pengzixuan30.gamesai.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.pengzixuan30.gamesai.client.screen.GamesAIConfigScreen;
import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.openai.GamesAIRequestAI;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;

import com.mojang.blaze3d.platform.InputConstants;

public class GamesAIClient implements ClientModInitializer {

    private static KeyMapping configKey;
    private static boolean keyRegistered = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Defer KeyMapping creation until options are available
            if (!keyRegistered) {
                keyRegistered = true;
                configKey = new KeyMapping(
                        "key.games_ai.config",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_F6,
                        client.options.keyChat.getCategory()
                );
            }

            while (configKey != null && configKey.consumeClick()) {
                client.setScreenAndShow(new GamesAIConfigScreen(null));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("c-ask")
                    .then(literal("-m")
                        .then(argument("model", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                for (String id : GamesAI.getConfig().getAllAi().keySet()) {
                                    builder.suggest(id);
                                }
                                return builder.buildFuture();
                            })
                            .then(argument("content", StringArgumentType.greedyString())
                                .executes(GamesAIClient::executeAsk)
                            )
                            .executes(GamesAIClient::executeAskHelp)
                        )
                        .executes(GamesAIClient::executeAskHelp)
                    )
                    .then(literal("-n")
                        .then(literal("-m")
                            .then(argument("model", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String id : GamesAI.getConfig().getAllAi().keySet()) {
                                        builder.suggest(id);
                                    }
                                    return builder.buildFuture();
                                })
                                .then(argument("content", StringArgumentType.greedyString())
                                    .executes(GamesAIClient::executeAsk)
                                )
                                .executes(GamesAIClient::executeAskHelp)
                            )
                            .executes(GamesAIClient::executeAskHelp)
                        )
                        .then(argument("content", StringArgumentType.greedyString())
                            .executes(GamesAIClient::executeAsk)
                        )
                        .executes(GamesAIClient::executeAskHelp)
                    )
                    .then(argument("content", StringArgumentType.greedyString())
                        .executes(GamesAIClient::executeAsk)
                    )
                    .executes(GamesAIClient::executeAskHelp)
            );
        });
    }

    private static int executeAsk(CommandContext<FabricClientCommandSource> ctx) {
        String content = StringArgumentType.getString(ctx, "content");
        FabricClientCommandSource source = ctx.getSource();
        String playerName = source.getPlayer().getGameProfile().name();

        String model;
        try {
            model = StringArgumentType.getString(ctx, "model");
        } catch (IllegalArgumentException e) {
            model = GamesAI.getConfig().getDefaultAi();
        }
        final String finalModel = model;

        boolean noHistory = ctx.getNodes().stream()
                .anyMatch(node -> node.getNode().getName().equals("-n"));

        source.sendFeedback(Component.literal(
                GamesAITranslations.tr("command.games_ai.ask.thinking",
                        GamesAI.getConfig().getAllAi().get(finalModel).getAiName())));

        Consumer<String> feedback = msg ->
                Minecraft.getInstance().execute(() ->
                        source.sendFeedback(Component.literal(msg)));

        CompletableFuture.supplyAsync(() ->
                GamesAIRequestAI.askAi(playerName, finalModel, content, noHistory, feedback))
            .exceptionally(ex -> {
                GamesAI.LOGGER.error("Async AI request failed", ex);
                return GamesAITranslations.tr("command.games_ai.ask.exception", ex.getMessage());
            })
            .thenAccept(result ->
                Minecraft.getInstance().execute(() ->
                        source.sendFeedback(Component.literal(result))));

        return 1;
    }

    private static int executeAskHelp(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        String version = FabricLoader.getInstance()
                .getModContainer("games_ai")
                .orElseThrow()
                .getMetadata()
                .getVersion()
                .getFriendlyString();
        GamesAIConfig config = GamesAI.getConfig();
        String raw = ctx.getInput();

        source.sendFeedback(Component.literal(config.getPrefix()
                + GamesAITranslations.tr("help.games_ai.basic", version)));

        if (!raw.contains(" -m") && !raw.contains(" -n")) {
            sendHelpLine(source, "/ask <content>", "help.games_ai.command.ask");
        }

        sendHelpLine(source, "/ask -m <model> <content>", "help.games_ai.command.ask");
        sendHelpLine(source, "/ask -n <content>", "help.games_ai.command.ask.n");
        sendHelpLine(source, "/ask -n -m <model> <content>", "help.games_ai.command.ask.n");

        source.sendFeedback(Component.literal(config.getPrefix()
                        + GamesAITranslations.tr("help.games_ai.ai.model",
                        String.join(", ", config.getAllAi().keySet()))));

        return 1;
    }

    private static void sendHelpLine(FabricClientCommandSource source, String command, String descriptionKey) {
        GamesAIConfig config = GamesAI.getConfig();
        int bracketIdx = command.indexOf(" <");
        String suggestText = bracketIdx > 0 ? command.substring(0, bracketIdx) : command;
        source.sendFeedback(Component.literal(config.getPrefix()
                                + GamesAITranslations.tr("help.games_ai.command.basic"))
                        .append(Component.literal(command)
                                .withStyle(ChatFormatting.GRAY)
                                .withStyle(style -> style
                                        .withClickEvent(new ClickEvent.SuggestCommand(
                                                suggestText + " "
                                        )))
                        )
                        .append(Component.literal(" — " + GamesAITranslations.tr(descriptionKey))));
    }
}
