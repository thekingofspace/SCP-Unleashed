package net.rai.scp_unleashed.scp024.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp024.Scp024ExitDoorBlock;
import net.rai.scp_unleashed.scp024.Scp024Stage;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp024DoorJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_stage_door");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (Accessor.getBlock() instanceof Scp024ExitDoorBlock) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp024_exit_door").withStyle(ChatFormatting.GREEN));
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp024_exit_door_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        if (Scp024Stage.inStage(Accessor.getLevel())) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp024_stage_door_inside").withStyle(ChatFormatting.GRAY));
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp024_stage_door").withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp024_stage_door_hint").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }
}
