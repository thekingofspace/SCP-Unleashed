package net.rai.scp_unleashed.grenade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.gas.GasRegistry;

public final class GrenadeGas {
    public static final double GasLift = 0.3;
    public static final Direction[] Drift = {Direction.UP, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};

    private GrenadeGas() {
    }

    public static void detonate(ServerLevel World, GrenadeEntity Grenade, boolean Airless) {
        Vec3 Centre = Grenade.position().add(0.0, GasLift, 0.0);
        GrenadeSounds.play(World, Centre, GrenadeRegistry.GasBurst, SoundSource.BLOCKS, 1.0F, 1.0F);
        GrenadeSounds.play(World, Centre, GrenadeRegistry.GasSpit, SoundSource.BLOCKS, 1.0F, 0.9F);
        if (Airless) {
            World.sendParticles(FissionRegistry.ToxicGasParticle.get(), Centre.x, Centre.y, Centre.z, 40, 0.2, 0.2, 0.2, 0.45);
            World.sendParticles(ParticleTypes.POOF, Centre.x, Centre.y, Centre.z, 10, 0.15, 0.15, 0.15, 0.2);
            Grenade.discard();
            return;
        }
        World.sendParticles(FissionRegistry.ToxicGasParticle.get(), Centre.x, Centre.y, Centre.z, 24, 0.3, 0.2, 0.3, 0.08);
        Grenade.enterPhase(GrenadeEntity.PhaseVenting, GrenadeTuning.ReleaseTicks);
        puff(World, Grenade);
    }

    public static void ventTick(ServerLevel World, GrenadeEntity Grenade) {
        int Age = GrenadeTuning.ReleaseTicks - Grenade.getPhaseTicks();
        if (Age > 0 && Age % GrenadeTuning.PuffInterval == 0) {
            puff(World, Grenade);
        }
        if (Age % GrenadeTuning.SpitInterval == 0) {
            float Left = Grenade.getPhaseTicks() / (float) GrenadeTuning.ReleaseTicks;
            GrenadeSounds.play(World, Grenade.position().add(0.0, GasLift, 0.0), GrenadeRegistry.GasSpit, SoundSource.BLOCKS, 0.35F + 0.4F * Left, 1.0F);
        }
    }

    public static void puff(ServerLevel World, GrenadeEntity Grenade) {
        Vec3 Spot = wander(World, Grenade);
        GasRegistry.ToxicGas.get().release(World, Spot, GrenadeTuning.GasPerPuff);
    }

    public static Vec3 wander(ServerLevel World, GrenadeEntity Grenade) {
        RandomSource Random = World.getRandom();
        Vec3 Start = Grenade.position().add(0.0, GasLift, 0.0);
        BlockPos Origin = BlockPos.containing(Start);
        BlockPos Pos = Origin;
        int Steps = 1 + Random.nextInt(GrenadeTuning.GasWanderSteps);
        for (int Step = 0; Step < Steps; Step++) {
            BlockPos Next = Pos.relative(Drift[Random.nextInt(Drift.length)]);
            if (Vec3.atCenterOf(Next).distanceTo(Start) > GrenadeTuning.GasReach || !open(World, Next)) {
                continue;
            }
            Pos = Next;
        }
        if (Pos.equals(Origin)) {
            return Start;
        }
        return Vec3.atCenterOf(Pos).add((Random.nextDouble() - 0.5) * 0.6, (Random.nextDouble() - 0.5) * 0.6, (Random.nextDouble() - 0.5) * 0.6);
    }

    public static boolean open(ServerLevel World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            return false;
        }
        BlockState State = World.getBlockState(Pos);
        return State.getCollisionShape(World, Pos).isEmpty() && State.getFluidState().isEmpty();
    }
}
