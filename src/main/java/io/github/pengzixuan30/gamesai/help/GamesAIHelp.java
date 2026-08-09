package io.github.pengzixuan30.gamesai.help;

import com.mojang.brigadier.context.CommandContext;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;
import net.fabricmc.loader.api.FabricLoader;

//import net.minecraft.command.DefaultPermissions;
import net.minecraft.server.permissions.Permissions;

import net.minecraft.commands.CommandSourceStack;
// import net.minecraft.server.command.ServerCommandSource;

//import net.minecraft.text.Text;
import net.minecraft.network.chat.Component;

//import net.minecraft.text.ClickEvent;
//import net.minecraft.util.Formatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.ChatFormatting;

public class GamesAIHelp {

    public static int executeAskHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String raw = ctx.getInput();
        GamesAIConfig config = GamesAI.getConfig();
        String version = FabricLoader.getInstance()
            .getModContainer("games_ai")
            .orElseThrow()
            .getMetadata()
            .getVersion()
            .getFriendlyString();

        source.sendSuccess(() -> Component.literal(config.getPrefix()
                + GamesAITranslations.tr("help.games_ai.basic", version)),
                    false);

        if (!raw.contains(" -m") && !raw.contains(" -n")) {
            sendHelpLine(source, "/ask <content>", "help.games_ai.command.ask");
        }

        sendHelpLine(source, "/ask -m <model> <content>", "help.games_ai.command.ask");
        sendHelpLine(source, "/ask -n <content>", "help.games_ai.command.ask.n");
        sendHelpLine(source, "/ask -n -m <model> <content>", "help.games_ai.command.ask.n");

        source.sendSuccess(() -> Component.literal(config.getPrefix()
                + GamesAITranslations.tr("help.games_ai.ai.model",
                    String.join(", ", config.getAllAi().keySet()))),
            false);

        return 1;
    }

    public static int executeGamesAIHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        GamesAIConfig config = GamesAI.getConfig();
        String version = FabricLoader.getInstance()
                .getModContainer("games_ai")
                .orElseThrow()
                .getMetadata()
                .getVersion()
                .getFriendlyString();

        String raw = ctx.getInput().trim();
        if (raw.startsWith("/")) raw = raw.substring(1);
        String[] parts = raw.split("\\s+");
        String subCommand = parts.length > 1 ? parts[1] : "";

        source.sendSuccess(() -> Component.literal(config.getPrefix()
                    + GamesAITranslations.tr("help.games_ai.basic", version)),
            false);

        switch(subCommand) {
            case "config" -> {
                if (source.permissions().hasPermission(Permissions.COMMANDS_OWNER)) {
                    sendHelpLine(source, "/gamesai config lang <lang>", "help.games_ai.config.lang");
                    sendHelpLine(source, "/gamesai config defaultAi <aiID>", "help.games_ai.config.default_ai");
                    sendHelpLine(source, "/gamesai config maxHistory <value>", "help.games_ai.config.max_history");
                }
            }
            case "history" -> {
                sendHelpLine(source, "/gamesai history clear", "help.games_ai.history.clear");
                if (source.permissions().hasPermission(Permissions.COMMANDS_OWNER)) {
                    sendHelpLine(source, "/gamesai history clearall", "help.games_ai.history.clearall");
                }
            }
            case "data" -> {
                if (source.permissions().hasPermission(Permissions.COMMANDS_OWNER)) {
                    sendHelpLine(source, "/gamesai data write <key> <value>", "help.games_ai.data.write");
                    sendHelpLine(source, "/gamesai data add <key> <value>", "help.games_ai.data.add");
                    sendHelpLine(source, "/gamesai data del <key>", "help.games_ai.data.del");
                    sendHelpLine(source, "/gamesai data read <key>", "help.games_ai.data.read");
                    sendHelpLine(source, "/gamesai data list", "help.games_ai.data.list");
                    sendHelpLine(source, "/gamesai data list keys", "help.games_ai.data.listkeys");
                }
            }
            default -> {
                sendHelpLine(source, "/gamesai history", "help.games_ai.history");
                sendHelpLine(source, "/gamesai help", "help.games_ai.help");

                if (source.permissions().hasPermission(Permissions.COMMANDS_OWNER)) {
                    sendHelpLine(source, "/gamesai reload", "help.games_ai.reload");
                    sendHelpLine(source, "/gamesai config", "help.games_ai.config");
                    sendHelpLine(source, "/gamesai data", "help.games_ai.data");
                }
            }
        }

        return 1;
    }

    private static void sendHelpLine(CommandSourceStack source, String command, String descriptionKey) {
        GamesAIConfig config = GamesAI.getConfig();
        int bracketIdx = command.indexOf(" <");
        String suggestText = bracketIdx > 0 ? command.substring(0, bracketIdx) : command;
        source.sendSuccess(() -> Component.literal(config.getPrefix()
                        + GamesAITranslations.tr("help.games_ai.command.basic"))
                        .append(Component.literal(command)
                                .withStyle(ChatFormatting.GRAY)
                                .withStyle(style -> style
                                        .withClickEvent(new ClickEvent.SuggestCommand(
                                                suggestText + " "
                                        )))
                        )
                        .append(Component.literal(" — " + GamesAITranslations.tr(descriptionKey))),
                false);
    }
}
