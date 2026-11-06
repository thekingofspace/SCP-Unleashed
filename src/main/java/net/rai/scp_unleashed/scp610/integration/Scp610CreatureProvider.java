package net.rai.scp_unleashed.scp610.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Growth;
import net.rai.scp_unleashed.scp610.Scp610Hive;
import net.rai.scp_unleashed.scp610.Scp610HiveLord;
import net.rai.scp_unleashed.scp610.Scp610Hives;
import net.rai.scp_unleashed.scp610.Scp610Pact;
import net.rai.scp_unleashed.scp610.Scp610Spewer;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp610CreatureProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_creature");
    private static final String MoodKey = "Scp610Mood";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        FactionState State = Scp610Hives.state(World.getServer());
        if (Accessor.getEntity() instanceof Scp610Spewer Spewer) {
            Scp610Hive Hive = Scp610Hives.hive(World.getServer(), Spewer.hive());
            String Mood = Hive == null || Scp610Growth.halted(World.getServer(), State) ? "dormant" : Hive.Plan.isEmpty() && Hive.flesh() < 64 ? "swelling" : "spreading";
            Data.putString(MoodKey, Mood);
        } else if (Accessor.getEntity() instanceof Scp610HiveLord Lord) {
            long Now = World.getServer().overworld().getGameTime();
            String Mood = Lord.agitated() ? "agitated" : Scp610Pact.sated(State, Now) ? "sated" : Lord.watched() != null ? "watching" : "calm";
            Data.putString(MoodKey, Mood);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp610Creature Creature)) {
            return;
        }
        Scp610Form Form = Creature.form();
        CompoundTag Data = Accessor.getServerData();
        if (Creature instanceof Scp610Spewer && Data.contains(MoodKey)) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp610.spewer." + Data.getString(MoodKey)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            return;
        }
        if (Creature instanceof Scp610HiveLord && Data.contains(MoodKey)) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp610.lord." + Data.getString(MoodKey)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp610.form." + Form.id()).withStyle(ChatFormatting.RED));
        if (Creature.isEmerging()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp610.emerging").withStyle(ChatFormatting.GOLD));
        }
    }
}
