package net.rai.scp_unleashed.scp3008.econ.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconEvents;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp3008FlatPackProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp3008EconRegistry.location("scp3008_econ_flatpack");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!Scp3008EconEvents.disassembling(Accessor.getPlayer()) || !Scp3008Carry.liftable(Accessor.getBlockState())) {
            return;
        }
        boolean Fixed = Scp3008Carry.fixed(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        Tooltip.add(Component.translatable(Fixed ? "jade.scp_unleashed.scp3008_econ_flatpack_fixed" : "jade.scp_unleashed.scp3008_econ_flatpack")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
