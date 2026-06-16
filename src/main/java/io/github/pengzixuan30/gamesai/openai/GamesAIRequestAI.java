package io.github.pengzixuan30.gamesai.openai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;

import net.minecraft.text.Text;

public class GamesAIRequestAI {
    public static String askAi(String playerName, String model, String content) {
        if (content == null || content.isBlank()) {
            return Text.translatable("command.games_ai.ask.empty").getString();
        }

        Map<String, GamesAIConfig.AiProfile> map = GamesAI.getConfig().getAllAi();
        GamesAIConfig.AiProfile config = map.get(model);

        if (config == null) {
            return Text.translatable("command.games_ai.ask.missing_config").getString();
        }

        String userContent;
        if (Objects.equals(playerName, "Server") || Objects.equals(playerName, "@")) {
            userContent = Text.translatable("ask.games_ai.ask.source.console", content).getString();
        } else {
            userContent = Text.translatable("ask.games_ai.ask.source.player", playerName, content).getString();
        }

        ChatCompletionMessageParam userMsg = ChatCompletionMessageParam.ofUser(
            ChatCompletionUserMessageParam.builder().content(userContent).build()
        );

        List<ChatCompletionMessageParam> history = GamesAI.getHistory(playerName, model);

        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofSystem(
            ChatCompletionSystemMessageParam.builder().content(config.getPrompt()).build()
        ));
        messages.addAll(history);
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
                    .messages(messages);

            ChatCompletionCreateParams params = builder.build();

            try {
                ChatCompletion completion = client
                        .chat()
                        .completions()
                        .create(params);

                String reply = completion.choices().stream()
                        .flatMap(choice -> choice.message().content().stream())
                        .collect(Collectors.joining());

                if (reply.isBlank()) {
                    reply = Text.translatable("command.games_ai.ask.empty_reply").getString();
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
                return Text.translatable("command.games_ai.ask.failed", e.getMessage()).getString();
            }

        }
    }
}
