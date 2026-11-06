package net.rai.scp_unleashed.scp253.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp253.Scp253Display;
import net.rai.scp_unleashed.scp253.Scp253Growths;
import net.rai.scp_unleashed.scp253.Scp253Registry;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp253GrowthProvider implements IEntityComponentProvider {
    Instance;

    public static final int Spreading = 6;
    public static final int Covered = 15;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp253_growths");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof LivingEntity Subject) || Subject.isInvisible()) {
            return;
        }
        Scp253Display Shown = Subject.getExistingDataOrNull(Scp253Registry.Display.get());
        if (Shown == null) {
            return;
        }
        int Visible = Scp253Growths.count(Shown.progressAt(Subject.level().getGameTime(), 0.0f));
        if (Visible <= 0) {
            return;
        }
        String Key = Visible >= Covered ? "covered" : Visible >= Spreading ? "spreading" : "few";
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp253." + Key).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
    }
}
