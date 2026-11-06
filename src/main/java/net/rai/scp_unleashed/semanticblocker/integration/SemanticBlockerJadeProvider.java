package net.rai.scp_unleashed.semanticblocker.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp148.Scp148Shield;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerBlock;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerBlockEntity;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerRegistry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SemanticBlockerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = SemanticBlockerRegistry.location(SemanticBlockerRegistry.BlockerId);
    private static final String Prefix = "jade.scp_unleashed." + SemanticBlockerRegistry.BlockerId + ".";
    private static final String DataKey = "SemanticBlocker";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (Accessor.getBlockEntity() instanceof SemanticBlockerBlockEntity Blocker) {
            CompoundTag Status = new CompoundTag();
            Status.putInt("Received", Blocker.getLastReceived());
            Data.put(DataKey, Status);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        boolean Active = Accessor.getBlockState().getValue(SemanticBlockerBlock.Powered);
        int Received = Accessor.getServerData().getCompound(DataKey).getInt("Received");
        if (Active) {
            Tooltip.add(Component.translatable(Prefix + "powered").withStyle(ChatFormatting.GREEN));
        } else if (Received > 0) {
            Tooltip.add(Component.translatable(Prefix + "underpowered").withStyle(ChatFormatting.GOLD));
        } else {
            Tooltip.add(Component.translatable(Prefix + "unpowered").withStyle(ChatFormatting.RED));
        }
        Tooltip.add(Component.translatable(Prefix + "radius", SemanticBlockerBlockEntity.radiusBlocks()).withStyle(Active ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Prefix + "demand", SemanticBlockerBlockEntity.PowerDraw).withStyle(ChatFormatting.GRAY));
        if (Active && Scp148Shield.isWarded(Accessor.getPlayer())) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp148_block.quiet").withStyle(ChatFormatting.GREEN));
        }
    }
}
