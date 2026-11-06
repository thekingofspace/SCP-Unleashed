package net.rai.scp_unleashed.scp939.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp939.Scp939;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp939JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp939Registry.location("scp939");
    private static final String Prefix = "jade.scp_unleashed.scp939.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp939 Owner)) {
            return;
        }
        if (Owner.isBaby()) {
            Tooltip.add(Component.translatable(Prefix + "young").withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable(Prefix + (Owner.isContained() ? "pacing" : "listening")).withStyle(Owner.isContained() ? ChatFormatting.GOLD : ChatFormatting.RED));
    }
}
