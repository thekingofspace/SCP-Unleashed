package net.rai.scp_unleashed.scp001day.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayRift;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp001DayJadeProvider implements IEntityComponentProvider {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.scp001_day_rift.";
    public static final float Fading = 0.5f;
    private static final ResourceLocation Uid = Scp001DayRegistry.location("scp001_day_rift");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp001DayRift Rift)) {
            return;
        }
        boolean Steady = Rift.openness(0.0f) > Fading && Rift.life() - Rift.age() > Scp001DayRift.CloseTicks * 4;
        Tooltip.add(Component.translatable(Prefix + (Steady ? "steady" : "fading")).withStyle(Steady ? ChatFormatting.GOLD : ChatFormatting.GRAY));
    }
}
