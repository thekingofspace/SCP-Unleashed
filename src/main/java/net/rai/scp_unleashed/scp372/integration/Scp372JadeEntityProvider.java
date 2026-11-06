package net.rai.scp_unleashed.scp372.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp372.Scp372Insect;
import net.rai.scp_unleashed.scp372.Scp372Registry;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp372JadeEntityProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp372Registry.location(Scp372Registry.InsectId);
    private static final String Prefix = "jade.scp_unleashed.scp372.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp372Insect Creature)) {
            return;
        }
        String Line = Creature.isFlickering() ? "slipping" : Creature.isHeld() ? "held" : "loose";
        Tooltip.add(Component.translatable(Prefix + Line).withStyle(Line.equals("held") ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
