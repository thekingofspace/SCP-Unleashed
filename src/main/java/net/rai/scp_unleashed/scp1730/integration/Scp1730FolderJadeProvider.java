package net.rai.scp_unleashed.scp1730.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlock;
import net.rai.scp_unleashed.scp1730.block.RealityFolderLayout;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp1730FolderJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.reality_folder.";
    public static final String DataKey = "RealityFolder";
    private static final ResourceLocation Uid = Scp1730Registry.location("reality_folder");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        BlockPos Master = RealityFolderBlock.masterPos(Accessor.getBlockState(), Accessor.getPosition());
        if (Accessor.getLevel().getBlockEntity(Master) instanceof RealityFolderBlockEntity Folder) {
            CompoundTag Info = new CompoundTag();
            Info.putInt("Status", Folder.status());
            Info.putInt("Supply", Folder.lastDelivered());
            Info.putFloat("Spin", Folder.spinShare());
            Data.put(DataKey, Info);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Info = Accessor.getServerData().getCompound(DataKey);
        if (Info.isEmpty()) {
            return;
        }
        int Status = Info.getInt("Status");
        int Supply = Info.getInt("Supply");
        BlockState Here = Accessor.getBlockState();
        if (Here.getBlock() instanceof RealityFolderBlock && RealityFolderLayout.portSide(Here.getValue(RealityFolderBlock.Part), Here.getValue(RealityFolderBlock.Facing)) != null) {
            Tooltip.add(Component.translatable(Prefix + "port").withStyle(ChatFormatting.GOLD));
        }
        Tooltip.add(Component.translatable(Prefix + "draw", RealityFolderBlockEntity.Demand).withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Prefix + "supply", Supply).withStyle(Supply >= RealityFolderBlockEntity.Demand ? ChatFormatting.GREEN : ChatFormatting.RED));
        switch (Status) {
            case RealityFolderBlockEntity.StatusRunning -> Tooltip.add(Component.translatable(Prefix + "running").withStyle(ChatFormatting.AQUA));
            case RealityFolderBlockEntity.StatusSpinning -> Tooltip.add(Component.translatable(Prefix + "spinning", Math.round(Info.getFloat("Spin") * 100.0f))
                    .withStyle(ChatFormatting.GOLD));
            case RealityFolderBlockEntity.StatusShutdown -> Tooltip.add(Component.translatable(Prefix + "stopping").withStyle(ChatFormatting.RED));
            default -> {
                Tooltip.add(Component.translatable(Prefix + "off").withStyle(ChatFormatting.DARK_GRAY));
                if (Supply > 0 && Supply < RealityFolderBlockEntity.Demand) {
                    Tooltip.add(Component.translatable(Prefix + "short").withStyle(ChatFormatting.RED));
                }
            }
        }
    }
}
