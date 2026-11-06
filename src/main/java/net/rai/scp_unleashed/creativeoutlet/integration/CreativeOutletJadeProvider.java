package net.rai.scp_unleashed.creativeoutlet.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletBlockEntity;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletRegistry;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CreativeOutletJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = CreativeOutletRegistry.location(CreativeOutletRegistry.OutletId);
    private static final String Prefix = "jade.scp_unleashed." + CreativeOutletRegistry.OutletId + ".";
    private static final String DataKey = "CreativeOutlet";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World) || !(Accessor.getBlockEntity() instanceof CreativeOutletBlockEntity)) {
            return;
        }
        WireNetwork Network = WireNetworkManager.get(World).networkAt(World, Accessor.getPosition());
        CompoundTag Status = new CompoundTag();
        Status.putLong("Feeding", Network == null ? 0 : Network.getLastOutput(Accessor.getPosition()));
        Data.put(DataKey, Status);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        Tooltip.add(Component.translatable(Prefix + "supply").withStyle(ChatFormatting.LIGHT_PURPLE));
        CompoundTag Data = Accessor.getServerData();
        if (Data.contains(DataKey)) {
            Tooltip.add(Component.translatable(Prefix + "feeding", Data.getCompound(DataKey).getLong("Feeding")).withStyle(ChatFormatting.GRAY));
        }
    }
}
