package net.rai.scp_unleashed.mining;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum MinerStatus implements StringRepresentable {
    Off("off", ChatFormatting.DARK_GRAY, 0),
    Running("running", ChatFormatting.GREEN, 4),
    Stalled("stalled", ChatFormatting.GOLD, 3),
    Hot("hot", ChatFormatting.RED, 3);

    private final String SerializedName;
    private final ChatFormatting Colour;
    private final int Light;

    MinerStatus(String SerializedName, ChatFormatting Colour, int Light) {
        this.SerializedName = SerializedName;
        this.Colour = Colour;
        this.Light = Light;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public int getLight() {
        return Light;
    }

    public String getTranslationKey() {
        return "jade.scp_unleashed.mining.status." + SerializedName;
    }

    public static MinerStatus of(MinerRules.Engine Core) {
        if (!Core.isPowered()) {
            return Off;
        }
        if (Core.isOverheated()) {
            return Hot;
        }
        return Core.isFull() ? Stalled : Running;
    }

    public static MinerStatus byId(int Id) {
        MinerStatus[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Id))];
    }
}
