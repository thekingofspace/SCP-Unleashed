package net.rai.scp_unleashed.scp224.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp224.Scp224ClockBlock;
import net.rai.scp_unleashed.scp224.Scp224ClockBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp224JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp224_clock");
    private static final String Prefix = "jade.scp_unleashed.scp224_clock.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        Scp224ClockBlockEntity Clock = Scp224ClockBlock.clockAt(Accessor.getLevel(), Accessor.getPosition());
        if (Clock == null) {
            return;
        }
        if (Clock.isStilled()) {
            Tooltip.add(Component.translatable(Prefix + "still").withStyle(ChatFormatting.GRAY));
        } else if (Clock.isTolling()) {
            Tooltip.add(Component.translatable(Prefix + "tolling").withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable(Prefix + "ticking").withStyle(ChatFormatting.GOLD));
        }
    }
}
