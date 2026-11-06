package net.rai.scp_unleashed.storage.gel.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.gel.GelRules;
import net.rai.scp_unleashed.storage.gel.GelStatus;
import net.rai.scp_unleashed.storage.gel.GelStorageContainerBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum GelStorageJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "storage_container_gel");
    private static final String DataKey = "GelStorage";
    private static final String Prefix = "jade.scp_unleashed.storage_container_gel.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof GelStorageContainerBlockEntity Locker)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putInt("Gel", Locker.getGel());
        Info.putBoolean("Powered", Locker.isPowered());
        Info.putBoolean("Watered", Locker.isWatered());
        Info.putInt("Water", Locker.getTank().getFluidAmount());
        Info.putString("Status", Locker.getStatus().getSerializedName());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        GelStatus Status = GelStatus.Idle;
        for (GelStatus Candidate : GelStatus.values()) {
            if (Candidate.getSerializedName().equals(Info.getString("Status"))) {
                Status = Candidate;
            }
        }
        int Percent = GelRules.percent(Info.getInt("Gel"));
        Tooltip.add(Component.translatable(Prefix + Status.getSerializedName(), Percent).withStyle(Status.colour()));
        boolean Powered = Info.getBoolean("Powered");
        Tooltip.add(Component.translatable(Prefix + (Powered ? "powered" : "unpowered")).withStyle(Powered ? ChatFormatting.AQUA : ChatFormatting.RED));
        int Water = Info.getInt("Water");
        if (Water > 0) {
            Tooltip.add(Component.translatable(Prefix + "water", Water, GelRules.TankCapacity).withStyle(ChatFormatting.BLUE));
        } else {
            Tooltip.add(Component.translatable(Prefix + "dry").withStyle(ChatFormatting.RED));
        }
    }
}
