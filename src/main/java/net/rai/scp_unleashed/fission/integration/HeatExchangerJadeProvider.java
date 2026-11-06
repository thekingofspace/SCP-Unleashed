package net.rai.scp_unleashed.fission.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.FissionReactorBlock;
import net.rai.scp_unleashed.fission.HeatExchangerBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum HeatExchangerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fission_heat_exchanger");
    private static final String DataKey = "FissionExchanger";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof HeatExchangerBlockEntity Exchanger)) {
            return;
        }
        Level World = Accessor.getLevel();
        BlockPos Above = Accessor.getPosition().above();
        BlockState AboveState = World.getBlockState(Above);
        CompoundTag Info = new CompoundTag();
        Info.putInt("Water", Exchanger.getWater());
        Info.putBoolean("Cooling", Exchanger.isCooling());
        Info.putInt("Steam", Exchanger.getSteam().getAmount());
        Info.putBoolean("Reactor", AboveState.getBlock() instanceof FissionReactorBlock && AboveState.getValue(FissionReactorBlock.CellY) == 0);
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        int Water = Info.getInt("Water");
        Tooltip.add(Component.translatable("jade.scp_unleashed.fission_heat_exchanger.water", Water, HeatExchangerBlockEntity.Capacity).withStyle(ChatFormatting.AQUA));
        int Steam = Info.getInt("Steam");
        if (Steam > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.fission_heat_exchanger.steam", Steam, HeatExchangerBlockEntity.SteamCapacity).withStyle(ChatFormatting.WHITE));
        }
        String Key;
        ChatFormatting Colour;
        if (!Info.getBoolean("Reactor")) {
            Key = "no_reactor";
            Colour = ChatFormatting.GRAY;
        } else if (Water <= 0) {
            Key = "dry";
            Colour = ChatFormatting.RED;
        } else if (Info.getBoolean("Cooling")) {
            Key = "cooling";
            Colour = ChatFormatting.GREEN;
        } else {
            Key = "ready";
            Colour = ChatFormatting.GRAY;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.fission_heat_exchanger." + Key).withStyle(Colour));
    }
}
