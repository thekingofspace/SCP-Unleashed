package net.rai.scp_unleashed.terminal;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

import java.util.Locale;

public enum TerminalColour implements TranslatableEnum {
    Green,
    Amber;

    public String getKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Component getTranslatedName() {
        return Component.translatable("config.scp_unleashed.terminal_theme." + getKey());
    }
}
