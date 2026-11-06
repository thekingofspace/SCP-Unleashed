package net.rai.scp_unleashed.scp076.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp076.Scp076Abel;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp076AbelProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp076_abel");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp076Abel Abel)) {
            return;
        }
        String Key = switch (Abel.phase()) {
            case Scp076Abel.PhaseRising -> "rising";
            case Scp076Abel.PhaseSinking -> "sinking";
            case Scp076Abel.PhaseReturning -> "returning";
            default -> Abel.isAggressive() ? "hunting" : "searching";
        };
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp076." + Key).withStyle(Key.equals("hunting") ? ChatFormatting.RED : ChatFormatting.GRAY));
    }
}
