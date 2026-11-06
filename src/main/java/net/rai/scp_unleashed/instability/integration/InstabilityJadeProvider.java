package net.rai.scp_unleashed.instability.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.instability.InstabilityReactorBlock;
import net.rai.scp_unleashed.instability.InstabilityReactorBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum InstabilityJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "instability_reactor");
    private static final String DataKey = "ScpInstability";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        BlockState State = Accessor.getBlockState();
        InstabilityReactorBlockEntity Reactor = InstabilityReactorBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), State);
        if (Reactor == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putInt("Queued", Reactor.getFuel().getStackInSlot(0).getCount());
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
        int Queued = Info.getInt("Queued");
        if (Queued > 0) {
            Tooltip.add(Component.translatable("container.scp_unleashed.instability_reactor.queued", Queued).withStyle(ChatFormatting.GRAY));
        }
    }
}
