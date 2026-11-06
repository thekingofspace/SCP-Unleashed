package net.rai.scp_unleashed.scp303.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp303.Scp303Doorman;
import net.rai.scp_unleashed.scp303.Scp303Registry;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp303JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = Scp303Registry.location("scp303_doorman");
    private static final String ModeKey = "Scp303Mode";
    private static final String Prefix = "jade.scp_unleashed.scp303.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp303Doorman Doorman) {
            Data.putString(ModeKey, Doorman.mode().name());
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp303Doorman Doorman)) {
            return;
        }
        String Mode = Accessor.getServerData().getString(ModeKey);
        String Line = switch (Mode) {
            case "Hold" -> "waiting";
            case "Flee", "Leave" -> "leaving";
            case "Bound" -> "still";
            default -> Doorman.folded() ? "waiting" : "watching";
        };
        Tooltip.add(Component.translatable(Prefix + Line).withStyle(Line.equals("still") ? ChatFormatting.GRAY : ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
