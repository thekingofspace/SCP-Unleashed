package net.rai.scp_unleashed.scp294.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp294.Scp294MachineBlock;
import net.rai.scp_unleashed.scp294.Scp294MachineBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public enum Scp294MachineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp294_machine");
    private static final String DataKey = "Scp294Machine";
    private static final String Prefix = "jade.scp_unleashed.scp294_machine.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        Scp294MachineBlockEntity Machine = Scp294MachineBlock.machineAt(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putBoolean("Busy", Machine.isBusy());
        Info.putString("Last", Machine.getLastOrder());
        Info.putInt("Orders", Machine.getOrders());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        if (Info.getBoolean("Busy")) {
            Tooltip.add(Component.translatable(Prefix + "busy").withStyle(ChatFormatting.YELLOW));
        } else {
            Tooltip.add(Component.translatable(Prefix + "ready").withStyle(ChatFormatting.GREEN));
        }
        String Last = Info.getString("Last");
        if (!Last.isEmpty()) {
            Tooltip.add(Component.translatable(Prefix + "last", Last.toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        }
        int Orders = Info.getInt("Orders");
        if (Orders > 0) {
            Tooltip.add(Component.translatable(Prefix + "orders", Orders).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
