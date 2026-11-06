package net.rai.scp_unleashed.scp019.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp019.Scp019PotBlock;
import net.rai.scp_unleashed.scp019.Scp019PotBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp019PotProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp019_pot");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof Scp019PotBlock)) {
            return;
        }
        if (!State.getValue(Scp019PotBlock.Open)) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp019_pot.sealed").withStyle(ChatFormatting.GREEN));
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp019_pot.movable").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp019_pot.open").withStyle(ChatFormatting.RED));
        if (!(Accessor.getLevel().getBlockEntity(Scp019PotBlock.lowerPos(State, Accessor.getPosition())) instanceof Scp019PotBlockEntity Pot)) {
            return;
        }
        if (Pot.alarmed()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp019_pot.alarm").withStyle(ChatFormatting.GOLD));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp019_pot.drones", Pot.droneCount(), Scp019PotBlockEntity.MaxDrones).withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp019_pot.brutes", Pot.bruteCount(), Scp019PotBlockEntity.MaxBrutes).withStyle(ChatFormatting.GOLD));
        if (Pot.reforming() > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp019_pot.reforming", Pot.reforming()).withStyle(ChatFormatting.DARK_GRAY));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp019_pot.search", Pot.searchPercent()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
