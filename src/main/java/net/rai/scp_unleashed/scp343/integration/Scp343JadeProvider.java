package net.rai.scp_unleashed.scp343.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp343.Scp343God;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp343JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp343");
    private static final String Prefix = "jade.scp_unleashed.scp343.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp343God God)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withStyle(ChatFormatting.GREEN));
        String State = God.isDeparting() ? "away" : God.isEmerging() ? "arriving" : God.isSittingPose() ? "seated" : "wandering";
        Tooltip.add(Component.translatable(Prefix + State).withStyle(God.isPhasing() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
    }
}
