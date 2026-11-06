package net.rai.scp_unleashed.scp999.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp999.Scp999Blob;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp999JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp999");
    private static final String Prefix = "jade.scp_unleashed.scp999.";
    private static final String FondKey = "Scp999Fondness";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public int getDefaultPriority() {
        return Priority;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp999Blob Blob && Accessor.getPlayer() != null) {
            Data.putInt(FondKey, Blob.affection(Accessor.getPlayer().getUUID()));
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp999Blob Blob)) {
            return;
        }
        if (Blob.isFlat()) {
            Tooltip.add(Component.translatable(Prefix + "hiding").withStyle(ChatFormatting.GOLD));
        } else if (Blob.isNapping()) {
            Tooltip.add(Component.translatable(Prefix + "napping").withStyle(ChatFormatting.GRAY));
        } else {
            Tooltip.add(Component.translatable(Prefix + "happy").withStyle(ChatFormatting.GREEN));
        }
        CompoundTag Data = Accessor.getServerData();
        if (Data.contains(FondKey)) {
            int Fondness = Data.getInt(FondKey);
            if (Fondness >= Scp999Blob.LikeThreshold) {
                Tooltip.add(Component.translatable(Prefix + "fond").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (Fondness < 0) {
                Tooltip.add(Component.translatable(Prefix + "wary").withStyle(ChatFormatting.GRAY));
            }
        }
        if (Blob.getHealth() < Blob.getMaxHealth()) {
            Tooltip.add(Component.translatable(Prefix + "squished").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
