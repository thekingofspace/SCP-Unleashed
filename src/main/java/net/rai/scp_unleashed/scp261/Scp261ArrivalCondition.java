package net.rai.scp_unleashed.scp261;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record Scp261ArrivalCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp261_arrival";
    public static final String ReadyKey = "Ready";
    public static final int Attempts = 40;
    public static final int Vertical = 5;
    public static final int WallGrace = 1200;
    public static final int HiddenGrace = 2400;
    public static final double ViewCone = 0.45;
    public static final MapCodec<Scp261ArrivalCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp261ArrivalCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 180).forGetter(Scp261ArrivalCondition::MaxDelay),
            Codec.intRange(4, 48).optionalFieldOf("min_distance", 8).forGetter(Scp261ArrivalCondition::MinDistance),
            Codec.intRange(4, 48).optionalFieldOf("max_distance", 20).forGetter(Scp261ArrivalCondition::MaxDistance)
    ).apply(Instance, Scp261ArrivalCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private long rollDelay(RandomSource Random) {
        int Low = Math.min(MinDelay, MaxDelay);
        int High = Math.max(MinDelay, MaxDelay);
        return (Low + (long) Random.nextInt(High - Low + 1)) * 20L;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Case.getState().putLong(ReadyKey, Reader.serverLevel().getGameTime() + rollDelay(Reader.getRandom()));
        Case.markDirty();
        return List.of(Component.translatable("archive.scp_unleashed.notes.scp261.0"), Component.translatable("archive.scp_unleashed.notes.scp261.1"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (Reader.isSpectator() || !Reader.isAlive() || Reader.isSleeping()) {
            return Optional.empty();
        }
        ServerLevel World = Reader.serverLevel();
        CompoundTag State = Case.getState();
        long Now = World.getGameTime();
        if (!State.contains(ReadyKey)) {
            State.putLong(ReadyKey, Now + rollDelay(Reader.getRandom()));
            Case.markDirty();
            return Optional.empty();
        }
        long Ready = State.getLong(ReadyKey);
        if (Now < Ready) {
            return Optional.empty();
        }
        long Waited = Now - Ready;
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        return findSpot(World, Reader, Low, High, Waited < WallGrace, Waited < HiddenGrace, Reader.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public static Optional<BlockPos> findSpot(ServerLevel World, ServerPlayer Reader, int Low, int High, boolean NeedWall, boolean Hidden, RandomSource Random) {
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = Low + Random.nextDouble() * Math.max(0, High - Low);
            BlockPos Column = BlockPos.containing(Reader.getX() + Math.cos(Angle) * Radius, Reader.getY(), Reader.getZ() + Math.sin(Angle) * Radius);
            if (!World.isLoaded(Column)) {
                continue;
            }
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Spot = Column.above(Dy);
                if (!roomFor(World, Spot)) {
                    continue;
                }
                if (NeedWall && wallFacing(World, Spot) == null) {
                    break;
                }
                if (Hidden && seen(World, Reader, Spot)) {
                    break;
                }
                return Optional.of(Spot);
            }
        }
        return Optional.empty();
    }

    public static boolean roomFor(ServerLevel World, BlockPos Spot) {
        if (!LeadPlanner.isDryStandable(World, Spot) || World.isOutsideBuildHeight(Spot.above(2))) {
            return false;
        }
        BlockState Lower = World.getBlockState(Spot);
        BlockState Upper = World.getBlockState(Spot.above());
        return Lower.getCollisionShape(World, Spot).isEmpty() && Upper.getCollisionShape(World, Spot.above()).isEmpty()
                && World.getEntitiesOfClass(Entity.class, new AABB(Spot).expandTowards(0.0, 1.0, 0.0)).isEmpty();
    }

    @Nullable
    public static Direction wallFacing(ServerLevel World, BlockPos Spot) {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            BlockPos Behind = Spot.relative(Side.getOpposite());
            BlockPos Ahead = Spot.relative(Side);
            boolean Wall = World.getBlockState(Behind).isFaceSturdy(World, Behind, Side) && World.getBlockState(Behind.above()).isFaceSturdy(World, Behind.above(), Side);
            boolean Open = World.getBlockState(Ahead).getCollisionShape(World, Ahead).isEmpty() && World.getBlockState(Ahead.above()).getCollisionShape(World, Ahead.above()).isEmpty();
            if (Wall && Open) {
                return Side;
            }
        }
        return null;
    }

    public static boolean seen(ServerLevel World, ServerPlayer Reader, BlockPos Spot) {
        Vec3 Eye = Reader.getEyePosition();
        Vec3 Target = Vec3.atBottomCenterOf(Spot).add(0.0, 1.0, 0.0);
        Vec3 Toward = Target.subtract(Eye).normalize();
        if (Toward.dot(Reader.getViewVector(1.0f)) < ViewCone) {
            return false;
        }
        HitResult Hit = World.clip(new ClipContext(Eye, Target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader));
        return Hit.getType() == HitResult.Type.MISS;
    }

    public static Direction facingFor(ServerLevel World, BlockPos Spot, Vec3 Toward) {
        Direction Wall = wallFacing(World, Spot);
        if (Wall != null) {
            return Wall;
        }
        return Direction.getNearest(Toward.x - (Spot.getX() + 0.5), 0.0, Toward.z - (Spot.getZ() + 0.5));
    }

    public static boolean place(ServerLevel World, BlockPos Spot, Direction Front) {
        if (!roomFor(World, Spot)) {
            return false;
        }
        Direction Facing = Front.getAxis().isHorizontal() ? Front : Direction.NORTH;
        BlockState Lower = Scp261Registry.Vending.get().defaultBlockState().setValue(Scp261VendingBlock.Facing, Facing).setValue(Scp261VendingBlock.Half, DoubleBlockHalf.LOWER);
        World.setBlock(Spot, Lower, Block.UPDATE_ALL);
        World.setBlock(Spot.above(), Lower.setValue(Scp261VendingBlock.Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (!World.getBlockState(Spot.above()).is(Scp261Registry.Vending.get())) {
            World.removeBlock(Spot, false);
            return false;
        }
        Vec3 Middle = Vec3.atBottomCenterOf(Spot).add(0.0, 1.0, 0.0);
        World.sendParticles(ParticleTypes.PORTAL, Middle.x, Middle.y, Middle.z, 40, 0.4, 0.9, 0.4, 0.4);
        World.playSound(null, Middle.x, Middle.y, Middle.z, Scp261Registry.Hum.get(), SoundSource.BLOCKS, 0.8f, 0.7f);
        return true;
    }
}
