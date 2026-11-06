package net.rai.scp_unleashed.labware.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabViralStorageBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum LabViralStorageJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "lab_viral_storage");
    private static final String DataKey = "LabViralStorage";
    private static final String Prefix = "jade.scp_unleashed.lab_viral_storage.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof LabViralStorageBlockEntity Locker)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putBoolean("Powered", Locker.isPowered());
        Info.putBoolean("Open", Locker.isLidOpen());
        Info.putInt("Vials", Locker.getRack().countVials());
        Info.putInt("Cultures", Locker.getRack().countCultures());
        Info.putBoolean("Breached", Locker.isBreached());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        boolean Powered = Info.getBoolean("Powered");
        boolean Open = Info.getBoolean("Open");
        Tooltip.add(Component.translatable(Prefix + (Open ? "lid_open" : "lid_shut")).withStyle(Open ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        if (Info.getInt("Vials") == 0) {
            Tooltip.add(Component.translatable(Prefix + (Powered ? "standby" : "unpowered")).withStyle(Powered ? ChatFormatting.GRAY : ChatFormatting.RED));
            return;
        }
        Tooltip.add(Component.translatable(Prefix + (Powered ? "powered" : "unpowered")).withStyle(Powered ? ChatFormatting.AQUA : ChatFormatting.RED));
        Tooltip.add(Component.translatable(Prefix + "vials", Info.getInt("Vials"), LabViralStorageBlockEntity.SlotCount));
        if (Info.getInt("Cultures") > 0) {
            Tooltip.add(Component.translatable(Prefix + "cultures", Info.getInt("Cultures")).withStyle(ChatFormatting.GOLD));
        }
        if (Info.getBoolean("Breached")) {
            Tooltip.add(Component.translatable(Prefix + "breached").withStyle(ChatFormatting.RED));
        }
    }
}
