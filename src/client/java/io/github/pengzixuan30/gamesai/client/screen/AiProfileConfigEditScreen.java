package io.github.pengzixuan30.gamesai.client.screen;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.config.GamesAIConfigManager;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class AiProfileConfigEditScreen extends Screen {

    private final Screen parent;
    private final GamesAIConfig config;

    // Snapshots for undo (updated on save)
    private String snapshotDefaultAi;
    private Map<String, GamesAIConfig.AiProfile> snapshotAllAiKeys;

    private CycleButton<String> defaultAiCycle;
    private List<String> aiIds = new ArrayList<>();

    private Button backBtn, undoBtn, saveBtn;

    private boolean saved = false;

    private static final int BTN_W = 260;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 6;
    private static final int CONTENT_W = 300;

    public AiProfileConfigEditScreen(Screen parent) {
        super(Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_major.title")));
        this.parent = parent;
        this.config = GamesAI.getConfig();

        this.snapshotDefaultAi = config.getDefaultAi();
        this.snapshotAllAiKeys = new java.util.HashMap<>(config.getAllAi()); // shallow copy of map
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int btnX = cx - BTN_W / 2;
        int y = 25;

        // === Title (dynamically sized for true center) ===
        String title = GamesAITranslations.tr("client.config_screen.aiprofile_major.title");
        int titleW = font.width(title);
        addRenderableWidget(new StringWidget(cx - titleW / 2, y, titleW, ROW_H,
                Component.literal(title), font));
        y += 28;

        // === AI profile buttons ===
        aiIds = new ArrayList<>(config.getAllAi().keySet());

        if (aiIds.isEmpty()) {
            addRenderableWidget(new StringWidget(cx - 100, y, 200, ROW_H,
                    Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_major.no_profiles")), font));
            y += ROW_H + ROW_GAP;
        } else {
            for (String aiId : aiIds) {
                GamesAIConfig.AiProfile profile = config.getAllAi().get(aiId);
                String label = aiId + "  " + (profile != null ? profile.getAiName() : "[?]");
                final String fAiId = aiId;
                addRenderableWidget(Button.builder(
                        Component.literal(label),
                        btn -> Minecraft.getInstance().setScreenAndShow(new AiProfileDetailConfigEditScreen(this, fAiId)))
                        .pos(btnX, y).size(BTN_W, ROW_H).build());
                y += ROW_H + ROW_GAP;
            }
        }

        y += 4;

        // === default_ai selector ===
        List<String> cycleValues = aiIds.isEmpty() ? List.of("(none)") : aiIds;
        String curDefault = config.getDefaultAi();
        if (!cycleValues.contains(curDefault)) curDefault = cycleValues.get(0);
        final String initDefault = curDefault;

        defaultAiCycle = CycleButton.<String>builder(
                        val -> Component.literal(val), () -> initDefault)
                .withValues(cycleValues)
                .create(btnX, y, BTN_W, ROW_H,
                        Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_major.default_ai")),
                        (btn, val) -> config.setDefaultAi(val));
        addRenderableWidget(defaultAiCycle);
        y += ROW_H + ROW_GAP + 6;

        // === Add AI button ===
        addRenderableWidget(Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_major.add")),
                btn -> Minecraft.getInstance().setScreenAndShow(new AiProfileDetailConfigEditScreen(this, null)))
                .pos(btnX, y).size(BTN_W, ROW_H).build());
        y += ROW_H + ROW_GAP;

        // === Bottom buttons ===
        addBottomButtons(y);
    }

    // ─── Bottom buttons ─────────────────────────────────────────

    private void addBottomButtons(int contentBottom) {
        int btnY = this.height - 30;
        int cx = this.width / 2;

        backBtn = Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_major.back")),
                btn -> onClose())
                .pos(cx - 125, btnY).size(75, ROW_H).build();

        undoBtn = Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_major.undo")),
                btn -> doUndo())
                .pos(cx - 40, btnY).size(75, ROW_H).build();

        saveBtn = Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.aiprofile_major.save")),
                btn -> doSave())
                .pos(cx + 45, btnY).size(75, ROW_H).build();

        addRenderableWidget(backBtn);
        addRenderableWidget(undoBtn);
        addRenderableWidget(saveBtn);

        updateButtonBar();
    }

    private void updateButtonBar() {
        if (undoBtn == null) return;
        boolean dataChanged = isModified();
        if (dataChanged && saved) saved = false;
        boolean showButtons = dataChanged && !saved;
        undoBtn.visible = showButtons;
        saveBtn.visible = showButtons;
        int cx = this.width / 2;
        backBtn.setX(showButtons ? cx - 125 : cx - 50);
        backBtn.setWidth(showButtons ? 75 : 100);
    }

    // ─── Logic ──────────────────────────────────────────────────

    private boolean isModified() {
        if (!snapshotDefaultAi.equals(config.getDefaultAi())) return true;
        // Check if keys changed or references changed
        Map<String, GamesAIConfig.AiProfile> current = config.getAllAi();
        if (current.size() != snapshotAllAiKeys.size()) return true;
        for (String key : snapshotAllAiKeys.keySet()) {
            if (!current.containsKey(key)) return true;
            // Compare reference — if any profile object was replaced
            if (snapshotAllAiKeys.get(key) != current.get(key)) return true;
        }
        for (String key : current.keySet()) {
            if (!snapshotAllAiKeys.containsKey(key)) return true;
        }
        return false;
    }

    private void doUndo() {
        config.setDefaultAi(snapshotDefaultAi);
        config.getAllAi().clear();
        config.getAllAi().putAll(snapshotAllAiKeys);
        refreshWidgets();
    }

    private void doSave() {
        if (defaultAiCycle != null) config.setDefaultAi(defaultAiCycle.getValue());
        GamesAIConfigManager.saveConfig(config);
        GamesAI.reload();

        snapshotDefaultAi = config.getDefaultAi();
        snapshotAllAiKeys = new java.util.HashMap<>(config.getAllAi());

        saved = true;
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();
        init();
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
