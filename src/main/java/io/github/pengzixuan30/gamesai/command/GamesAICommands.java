package io.github.pengzixuan30.gamesai.command;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfigManager;
import io.github.pengzixuan30.gamesai.help.GamesAIHelp;
import io.github.pengzixuan30.gamesai.openai.GamesAIRequestAI;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.permissions.Permissions;

public class GamesAICommands {

    // dispatcher: CommandDispatcher<ServerCommandSource>
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("ask")
                .requires(source -> source.getEntity() != null)
                .then(literal("-m")
                    .then(argument("model", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String id : GamesAI.getConfig().getAllAi().keySet()) {
                                builder.suggest(id);
                            }
                            return builder.buildFuture();
                        })
                        .then(argument("content", StringArgumentType.greedyString())
                            .executes(GamesAICommands::executeAsk)
                        )
                        .executes(GamesAIHelp::executeAskHelp)
                    )
                    .executes(GamesAIHelp::executeAskHelp)
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
                                                .executes(GamesAICommands::executeAsk)
                                        )
                                        .executes(GamesAIHelp::executeAskHelp)
                                )
                                .executes(GamesAIHelp::executeAskHelp)
                        )
                        .then(argument("content", StringArgumentType.greedyString())
                                .executes(GamesAICommands::executeAsk)
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
                                    GamesAI.reload();
                                    GamesAI.LOGGER.info("[GamesAI] Config, tools and translations reloaded");
                                    ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                    + GamesAITranslations.tr("command.games_ai.reload")),
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
                        .then(literal("data")
                                .then(literal("write")
                                        .then(argument("key", StringArgumentType.word())
                                                .then(argument("value", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            String key = StringArgumentType.getString(ctx, "key");
                                                            String value = StringArgumentType.getString(ctx, "value");
                                                            boolean ok = GamesAI.getDatabase().writeData(key, value);
                                                            ctx.getSource().sendSuccess(() ->
                                                                Component.literal(GamesAI.getConfig().getPrefix()
                                                                        + GamesAITranslations.tr(ok ? "command.games_ai.data.write" : "command.games_ai.data.write.failed", key, value)),
                                                                    false
                                                            );
                                                            return 1;
                                                        })
                                                )
                                                .executes(GamesAIHelp::executeGamesAIHelp)
                                        )
                                        .executes(GamesAIHelp::executeGamesAIHelp)
                                )
                                .then(literal("add")
                                        .then(argument("key", StringArgumentType.word())
                                                .then(argument("value", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            String key = StringArgumentType.getString(ctx, "key");
                                                            String value = StringArgumentType.getString(ctx, "value");
                                                            boolean ok = GamesAI.getDatabase().appendData(key, value);
                                                            ctx.getSource().sendSuccess(() ->
                                                                    Component.literal(GamesAI.getConfig().getPrefix()
                                                                            + GamesAITranslations.tr(ok ? "command.games_ai.data.add" : "command.games_ai.data.add.failed", key, value)),
                                                                    false
                                                                    );
                                                            return 1;
                                                        })
                                                )
                                                .executes(GamesAIHelp::executeGamesAIHelp)
                                        )
                                        .executes(GamesAIHelp::executeGamesAIHelp)
                                )
                                .then(literal("del")
                                        .then(argument("key", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String key = StringArgumentType.getString(ctx, "key");
                                                    boolean ok = GamesAI.getDatabase().deleteData(key);
                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                                    + GamesAITranslations.tr(ok ? "command.games_ai.data.del" : "command.games_ai.data.del.failed", key)),
                                                            false
                                                            );
                                                    return 1;
                                                })
                                        )
                                        .executes(GamesAIHelp::executeGamesAIHelp)
                                )
                                .then(literal("read")
                                        .then(argument("key", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String key = StringArgumentType.getString(ctx, "key");
                                                    String value = GamesAI.getDatabase().readData(key);
                                                    String displayValue = value != null ? value : GamesAITranslations.tr("command.games_ai.data.read.null");
                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                                    + GamesAITranslations.tr("command.games_ai.data.read", key, displayValue)),
                                                            false
                                                    );
                                                    if (value != null) {
                                                        ctx.getSource().sendSuccess(() ->
                                                                Component.literal(GamesAI.getConfig().getPrefix())
                                                                        .append(Component.literal(GamesAITranslations.tr("command.games_ai.data.read.fill"))
                                                                                .withStyle(ChatFormatting.GRAY)
                                                                                .withStyle(style -> style
                                                                                        .withClickEvent(new ClickEvent.SuggestCommand(
                                                                                                "/gamesai data write " + key + " " + value
                                                                                        ))
                                                                                        .withHoverEvent(new HoverEvent.ShowText(
                                                                                                Component.literal(GamesAITranslations.tr("command.games_ai.data.read.fill.hover"))
                                                                                        ))))
                                                                        .append(Component.literal("  OR  "))
                                                                        .append(Component.literal(GamesAITranslations.tr("command.games_ai.data.read.copy"))
                                                                                .withStyle(ChatFormatting.BLUE)
                                                                                .withStyle(style -> style
                                                                                        .withClickEvent(new ClickEvent.CopyToClipboard(
                                                                                                value
                                                                                        ))
                                                                                        .withHoverEvent(new HoverEvent.ShowText(
                                                                                                Component.literal(GamesAITranslations.tr("command.games_ai.data.read.copy.hover"))
                                                                                        )))),
                                                                false
                                                        );
                                                    }
                                                    return 1;
                                                })
                                        )
                                        .executes(GamesAIHelp::executeGamesAIHelp)
                                )
                                .then(literal("list")
                                        .then(literal("keys")
                                                .executes(ctx -> {
                                                    List<String> keys = GamesAI.getDatabase().getAllKeys();
                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal(GamesAI.getConfig().getPrefix()
                                                                    + GamesAITranslations.tr("command.games_ai.data.keys", keys)),
                                                            false
                                                            );
                                                    return 1;
                                                })
                                        )
                                        .executes(ctx -> {
                                            Map<String, String> dataList = GamesAI.getDatabase().dataList();
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal(GamesAI.getConfig().getPrefix()
                                                            + GamesAITranslations.tr("command.games_ai.data.list", dataList)),
                                            false
                                            );
                                            return 1;
                                        })
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

        String model;
        try {
            model = StringArgumentType.getString(ctx, "model");
        } catch (IllegalArgumentException e) {
            model = GamesAI.getConfig().getDefaultAi();
        }
        final String finalModel = model;

        boolean noHistory = ctx.getNodes().stream()
                .anyMatch(node -> node.getNode().getName().equals("-n"));

        source.sendSuccess(() -> Component.literal(GamesAITranslations.tr("command.games_ai.ask.thinking", GamesAI.getConfig().getAllAi().get(finalModel).getAiName())), false);

        Consumer<String> feedback = msg -> {
            try {
                source.getServer().execute(() ->
                    source.sendSuccess(() -> Component.literal(msg), false)
                );
            } catch (Exception e) {
                GamesAI.LOGGER.error("Failed to send tool feedback", e);
            }
        };

        CompletableFuture.supplyAsync(() -> GamesAIRequestAI.askAi(playerName, finalModel, content, noHistory, feedback))
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
