package net.rai.scp_unleashed.scp105.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp105.Scp105Iris;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp105JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp105_iris");
    private static final String Prefix = "jade.scp_unleashed.scp105.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp105Iris Iris)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withStyle(ChatFormatting.GREEN));
        String State = Iris.isSnapping() ? "photographing" : Iris.isGiving() ? "offering" : Iris.isTalking() ? "chatting" : Iris.isStriding() ? "wandering" : "still";
        Tooltip.add(Component.translatable(Prefix + State).withStyle(ChatFormatting.GRAY));
    }
}
