package net.rai.scp_unleashed.fission.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.FissionReactorBlock;
import net.rai.scp_unleashed.fission.FissionReactorBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum FissionReactorJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fission_reactor");
    private static final String DataKey = "FissionReactor";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        BlockState State = Accessor.getBlockState();
        FissionReactorBlockEntity Reactor = FissionReactorBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), State);
        if (Reactor == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putInt("Waste", Reactor.getWasteTank().getFluidAmount());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        Tooltip.add(Component.translatable("jade.scp_unleashed.reactor.decommissioned").withStyle(ChatFormatting.GOLD));
        Tooltip.add(Component.translatable("jade.scp_unleashed.reactor.decommissioned.hint").withStyle(ChatFormatting.GRAY));
        if (Info.getInt("Waste") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.fission_reactor.waste", Info.getInt("Waste"), FissionReactorBlockEntity.WasteCapacity));
        }
    }
}
