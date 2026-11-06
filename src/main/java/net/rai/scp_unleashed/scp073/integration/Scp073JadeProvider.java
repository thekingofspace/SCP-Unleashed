package net.rai.scp_unleashed.scp073.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp073.Scp073Cain;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp073JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp073_cain");
    private static final String Prefix = "jade.scp_unleashed.scp073.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp073Cain Cain)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withStyle(ChatFormatting.GREEN));
        String State = Cain.isInWater() ? "water" : Cain.isStriding() ? "wandering" : "still";
        Tooltip.add(Component.translatable(Prefix + State).withStyle(Cain.isInWater() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
    }
}
