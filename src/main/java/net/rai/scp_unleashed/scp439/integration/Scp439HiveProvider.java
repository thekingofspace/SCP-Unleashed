package net.rai.scp_unleashed.scp439.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp439.Scp439Colonies;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp439HiveProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.scp439.";
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp439_hive");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp439Hive Hive && Hive.level() instanceof ServerLevel World) {
            Data.putInt("Scp439Stage", Hive.stage());
            Data.putBoolean("Scp439Seeking", Hive.seeking());
            Data.putInt("Scp439Food", Hive.food());
            Data.putInt("Scp439Workers", Hive.count(World, Scp439Colonies.Workers));
            Data.putInt("Scp439Brutes", Hive.count(World, Scp439Colonies.Brutes));
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp439Hive Hive)) {
            return;
        }
        Component Named = Hive.look().name(Hive.registryAccess());
        if (Named != null) {
            Tooltip.add(Component.translatable(Prefix + "host", Named).withStyle(ChatFormatting.GRAY));
        }
        CompoundTag Data = Accessor.getServerData();
        if (Data.getBoolean("Scp439Seeking")) {
            Tooltip.add(Component.translatable(Prefix + "seeking").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        } else if (Data.contains("Scp439Stage")) {
            Tooltip.add(Component.translatable(Prefix + "growth", Data.getInt("Scp439Stage"), Scp439Hive.Tiers).withStyle(ChatFormatting.GRAY));
        }
        if (Data.contains("Scp439Food")) {
            Tooltip.add(Component.translatable(Prefix + "food", Data.getInt("Scp439Food")).withStyle(ChatFormatting.GRAY));
            Tooltip.add(Component.translatable(Prefix + "members", Data.getInt("Scp439Workers"), Data.getInt("Scp439Brutes")).withStyle(ChatFormatting.GRAY));
        }
    }
}
