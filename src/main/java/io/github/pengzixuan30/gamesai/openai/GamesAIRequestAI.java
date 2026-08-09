package io.github.pengzixuan30.gamesai.openai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.tools.GamesAIToolsRegister;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;

public class GamesAIRequestAI {
    public static String askAi(String playerName, String model, String content, boolean noHistory, Consumer<String> feedback) {
        if (content == null || content.isBlank()) {
            return GamesAITranslations.tr("command.games_ai.ask.empty");
        }

        Map<String, GamesAIConfig.AiProfile> map = GamesAI.getConfig().getAllAi();
        GamesAIConfig.AiProfile config = map.get(model);

        if (config == null) {
            return GamesAITranslations.tr("command.games_ai.ask.missing_config");
        }

        String userContent;
        if (Objects.equals(playerName, "Server") || Objects.equals(playerName, "@")) {
            userContent = "Console\nMessages content: " + content;
        } else {
            userContent = "Player name: " + playerName + "\nMessages content: " + content;
        }

        ChatCompletionMessageParam userMsg = ChatCompletionMessageParam.ofUser(
            ChatCompletionUserMessageParam.builder().content(userContent).build()
        );

        List<ChatCompletionMessageParam> history = GamesAI.getHistory(playerName, model);

        ChatCompletionMessageParam data = ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder().content(GamesAI.getDatabase().dataList().values().toString()).build()
        );

        List<Map<String, String>> skillsIndex = GamesAI.getSkillsIndex();
        StringBuilder skillsText = new StringBuilder();
        if (!skillsIndex.isEmpty()) {
            for (Map<String, String> entry : skillsIndex) {
                skillsText.append("- ").append(entry.get("skills"))
                        .append(": ").append(entry.get("summary")).append("\n");
            }
        }

        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofSystem(
            ChatCompletionSystemMessageParam.builder()
                    .content(GamesAI.resolvePrompt(config.getPrompt()) + "\n\n"
                            + GamesAITranslations.tr("messages.games_ai.skills_index",
                                    skillsText.toString()))
                    .build()
        ));
        messages.add(data);
        if (!noHistory) messages.addAll(history);
        messages.add(userMsg);

        if (GamesAI.isDebugMode()) {
            StringBuilder sb = new StringBuilder("Request messages:\n");
            for (ChatCompletionMessageParam msg : messages) {
                String role = msg.isSystem() ? "SYSTEM" : msg.isUser() ? "USER" : msg.isAssistant() ? "ASSISTANT" : "TOOL";
                sb.append("  [").append(role).append("] ").append(msg).append("\n");
            }
            GamesAI.LOGGER.info(sb.toString());
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();

        while (true) {

            ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                    .model(config.getAiModel())
                    .tools(GamesAIToolsRegister.buildToolList())
                    .messages(messages);

            ChatCompletionCreateParams params = builder.build();

            try {
                ChatCompletion completion = client
                        .chat()
                        .completions()
                        .create(params);

                var message = completion.choices().getFirst().message();

                String reply = completion.choices().stream()
                        .flatMap(choice -> choice.message().content().stream())
                        .collect(Collectors.joining());

                if (reply.isBlank()) {
                    reply = GamesAITranslations.tr("command.games_ai.ask.empty_reply");
                }

                if (message.toolCalls().isPresent() && !message.toolCalls().get().isEmpty()) {
                    List<ChatCompletionMessageToolCall> toolCalls = message.toolCalls().get();

                    messages.add(ChatCompletionMessageParam.ofAssistant(
                            ChatCompletionAssistantMessageParam.builder()
                                    .toolCalls(toolCalls)
                                    .build()
                    ));

                    if (!reply.isBlank()) feedback.accept(config.getAiName() + reply);

                    for (ChatCompletionMessageToolCall toolCall : toolCalls) {
                        ChatCompletionMessageFunctionToolCall funcCall = toolCall.asFunction();
                        String funcName = funcCall.function().name();
                        String rawArgs = funcCall.function().arguments();
                        String callId = funcCall.id();

                        feedback.accept(config.getAiName()
                                + GamesAITranslations.tr("command.games_ai.ask.tool_calling", funcName));

                        try {
                            String toolResult = GamesAIToolsRegister.dispatch(
                                    funcName, feedback, config.getAiName(), rawArgs);

                            feedback.accept(config.getAiName()
                                    + GamesAITranslations.tr("command.games_ai.ask.tool_success", funcName));
                            messages.add(ChatCompletionMessageParam.ofTool(
                                    ChatCompletionToolMessageParam.builder()
                                            .toolCallId(callId)
                                            .content(toolResult)
                                            .build()
                            ));
                        } catch (Exception e) {
                            GamesAI.LOGGER.error("[GamesAI] Tool dispatch failed: {}", funcName, e);
                            feedback.accept(config.getAiName()
                                    + GamesAITranslations.tr("command.games_ai.ask.tool_error", e.getMessage()));
                            messages.add(ChatCompletionMessageParam.ofTool(
                                    ChatCompletionToolMessageParam.builder()
                                            .toolCallId(callId)
                                            .content("Tool execution failed: " + e)
                                            .build()
                            ));
                        }
                    }
                    continue;
                }

                ChatCompletionMessageParam assistantMsg = ChatCompletionMessageParam.ofAssistant(
                        ChatCompletionAssistantMessageParam.builder().content(reply).build()
                );

                history.add(userMsg);
                history.add(assistantMsg);

                int maxLen = GamesAI.getConfig().getMaxHistory() * 2;
                if (history.size() > maxLen) {
                    List<ChatCompletionMessageParam> trimmed = GamesAI.safeTrimHistory(history, maxLen);
                    GamesAI.setHistory(playerName, model, trimmed);
                }

                return config.getAiName() + reply;

            } catch (Exception e) {
                GamesAI.LOGGER.error("Failed to call OpenAI API", e);
                return GamesAITranslations.tr("command.games_ai.ask.failed", e.getMessage());
            }

        }
    }
}
