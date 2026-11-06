package net.rai.scp_unleashed.scp207.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp207.Scp207BottlesBlock;
import net.rai.scp_unleashed.scp207.Scp207Registry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp207BottlesJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp207Registry.location("scp207_bottles");
    private static final String Prefix = "jade.scp_unleashed.scp207_bottles.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof Scp207BottlesBlock)) {
            return;
        }
        int Total = Scp207BottlesBlock.count(State);
        int Blue = Scp207BottlesBlock.blueCount(State);
        Tooltip.add(Component.translatable(Prefix + "count", Total, Scp207BottlesBlock.MaxBottles).withStyle(ChatFormatting.GRAY));
        if (Blue > 0) {
            Tooltip.add(Component.translatable(Prefix + "blue", Blue).withStyle(ChatFormatting.BLUE));
        }
    }
}
