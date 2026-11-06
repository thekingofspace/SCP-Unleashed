package net.rai.scp_unleashed.scp3008.staff.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.staff.Scp3008Staff;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp3008StaffJadeProvider implements IEntityComponentProvider {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp3008_staff");
    private static final String Prefix = "jade.scp_unleashed.scp3008_staff.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public int getDefaultPriority() {
        return Priority;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp3008Staff Staff)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "variant." + Staff.variant().Name).withStyle(ChatFormatting.GRAY));
        if (Staff.isHostile()) {
            Tooltip.add(Component.translatable(Prefix + "closed").withStyle(ChatFormatting.RED));
        } else if (Staff.isAggressive()) {
            Tooltip.add(Component.translatable(Prefix + "agitated").withStyle(ChatFormatting.RED));
        } else {
            switch (Staff.activity()) {
                case Scp3008Staff.ActivityTidy -> Tooltip.add(Component.translatable(Prefix + "tidying").withStyle(ChatFormatting.GOLD));
                case Scp3008Staff.ActivityStare -> Tooltip.add(Component.translatable(Prefix + "watching").withStyle(ChatFormatting.GOLD));
                default -> Tooltip.add(Component.translatable(Prefix + "on_shift").withStyle(ChatFormatting.YELLOW));
            }
        }
    }
}
