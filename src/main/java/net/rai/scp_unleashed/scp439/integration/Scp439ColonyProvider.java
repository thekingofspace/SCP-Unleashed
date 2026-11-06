package net.rai.scp_unleashed.scp439.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp439.Scp439Creature;
import net.rai.scp_unleashed.scp439.Scp439Queen;
import net.rai.scp_unleashed.scp439.Scp439Worker;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp439ColonyProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp439_colony");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp439Queen Queen) {
            Data.putBoolean("Scp439Wandering", Queen.stillTravelling());
        } else if (Accessor.getEntity() instanceof Scp439Creature Member) {
            Data.putBoolean("Scp439Bound", Member.hiveId() != null);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        String Prefix = Scp439HiveProvider.Prefix;
        if (Data.getBoolean("Scp439Wandering")) {
            Tooltip.add(Component.translatable(Prefix + "wandering").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        if (Data.contains("Scp439Bound")) {
            Tooltip.add(Component.translatable(Prefix + (Data.getBoolean("Scp439Bound") ? "bound" : "stray")).withStyle(ChatFormatting.GRAY));
        }
        if (Accessor.getEntity() instanceof Scp439Worker Worker && Worker.isCarrying()) {
            Tooltip.add(Component.translatable(Prefix + "carrying", Worker.carried().getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }
}
