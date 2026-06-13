package io.github.pengzixuan30.gamesai.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import net.fabricmc.loader.api.FabricLoader;

public class GamesAIConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("games_ai");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String CONFIG_DIR_NAME = "games_ai";
    private static final String CONFIG_FILE_NAME = "config.json";

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(CONFIG_DIR_NAME)
                .resolve(CONFIG_FILE_NAME);
    }

    public static GamesAIConfig loadConfig() {
        Path configPath = getConfigPath();
        GamesAIConfig config;

        if (Files.exists(configPath)) {
            config = readFromFile(configPath);
            if (config != null) {
                LOGGER.info("Loaded config from {}", configPath);
                return config;
            }
            LOGGER.warn("Failed to parse config, using defaults");
        }

        config = new GamesAIConfig();
        saveConfig(config);
        LOGGER.info("Created default config at {}", configPath);
        return config;
    }

    public static void saveConfig(GamesAIConfig config) {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(config, writer);
            }
            LOGGER.info("Saved config to {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    private static GamesAIConfig readFromFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, GamesAIConfig.class);
        } catch (IOException e) {
            LOGGER.error("Failed to read config file", e);
        } catch (JsonParseException e) {
            LOGGER.error("Failed to parse config JSON", e);
        }
        return null;
    }
}
