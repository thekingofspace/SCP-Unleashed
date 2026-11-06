package net.rai.scp_unleashed.scp009.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp009.Scp009Doom;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DoomJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp009_doom");
    private static final String DataKey = "Scp009Doom";
    private static final String Prefix = "jade.scp_unleashed.scp009_doom";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (!(Accessor.getEntity() instanceof LivingEntity Target)) {
            return;
        }
        MobEffectInstance Doom = Scp009Doom.instance(Target);
        if (Doom == null) {
            return;
        }
        CompoundTag Details = new CompoundTag();
        float Progress = Scp009Doom.progress(Doom);
        Details.putInt("Stage", Scp009Doom.stage(Progress));
        Details.putInt("Seconds", Doom.isInfiniteDuration() ? -1 : (Doom.getDuration() + 19) / 20);
        Details.putInt("Slower", (int) Math.round(Scp009Doom.slowFraction(Progress) * 100.0));
        Data.put(DataKey, Details);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        CompoundTag Details = Accessor.getServerData().getCompound(DataKey);
        if (Details.isEmpty()) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix, Details.getInt("Stage"), Scp009Doom.Stages).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        int Seconds = Details.getInt("Seconds");
        if (Seconds >= 0) {
            Tooltip.add(Component.translatable(Prefix + ".time", Seconds, Details.getInt("Slower")).withStyle(ChatFormatting.RED));
        }
    }
}
