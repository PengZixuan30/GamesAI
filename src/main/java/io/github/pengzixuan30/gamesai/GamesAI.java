package io.github.pengzixuan30.gamesai;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import io.github.pengzixuan30.gamesai.command.GamesAICommands;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.config.GamesAIConfigManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.models.chat.completions.ChatCompletionMessageParam;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GamesAI implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("games_ai");

    private static GamesAIConfig config;

    private static final Map<String, Map<String, List<ChatCompletionMessageParam>>> allHistory = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("GamesAI mod initializing...");

        config = GamesAIConfigManager.loadConfig();

        if (config.getAllAi().isEmpty()) {
            LOGGER.warn("OpenAI API Key is empty!");
            LOGGER.warn("Please edit: {}", GamesAIConfigManager.getConfigPath());
            LOGGER.warn("Mod Unload");
            return;
        }

        LOGGER.info("GamesAI initialized — AI Profile: {}",
                config.getAllAi());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            GamesAICommands.register(dispatcher);
        });
    }

    // ==================== 供其他类调用的 getter ====================

    public static GamesAIConfig getConfig() {
        return config;
    }

    public static List<ChatCompletionMessageParam> getHistory(String playerName, String aiName) {
        return allHistory
            .computeIfAbsent(playerName, k -> new ConcurrentHashMap<String, List<ChatCompletionMessageParam>>())
            .computeIfAbsent(aiName, k -> new CopyOnWriteArrayList<ChatCompletionMessageParam>());
    }

    public static void appendMessages(String playerName, String aiName, List<ChatCompletionMessageParam> messages) {
        List<ChatCompletionMessageParam> history = getHistory(playerName, aiName);
        history.addAll(messages);

        int maxLen = config.getMaxHistory() * 2;
        if (history.size() > maxLen) {
            setHistory(playerName, aiName, safeTrimHistory(history, maxLen));
        }
    }

    public static List<ChatCompletionMessageParam> safeTrimHistory(List<ChatCompletionMessageParam> history, int maxLen) {
        if (history.size() <= maxLen) {
            return history;
        }

        List<ChatCompletionMessageParam> trimmed = new ArrayList<>(
            history.subList(history.size() - maxLen, history.size())
        );

        for (int i = 0; i < trimmed.size(); i++) {
            ChatCompletionMessageParam msg = trimmed.get(i);
            if (msg.isUser()) {
                if (i == 0) return trimmed;
                return new ArrayList<>(trimmed.subList(i, trimmed.size()));
            }
        }

        return trimmed;
    }

    public static void setHistory(String playerName, String aiName, List<ChatCompletionMessageParam> newHistory) {
        Map<String, List<ChatCompletionMessageParam>> playerMap = allHistory.get(playerName);
        if (playerMap != null) {
            List<ChatCompletionMessageParam> list = playerMap.get(aiName);
            if (list != null) {
                list.clear();
                list.addAll(newHistory);
            } else {
                playerMap.put(aiName, new CopyOnWriteArrayList<>(newHistory));
            }
        }
    }

    public static void clearHistory(String playerName, String aiName) {
        Map<String, List<ChatCompletionMessageParam>> playerMap = allHistory.get(playerName);
        if (playerMap != null) {
            List<ChatCompletionMessageParam> list = playerMap.remove(aiName);
            if (list != null) list.clear();
        }
    }
}
