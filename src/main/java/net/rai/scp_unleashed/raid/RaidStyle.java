package net.rai.scp_unleashed.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.raid.objective.RaidObjective;

import java.util.List;

public interface RaidStyle {
    ResourceLocation id();

    default int warningTicks() {
        return 400;
    }

    default long durationTicks() {
        return 6000L;
    }

    default long retreatTicks() {
        return 300L;
    }

    default long cooldownTicks() {
        return 48000L;
    }

    default int maxAlive() {
        return RaidRules.MaxAlive;
    }

    default double minConfidence() {
        return RaidRules.MinConfidence;
    }

    default boolean allowsDimension(ResourceKey<Level> Dimension) {
        return true;
    }

    default double heading(ServerLevel World, FactionType Faction, FactionState State, BlockPos Target) {
        return World.random.nextDouble() * Math.PI * 2.0;
    }

    default List<RaidObjective> objectives() {
        return List.of();
    }

    default String noticeKey(String Event) {
        return "raid.scp_unleashed." + Event;
    }

    default void warn(RaidContext Context) {
    }

    default void tickWarning(RaidContext Context) {
    }

    boolean begin(RaidContext Context);

    void tick(RaidContext Context);

    default boolean exhausted(RaidContext Context) {
        return true;
    }

    default void raiderLost(RaidContext Context, LivingEntity Raider, DamageSource Source, boolean WasCaster) {
    }

    default void retreat(RaidContext Context) {
        for (Mob Raider : Context.loadedRaiders()) {
            Raider.setTarget(null);
        }
    }

    default void tickRetreat(RaidContext Context) {
    }

    default void dismiss(ServerLevel World, Mob Raider) {
        Raider.discard();
    }

    default void ended(RaidContext Context, RaidOutcome Outcome) {
    }
}
