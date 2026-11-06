package net.rai.scp_unleashed.scp352.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp352.Scp352Crone;
import net.rai.scp_unleashed.scp352.Scp352Phase;
import net.rai.scp_unleashed.scp352.Scp352Rest;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp352JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp352_crone");
    private static final String LeftKey = "Scp352RestLeft";
    private static final String Prefix = "jade.scp_unleashed.scp352.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp352Crone Creature && Creature.phase() == Scp352Phase.Rest) {
            Data.putLong(LeftKey, Creature.restLeft());
        }
    }

    public static String state(Scp352Phase Phase) {
        return switch (Phase) {
            case Wander -> "wandering";
            case Transform, Rouse -> "rising";
            case Hunt -> "hunting";
            case Fold -> "folding";
            case Seek, Settle -> "settling";
            case Rest -> "resting";
            case Wake -> "waking";
        };
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp352Crone Creature)) {
            return;
        }
        Scp352Phase Phase = Creature.phase();
        ChatFormatting Colour = Phase == Scp352Phase.Rest ? ChatFormatting.DARK_AQUA : Phase == Scp352Phase.Wander ? ChatFormatting.GRAY : ChatFormatting.RED;
        Tooltip.add(Component.translatable(Prefix + "state", Component.translatable(Prefix + "state." + state(Phase)).withStyle(Colour)).withStyle(ChatFormatting.GRAY));
        CompoundTag Data = Accessor.getServerData();
        if (Phase == Scp352Phase.Rest && Data.contains(LeftKey)) {
            Tooltip.add(Component.translatable(Prefix + "sleep." + Scp352Rest.depth(Data.getLong(LeftKey))).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
