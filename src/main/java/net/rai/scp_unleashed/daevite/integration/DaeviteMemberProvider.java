package net.rai.scp_unleashed.daevite.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.entity.DaeviteKnight;
import net.rai.scp_unleashed.daevite.entity.DaeviteMob;
import net.rai.scp_unleashed.daevite.entity.DaeviteOverseer;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DaeviteMemberProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_member");
    private static final String Prefix = "jade.scp_unleashed.daevite.";
    private static final String PatrolKey = "DaevitePatrol";
    private static final String DoomKey = "DaeviteDoom";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof DaeviteMob Member) {
            Data.putBoolean(PatrolKey, Member.onPatrol());
            Data.putBoolean(DoomKey, Member.doomed());
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof DaeviteMob Member)) {
            return;
        }
        CompoundTag Data = Accessor.getServerData();
        if (Member instanceof DaeviteKnight Knight) {
            Tooltip.add(Component.translatable(Prefix + (Knight.spearman() ? "spear" : "sword")).withStyle(ChatFormatting.GOLD));
        } else {
            Tooltip.add(Component.translatable(Prefix + "caster").withStyle(ChatFormatting.GOLD));
        }
        String Key = status(Member, Data.getBoolean(PatrolKey), Data.getBoolean(DoomKey));
        boolean Hostile = Key.equals("battle") || Key.equals("guard") || Key.equals("charging") || Key.equals("casting") || Key.equals("hunting");
        Tooltip.add(Component.translatable(Prefix + "status." + Key).withStyle(Hostile ? ChatFormatting.RED : ChatFormatting.GRAY));
    }

    private static String status(DaeviteMob Member, boolean Patrol, boolean Doomed) {
        if (Member.isDeadOrDying() || Doomed) {
            return "crumbling";
        }
        if (Member instanceof DaeviteOverseer Seer) {
            if (Seer.channeling()) {
                return "channelling";
            }
            if (Seer.mending()) {
                return "mending";
            }
            if (Seer.move() != DaeviteMob.MoveNone) {
                return "casting";
            }
            if (Seer.isAggressive()) {
                return "hunting";
            }
            return Patrol ? "marching" : "watching";
        }
        if (Member instanceof DaeviteKnight Knight) {
            if (Knight.guarding()) {
                return "guard";
            }
            if (Knight.charging()) {
                return "charging";
            }
        }
        if (Member.isAggressive()) {
            return "battle";
        }
        return Patrol ? "marching" : "watch";
    }
}
