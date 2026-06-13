package io.github.pengzixuan30.gamesai.openai;

import java.util.stream.Collectors;
import java.util.Map;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;

public class GamesAIRequestAI {
    public static String askAi(String model, String content) {
        if (content == null || content.isBlank()) {
            return "§c请输入你的问题。§r";
        }

        Map<String, GamesAIConfig.AiProfile> map = GamesAI.getConfig().getAllAi();
        GamesAIConfig.AiProfile config = map.get(model);

        if (config == null) {
            return "§c缺失的AI配置!§r";
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
            .model(config.getAiModel())
            .addSystemMessage(config.getPrompt())
            .addUserMessage(content);

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
                return "§7(AI 返回了空内容)§r";
            }

            return reply;

        } catch (Exception e) {
            GamesAI.LOGGER.error("Failed to call OpenAI API", e);
            return "§cAI 请求失败: " + e.getMessage() + "§r";
        }
    }
}
