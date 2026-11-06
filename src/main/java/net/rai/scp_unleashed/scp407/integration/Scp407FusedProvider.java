package net.rai.scp_unleashed.scp407.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp407.Scp407Hearing;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407Sessions;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp407FusedProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = Scp407Registry.location("scp407_fused");
    private static final String FusedKey = "Scp407Fused";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof LivingEntity Living && Living.level() instanceof ServerLevel World && Scp407Hearing.counts(Living)
                && Scp407Sessions.get(World).shielding(Scp407Hearing.keyOf(Living)) != null) {
            Data.putBoolean(FusedKey, true);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (Accessor.getServerData().getBoolean(FusedKey)) {
            Tooltip.add(Component.translatable(Scp407SpeakerProvider.Prefix + "fused").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        }
    }
}
