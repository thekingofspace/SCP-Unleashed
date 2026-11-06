package net.rai.scp_unleashed.scp548;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import net.rai.scp_unleashed.cooler.CoolerClimate;
import net.rai.scp_unleashed.cooler.CoolerFreeze;
import net.rai.scp_unleashed.fluiddump.PollutionRules;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class Scp548Frost {
    public static final int HotPulse = 8;
    public static final int MildPulse = 16;
    public static final int BaseStages = 10;
    public static final float StagesPerHardness = 4.0f;
    public static final float HardnessCutoff = 10.0f;
    public static final float BlastCutoff = 100.0f;
    public static final int Forget = 100;
    public static final int SweepInterval = 20;
    public static final double RayReach = 3.0;
    public static final int WaterReach = 2;
    public static final int WaterPulse = 40;
    public static final float CreakVolume = 0.35f;
    public static final float ShatterVolume = 0.9f;
    public static final TagKey<Block> FrostProof = TagKey.create(Registries.BLOCK, Scp548Registry.location("scp548_frost_proof"));
    private static final Map<Level, Map<Long, Patch>> Patches = new WeakHashMap<>();

    private Scp548Frost() {
    }

    private static final class Patch {
        private int Progress;
        private int Required;
        private long Last;
    }

    public static int pulseFor(CoolerClimate Climate) {
        return switch (Climate) {
            case Hot -> HotPulse;
            case Mild -> MildPulse;
            default -> 0;
        };
    }

    public static boolean allowed(ServerLevel World, Entity Source) {
        return EventHooks.canEntityGrief(World, Source) && !PollutionRules.isProtected(World);
    }

    public static boolean frostable(Level World, BlockPos Pos, BlockState State) {
        if (State.isAir() || !State.getFluidState().isEmpty() || State.getCollisionShape(World, Pos).isEmpty() || !World.getWorldBorder().isWithinBounds(Pos)) {
            return false;
        }
        if (State.is(FrostProof) || State.is(BlockTags.WITHER_IMMUNE)) {
            return false;
        }
        float Hardness = State.getDestroySpeed(World, Pos);
        return Hardness >= 0.0f && Hardness < HardnessCutoff && State.getBlock().getExplosionResistance() < BlastCutoff;
    }

    public static boolean shatterable(Level World, BlockPos Pos, BlockState State) {
        return frostable(World, Pos, State) && !State.hasBlockEntity();
    }

    public static int required(Level World, BlockPos Pos, BlockState State) {
        return BaseStages + Math.round(Math.max(0.0f, State.getDestroySpeed(World, Pos)) * StagesPerHardness);
    }

    public static int progress(Level World, BlockPos Pos) {
        Map<Long, Patch> Known = Patches.get(World);
        Patch Found = Known == null ? null : Known.get(Pos.asLong());
        return Found == null ? 0 : Found.Progress;
    }

    private static int breaker(BlockPos Pos) {
        return -1 - (Long.hashCode(Pos.asLong()) & 0x3FFFFFFF);
    }

    @Nullable
    public static BlockPos pick(ServerLevel World, Scp548Spider Spider) {
        Vec3 From = Spider.position().add(0.0, Spider.getBbHeight() * 0.5, 0.0);
        LivingEntity Prey = Spider.getTarget();
        if (Prey != null) {
            Vec3 Toward = Prey.getEyePosition().subtract(From);
            Vec3 To = Toward.length() > RayReach ? From.add(Toward.normalize().scale(RayReach)) : Prey.getEyePosition();
            BlockHitResult Hit = World.clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Spider));
            if (Hit.getType() == HitResult.Type.BLOCK && frostable(World, Hit.getBlockPos(), World.getBlockState(Hit.getBlockPos()))) {
                return Hit.getBlockPos();
            }
        }
        BlockPos Here = Spider.blockPosition();
        if (Spider.horizontalCollision) {
            BlockPos Ahead = Here.relative(Spider.getDirection());
            for (BlockPos Each : new BlockPos[]{Ahead, Ahead.above()}) {
                if (frostable(World, Each, World.getBlockState(Each))) {
                    return Each;
                }
            }
        }
        List<BlockPos> Around = new ArrayList<>();
        for (Direction Side : Direction.values()) {
            if (Side == Direction.DOWN) {
                continue;
            }
            BlockPos Each = Here.relative(Side);
            if (frostable(World, Each, World.getBlockState(Each))) {
                if (progress(World, Each) > 0) {
                    return Each;
                }
                Around.add(Each);
            }
        }
        return Around.isEmpty() ? null : Around.get(Spider.getRandom().nextInt(Around.size()));
    }

    public static boolean frost(ServerLevel World, BlockPos Target, @Nullable Entity Source) {
        BlockState State = World.getBlockState(Target);
        Map<Long, Patch> Known = Patches.computeIfAbsent(World, Key -> new HashMap<>());
        if (!frostable(World, Target, State)) {
            if (Known.remove(Target.asLong()) != null) {
                World.destroyBlockProgress(breaker(Target), Target, -1);
            }
            return false;
        }
        if (Source instanceof LivingEntity Living && !CommonHooks.canEntityDestroy(World, Target, Living)) {
            return false;
        }
        Patch Found = Known.computeIfAbsent(Target.asLong(), Key -> new Patch());
        boolean Shatters = !State.hasBlockEntity();
        Found.Required = required(World, Target, State);
        Found.Last = World.getGameTime();
        Found.Progress = Math.min(Shatters ? Found.Required : Found.Required - 1, Found.Progress + 1);
        World.destroyBlockProgress(breaker(Target), Target, Math.min(9, Found.Progress * 10 / Found.Required));
        World.sendParticles(ParticleTypes.SNOWFLAKE, Target.getX() + 0.5, Target.getY() + 0.5, Target.getZ() + 0.5, 5, 0.42, 0.42, 0.42, 0.005);
        if (Found.Progress % 3 == 1) {
            Scp548Swarm.playLayers(World, Vec3.atCenterOf(Target), Scp548Registry.CrackleLayers, CreakVolume, 1.2f + World.getRandom().nextFloat() * 0.2f);
        }
        if (Shatters && Found.Progress >= Found.Required) {
            Known.remove(Target.asLong());
            shatter(World, Target, State, Source);
            return true;
        }
        return false;
    }

    private static void shatter(ServerLevel World, BlockPos Target, BlockState State, @Nullable Entity Source) {
        World.destroyBlockProgress(breaker(Target), Target, -1);
        World.destroyBlock(Target, false, Source);
        Vec3 Centre = Vec3.atCenterOf(Target);
        World.sendParticles(ParticleTypes.SNOWFLAKE, Centre.x, Centre.y, Centre.z, 18, 0.35, 0.35, 0.35, 0.04);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), Centre.x, Centre.y, Centre.z, 12, 0.3, 0.3, 0.3, 0.08);
        Scp548Swarm.playLayers(World, Centre, Scp548Registry.ShatterLayers, ShatterVolume, 0.95f + World.getRandom().nextFloat() * 0.1f);
    }

    public static boolean chillWater(ServerLevel World, Scp548Spider Spider) {
        BlockPos Here = Spider.blockPosition();
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Each : BlockPos.betweenClosed(Here.offset(-WaterReach, -1, -WaterReach), Here.offset(WaterReach, 1, WaterReach))) {
            if (CoolerFreeze.isStillWater(World.getBlockState(Each))) {
                double Distance = Each.distSqr(Here);
                if (Distance < BestDistance) {
                    BestDistance = Distance;
                    Best = Each.immutable();
                }
            }
        }
        return Best != null && CoolerFreeze.freeze(World, Best, Here);
    }

    public static void sweep(ServerLevel World) {
        Map<Long, Patch> Known = Patches.get(World);
        if (Known == null || Known.isEmpty() || World.getGameTime() % SweepInterval != 0) {
            return;
        }
        long Now = World.getGameTime();
        Iterator<Map.Entry<Long, Patch>> Entries = Known.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<Long, Patch> Entry = Entries.next();
            if (Now - Entry.getValue().Last > Forget) {
                BlockPos Pos = BlockPos.of(Entry.getKey());
                World.destroyBlockProgress(breaker(Pos), Pos, -1);
                Entries.remove();
            }
        }
    }

    public static void forget() {
        Patches.clear();
    }
}
