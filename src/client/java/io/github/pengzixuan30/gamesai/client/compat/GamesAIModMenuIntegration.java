package io.github.pengzixuan30.gamesai.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.pengzixuan30.gamesai.client.screen.GamesAIConfigScreen;

public class GamesAIModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GamesAIConfigScreen::new;
    }
}