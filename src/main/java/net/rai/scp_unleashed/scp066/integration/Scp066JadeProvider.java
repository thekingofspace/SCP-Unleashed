package net.rai.scp_unleashed.scp066.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp066.Scp066Entity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp066JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp066");
    private static final String Prefix = "jade.scp_unleashed.scp066.";
    private static final String DataKey = "Scp066Treat";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp066Entity Heap && Heap.isCalm() && Accessor.getPlayer() != null) {
            Data.putInt(DataKey, Heap.treatSeconds(Accessor.getPlayer()));
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp066Entity Heap)) {
            return;
        }
        if (Heap.isHostile()) {
            Tooltip.add(Component.translatable(Prefix + (Heap.getTarget() != null ? "hunting" : "hostile")).withStyle(ChatFormatting.RED));
            Tooltip.add(Component.translatable(Prefix + "permanent").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            return;
        }
        if (Heap.isChanging()) {
            Tooltip.add(Component.translatable(Prefix + "changing").withStyle(ChatFormatting.GOLD));
            return;
        }
        Tooltip.add(Component.translatable(Prefix + (Heap.isSinging() ? "singing" : "calm")).withStyle(ChatFormatting.LIGHT_PURPLE));
        CompoundTag Data = Accessor.getServerData();
        if (Data.contains(DataKey)) {
            int Seconds = Data.getInt(DataKey);
            Tooltip.add(Seconds > 0 ? Component.translatable(Prefix + "treat_wait", Seconds).withStyle(ChatFormatting.GRAY)
                    : Component.translatable(Prefix + "treat_ready").withStyle(ChatFormatting.GREEN));
        }
        Tooltip.add(Component.translatable(Prefix + "warning").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
