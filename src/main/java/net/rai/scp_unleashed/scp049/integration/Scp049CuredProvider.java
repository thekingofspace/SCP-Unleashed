package net.rai.scp_unleashed.scp049.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp049.Scp049Cured;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp049CuredProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_cured");
    private static final String DataKey = "Scp049Cured";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp049Cured Risen) {
            CompoundTag Details = new CompoundTag();
            Details.putBoolean("Masterless", Risen.isMasterless() || Risen.master() == null);
            Details.putBoolean("Ordered", Risen.orderTarget() != null);
            Data.put(DataKey, Details);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp049Cured Risen)) {
            return;
        }
        String Origin = switch (Risen.lookKind()) {
            case Scp049Cured.LookVillager -> "villager";
            case Scp049Cured.LookPlayer -> "player";
            case Scp049Cured.LookMimic -> "person";
            default -> "unknown";
        };
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp049_2.origin." + Origin).withStyle(ChatFormatting.GRAY));
        CompoundTag Data = Accessor.getServerData().getCompound(DataKey);
        if (Data.isEmpty()) {
            return;
        }
        if (Data.getBoolean("Masterless")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp049_2.masterless").withStyle(ChatFormatting.DARK_RED));
        } else if (Data.getBoolean("Ordered")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp049_2.ordered").withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp049_2.guarding").withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
