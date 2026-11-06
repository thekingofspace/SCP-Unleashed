package net.rai.scp_unleashed.scp4666.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp4666.Scp4666YuleMan;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp4666JadeProvider implements IEntityComponentProvider {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp4666");
    private static final String Prefix = "jade.scp_unleashed.scp4666.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public int getDefaultPriority() {
        return Priority;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp4666YuleMan Man)) {
            return;
        }
        String Key = switch (Man.mode()) {
            case Scp4666YuleMan.ModeStalk -> "circling";
            case Scp4666YuleMan.ModeFlee -> "gone";
            case Scp4666YuleMan.ModeStare, Scp4666YuleMan.ModeSink, Scp4666YuleMan.ModePeek -> "watching";
            case Scp4666YuleMan.ModeLoom, Scp4666YuleMan.ModeDrag, Scp4666YuleMan.ModeBeat, Scp4666YuleMan.ModeLift -> "working";
            default -> "still";
        };
        Tooltip.add(Component.translatable(Prefix + Key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        Tooltip.add(Component.translatable(Prefix + "untouchable").withStyle(ChatFormatting.GRAY));
    }
}
