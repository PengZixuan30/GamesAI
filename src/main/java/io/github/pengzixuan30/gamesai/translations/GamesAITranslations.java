package io.github.pengzixuan30.gamesai.translations;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import io.github.pengzixuan30.gamesai.GamesAI;

public class GamesAITranslations {

    private static final Map<String, String> map = new HashMap<>();

    public static void init(String language) {
        String path = "/assets/games_ai/lang/" + language + ".json";
        try (Reader reader = new InputStreamReader(
                GamesAITranslations.class.getResourceAsStream(path))) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            map.clear();
            map.putAll(new Gson().fromJson(reader, type));
        } catch (Exception e) {
            GamesAI.LOGGER.warn("Failed to load language '{}', falling back to en_us", language);
            if (!"en_us".equals(language)) init("en_us");
        }
    }

    public static void reloadTranslations() {
        init(GamesAI.getConfig().getLang());
    }

    public static String tr(String key, Object... args) {
        String template = map.getOrDefault(key, key);
        return String.format(template, args);
    }
}
