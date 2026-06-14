package io.github.pengzixuan30.gamesai.config;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class GamesAIConfig {

    @SerializedName("prefix")
    private String prefix = "[GamesAI]";

    @SerializedName("max_history")
    private int maxHistory = 10;

    @SerializedName("all_ai")
    private Map<String, AiProfile> allAi = new HashMap<>();

    @SerializedName("default_ai")
    private String defaultAi;

    public static class AiProfile {
        @SerializedName("prompt")
        private String prompt = "You are a helpful assistant in Minecraft.";

        @SerializedName("ai_name")
        private String aiName = "[GamesAI]";

        @SerializedName("base_url")
        private String baseUrl = "<Your Base URL>";

        @SerializedName("ai_model")
        private String aiModel = "<Your AI Model>";

        @SerializedName("api_key")
        private String apiKey = "<Your API Key>";

        public String getPrompt() { return prompt; }
        public String getAiName() { return aiName; }
        public String getBaseUrl() { return baseUrl; }
        public String getAiModel() { return aiModel; }
        public String getApiKey() { return apiKey; }

        public void setPrompt(String prompt) { this.prompt = prompt; }
        public void setAiName(String aiName) { this.aiName = aiName; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public void setAiModel(String aiModel) { this.aiModel = aiModel; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public GamesAIConfig() {
        AiProfile example = new AiProfile();
        this.allAi.put("example_ai", example);
        this.defaultAi = "example_ai";
    }

    public Map<String, AiProfile> getAllAi() {
        return allAi;
    }

    public String getDefaultAi() {
        return defaultAi;
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setAllAi(Map<String, AiProfile> allAi) {
        this.allAi = allAi;
    }

    public void setDefaultAi(String defaultAi) {
        this.defaultAi = defaultAi;
    }

    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
