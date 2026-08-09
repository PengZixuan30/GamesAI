package io.github.pengzixuan30.gamesai.client.screen;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.config.GamesAIConfig;
import io.github.pengzixuan30.gamesai.config.GamesAIConfigManager;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class GeneralConfigEditScreen extends Screen {

    private final Screen parent;

    // === Editing config (global reference, real-time sync) ===
    private final GamesAIConfig config;

    // === Snapshots for undo (updated on save to establish new baseline) ===
    private String snapshotPrefix;
    private int snapshotMaxHistory;
    private String snapshotLang;
    private String snapshotDefaultAi;

    // === Widgets ===
    private EditBox prefixField;
    private IntSliderWidget maxHistorySlider;
    private CycleButton<String> langCycle;
    private CycleButton<String> defaultAiCycle;

    // === State ===
    // === Bottom button references (for visibility toggle without rebuild) ===
    private Button backBtn, undoBtn, saveBtn;

    private boolean saved = false;

    private static final int FIELD_W = 200;
    private static final int LABEL_W = 120;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 8;
    private static final int CONTENT_W = 400; // total width of label + gap + field

    public GeneralConfigEditScreen(Screen parent) {
        super(Component.literal(GamesAITranslations.tr("client.config_screen.general.title")));
        this.parent = parent;
        this.config = GamesAI.getConfig();

        // Snapshot current values for undo detection
        this.snapshotPrefix = config.getPrefix();
        this.snapshotMaxHistory = config.getMaxHistory();
        this.snapshotLang = config.getLang();
        this.snapshotDefaultAi = config.getDefaultAi();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int contentX = cx - CONTENT_W / 2;
        int y = 25;

        // === Title (dynamically sized for true center) ===
        String title = GamesAITranslations.tr("client.config_screen.general.title");
        int titleW = font.width(title);
        addRenderableWidget(new StringWidget(cx - titleW / 2, y, titleW, ROW_H,
                Component.literal(title), font));
        y += 30;

        // === prefix (String → EditBox) ===
        y = addEditBoxRow(contentX, y,
                "client.config_screen.general.prefix",
                config.getPrefix(),
                field -> prefixField = field);
        y += ROW_GAP;

        // === max_history (int → Slider 1–50) ===
        y = addSliderRow(contentX, y,
                "client.config_screen.general.max_history",
                config.getMaxHistory(), 1, 100,
                slider -> maxHistorySlider = slider);
        y += ROW_GAP;

        // === lang (restricted → CycleButton) ===
        y = addCycleRow(contentX, y,
                "client.config_screen.general.lang",
                List.of("en_us", "zh_cn"),
                config.getLang(),
                cycle -> langCycle = cycle);
        y += ROW_GAP;

        // === default_ai (restricted → CycleButton from allAi keys) ===
        List<String> aiIds = new ArrayList<>(config.getAllAi().keySet());
        if (aiIds.isEmpty()) aiIds.add("(none)");
        String curDefault = config.getDefaultAi();
        if (!aiIds.contains(curDefault)) curDefault = aiIds.get(0);
        y = addCycleRow(contentX, y,
                "client.config_screen.general.default_ai",
                aiIds,
                curDefault,
                cycle -> defaultAiCycle = cycle);
        y += ROW_GAP + 6;

        // === Shortcut to AI Profile page ===
        addRenderableWidget(Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.general.to_aiprofile")),
                btn -> Minecraft.getInstance().setScreenAndShow(new AiProfileConfigEditScreen(this)))
                .pos(cx - 85, y).size(170, ROW_H).build());
        y += ROW_H + ROW_GAP;

        // === Bottom buttons ===
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

    private int addSliderRow(int contentX, int y, String labelKey, int initialValue, int min, int max,
                             java.util.function.Consumer<IntSliderWidget> ref) {
        addLabel(contentX, y, labelKey);
        IntSliderWidget slider = new IntSliderWidget(contentX + LABEL_W + 10, y, FIELD_W, ROW_H,
                initialValue, min, max,
                val -> config.setMaxHistory(val));
        addRenderableWidget(slider);
        ref.accept(slider);
        return y + ROW_H;
    }

    private int addCycleRow(int contentX, int y, String labelKey, List<String> values, String initialValue,
                            java.util.function.Consumer<CycleButton<String>> ref) {
        String initVal = values.contains(initialValue) ? initialValue : values.get(0);
        String labelText = GamesAITranslations.tr(labelKey);
        CycleButton<String> cycle = CycleButton.<String>builder(
                        val -> Component.literal(val), () -> initVal)
                .withValues(values)
                .create(contentX, y, CONTENT_W, ROW_H,
                        Component.literal(labelText),
                        (btn, val) -> { /* handled on save */ });
        addRenderableWidget(cycle);
        ref.accept(cycle);
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

        // Always create all 3 buttons; visibility is toggled by updateButtonBar()
        backBtn = Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.general.back")),
                btn -> onClose())
                .pos(cx - 125, btnY).size(75, ROW_H).build();

        undoBtn = Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.general.undo")),
                btn -> doUndo())
                .pos(cx - 40, btnY).size(75, ROW_H).build();

        saveBtn = Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.general.save")),
                btn -> doSave())
                .pos(cx + 45, btnY).size(75, ROW_H).build();

        addRenderableWidget(backBtn);
        addRenderableWidget(undoBtn);
        addRenderableWidget(saveBtn);

        updateButtonBar();
    }

    /** Toggle undo/save visibility without destroying widgets. */
    private void updateButtonBar() {
        if (undoBtn == null) return;
        boolean dataChanged = isModified();
        if (dataChanged && saved) saved = false; // allow re-editing after save
        boolean showButtons = dataChanged && !saved;
        undoBtn.visible = showButtons;
        saveBtn.visible = showButtons;
        int cx = this.width / 2;
        backBtn.setX(showButtons ? cx - 125 : cx - 50);
        backBtn.setWidth(showButtons ? 75 : 100);
    }

    // ─── Logic ──────────────────────────────────────────────────

    private boolean isModified() {
        if (prefixField != null && !snapshotPrefix.equals(prefixField.getValue())) return true;
        if (maxHistorySlider != null && snapshotMaxHistory != maxHistorySlider.getValue()) return true;
        if (langCycle != null && !snapshotLang.equals(langCycle.getValue())) return true;
        if (defaultAiCycle != null && !snapshotDefaultAi.equals(defaultAiCycle.getValue())) return true;
        return false;
    }


    private void doUndo() {
        config.setPrefix(snapshotPrefix);
        config.setMaxHistory(snapshotMaxHistory);
        config.setLang(snapshotLang);
        config.setDefaultAi(snapshotDefaultAi);
        refreshWidgets();
    }

    private void doSave() {
        // Collect widget values into config
        if (prefixField != null) config.setPrefix(prefixField.getValue());
        if (maxHistorySlider != null) config.setMaxHistory(maxHistorySlider.getValue());
        if (langCycle != null) config.setLang(langCycle.getValue());
        if (defaultAiCycle != null) config.setDefaultAi(defaultAiCycle.getValue());

        GamesAIConfigManager.saveConfig(config);
        GamesAI.reload();

        // Update snapshots to saved values (new undo baseline)
        snapshotPrefix = config.getPrefix();
        snapshotMaxHistory = config.getMaxHistory();
        snapshotLang = config.getLang();
        snapshotDefaultAi = config.getDefaultAi();

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



    // ─── Int Slider Widget ──────────────────────────────────────

    private static class IntSliderWidget extends AbstractSliderButton {
        private final int min, max;
        private final java.util.function.IntConsumer onApply;

        IntSliderWidget(int x, int y, int width, int height, int value, int min, int max,
                        java.util.function.IntConsumer onApply) {
            super(x, y, width, height, Component.empty(),
                    (double) (value - min) / Math.max(1, max - min));
            this.min = min;
            this.max = max;
            this.onApply = onApply;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.valueOf(getValue())));
        }

        @Override
        protected void applyValue() {
            onApply.accept(getValue());
        }

        int getValue() {
            return (int) Math.round(this.value * (max - min) + min);
        }
    }
}
