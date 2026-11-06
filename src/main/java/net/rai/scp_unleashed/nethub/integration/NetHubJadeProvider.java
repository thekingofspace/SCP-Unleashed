package net.rai.scp_unleashed.nethub.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.nethub.NetworkHubBlock;
import net.rai.scp_unleashed.nethub.NetworkHubBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public enum NetHubJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "network_hub");
    private static final String DataKey = "NetworkHub";
    private static final String Prefix = "jade.scp_unleashed.network_hub.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        NetworkHubBlockEntity Hub = NetworkHubBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Hub == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putInt("Port", Hub.getPort());
        Info.putBoolean("Powered", Hub.isPowered());
        Info.putBoolean("Bridging", Hub.isBridging());
        Info.putInt("Linked", Hub.linkedCount());
        Info.putString("Name", Hub.getNetworkName());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        Tooltip.add(Component.translatable(Prefix + "port", String.format(Locale.ROOT, "%04d", Info.getInt("Port"))).withStyle(ChatFormatting.AQUA));
        if (!Info.getBoolean("Powered")) {
            Tooltip.add(Component.translatable(Prefix + "offline").withStyle(ChatFormatting.DARK_GRAY));
        } else if (Info.getBoolean("Bridging")) {
            Tooltip.add(Component.translatable(Prefix + "bridging", Info.getInt("Linked")).withStyle(ChatFormatting.GREEN));
        } else {
            Tooltip.add(Component.translatable(Prefix + "standby").withStyle(ChatFormatting.GOLD));
        }
        Tooltip.add(Component.translatable(Prefix + "local", Info.getString("Name")).withStyle(ChatFormatting.GRAY));
    }
}
