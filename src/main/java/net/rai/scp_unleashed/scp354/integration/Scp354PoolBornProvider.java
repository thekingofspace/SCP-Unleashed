package net.rai.scp_unleashed.scp354.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp354.Scp354Emergence;
import net.rai.scp_unleashed.scp354.Scp354Registry;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp354PoolBornProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = Scp354Registry.location("scp354_pool_born");
    private static final String BornKey = "Scp354PoolBorn";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Scp354Emergence.isPoolBorn(Accessor.getEntity())) {
            Data.putBoolean(BornKey, true);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (Accessor.getServerData().getBoolean(BornKey)) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp354.pool_born").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }
}
