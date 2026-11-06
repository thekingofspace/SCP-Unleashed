package net.rai.scp_unleashed.scp261.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp261.Scp261VendingBlock;
import net.rai.scp_unleashed.scp261.Scp261VendingBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp261VendingJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp261_vending");
    private static final String DataKey = "Scp261Vending";
    private static final String Prefix = "jade.scp_unleashed.scp261_vending.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        Scp261VendingBlockEntity Machine = Scp261VendingBlock.machineAt(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putBoolean("Idle", Machine.isIdle());
        Info.putBoolean("Dispensing", !Machine.isIdle() && Machine.getTimer() < Scp261VendingBlockEntity.PopTick);
        Info.putInt("Vends", Machine.getVends());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        if (Info.getBoolean("Idle")) {
            Tooltip.add(Component.translatable(Prefix + "idle").withStyle(ChatFormatting.GRAY));
        } else if (Info.getBoolean("Dispensing")) {
            Tooltip.add(Component.translatable(Prefix + "dispensing").withStyle(ChatFormatting.YELLOW));
        } else {
            Tooltip.add(Component.translatable(Prefix + "settling").withStyle(ChatFormatting.GOLD));
        }
        int Vends = Info.getInt("Vends");
        if (Vends > 0) {
            Tooltip.add(Component.translatable(Prefix + "vends", Vends).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
