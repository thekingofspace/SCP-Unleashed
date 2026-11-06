package net.rai.scp_unleashed.scp179.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp179.SatelliteTerminalBlockEntity;
import net.rai.scp_unleashed.scp179.Scp179Omen;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SatelliteTerminalJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "satellite_terminal");
    private static final String DataKey = "SatelliteTerminal";
    private static final String Prefix = "jade.scp_unleashed.satellite_terminal.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof SatelliteTerminalBlockEntity Terminal)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putBoolean("Running", Terminal.isRunning());
        Info.putInt("Satellites", Terminal.getSatelliteCount());
        Info.putInt("Locations", Terminal.getLocations().size());
        Info.putString("Omen", Terminal.getOmen().getSerializedName());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        boolean Running = Info.getBoolean("Running");
        Tooltip.add(Component.translatable(Running ? Prefix + "online" : Prefix + "offline").withStyle(Running ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        int Satellites = Info.getInt("Satellites");
        if (Satellites > 0) {
            Tooltip.add(Component.translatable(Prefix + "uplinks", Satellites).withStyle(ChatFormatting.AQUA));
        } else {
            Tooltip.add(Component.translatable(Prefix + "no_uplink").withStyle(ChatFormatting.GOLD));
        }
        Tooltip.add(Component.translatable(Prefix + "locations", Info.getInt("Locations"), SatelliteTerminalBlockEntity.MaxLocations).withStyle(ChatFormatting.GRAY));
        Scp179Omen Omen = Scp179Omen.byName(Info.getString("Omen"));
        if (Running && Omen.isActive()) {
            Tooltip.add(Component.translatable(Prefix + "pointing", Omen.word()).withStyle(Look -> Look.withColor(Omen.getColour() & 0xFFFFFF)));
        }
    }
}
