package io.github.pengzixuan30.gamesai;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import io.github.pengzixuan30.gamesai.command.GamesAICommands;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.config.GamesAIConfigManager;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;
import io.github.pengzixuan30.gamesai.tools.GamesAIBuiltinTools;
import io.github.pengzixuan30.gamesai.tools.GamesAIToolsRegister;
import io.github.pengzixuan30.gamesai.tools.GamesAIExternalToolsLoader;
import io.github.pengzixuan30.gamesai.database.GamesAIDatabase;

import net.minecraft.server.MinecraftServer;

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

    private static MinecraftServer server;

    private static GamesAIDatabase database;

    public static MinecraftServer getServer() {
        return server;
    }

    public static GamesAIDatabase getDatabase() {
        return database;
    }

    public static String resolvePrompt(String rawPrompt) {
        if (rawPrompt == null || !rawPrompt.startsWith("> ")) {
            return rawPrompt;
        }
        String fileName = rawPrompt.substring(2).trim();
        java.nio.file.Path promptFile = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir()
                .resolve("games_ai")
                .resolve("prompt")
                .resolve(fileName);
        try {
            return java.nio.file.Files.readString(promptFile);
        } catch (java.io.IOException e) {
            LOGGER.error("[GamesAI] Failed to read prompt file: {}", promptFile, e);
            return rawPrompt;
        }
    }

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

        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);

        database = new GamesAIDatabase();

        initDirectories();

        GamesAIToolsRegister.clear();
        GamesAIToolsRegister.scanAndRegister(GamesAIBuiltinTools.class);

        GamesAIExternalToolsLoader.loadAll();
        LOGGER.info("GamesAI tools registered: {}", GamesAIToolsRegister.size());
    }

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

    public static void reload() {
        config = GamesAIConfigManager.loadConfig();

        GamesAIToolsRegister.clear();
        GamesAIToolsRegister.scanAndRegister(GamesAIBuiltinTools.class);

        GamesAIExternalToolsLoader.reset();
        GamesAIExternalToolsLoader.loadAll();

        GamesAITranslations.reloadTranslations();

        initDirectories();
    }

    private static void initDirectories() {
        java.nio.file.Path configDir = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir().resolve("games_ai");
        java.nio.file.Path promptDir = configDir.resolve("prompt");
        java.nio.file.Path skillsDir = configDir.resolve("skills");
        java.nio.file.Path skillsIndexFile = skillsDir.resolve("skills.json");
        java.nio.file.Path toolsDir = configDir.resolve("tools");
        java.nio.file.Path toolsFile = toolsDir.resolve("tools.groovy");

        try {
            java.nio.file.Files.createDirectories(promptDir);
            java.nio.file.Files.createDirectories(skillsDir);
            java.nio.file.Files.createDirectories(toolsDir);

            GamesAIExternalToolsLoader.init(configDir);

            if (!java.nio.file.Files.exists(skillsIndexFile)) {
                java.nio.file.Files.writeString(skillsIndexFile, "[]");
            }
            if (!java.nio.file.Files.exists(toolsFile)) {
                java.nio.file.Files.writeString(toolsFile,
"""
import java.util.function.Consumer;
import io.github.pengzixuan30.gamesai.tools.GamesAIToolsRegister;

@GamesAIToolsRegister.RegisterTool(name = "example_tool", description = "This is an example tool.")
String exampleTool(Consumer<String> feedback, String aiName) {
    feedback.accept("Example tool executed");
    return "Tools executed successfully.";
}
""");
            }
        } catch (java.io.IOException e) {
            LOGGER.error("[GamesAI] Failed to init directories and files.", e);
        }
    }

    public static List<Map<String, String>> getSkillsIndex() {
        java.nio.file.Path indexFile = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir().resolve("games_ai").resolve("skills").resolve("skills.json");

        if (!java.nio.file.Files.exists(indexFile)) {
            return List.of();
        }

        try {
            String json = java.nio.file.Files.readString(indexFile);
            com.google.gson.reflect.TypeToken<List<Map<String, String>>> typeToken =
                    new com.google.gson.reflect.TypeToken<>() {};
            List<Map<String, String>> result = new com.google.gson.Gson().fromJson(json, typeToken.getType());
            return result != null ? result : List.of();
        } catch (Exception e) {
            LOGGER.error("[GamesAI] Failed to read skills index: {}", indexFile, e);
            return List.of();
        }
    }
}
