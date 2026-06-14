package io.github.pengzixuan30.gamesai.openai;

import java.util.stream.Collectors;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import net.minecraft.text.Text;

public class GamesAIRequestAI {

    /**
     * 带历史记录的 AI 请求
     * @param playerName 玩家名
     * @param model      AI 配置名
     * @param content    用户输入
     * @return 格式化后的 AI 回复
     */
    public static String askAi(String playerName, String model, String content) {
        if (content == null || content.isBlank()) {
            return Text.translatable("command.games_ai.ask.empty").getString();
        }

        Map<String, GamesAIConfig.AiProfile> map = GamesAI.getConfig().getAllAi();
        GamesAIConfig.AiProfile config = map.get(model);

        if (config == null) {
            return Text.translatable("command.games_ai.ask.missing_config").getString();
        }

        // 构建 user 消息
        ChatCompletionMessageParam userMsg = ChatCompletionMessageParam.ofUser(
            ChatCompletionUserMessageParam.builder().content(content).build()
        );

        // 获取历史，确保第一条是 system 消息
        List<ChatCompletionMessageParam> history = GamesAI.getHistory(playerName, model);
        if (history.isEmpty()) {
            history.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(config.getPrompt()).build()
            ));
        }

        // 构建完整消息列表：历史 + 新 user 消息
        List<ChatCompletionMessageParam> messages = new ArrayList<>(history);
        messages.add(userMsg);

        OpenAIClient client = OpenAIOkHttpClient.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
            .model(config.getAiModel());

        for (ChatCompletionMessageParam msg : messages) {
            builder.addMessage(msg);
        }

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

            // 构建 assistant 消息
            ChatCompletionMessageParam assistantMsg = ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder().content(reply).build()
            );

            // 追加 user + assistant 到历史
            history.add(userMsg);
            history.add(assistantMsg);

            // 裁剪历史
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
