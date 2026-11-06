package net.rai.scp_unleashed.labware.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabTubeHolderBlockEntity;
import net.rai.scp_unleashed.labware.LabVials;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum LabTubeHolderJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "lab_tube_holder");
    private static final String Prefix = "jade.scp_unleashed.lab_tube_holder.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockEntity() instanceof LabTubeHolderBlockEntity Holder)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "filled", Holder.getFilled(), LabTubeHolderBlockEntity.SlotCount));
        for (int Slot = 0; Slot < LabTubeHolderBlockEntity.SlotCount; Slot++) {
            ItemStack Tube = Holder.getTube(Slot);
            if (Tube.isEmpty()) {
                Tooltip.add(Component.translatable(Prefix + "empty", Slot + 1).withStyle(ChatFormatting.DARK_GRAY));
                continue;
            }
            ResourceLocation Sample = LabVials.sampleOf(Tube);
            Component Contents = Sample == null ? Component.translatable(Prefix + "nothing").withStyle(ChatFormatting.DARK_GRAY) : LabOrganisms.styledNameOf(Sample);
            boolean Open = LabVials.isOpen(Tube);
            Tooltip.add(Component.translatable(Prefix + (Open ? "open" : "sealed"), Slot + 1, Contents).withStyle(Open ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        }
    }
}
