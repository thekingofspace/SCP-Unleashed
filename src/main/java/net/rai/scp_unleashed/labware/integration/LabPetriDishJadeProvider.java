package net.rai.scp_unleashed.labware.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabPetriDishBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum LabPetriDishJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "lab_petri_dish");
    private static final String Prefix = "jade.scp_unleashed.lab_petri_dish.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockEntity() instanceof LabPetriDishBlockEntity Dish)) {
            return;
        }
        ResourceLocation Sample = Dish.getSample();
        if (Sample == null) {
            Tooltip.add(Component.translatable(Prefix + "sterile").withStyle(ChatFormatting.GRAY));
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "culture", LabOrganisms.styledNameOf(Sample)).withStyle(ChatFormatting.GOLD));
        if (LabOrganisms.isKnown(Sample)) {
            Tooltip.add(Component.translatable(Prefix + "source", (int) LabOrganisms.radiusOf(Sample)).withStyle(ChatFormatting.RED));
        }
    }
}
