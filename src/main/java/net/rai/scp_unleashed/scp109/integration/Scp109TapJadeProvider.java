package net.rai.scp_unleashed.scp109.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp109.Scp109TapBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp109TapJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp109_canteen_tap");
    private static final String DataKey = "Scp109Tap";
    private static final String Prefix = "jade.scp_unleashed.scp109_canteen_tap.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof Scp109TapBlockEntity Tap)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putBoolean("Seated", Tap.hasCanteen());
        Info.putBoolean("Flowing", Tap.isFlowing());
        Info.putInt("Rate", Tap.getRate());
        Info.putLong("Total", Tap.getTotal());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        if (!Info.getBoolean("Seated")) {
            Tooltip.add(Component.translatable(Prefix + "empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "seated").withStyle(ChatFormatting.AQUA));
        int Rate = Info.getInt("Rate");
        if (Info.getBoolean("Flowing") || Rate > 0) {
            Tooltip.add(Component.translatable(Prefix + "output", Rate, Scp109TapBlockEntity.MaxPerSecond).withStyle(ChatFormatting.GREEN));
        } else {
            Tooltip.add(Component.translatable(Prefix + "idle", Scp109TapBlockEntity.MaxPerSecond).withStyle(ChatFormatting.GRAY));
        }
        if (Accessor.showDetails()) {
            Tooltip.add(Component.translatable(Prefix + "total", Info.getLong("Total")).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
