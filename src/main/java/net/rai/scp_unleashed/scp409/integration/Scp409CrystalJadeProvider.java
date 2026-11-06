package net.rai.scp_unleashed.scp409.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp409.Scp409Infection;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp409CrystalJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp409_crystal");
    private static final String DataKey = "Scp409Crystal";
    private static final String Prefix = "jade.scp_unleashed.scp409_crystal";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof LivingEntity Target && Scp409Infection.isInfected(Target)) {
            Data.putInt(DataKey, Scp409Infection.stageOf(Target));
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        int Stage = Accessor.getServerData().getInt(DataKey);
        if (Stage <= 0) {
            return;
        }
        ChatFormatting Colour = Stage >= Scp409Infection.Stages ? ChatFormatting.RED : ChatFormatting.AQUA;
        Tooltip.add(Component.translatable(Prefix, Stage, Scp409Infection.Stages).withStyle(Colour));
    }
}
