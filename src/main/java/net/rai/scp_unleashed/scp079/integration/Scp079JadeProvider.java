package net.rai.scp_unleashed.scp079.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp079.Scp079Block;
import net.rai.scp_unleashed.scp079.Scp079BlockEntity;
import net.rai.scp_unleashed.scp079.Scp079Mood;
import net.rai.scp_unleashed.scp079.Scp079Registry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp079JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp079_computer");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        Scp079BlockEntity Computer = Scp079Block.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Computer == null) {
            return;
        }
        boolean Running = Computer.isPowered();
        Tooltip.add(Component.translatable(Running ? "jade.scp_unleashed.scp079.powered" : "jade.scp_unleashed.scp079.unpowered")
                .withStyle(Running ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        if (Running) {
            Scp079Mood Mood = Computer.getMood();
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp079.mood", Mood.getDisplayName()).withStyle(ChatFormatting.GRAY));
        }
        if (Computer.isUpgraded()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp079.upgrade", Scp079Registry.AnomalousUpgrade.get().getDescription())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
