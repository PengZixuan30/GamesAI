package io.github.pengzixuan30.gamesai.tools;

import groovy.lang.GroovyClassLoader;
import io.github.pengzixuan30.gamesai.GamesAI;
import java.nio.file.*;
import java.util.*;

public class GamesAIExternalToolsLoader {

    private static final GroovyClassLoader GCL = new GroovyClassLoader();
    private static final Set<String> LOADED_FILES = new HashSet<>();
    private static Path toolsDir;

    private GamesAIExternalToolsLoader() {}

    public static void init(Path configDir) {
        toolsDir = configDir.resolve("tools");
        try {
            Files.createDirectories(toolsDir);
        } catch (Exception e) {
            GamesAI.LOGGER.error("[ExternalTools] Failed to create tools dir", e);
        }
    }

    public static List<String> loadAll() {
        if (toolsDir == null || !Files.exists(toolsDir)) return List.of();

        List<String> allRegistered = new ArrayList<>();

        try (var stream = Files.list(toolsDir)) {
            List<Path> files = stream
                    .filter(f -> f.toString().endsWith(".groovy"))
                    .sorted()
                    .toList();

            for (Path file : files) {
                String fileName = file.getFileName().toString();
                try {
                    Class<?> clazz = GCL.parseClass(file.toFile());
                    List<String> registered = GamesAIToolsRegister.scanAndRegister(clazz);
                    allRegistered.addAll(registered);
                    LOADED_FILES.add(fileName);
                    GamesAI.LOGGER.info("[ExternalTools] Loaded {} -> {}", fileName, registered);
                } catch (Exception e) {
                    GamesAI.LOGGER.error("[ExternalTools] Failed to load {}", fileName, e);
                }
            }
        } catch (Exception e) {
            GamesAI.LOGGER.error("[ExternalTools] Failed to list tools dir", e);
        }

        return allRegistered;
    }

    public static void reset() {
        LOADED_FILES.clear();
    }
}
