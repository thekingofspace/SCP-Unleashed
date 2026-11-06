package net.rai.scp_unleashed.scp427.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp427.Scp427LocketItem;
import net.rai.scp_unleashed.scp427.client.Scp427ClientLocket;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp427LocketJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp427_locket");
    private static final String Prefix = "jade.scp_unleashed.scp427_locket.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof LivingEntity Holder) || !Scp427LocketItem.is(Holder.getMainHandItem())) {
            return;
        }
        boolean Open = Scp427ClientLocket.opening(Holder);
        Component State = Component.translatable(Prefix + (Open ? "open" : "closed")).withStyle(Open ? ChatFormatting.AQUA : ChatFormatting.GRAY);
        Tooltip.add(Component.translatable(Prefix + "held", State).withStyle(ChatFormatting.GRAY));
    }
}
