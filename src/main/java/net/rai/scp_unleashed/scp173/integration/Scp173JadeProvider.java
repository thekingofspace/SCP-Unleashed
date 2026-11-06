package net.rai.scp_unleashed.scp173.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp173JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp173_statue");
    private static final String Prefix = "jade.scp_unleashed.scp173.";
    private static final String[] Cracks = {"none", "hairline", "deep", "crumbling"};

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp173Statue Statue)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "still").withStyle(ChatFormatting.GRAY));
        String Surface = Statue.isClone() ? "fresh" : "weathered";
        Tooltip.add(Component.translatable(Prefix + "surface", Component.translatable(Prefix + "surface." + Surface).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        int Stage = Math.max(0, Math.min(Cracks.length - 1, Statue.getCrackStage()));
        if (Stage > 0) {
            Tooltip.add(Component.translatable(Prefix + "cracks", Component.translatable(Prefix + "cracks." + Cracks[Stage]).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
        }
    }
}
