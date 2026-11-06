package net.rai.scp_unleashed.scp007.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp007.Scp007Entity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp007JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp007");
    private static final String Prefix = "jade.scp_unleashed.scp007.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp007Entity Subject)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withStyle(ChatFormatting.GREEN));
        String State = Subject.isSeated() ? "gazing" : Subject.isStriding() ? "wandering" : "still";
        Tooltip.add(Component.translatable(Prefix + State).withStyle(ChatFormatting.GRAY));
    }
}
