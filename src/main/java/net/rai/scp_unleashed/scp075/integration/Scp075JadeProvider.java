package net.rai.scp_unleashed.scp075.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp075.Scp075Snail;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp075JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp075_snail");
    private static final String TargetKey = "Scp075Target";
    private static final String Prefix = "jade.scp_unleashed.scp075.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp075Snail Creature) {
            LivingEntity Target = Creature.getTarget();
            if (Target != null && Target.isAlive() && Creature.canHunt()) {
                Data.putString(TargetKey, Target.getDisplayName().getString());
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp075Snail Creature)) {
            return;
        }
        if (Creature.isBaby()) {
            Tooltip.add(Component.translatable(Prefix + "juvenile").withStyle(ChatFormatting.YELLOW));
        }
        CompoundTag Data = Accessor.getServerData();
        if (Data.contains(TargetKey)) {
            Tooltip.add(Component.translatable(Prefix + "hunting", Data.getString(TargetKey)).withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable(Prefix + "creeping").withStyle(ChatFormatting.GRAY));
        }
    }
}
