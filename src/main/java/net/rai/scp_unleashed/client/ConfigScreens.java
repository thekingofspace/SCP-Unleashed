package net.rai.scp_unleashed.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ConfigScreens {
    private ConfigScreens() {
    }

    public static void register(ModContainer Container) {
        Container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
