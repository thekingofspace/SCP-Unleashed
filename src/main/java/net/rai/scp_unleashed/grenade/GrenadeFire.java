package net.rai.scp_unleashed.grenade;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.rai.scp_unleashed.space.SpaceRegistry;

public final class GrenadeFire {
    public static final String NoFireKey = "message.scp_unleashed.space.no_fire";
    public static final double FireLift = 0.3;
    public static final double MessageReach = 32.0;

    private GrenadeFire() {
    }

    public static void detonate(ServerLevel World, GrenadeEntity Grenade, boolean Airless) {
        Vec3 Centre = Grenade.position().add(0.0, FireLift, 0.0);
        if (Airless || Grenade.isInWater()) {
            snuff(World, Grenade, Centre, Airless);
            Grenade.discard();
            return;
        }
        GrenadeSounds.play(World, Centre, GrenadeRegistry.IncendiaryWhoosh, SoundSource.BLOCKS, 1.0F, 1.0F);
        GrenadeSounds.play(World, Centre, GrenadeRegistry.IncendiaryIgnite, SoundSource.BLOCKS, 1.0F, 1.0F);
        World.sendParticles(ParticleTypes.FLAME, Centre.x, Centre.y, Centre.z, 70, 0.6, 0.3, 0.6, 0.12);
        World.sendParticles(ParticleTypes.LAVA, Centre.x, Centre.y, Centre.z, 14, 0.4, 0.2, 0.4, 0.0);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Centre.y + 0.4, Centre.z, 12, 0.7, 0.4, 0.7, 0.02);
        if (EventHooks.canEntityGrief(World, Grenade)) {
            spread(World, Grenade, Centre);
        }
        ignite(World, Grenade, Centre, GrenadeTuning.BurstIgniteSeconds, true);
        Grenade.enterPhase(GrenadeEntity.PhaseBurning, GrenadeTuning.BurnTicks);
    }

    public static void burnTick(ServerLevel World, GrenadeEntity Grenade) {
        Vec3 Centre = Grenade.position().add(0.0, FireLift, 0.0);
        if (Grenade.isInWater()) {
            snuff(World, Grenade, Centre, false);
            Grenade.discard();
            return;
        }
        int Age = GrenadeTuning.BurnTicks - Grenade.getPhaseTicks();
        if (Age % GrenadeTuning.IgniteInterval == 0) {
            ignite(World, Grenade, Centre, GrenadeTuning.LingerIgniteSeconds, false);
        }
        if (Age % GrenadeTuning.BurnSoundInterval == 0) {
            float Left = Grenade.getPhaseTicks() / (float) GrenadeTuning.BurnTicks;
            GrenadeSounds.play(World, Centre, GrenadeRegistry.IncendiaryBurn, SoundSource.BLOCKS, 0.5F + 0.5F * Left, 1.0F);
        }
    }

    public static void spread(ServerLevel World, GrenadeEntity Grenade, Vec3 Centre) {
        RandomSource Random = World.getRandom();
        double Radius = GrenadeTuning.FireRadius;
        int Reach = Mth.ceil(Radius);
        BlockPos Origin = BlockPos.containing(Centre);
        for (BlockPos Pos : BlockPos.betweenClosed(Origin.offset(-Reach, -Reach, -Reach), Origin.offset(Reach, Reach, Reach))) {
            double Distance = Vec3.atCenterOf(Pos).distanceTo(Centre);
            if (Distance > Radius) {
                continue;
            }
            float Chance = Mth.lerp((float) (Distance / Radius), GrenadeTuning.FireChanceNear, GrenadeTuning.FireChanceFar);
            if (Random.nextFloat() > Chance) {
                continue;
            }
            BlockState State = World.getBlockState(Pos);
            if (lightable(State)) {
                if (GrenadeSight.reaches(World, Grenade, Centre, Pos)) {
                    World.setBlock(Pos, State.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
                }
                continue;
            }
            if (!State.isAir()) {
                continue;
            }
            BlockState Fire = BaseFireBlock.getState(World, Pos);
            if (!Fire.canSurvive(World, Pos) || !GrenadeSight.reaches(World, Grenade, Centre, Pos)) {
                continue;
            }
            World.setBlock(Pos, Fire, Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    public static boolean lightable(BlockState State) {
        return CampfireBlock.canLight(State) || CandleBlock.canLight(State) || CandleCakeBlock.canLight(State);
    }

    public static void ignite(ServerLevel World, GrenadeEntity Grenade, Vec3 Centre, float Seconds, boolean Burst) {
        double Radius = GrenadeTuning.FireRadius;
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, new AABB(Centre, Centre).inflate(Radius + 1.0),
                Living -> Living.isAlive() && !Living.isSpectator())) {
            AABB Box = Target.getBoundingBox();
            Vec3 Nearest = new Vec3(Mth.clamp(Centre.x, Box.minX, Box.maxX), Mth.clamp(Centre.y, Box.minY, Box.maxY), Mth.clamp(Centre.z, Box.minZ, Box.maxZ));
            if (Nearest.distanceTo(Centre) > Radius || Target.isInWaterRainOrBubble() || Target.fireImmune()) {
                continue;
            }
            Vec3 Middle = Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0);
            if (!GrenadeSight.clear(World, Grenade, Centre, Middle) && !GrenadeSight.clear(World, Grenade, Centre, Target.getEyePosition())) {
                continue;
            }
            Target.igniteForSeconds(Seconds);
            if (Burst) {
                Target.invulnerableTime = 0;
                Target.hurt(World.damageSources().inFire(), GrenadeTuning.BurstDamage);
            }
        }
    }

    public static void snuff(ServerLevel World, GrenadeEntity Grenade, Vec3 Centre, boolean Airless) {
        GrenadeSounds.fizzle(World, Centre);
        World.playSound(null, Centre.x, Centre.y, Centre.z, SpaceRegistry.FlameSnuff.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        World.sendParticles(ParticleTypes.SMOKE, Centre.x, Centre.y, Centre.z, 16, 0.25, 0.15, 0.25, 0.02);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Centre.y, Centre.z, 4, 0.2, 0.1, 0.2, 0.01);
        if (Airless && Grenade.getOwner() instanceof ServerPlayer User && User.distanceToSqr(Centre) < MessageReach * MessageReach) {
            User.displayClientMessage(Component.translatable(NoFireKey).withStyle(ChatFormatting.GRAY), true);
        }
    }
}
