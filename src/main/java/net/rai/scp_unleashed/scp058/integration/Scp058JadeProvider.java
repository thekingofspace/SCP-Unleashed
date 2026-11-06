package net.rai.scp_unleashed.scp058.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp058.Scp058Heart;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp058JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp058_heart");
    private static final String TargetKey = "Scp058Target";
    private static final String Prefix = "jade.scp_unleashed.scp058.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp058Heart Creature) {
            LivingEntity Target = Creature.getTarget();
            if (Target != null && Target.isAlive()) {
                Data.putString(TargetKey, Target.getDisplayName().getString());
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp058Heart Creature)) {
            return;
        }
        if (Creature.isEmerging()) {
            Tooltip.add(Component.translatable(Prefix + "emerging").withStyle(ChatFormatting.GOLD));
        }
        CompoundTag Data = Accessor.getServerData();
        if (Data.contains(TargetKey)) {
            Tooltip.add(Component.translatable(Prefix + "hunting", Data.getString(TargetKey)).withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable(Prefix + "restless").withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable(Prefix + "pulse", Creature.heartRate()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
