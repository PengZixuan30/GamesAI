package io.github.pengzixuan30.gamesai.client.screen;

import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class GamesAIConfigScreen extends Screen {

    private final Screen parent;

    private static final int BTN_W = 160;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 10;
    private static final int TITLE_H = 20;

    public GamesAIConfigScreen(Screen parent) {
        super(Component.literal(GamesAITranslations.tr("client.config_screen.major.title")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Title — dynamically sized for true center alignment
        String title = GamesAITranslations.tr("client.config_screen.major.title");
        int titleW = font.width(title);
        addRenderableWidget(new StringWidget(
                cx - titleW / 2, cy - TITLE_H - BTN_H / 2 - 5,
                titleW, TITLE_H,
                Component.literal(title),
                font));

        // Two buttons side by side, centered
        int totalW = BTN_W * 2 + BTN_GAP;
        int startX = cx - totalW / 2;
        int btnY = cy - BTN_H / 2 + 5;

        addRenderableWidget(Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.major.general")),
                btn -> Minecraft.getInstance().setScreenAndShow(new GeneralConfigEditScreen(this)))
                .pos(startX, btnY).size(BTN_W, BTN_H).build());

        addRenderableWidget(Button.builder(
                Component.literal(GamesAITranslations.tr("client.config_screen.major.aiprofile")),
                btn -> Minecraft.getInstance().setScreenAndShow(new AiProfileConfigEditScreen(this)))
                .pos(startX + BTN_W + BTN_GAP, btnY).size(BTN_W, BTN_H).build());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}
