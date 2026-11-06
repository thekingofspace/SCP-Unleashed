package net.rai.scp_unleashed.scp4845.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp4845CounterJadeProvider implements IBlockComponentProvider {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = Scp4845Registry.location("scp4845_counter");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public int getDefaultPriority() {
        return Priority;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp4845_counter.order").withStyle(ChatFormatting.GRAY));
    }
}
