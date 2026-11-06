package net.rai.scp_unleashed.scp087.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp087.Scp087Hunter;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp087HunterProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp087_hunter");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp087Hunter Hunter)) {
            return;
        }
        String Key = switch (Hunter.action()) {
            case Scp087Hunter.ActionWindup, Scp087Hunter.ActionLunge, Scp087Hunter.ActionSwipe -> "striking";
            case Scp087Hunter.ActionFreeze -> "still";
            default -> Hunter.isAggressive() ? "hunting" : "waiting";
        };
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp087_hunter." + Key).withStyle(Key.equals("waiting") ? ChatFormatting.GRAY : ChatFormatting.DARK_RED));
    }
}
