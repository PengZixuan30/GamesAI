package io.github.pengzixuan30.gamesai;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import io.github.pengzixuan30.gamesai.command.GamesAICommands;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.config.GamesAIConfigManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GamesAI implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("games_ai");

    private static GamesAIConfig config;

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
}
