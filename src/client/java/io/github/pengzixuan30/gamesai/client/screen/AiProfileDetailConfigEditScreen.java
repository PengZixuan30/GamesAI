package io.github.pengzixuan30.gamesai.client.screen;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.config.GamesAIConfigManager;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class AiProfileDetailConfigEditScreen extends Screen {

    private final Screen parent;
    private final GamesAIConfig config;
    private final String aiId; // null = new profile mode

    // The profile being edited (global reference)
    private final GamesAIConfig.AiProfile profile;

    // Snapshots for undo (updated on save)
    private String snapshotAiName;
    private String snapshotBaseUrl;
    private String snapshotAiModel;
    private String snapshotApiKey;
    private String snapshotPrompt;
    private String snapshotExtraBodyJson;

    // Widgets
    private EditBox aiNameField;
    private EditBox baseUrlField;
    private EditBox aiModelField;
    private EditBox apiKeyField;
    private EditBox promptField;
    private EditBox extraBodyField;

    private Button backBtn, undoBtn, saveBtn, deleteBtn;

    private boolean saved = false;
    private boolean deleteConfirm = false;

    private static final int FIELD_W = 240;
    private static final int LABEL_W = 110;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 6;
    private static final int CONTENT_W = LABEL_W + 10 + FIELD_W;
    private static final int PROMPT_H = 80;

    /**
     * @param parent parent screen
     * @param aiId   AI ID to edit, or null for new profile
     */
    public AiProfileDetailConfigEditScreen(Screen parent, String aiId) {
        super(Component.literal(aiId != null
                ? GamesAITranslations.tr("client.config_screen.aiprofile_detail.title", aiId)
                : GamesAITranslations.tr("client.config_screen.aiprofile_detail.new_title")));
        this.parent = parent;
        this.config = GamesAI.getConfig();
        this.aiId = aiId;

        if (aiId != null) {
            GamesAIConfig.AiProfile existing = config.getAllAi().get(aiId);
            this.profile = existing != null ? existing : new GamesAIConfig.AiProfile();
        } else {
            this.profile = new GamesAIConfig.AiProfile();
        }

        this.snapshotAiName = profile.getAiName();
        this.snapshotBaseUrl = profile.getBaseUrl();
        this.snapshotAiModel = profile.getAiModel();
        this.snapshotApiKey = profile.getApiKey();
        this.snapshotPrompt = profile.getPrompt();
        this.snapshotExtraBodyJson = extraBodyToJson(profile.getExtraBody());
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int contentX = cx - CONTENT_W / 2;
        int y = 22;

        // === Title (dynamically sized for true center) ===
        String title = aiId != null
                ? GamesAITranslations.tr("client.config_screen.aiprofile_detail.title", aiId)
                : GamesAITranslations.tr("client.config_screen.aiprofile_detail.new_title");
        int titleW = font.width(title);
        addRenderableWidget(new StringWidget(cx - titleW / 2, y, titleW, ROW_H,
                Component.literal(title), font));
        y += 26;

        // === ai_name (String → EditBox) ===
        y = addEditBoxRow(contentX, y, "client.config_screen.aiprofile_detail.ai_name",
                profile.getAiName(), field -> aiNameField = field);
        y += ROW_GAP;

        // === base_url (String → EditBox) ===
        y = addEditBoxRow(contentX, y, "client.config_screen.aiprofile_detail.base_url",
                profile.getBaseUrl(), field -> baseUrlField = field);
        y += ROW_GAP;

        // === ai_model (String → EditBox) ===
        y = addEditBoxRow(contentX, y, "client.config_screen.aiprofile_detail.ai_model",
                profile.getAiModel(), field -> aiModelField = field);
        y += ROW_GAP;

        // === api_key (String → EditBox) ===
        y = addEditBoxRow(contentX, y, "client.config_screen.aiprofile_detail.api_key",
                profile.getApiKey(), field -> apiKeyField = field);
        y += ROW_GAP;

        // === prompt (String → multiline EditBox) ===
        addLabel(contentX, y, "client.config_screen.aiprofile_detail.prompt");
        promptField = new EditBox(font, contentX + LABEL_W + 10, y, FIELD_W, PROMPT_H, Component.empty());
        promptField.setMaxLength(32767);
        addRenderableWidget(promptField);
        promptField.setMaxLength(32767);  // re-apply after registration
        promptField.setValue(profile.getPrompt());
        y += PROMPT_H + 2;

        // Compact hint — full content width to avoid ellipsis
        addRenderableWidget(new StringWidget(
                contentX, y, CONTENT_W, ROW_H,
                Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_detail.prompt_file_hint")),
                font));
        y += ROW_H + ROW_GAP;

        // === extra_body (JSON String → EditBox) ===
        addLabel(contentX, y, "client.config_screen.aiprofile_detail.extra_body");
        extraBodyField = new EditBox(font, contentX + LABEL_W + 10, y, FIELD_W, PROMPT_H, Component.empty());
        extraBodyField.setMaxLength(32767);
        addRenderableWidget(extraBodyField);
        extraBodyField.setMaxLength(32767);  // re-apply after registration
        extraBodyField.setValue(extraBodyToJson(profile.getExtraBody()));
        y += PROMPT_H + ROW_GAP + 6;

        // === Buttons ===
        addBottomButtons(y);
    }

    // ─── Row builders ───────────────────────────────────────────

    private int addEditBoxRow(int contentX, int y, String labelKey, String initialValue,
                              java.util.function.Consumer<EditBox> ref) {
        addLabel(contentX, y, labelKey);
        EditBox field = new EditBox(font, contentX + LABEL_W + 10, y, FIELD_W, ROW_H, Component.empty());
        field.setMaxLength(256);
        addRenderableWidget(field);
        field.setMaxLength(256);  // re-apply after registration
        field.setValue(initialValue);
        ref.accept(field);
        return y + ROW_H;
    }

    private void addLabel(int x, int y, String translationKey) {
        addRenderableWidget(new StringWidget(x, y, LABEL_W, ROW_H,
                Component.literal(GamesAITranslations.tr(translationKey)), font));
    }

    // ─── Bottom buttons ─────────────────────────────────────────

    private void addBottomButtons(int contentBottom) {
        int btnY = this.height - 30;
        int cx = this.width / 2;

        // Always create all 3 bottom buttons; visibility toggled by updateButtonBar()
        backBtn = Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_detail.back")),
                btn -> onClose())
                .pos(cx - 125, btnY).size(75, ROW_H).build();

        undoBtn = Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_detail.undo")),
                btn -> doUndo())
                .pos(cx - 40, btnY).size(75, ROW_H).build();

        saveBtn = Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_detail.save")),
                btn -> doSave())
                .pos(cx + 45, btnY).size(75, ROW_H).build();

        addRenderableWidget(backBtn);
        addRenderableWidget(undoBtn);
        addRenderableWidget(saveBtn);

        // Delete button (existing profiles only) — placed above the bottom row
        if (aiId != null) {
            String deleteLabel = deleteConfirm
                    ? GamesAITranslations.tr("client.config_screen.aiprofile_detail.delete_confirm")
                    : GamesAITranslations.tr("client.config_screen.aiprofile_detail.delete");
            deleteBtn = Button.builder(
                    Component.literal(deleteLabel),
                    btn -> doDelete())
                    .pos(cx - 50, btnY - ROW_H - ROW_GAP).size(100, ROW_H).build();
            addRenderableWidget(deleteBtn);
        }

        updateButtonBar();
    }

    /** Toggle undo/save/delete visibility without destroying widgets. */
    private void updateButtonBar() {
        boolean dataChanged = isModified();
        if (dataChanged && saved) saved = false;
        boolean showButtons = dataChanged && !saved;
        if (undoBtn != null) undoBtn.visible = showButtons;
        if (saveBtn != null) saveBtn.visible = showButtons;
        if (deleteBtn != null) deleteBtn.visible = !saved;
        int cx = this.width / 2;
        if (backBtn != null) {
            backBtn.setX(showButtons ? cx - 125 : cx - 50);
            backBtn.setWidth(showButtons ? 75 : 100);
        }
    }

    // ─── Logic ──────────────────────────────────────────────────

    private boolean isModified() {
        if (aiNameField != null && !snapshotAiName.equals(aiNameField.getValue())) return true;
        if (baseUrlField != null && !snapshotBaseUrl.equals(baseUrlField.getValue())) return true;
        if (aiModelField != null && !snapshotAiModel.equals(aiModelField.getValue())) return true;
        if (apiKeyField != null && !snapshotApiKey.equals(apiKeyField.getValue())) return true;
        if (promptField != null && !snapshotPrompt.equals(promptField.getValue())) return true;
        if (extraBodyField != null && !snapshotExtraBodyJson.equals(extraBodyField.getValue())) return true;
        return false;
    }

    private void doUndo() {
        profile.setAiName(snapshotAiName);
        profile.setBaseUrl(snapshotBaseUrl);
        profile.setAiModel(snapshotAiModel);
        profile.setApiKey(snapshotApiKey);
        profile.setPrompt(snapshotPrompt);
        profile.getExtraBody().clear();
        profile.getExtraBody().putAll(parseExtraBody(snapshotExtraBodyJson));
        refreshWidgets();
    }

    private void doSave() {
        collectFields();

        // If new profile, register it
        if (aiId == null) {
            String newId = profile.getAiName().toLowerCase().replaceAll("[^a-z0-9_]", "_");
            if (newId.isEmpty()) newId = "new_ai";
            config.getAllAi().put(newId, profile);
        }

        GamesAIConfigManager.saveConfig(config);
        GamesAI.reload();

        snapshotAiName = profile.getAiName();
        snapshotBaseUrl = profile.getBaseUrl();
        snapshotAiModel = profile.getAiModel();
        snapshotApiKey = profile.getApiKey();
        snapshotPrompt = profile.getPrompt();
        snapshotExtraBodyJson = extraBodyToJson(profile.getExtraBody());

        saved = true;
        refreshWidgets();
    }

    private void doDelete() {
        if (!deleteConfirm) {
            deleteConfirm = true;
            refreshWidgets();
            return;
        }
        config.getAllAi().remove(aiId);
        // If the deleted AI was the default, pick another or clear
        if (aiId.equals(config.getDefaultAi())) {
            String newDefault = config.getAllAi().keySet().stream().findFirst().orElse("");
            config.setDefaultAi(newDefault);
        }
        GamesAIConfigManager.saveConfig(config);
        GamesAI.reload();
        onClose(); // Go back to list after deleting
    }

    private void collectFields() {
        if (aiNameField != null) profile.setAiName(aiNameField.getValue());
        if (baseUrlField != null) profile.setBaseUrl(baseUrlField.getValue());
        if (aiModelField != null) profile.setAiModel(aiModelField.getValue());
        if (apiKeyField != null) profile.setApiKey(apiKeyField.getValue());
        if (promptField != null) profile.setPrompt(promptField.getValue());
        if (extraBodyField != null) {
            profile.getExtraBody().clear();
            profile.getExtraBody().putAll(parseExtraBody(extraBodyField.getValue()));
        }
    }

    private void refreshWidgets() {
        clearWidgets();
        init();
    }

    // ─── Extra body JSON helpers ────────────────────────────────

    private static String extraBodyToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        try {
            return new com.google.gson.GsonBuilder().create().toJson(map);
        } catch (Exception e) {
            return map.toString();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseExtraBody(String json) {
        if (json == null || json.trim().isEmpty()) return new HashMap<>();
        try {
            Map<String, Object> result = new com.google.gson.Gson().fromJson(json, Map.class);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            // Invalid JSON — keep current value
            return new HashMap<>();
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public void tick() {
        super.tick();
        updateButtonBar();
    }
}
