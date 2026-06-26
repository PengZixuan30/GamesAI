package io.github.pengzixuan30.gamesai.help;

import com.mojang.brigadier.context.CommandContext;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.command.DefaultPermissions;

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
                + GamesAITranslations.tr("help.games_ai.basic", version)),
                    false);

        if (!raw.contains(" -m") && !raw.contains(" --model")) {
            // /ask <content>
            source.sendFeedback(() -> Text.literal(config.getPrefix()
                            + GamesAITranslations.tr("help.games_ai.command.basic"))
                            .append(Text.literal("/ask <content>")
                                    .formatted(Formatting.GRAY)
                                    .styled(style -> style
                                            .withClickEvent(new ClickEvent.SuggestCommand(
                                                    "/ask "
                                            ))
                                    ))
                            .append(Text.literal(GamesAITranslations.tr("help.games_ai.command.ask"))),
                    false);
        }

        // /ask -m <model> <content>
        source.sendFeedback(() -> Text.literal(config.getPrefix()
                        + GamesAITranslations.tr("help.games_ai.command.basic"))
                        .append(Text.literal("/ask -m <model> <content>")
                        .formatted(Formatting.GRAY)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.SuggestCommand(
                                        "/ask -m "
                                ))
                        ))
                .append(Text.literal(GamesAITranslations.tr("help.games_ai.command.ask"))),
            false);

        // 可用模型列表
        source.sendFeedback(() -> Text.literal(config.getPrefix()
                + GamesAITranslations.tr("help.games_ai.ai.model",
                    String.join(", ", config.getAllAi().keySet()))),
            false);

        return 1;
    }

    public static int executeGamesAIHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        GamesAIConfig config = GamesAI.getConfig();
        String version = FabricLoader.getInstance()
                .getModContainer("games_ai")
                .orElseThrow()
                .getMetadata()
                .getVersion()
                .getFriendlyString();

        // 解析输入确定当前子命令上下文
        // 输入格式: "/gamesai [sub] [subsub] ..."
        String raw = ctx.getInput().trim();
        // 去掉开头的 "/"
        if (raw.startsWith("/")) raw = raw.substring(1);
        String[] parts = raw.split("\\s+");
        // parts[0] = "gamesai", parts[1] = 一级子命令, parts[2] = 二级子命令
        String subCommand = parts.length > 1 ? parts[1] : "";

        // 欢迎信息
        source.sendFeedback(() -> Text.literal(config.getPrefix()
                    + GamesAITranslations.tr("help.games_ai.basic", version)),
            false);

        if ("config".equals(subCommand)) {
            // ── /gamesai config ── 只显示 config 子命令
            if (source.getPermissions().hasPermission(DefaultPermissions.OWNERS)) {
                sendHelpLine(source, "/gamesai config lang <lang>", "help.games_ai.config.lang");
                sendHelpLine(source, "/gamesai config defaultAi <aiID>", "help.games_ai.config.default_ai");
                sendHelpLine(source, "/gamesai config maxHistory <value>", "help.games_ai.config.max_history");
            }
        } else if ("history".equals(subCommand)) {
            // ── /gamesai history ── 只显示 history 子命令
            sendHelpLine(source, "/gamesai history clear", "help.games_ai.history.clear");
            if (source.getPermissions().hasPermission(DefaultPermissions.OWNERS)) {
                sendHelpLine(source, "/gamesai history clearall", "help.games_ai.history.clearall");
            }
        } else {
            // ── /gamesai（顶层）── 显示所有一级子命令
            // 所有用户
            sendHelpLine(source, "/gamesai history", "help.games_ai.history");
            sendHelpLine(source, "/gamesai debug", "help.games_ai.debug.toggle");
            sendHelpLine(source, "/gamesai help", "help.games_ai.help");

            // Lv4 管理员
            if (source.getPermissions().hasPermission(DefaultPermissions.OWNERS)) {
                sendHelpLine(source, "/gamesai reload", "help.games_ai.reload");
                sendHelpLine(source, "/gamesai config", "help.games_ai.config");
            }
        }

        return 1;
    }

    private static void sendHelpLine(ServerCommandSource source, String command, String descriptionKey) {
        GamesAIConfig config = GamesAI.getConfig();
        // 点击建议去掉占位符部分（如 "<lang>"），只保留命令前缀
        int bracketIdx = command.indexOf(" <");
        String suggestText = bracketIdx > 0 ? command.substring(0, bracketIdx) : command;
        source.sendFeedback(() -> Text.literal(config.getPrefix()
                        + GamesAITranslations.tr("help.games_ai.command.basic"))
                        .append(Text.literal(command)
                                .formatted(Formatting.GRAY)
                                .styled(style -> style
                                        .withClickEvent(new ClickEvent.SuggestCommand(
                                                suggestText + " "
                                        )))
                        )
                        .append(Text.literal(" — " + GamesAITranslations.tr(descriptionKey))),
                false);
    }
}
