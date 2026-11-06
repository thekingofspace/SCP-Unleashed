package net.rai.scp_unleashed.scp427;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Scp427Transformation {
    public static final int Length = 50;
    public static final int RetryInterval = 20;
    public static final int FleshInterval = 3;
    public static final int SlowAmplifier = 3;
    private static final Set<UUID> SpawnedThisTick = new HashSet<>();
    private static final List<LivingEntity> Vanishing = new ArrayList<>();
    private static int Spawned;

    private Scp427Transformation() {
    }

    public static int spawnedTotal() {
        return Spawned;
    }

    public static void begin(ServerLevel World, LivingEntity Subject, Scp427State State) {
        State.Transform = 1;
        State.OpenTicks = 0;
        if (Subject.isUsingItem() && Scp427LocketItem.is(Subject.getUseItem())) {
            Subject.stopUsingItem();
        }
        Scp427Light.release(Subject);
        if (Subject instanceof Mob Body) {
            Body.getNavigation().stop();
        }
        Subject.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Length + 20, SlowAmplifier, false, false, false));
        Scp427Registry.play(World, Subject.position().add(0.0, Subject.getBbHeight() * 0.6, 0.0), Scp427Registry.TransformLayers, SoundSource.PLAYERS, 1.1f, 0.9f);
        Scp427Sync.convulse(Subject, true);
    }

    public static void abort(LivingEntity Subject, Scp427State State) {
        State.Transform = 0;
        State.Ticks = Math.min(State.Ticks, Scp427Exposure.Limit);
        Subject.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        Scp427Sync.convulse(Subject, false);
    }

    public static void tick(ServerLevel World, LivingEntity Subject, Scp427State State) {
        if (Targeting.isStatue(Subject)) {
            return;
        }
        if (Scp427Exposure.isExempt(Subject)) {
            abort(Subject, State);
            return;
        }
        if (Subject instanceof ServerPlayer User && ControlManager.hasPendingDeath(User)) {
            return;
        }
        State.Transform++;
        int Elapsed = State.Transform;
        if (Subject.isUsingItem() && Scp427LocketItem.is(Subject.getUseItem())) {
            Subject.stopUsingItem();
        }
        if (Subject instanceof Mob Body) {
            Body.getNavigation().stop();
        }
        Vec3 Middle = Subject.position().add(0.0, Subject.getBbHeight() * 0.55, 0.0);
        if (Elapsed % FleshInterval == 0 && Elapsed <= Length) {
            int Count = 3 + Elapsed / 12;
            World.sendParticles(Scp427Registry.FleshParticle.get(), Middle.x, Middle.y, Middle.z, Count, Subject.getBbWidth() * 0.4, Subject.getBbHeight() * 0.3,
                    Subject.getBbWidth() * 0.4, 0.06);
        }
        if (Elapsed == 18 || Elapsed == 34) {
            World.playSound(null, Middle.x, Middle.y, Middle.z, Scp427Registry.TransformCreak.get(), SoundSource.PLAYERS, 1.0f, 0.8f + World.getRandom().nextFloat() * 0.2f);
        }
        if (Elapsed == Length - 6) {
            World.playSound(null, Middle.x, Middle.y, Middle.z, Scp427Registry.TransformRoar.get(), SoundSource.HOSTILE, 1.3f, 0.85f);
        }
        if (Elapsed == Length || Elapsed > Length && (Elapsed - Length) % RetryInterval == 0) {
            strike(Subject);
        }
    }

    public static void strike(LivingEntity Subject) {
        Subject.hurt(Subject.damageSources().source(Scp427Registry.Overcome), Float.MAX_VALUE);
    }

    public static void died(LivingDeathEvent Event) {
        LivingEntity Dead = Event.getEntity();
        if (!(Dead.level() instanceof ServerLevel World) || !Event.getSource().is(Scp427Registry.Overcome) || Dead instanceof Scp427Flesh) {
            return;
        }
        if (!SpawnedThisTick.add(Dead.getUUID())) {
            return;
        }
        Scp427Sync.vanish(Dead);
        Scp427Light.release(Dead);
        Scp427Flesh Monster = spawn(World, Dead.position(), Dead.getYRot());
        if (Monster != null) {
            Spawned++;
        }
        Vec3 Middle = Dead.position().add(0.0, Dead.getBbHeight() * 0.5, 0.0);
        World.sendParticles(Scp427Registry.FleshParticle.get(), Middle.x, Middle.y, Middle.z, 28, 0.5, 0.6, 0.5, 0.12);
        World.sendParticles(ParticleTypes.POOF, Middle.x, Middle.y, Middle.z, 10, 0.4, 0.5, 0.4, 0.02);
        if (Dead instanceof ServerPlayer User) {
            dropLockets(User);
        } else {
            Vanishing.add(Dead);
        }
    }

    @Nullable
    public static Scp427Flesh spawn(ServerLevel World, Vec3 At, float Yaw) {
        Scp427Flesh Monster = Scp427Registry.Flesh.get().create(World);
        if (Monster == null) {
            return null;
        }
        Monster.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Monster.setYHeadRot(Yaw);
        Monster.setYBodyRot(Yaw);
        EventHooks.finalizeMobSpawn(Monster, World, World.getCurrentDifficultyAt(BlockPos.containing(At)), MobSpawnType.CONVERSION, null);
        Monster.emerge();
        return World.addFreshEntity(Monster) ? Monster : null;
    }

    public static int dropLockets(ServerPlayer User) {
        Inventory Pockets = User.getInventory();
        int Dropped = 0;
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Scp427LocketItem.is(Stack)) {
                Pockets.setItem(Slot, ItemStack.EMPTY);
                User.drop(Stack, true, false);
                Dropped++;
            }
        }
        return Dropped;
    }

    public static void endTick(MinecraftServer Server) {
        SpawnedThisTick.clear();
        if (Vanishing.isEmpty()) {
            return;
        }
        for (LivingEntity Corpse : List.copyOf(Vanishing)) {
            if (!Corpse.isRemoved() && Corpse.level() instanceof ServerLevel World) {
                World.sendParticles(ParticleTypes.POOF, Corpse.getX(), Corpse.getY() + Corpse.getBbHeight() * 0.5, Corpse.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
                Corpse.discard();
            }
        }
        Vanishing.clear();
    }
}
