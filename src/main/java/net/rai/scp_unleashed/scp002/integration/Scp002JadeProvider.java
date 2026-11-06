package net.rai.scp_unleashed.scp002.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp002.BoneLampBlock;
import net.rai.scp_unleashed.scp002.BulkheadDoorBlock;
import net.rai.scp_unleashed.scp002.FleshBlock;
import net.rai.scp_unleashed.scp002.FleshFixtureBlock;
import net.rai.scp_unleashed.scp002.FleshSurfaceBlock;
import net.rai.scp_unleashed.scp002.MeteoricShellBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp002JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp002");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (State.getBlock() instanceof MeteoricShellBlock) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp002_shell").withStyle(ChatFormatting.GRAY));
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp002_shell_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        if (!FleshBlock.isFlesh(State)) {
            return;
        }
        if (State.getBlock() instanceof BulkheadDoorBlock) {
            boolean Open = State.getValue(BulkheadDoorBlock.Open);
            Tooltip.add(Component.translatable(Open ? "jade.scp_unleashed.scp002_door_open" : "jade.scp_unleashed.scp002_door_closed")
                    .withStyle(Open ? ChatFormatting.RED : ChatFormatting.GRAY));
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp002_door_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else if (State.getBlock() instanceof FleshFixtureBlock || State.getBlock() instanceof BoneLampBlock) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp002_fixture").withStyle(ChatFormatting.GRAY));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp002_flesh").withStyle(ChatFormatting.GRAY));
        }
        if (State.getBlock() instanceof FleshSurfaceBlock && State.getValue(FleshSurfaceBlock.Awake)) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp002_awake").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp002_unbreakable").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
