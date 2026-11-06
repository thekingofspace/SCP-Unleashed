package net.rai.scp_unleashed.scp016.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp016.Scp016Spawn;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp016SpawnProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp016_instance");
    private static final String RemainingKey = "Scp016Remaining";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp016Spawn Creature) {
            Data.putInt(RemainingKey, Creature.getRemaining());
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp016Spawn Creature)) {
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp016." + Creature.variant()).withStyle(ChatFormatting.RED));
        if (Creature.isEmerging()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp016.emerging").withStyle(ChatFormatting.GOLD));
        }
        if (Creature.isCollapsing()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp016.collapsing").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            return;
        }
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(RemainingKey)) {
            return;
        }
        int Minutes = Data.getInt(RemainingKey) / 1200;
        Tooltip.add(Component.translatable(Minutes > 0 ? "jade.scp_unleashed.scp016.minutes" : "jade.scp_unleashed.scp016.soon", Minutes)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
