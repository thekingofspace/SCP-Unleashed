package net.rai.scp_unleashed.scp001day.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFlesh;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp001DayFleshJadeProvider implements IEntityComponentProvider {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.scp001_day_flesh.";
    private static final ResourceLocation Uid = Scp001DayRegistry.location("scp001_day_flesh");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp001DayFlesh Flesh)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withColor(DocumentClass.Apollyon.getColour() & 0xFFFFFF));
        if (Flesh.holding()) {
            Tooltip.add(Component.translatable(Prefix + "holding").withStyle(ChatFormatting.RED));
        } else if (Flesh.sneaking()) {
            Tooltip.add(Component.translatable(Prefix + "creeping").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
