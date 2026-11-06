package net.rai.scp_unleashed.scp003.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp003.Scp003BoardBlock;
import net.rai.scp_unleashed.scp003.Scp003BoardBlockEntity;
import net.rai.scp_unleashed.scp003.Scp003PlateBlock;
import net.rai.scp_unleashed.scp003.Scp003PolypBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp003BlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp003_blocks");
    private static final String ProgressKey = "Scp003Progress";
    private static final String LeftKey = "Scp003Left";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (Accessor.getBlockEntity() instanceof Scp003BoardBlockEntity Board) {
            Data.putFloat(ProgressKey, Board.fraction());
            Data.putInt(LeftKey, Board.ticksLeft());
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (State.getBlock() instanceof Scp003PlateBlock) {
            if (State.getValue(Scp003PlateBlock.Seated)) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.scp003_plate.seated").withStyle(ChatFormatting.AQUA));
                Tooltip.add(Component.translatable("jade.scp_unleashed.scp003_plate.lift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                Tooltip.add(Component.translatable("jade.scp_unleashed.scp003_plate.empty").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (State.getBlock() instanceof Scp003PolypBlock) {
            if (State.getValue(Scp003PolypBlock.Withered)) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.scp003_polyp.withering").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                int Age = State.getValue(Scp003PolypBlock.Age);
                Tooltip.add(Component.translatable("jade.scp_unleashed.scp003_polyp.growth", Age, Scp003PolypBlock.MaxAge)
                        .withStyle(Age >= Scp003PolypBlock.MaxAge ? ChatFormatting.RED : ChatFormatting.GOLD));
            }
            return;
        }
        if (!(State.getBlock() instanceof Scp003BoardBlock)) {
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp003_board.active").withStyle(ChatFormatting.RED));
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(ProgressKey)) {
            return;
        }
        float Progress = Data.getFloat(ProgressKey);
        int Minutes = Math.max(0, Data.getInt(LeftKey) / 1200);
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp003_board.progress", Math.round(Progress * 100.0f)).withStyle(ChatFormatting.GOLD));
        int Stage = Scp003BoardBlockEntity.stageOf(Progress);
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp003_board.stage." + Stage).withStyle(Stage >= 2 ? ChatFormatting.DARK_RED : ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Minutes > 0 ? "jade.scp_unleashed.scp003_board.minutes" : "jade.scp_unleashed.scp003_board.imminent", Minutes)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
