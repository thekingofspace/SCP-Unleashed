package net.rai.scp_unleashed.gas.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.CreativePressurizerBlock;
import net.rai.scp_unleashed.block.DuctBlock;
import net.rai.scp_unleashed.block.entity.PressurizerBlockEntity;
import net.rai.scp_unleashed.pressure.DuctKind;
import net.rai.scp_unleashed.pressure.PressureNetwork;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum PressureNoticeJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "pressure_notice");
    private static final String DataKey = "PressureNotice";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        if (Accessor.getBlock() instanceof DuctBlock Duct) {
            PressureNetwork Network = PressureNetworkManager.get(World).networkAt(World, Accessor.getPosition(), Duct.getKind());
            Info.putInt("Open", Network.getOpenEnds().size());
            Info.putBoolean("Vent", Duct.getKind() == DuctKind.Vent);
        } else if (Accessor.getBlockEntity() instanceof PressurizerBlockEntity Pump) {
            Info.putBoolean("Blocked", Pump.isBlocked());
        } else if (Accessor.getBlock() instanceof CreativePressurizerBlock) {
            Info.putBoolean("Voiding", Accessor.getBlockState().getValue(CreativePressurizerBlock.Voiding));
        } else {
            return;
        }
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        if (Info.contains("Voiding")) {
            boolean Voiding = Info.getBoolean("Voiding");
            Tooltip.add(Component.translatable(Voiding ? "jade.scp_unleashed.creative_pressurizer.voiding" : "jade.scp_unleashed.creative_pressurizer.pressure_only")
                    .withStyle(Voiding ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
            return;
        }
        if (Info.getBoolean("Blocked")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.pressurizer.open_network").withStyle(ChatFormatting.RED));
            return;
        }
        int Open = Info.getInt("Open");
        if (Open <= 0) {
            return;
        }
        if (Info.getBoolean("Vent")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.vent.open_ends", Open).withStyle(ChatFormatting.GRAY));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.pipe.open_ends", Open).withStyle(ChatFormatting.RED));
        }
    }
}
