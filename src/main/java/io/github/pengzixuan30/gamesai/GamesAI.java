package io.github.pengzixuan30.gamesai;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import io.github.pengzixuan30.gamesai.command.GamesAICommands;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.config.GamesAIConfigManager;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;

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

    private static boolean debugMode = false;

    @Override
    public void onInitialize() {
        LOGGER.info("GamesAI mod initializing...");

        config = GamesAIConfigManager.loadConfig();
        GamesAITranslations.init(config.getLang());

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

    public static List<ChatCompletionMessageParam> safeTrimHistory(List<ChatCompletionMessageParam> history, int maxLen) {
        if (history.size() <= maxLen) {
            return history;
        }

        List<ChatCompletionMessageParam> trimmed = new ArrayList<>(
            history.subList(history.size() - maxLen, history.size())
        );

        for (int i = 0; i < trimmed.size(); i++) {
            ChatCompletionMessageParam msg = trimmed.get(i);
            if (msg.isUser() || msg.isAssistant()) {
                if (i == 0) return trimmed;
                return new ArrayList<>(trimmed.subList(i, trimmed.size()));
            }
        }

        return trimmed;
    }

    public static void setHistory(String playerName, String aiName, List<ChatCompletionMessageParam> newHistory) {
        List<ChatCompletionMessageParam> history = getHistory(playerName, aiName);
        history.clear();
        history.addAll(newHistory);
    }

    public static void clearHistory(String playerName) {
        Map<String, List<ChatCompletionMessageParam>> playerMap = allHistory.remove(playerName);
        if (playerMap != null) {
            playerMap.values().forEach(List::clear);
        }
    }

    public static void clearAllHistory() {
        allHistory.clear();
    }

    public static void toggleDebugMode() {
        debugMode = !debugMode;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }
}
