package net.rai.scp_unleashed.scp149.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp149.Scp149Fly;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp149FlyJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp149_fly");
    private static final String TargetKey = "Scp149Target";
    private static final String Prefix = "jade.scp_unleashed.scp149_fly.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp149Fly Creature) {
            LivingEntity Target = Creature.getTarget();
            if (Target != null && Target.isAlive()) {
                Data.putString(TargetKey, Target.getDisplayName().getString());
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp149Fly Creature)) {
            return;
        }
        CompoundTag Data = Accessor.getServerData();
        if (Data.contains(TargetKey)) {
            Tooltip.add(Component.translatable(Prefix + "hunting", Data.getString(TargetKey)).withStyle(ChatFormatting.RED));
        }
        if (Creature.isRestless()) {
            Tooltip.add(Component.translatable(Prefix + "restless").withStyle(ChatFormatting.GOLD));
        } else if (Creature.isResting()) {
            Tooltip.add(Component.translatable(Prefix + "resting").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        } else if (Creature.isGorged()) {
            Tooltip.add(Component.translatable(Prefix + "gorged").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        } else {
            Tooltip.add(Component.translatable(Prefix + "searching").withStyle(ChatFormatting.GRAY));
        }
    }
}
