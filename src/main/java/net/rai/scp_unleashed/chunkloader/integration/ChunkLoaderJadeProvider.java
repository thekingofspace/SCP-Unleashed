package net.rai.scp_unleashed.chunkloader.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderArea;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderBlockEntity;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum ChunkLoaderJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "chunk_loader");
    private static final String DataKey = "ChunkLoader";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof ChunkLoaderBlockEntity Loader)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Loader.getStatus().getSerializedName());
        Info.putInt("Radius", Loader.getRadius());
        Info.putInt("Loaded", Loader.getLoadedCount());
        Info.putInt("Grace", Loader.getGraceLeft());
        Info.putInt("Demand", Loader.getDemand());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        ChunkLoaderStatus Status = ChunkLoaderStatus.byName(Info.getString("Status"));
        int Radius = Info.getInt("Radius");
        int Side = ChunkLoaderArea.side(Radius);
        int Total = Side * Side;
        Component Line = switch (Status) {
            case Active -> Component.translatable("jade.scp_unleashed.chunk_loader.active", Total);
            case Starting -> Component.translatable("jade.scp_unleashed.chunk_loader.starting", Math.min(Info.getInt("Loaded"), Total), Total);
            case LowPower -> Component.translatable("jade.scp_unleashed.chunk_loader.low_power", (Info.getInt("Grace") + 19) / 20);
            case Unpowered -> Component.translatable("jade.scp_unleashed.chunk_loader.unpowered");
            case Off -> Component.translatable("jade.scp_unleashed.chunk_loader.off");
        };
        Tooltip.add(Line.copy().withStyle(Status.getColour()));
        int Blocks = ChunkLoaderArea.blockSpan(Radius);
        Tooltip.add(Component.translatable("jade.scp_unleashed.chunk_loader.area", Side, Side, Blocks, Blocks).withStyle(ChatFormatting.GRAY));
        if (Status != ChunkLoaderStatus.Off) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.chunk_loader.draw", Info.getInt("Demand")).withStyle(ChatFormatting.GRAY));
        }
    }
}
