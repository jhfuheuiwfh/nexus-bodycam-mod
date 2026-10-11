package dev.bodycam;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.bodycam.gui.BodycamConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class BodycamModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> new BodycamConfigScreen(parentScreen);
    }
}