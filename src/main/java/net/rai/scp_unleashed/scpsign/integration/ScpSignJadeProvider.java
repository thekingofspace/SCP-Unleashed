package net.rai.scp_unleashed.scpsign.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.scpsign.ScpSignBlock;
import net.rai.scp_unleashed.scpsign.ScpSignBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum ScpSignJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_sign");
    private static final String Key = "jade.scp_unleashed.scp_sign.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlock() instanceof ScpSignBlock Owner)) {
            return;
        }
        ScpSignBlockEntity Sign = Owner.signAt(Accessor.getLevel(), Accessor.getBlockState(), Accessor.getPosition());
        if (Sign == null) {
            return;
        }
        ResourceLocation Id = Sign.getDocument();
        AnomalyDocument Document = Id == null ? null : AnomalyDocuments.lookup(Id);
        if (Id == null) {
            Tooltip.add(Component.translatable(Key + "unassigned").withStyle(ChatFormatting.GRAY));
        } else if (Document == null) {
            Tooltip.add(Component.translatable(Key + "unreadable").withStyle(ChatFormatting.DARK_RED));
        } else {
            int Colour = Document.Classification().getColour() & 0xFFFFFF;
            Tooltip.add(Component.translatable(Key + "shows", Document.designation(), Document.title()).withStyle(ChatFormatting.WHITE));
            Tooltip.add(Component.translatable(Key + "class", Document.objectClass().copy().withStyle(Look -> Look.withColor(Colour))).withStyle(ChatFormatting.GRAY));
        }
        if (Accessor.getBlockState().getValue(ScpSignBlock.Lit)) {
            Tooltip.add(Component.translatable(Key + "lit").withStyle(ChatFormatting.YELLOW));
        }
    }
}
