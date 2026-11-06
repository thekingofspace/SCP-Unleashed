package net.rai.scp_unleashed.scp217.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp217.Scp217Clockwork;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp217SignsProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    public static final int VisibleStage = 2;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp217_clockwork");
    private static final String DataKey = "Scp217Signs";
    private static final String PerfectKey = "Scp217Perfect";
    private static final String Prefix = "jade.scp_unleashed.scp217.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof LivingEntity Subject && Scp217Clockwork.stageOf(Subject) >= VisibleStage) {
            Data.putInt(DataKey, Scp217Clockwork.stageOf(Subject));
            if (Scp217Clockwork.isPerfect(Subject)) {
                Data.putBoolean(PerfectKey, true);
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        int Stage = Accessor.getServerData().getInt(DataKey);
        if (Stage >= 4 && Accessor.getServerData().getBoolean(PerfectKey)) {
            Tooltip.add(Component.translatable(Prefix + "perfect").withStyle(ChatFormatting.GOLD));
        } else if (Stage >= 4) {
            Tooltip.add(Component.translatable(Prefix + "winding").withStyle(ChatFormatting.RED));
        } else if (Stage == 3) {
            Tooltip.add(Component.translatable(Prefix + "clockwork").withStyle(ChatFormatting.GOLD));
        } else if (Stage == VisibleStage) {
            Tooltip.add(Component.translatable(Prefix + "ticking").withStyle(ChatFormatting.GRAY));
        }
    }
}
