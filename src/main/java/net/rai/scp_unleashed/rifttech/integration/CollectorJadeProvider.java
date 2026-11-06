package net.rai.scp_unleashed.rifttech.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import net.rai.scp_unleashed.rifttech.CollectorStatus;
import net.rai.scp_unleashed.rifttech.RiftCollectorBlock;
import net.rai.scp_unleashed.rifttech.RiftCollectorBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CollectorJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "rift_collector");
    private static final String DataKey = "RiftCollector";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        RiftCollectorBlockEntity Collector = RiftCollectorBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Collector == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Collector.getStatus().getSerializedName());
        Info.putInt("Kind", Collector.getKind());
        Info.putInt("Progress", Math.round(Collector.getProgress() * 100.0f));
        Info.putInt("Range", Collector.getRange());
        Info.putInt("Stored", Collector.countStored());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        CollectorStatus Status = CollectorStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable("jade.scp_unleashed.rift_collector.status", Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour())));
        int Kind = Info.getInt("Kind");
        if (Status.hasTarget() && Kind > 0) {
            boolean Apocalyptic = Kind == RiftCollectorBlockEntity.KindApocalyptic;
            String Label = Apocalyptic ? ApocalypticRiftEntity.Label : FragmentRarity.byTier(Kind).getSerializedName();
            int Colour = Apocalyptic ? ApocalypticRiftEntity.Colour : FragmentRarity.byTier(Kind).getColour();
            Component Name = Component.translatable("hud.scp_unleashed.extraction.rarity." + Label).withStyle(Style.EMPTY.withColor(Colour & 0xFFFFFF));
            Tooltip.add(Component.translatable("jade.scp_unleashed.rift_collector.target", Name, Info.getInt("Progress")));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.rift_collector.range", Info.getInt("Range")).withStyle(ChatFormatting.GRAY));
        int Stored = Info.getInt("Stored");
        if (Stored > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.rift_collector.stored", Stored).withStyle(ChatFormatting.GRAY));
        }
    }
}
