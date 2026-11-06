package net.rai.scp_unleashed.scp017.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp017.Scp017Light;
import net.rai.scp_unleashed.scp017.Scp017Shadow;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp017JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp017_shadow");
    private static final String Prefix = "jade.scp_unleashed.scp017.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp017Shadow Shadow)) {
            return;
        }
        byte Action = Shadow.action();
        if (Action == Scp017Shadow.ActionEmerge) {
            Tooltip.add(Component.translatable(Prefix + "emerging").withStyle(ChatFormatting.DARK_PURPLE));
        } else if (Shadow.isFleeing()) {
            Tooltip.add(Component.translatable(Prefix + "fleeing").withStyle(ChatFormatting.GOLD));
        } else if (Shadow.isHunting()) {
            Tooltip.add(Component.translatable(Action == Scp017Shadow.ActionReach ? Prefix + "reaching" : Prefix + "hunting").withStyle(ChatFormatting.RED));
        } else if (Shadow.isSettled()) {
            Tooltip.add(Component.translatable(Prefix + "dormant_corner").withStyle(ChatFormatting.GRAY));
        } else {
            Tooltip.add(Component.translatable(Prefix + "dormant_seeking").withStyle(ChatFormatting.GRAY));
        }
        int Light = Scp017Light.of(Shadow);
        boolean Lit = Light > Scp017Light.Threshold;
        Tooltip.add(Component.translatable(Prefix + (Lit ? "light_safe" : "light_danger"), Light, Scp017Light.Threshold)
                .withStyle(Lit ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
    }
}
