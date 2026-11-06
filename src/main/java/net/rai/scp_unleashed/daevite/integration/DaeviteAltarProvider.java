package net.rai.scp_unleashed.daevite.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.DaeviteFaction;
import net.rai.scp_unleashed.daevite.DaeviteRitual;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DaeviteAltarProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_altar");
    private static final String Prefix = "jade.scp_unleashed.daevite_altar.";
    private static final String MoodKey = "DaeviteMood";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        FactionState State = FactionControl.state(World.getServer(), DaeviteFaction.Instance);
        Data.putString(MoodKey, mood(State));
    }

    private static String mood(FactionState State) {
        if (State.phase() == FactionPhase.Defeated) {
            return "ash";
        }
        if (State.phase() == FactionPhase.Dormant) {
            return "cold";
        }
        if (State.phase() == FactionPhase.Ascendant) {
            return "answered";
        }
        if (DaeviteRitual.active(State)) {
            return "rite";
        }
        double Share = State.power() / DaeviteFaction.MaxPower;
        if (Share < 0.35) {
            return "stirring";
        }
        if (Share < 0.75) {
            return "smouldering";
        }
        return "hungry";
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        String Mood = Accessor.getServerData().getString(MoodKey);
        if (Mood.isEmpty()) {
            return;
        }
        boolean Danger = Mood.equals("hungry") || Mood.equals("rite") || Mood.equals("answered");
        Tooltip.add(Component.translatable(Prefix + Mood).withStyle(Danger ? ChatFormatting.RED : ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
