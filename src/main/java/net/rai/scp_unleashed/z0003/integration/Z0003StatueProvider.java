package net.rai.scp_unleashed.z0003.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0003.Z0003Petrify;
import net.rai.scp_unleashed.z0003.Z0003Statue;
import net.rai.scp_unleashed.z0003.Z0003VialItem;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Z0003StatueProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.z0003.";
    public static final String ClutchKey = "Z0003Clutching";
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0003_statue");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        Z0003Statue Statue = Z0003Petrify.statueOf(Accessor.getEntity());
        if (Statue == null) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + (Statue.cracking() ? "cracking" : "statue")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (Accessor.getEntity() instanceof Player && Accessor.getServerData().getBoolean(ClutchKey)) {
            Tooltip.add(Component.translatable(Prefix + "clutching").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Player Holder && Z0003Petrify.isStatue(Holder) && Z0003VialItem.carried(Holder)) {
            Data.putBoolean(ClutchKey, true);
        }
    }
}
