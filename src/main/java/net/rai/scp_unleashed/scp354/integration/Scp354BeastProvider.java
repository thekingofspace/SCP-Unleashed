package net.rai.scp_unleashed.scp354.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp354.Scp354Beast;
import net.rai.scp_unleashed.scp354.Scp354Registry;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp354BeastProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = Scp354Registry.location("scp354_beast");
    private static final String TargetKey = "Scp354Target";
    private static final String Prefix = "jade.scp_unleashed.scp354.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp354Beast Creature) {
            LivingEntity Target = Creature.getTarget();
            if (Target != null && Target.isAlive()) {
                Data.putString(TargetKey, Target.getDisplayName().getString());
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp354Beast Creature)) {
            return;
        }
        if (Creature.isEmerging()) {
            Tooltip.add(Component.translatable(Prefix + "emerging").withStyle(ChatFormatting.GOLD));
            return;
        }
        CompoundTag Data = Accessor.getServerData();
        if (Data.contains(TargetKey)) {
            Tooltip.add(Component.translatable(Prefix + "hunting", Data.getString(TargetKey)).withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable(Prefix + "prowling").withStyle(ChatFormatting.GRAY));
        }
        if (Creature.dripTicks() > 0) {
            Tooltip.add(Component.translatable(Prefix + "dripping").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
