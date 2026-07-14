package io.github.pengzixuan30.gamesai.command;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfigManager;
import io.github.pengzixuan30.gamesai.help.GamesAIHelp;
import io.github.pengzixuan30.gamesai.openai.GamesAIRequestAI;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;
//import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
//import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
//import static net.minecraft.server.command.CommandManager.argument;

//import net.minecraft.command.DefaultPermissions;
import net.minecraft.server.permissions.Permissions;

import net.minecraft.commands.CommandSourceStack;
// import net.minecraft.server.command.ServerCommandSource;

//import net.minecraft.text.Text;
import net.minecraft.network.chat.Component;

public class GamesAICommands {

    // dispatcher: CommandDispatcher<ServerCommandSource>
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("ask")
                //.requires(source -> source.isExecutedByPlayer() || source.getEntity() == null)
                .then(literal("-m")
                    .then(argument("model", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String id : GamesAI.getConfig().getAllAi().keySet()) {
                                builder.suggest(id);
                            }
                            return builder.buildFuture();
                        })
                        .then(argument("content", StringArgumentType.greedyString())
                            .executes(GamesAICommands::executeModelAsk)
                        )
                        .executes(GamesAIHelp::executeAskHelp)
                    )
                    .executes(GamesAIHelp::executeAskHelp)
                )
                .then(literal("--model")
                    .then(argument("model", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String id : GamesAI.getConfig().getAllAi().keySet()) {
                                builder.suggest(id);
                            }
                            return builder.buildFuture();
                        })
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
                                            GamesAI.clearHistory(ctx.getSource().getTextName());
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal(GamesAI.getConfig().getPrefix()
                                                        + GamesAITranslations.tr("command.games_ai.history.clear")),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                                .then(literal("clearall")
                                        // source -> source.getPermissions().hasPermission(DefaultPermissions.OWNERS)
                                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                        .executes(ctx -> {
                                            GamesAI.clearAllHistory();
                                            GamesAI.LOGGER.info("Clear all history");
                                            //ctx.getSource().getServer().getPlayerManager().broadcast
                                            ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                                    Component.literal(GamesAI.getConfig().getPrefix()
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
                                    ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                            Component.literal(GamesAI.getConfig().getPrefix()
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
                                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                .executes(ctx -> {
                                    GamesAITranslations.reloadTranslations();
                                    String lang = GamesAI.getConfig().getLang();
                                    GamesAI.LOGGER.info("Reload languages: {}", lang);
                                    ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                    + GamesAITranslations.tr("command.games_ai.reload", lang)),
                                            false
                                    );
                                    return 1;
                                })
                        )
                        .then(literal("config")
                                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                .then(literal("lang")
                                        .then(argument("lang", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("en_us");
                                                    builder.suggest("zh_cn");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String lang = StringArgumentType.getString(ctx, "lang");
                                                    String path = "/assets/games_ai/lang/" + lang + ".json";
                                                    boolean exists;
                                                    try (var in = GamesAITranslations.class.getResourceAsStream(path)) {
                                                        exists = in != null;
                                                    } catch (Exception ignored) {
                                                        exists = false;
                                                    }
                                                    if (!exists) {
                                                        ctx.getSource().sendSuccess(() ->
                                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                                    + GamesAITranslations.tr("command.games_ai.lang.notfound", lang)),
                                                            false
                                                        );
                                                        return 0;
                                                    }
                                                    GamesAI.getConfig().setLang(lang);
                                                    GamesAIConfigManager.saveConfig(GamesAI.getConfig());
                                                    GamesAITranslations.reloadTranslations();
                                                    GamesAI.LOGGER.info("Language config has been set: {}", lang);
                                                    ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                                    + GamesAITranslations.tr("command.games_ai.lang.set", lang)),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                        .executes(GamesAIHelp::executeGamesAIHelp)
                                )
                                .then(literal("defaultAi")
                                        .then(argument("aiID", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    for (String id : GamesAI.getConfig().getAllAi().keySet()) {
                                                        builder.suggest(id);
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String aiID = StringArgumentType.getString(ctx, "aiID");
                                                    if (!GamesAI.getConfig().getAllAi().containsKey(aiID)) {
                                                        ctx.getSource().sendSuccess(() ->
                                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                                    + GamesAITranslations.tr("command.games_ai.default_ai.notfound", aiID)),
                                                            false
                                                        );
                                                        return 0;
                                                    }
                                                    GamesAI.getConfig().setDefaultAi(aiID);
                                                    GamesAIConfigManager.saveConfig(GamesAI.getConfig());
                                                    GamesAI.LOGGER.info("Default AI changed to: {}", aiID);
                                                    ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                                    + GamesAITranslations.tr("command.games_ai.default_ai.set", aiID)),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                        .executes(GamesAIHelp::executeGamesAIHelp)
                                )
                                .then(literal("maxHistory")
                                        .then(argument("value", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String valueStr = StringArgumentType.getString(ctx, "value");
                                                    int value;
                                                    try {
                                                        value = Integer.parseInt(valueStr);
                                                    } catch (NumberFormatException e) {
                                                        ctx.getSource().sendSuccess(() ->
                                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                                    + GamesAITranslations.tr("command.games_ai.max_history.invalid", valueStr)),
                                                            false
                                                        );
                                                        return 0;
                                                    }
                                                    if (value < 1) {
                                                        ctx.getSource().sendSuccess(() ->
                                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                                    + GamesAITranslations.tr("command.games_ai.max_history.invalid", valueStr)),
                                                            false
                                                        );
                                                        return 0;
                                                    }
                                                    GamesAI.getConfig().setMaxHistory(value);
                                                    GamesAIConfigManager.saveConfig(GamesAI.getConfig());
                                                    GamesAI.LOGGER.info("Max history changed to: {}", value);
                                                    ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                                    + GamesAITranslations.tr("command.games_ai.max_history.set", value)),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                        .executes(GamesAIHelp::executeGamesAIHelp)
                                )
                                .executes(GamesAIHelp::executeGamesAIHelp)
                        )
                        .executes(GamesAIHelp::executeGamesAIHelp)
        );
    }

    private static int executeAsk(CommandContext<CommandSourceStack> ctx) {
        String content = StringArgumentType.getString(ctx, "content");
        CommandSourceStack source = ctx.getSource();
        String playerName = source.getTextName();
        String model = GamesAI.getConfig().getDefaultAi();

        source.sendSuccess(() -> Component.literal(GamesAITranslations.tr("command.games_ai.ask.thinking", GamesAI.getConfig().getAllAi().get(model).getAiName())), false);

        CompletableFuture.supplyAsync(() -> GamesAIRequestAI.askAi(playerName, model, content))
            .exceptionally(ex -> {
                GamesAI.LOGGER.error("Async AI request failed", ex);
                return GamesAITranslations.tr("command.games_ai.ask.exception", ex.getMessage());
            })
            .thenAccept(result -> {
                try {
                    source.getServer().execute(() -> {
                        try {
                            source.sendSuccess(() -> Component.literal(result), false);
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

    //CommandContext<ServerCommandSource>
    private static int executeModelAsk(CommandContext<CommandSourceStack> ctx) {
        String model = StringArgumentType.getString(ctx, "model");
        String content = StringArgumentType.getString(ctx, "content");
        //ServerCommandSource
        CommandSourceStack source = ctx.getSource();
        //getName
        String playerName = source.getTextName();

        //Text.literal
        //sendFeedback
        source.sendSuccess(() -> Component.literal(GamesAITranslations.tr("command.games_ai.ask.thinking_model", GamesAI.getConfig().getAllAi().get(model).getAiName(), model)), false);

        CompletableFuture.supplyAsync(() -> GamesAIRequestAI.askAi(playerName, model, content))
            .exceptionally(ex -> {
                GamesAI.LOGGER.error("Async AI request failed", ex);
                return GamesAITranslations.tr("command.games_ai.ask.exception", ex.getMessage());
            })
            .thenAccept(result -> {
                try {
                    source.getServer().execute(() -> {
                        try {
                            source.sendSuccess(() -> Component.literal(result), false);
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
