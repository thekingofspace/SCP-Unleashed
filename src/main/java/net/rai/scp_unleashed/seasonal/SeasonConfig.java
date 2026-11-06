package net.rai.scp_unleashed.seasonal;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

import java.util.EnumMap;
import java.util.Map;

public final class SeasonConfig {
    public static final String FileName = ScpConfigs.ServerFile;
    public static final String Prefix = "config.scp_unleashed.seasons.";
    private static final Map<Season, ModConfigSpec.EnumValue<SeasonMode>> Modes = new EnumMap<>(Season.class);

    public static void define(ModConfigSpec.Builder Builder) {
        for (Season Each : Season.values()) {
            Modes.put(Each, Builder.comment("Auto follows the server's calendar (" + Each.name() + ": " + Each.window()
                            + "). On keeps it running all year and Off turns it off. Players always see what the server decides.")
                    .translation(Prefix + Each.getKey()).defineEnum(Each.getKey(), SeasonMode.Auto));
        }
    }

    private SeasonConfig() {
    }

    public static SeasonMode mode(Season Which) {
        ModConfigSpec.EnumValue<SeasonMode> Value = Modes.get(Which);
        if (Value == null || !ScpConfigs.Server.isLoaded()) {
            return SeasonMode.Auto;
        }
        return Value.get();
    }
}
