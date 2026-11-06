package net.rai.scp_unleashed.tesla.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.tesla.TeslaGateBlock;
import net.rai.scp_unleashed.tesla.TeslaGateBlockEntity;
import net.rai.scp_unleashed.tesla.TeslaStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum TeslaJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "tesla_gate");
    private static final String DataKey = "TeslaGate";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof TeslaGateBlock)) {
            return;
        }
        TeslaGateBlockEntity Gate = TeslaGateBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), State);
        if (Gate != null) {
            Data.putString(DataKey, Gate.getStatus().getSerializedName());
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        String Status = Data.getString(DataKey);
        ChatFormatting Colour = Status.equals(TeslaStatus.Armed.getSerializedName()) ? ChatFormatting.RED
                : Status.equals(TeslaStatus.Ready.getSerializedName()) ? ChatFormatting.GREEN
                : Status.equals(TeslaStatus.Off.getSerializedName()) ? ChatFormatting.GRAY : ChatFormatting.GOLD;
        Tooltip.add(Component.translatable("jade.scp_unleashed.tesla." + Status).withStyle(Colour));
    }
}
