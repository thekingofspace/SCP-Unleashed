package net.rai.scp_unleashed.scp256;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scp256DeskCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp256_desk";
    public static final String ReadyKey = "Ready";
    public static final int Vertical = 4;
    public static final int Attempts = 48;
    public static final long HiddenDesk = 600L;
    public static final long AnyDesk = 1200L;
    public static final long HiddenSurface = 2400L;
    public static final double ViewCone = 0.5;
    public static final MapCodec<Scp256DeskCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp256DeskCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 150).forGetter(Scp256DeskCondition::MaxDelay),
            Codec.intRange(2, 48).optionalFieldOf("min_distance", 4).forGetter(Scp256DeskCondition::MinDistance),
            Codec.intRange(2, 48).optionalFieldOf("max_distance", 20).forGetter(Scp256DeskCondition::MaxDistance)
    ).apply(Instance, Scp256DeskCondition::new));

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
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp256.0"),
                Component.translatable("archive.scp_unleashed.notes.scp256.1"));
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
        Optional<BlockPos> Desk = deskSpot(World, Reader, Low, High, Waited < HiddenDesk, Reader.getRandom());
        if (Desk.isPresent() || Waited < AnyDesk) {
            return Desk.map(Pos -> new Finding(Pos, true));
        }
        return surfaceSpot(World, Reader, Low, High, Waited < HiddenSurface, Reader.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public static Optional<BlockPos> deskSpot(ServerLevel World, Player Reader, int Low, int High, boolean Hidden, RandomSource Random) {
        BlockPos Centre = Reader.blockPosition();
        List<BlockPos> Found = new ArrayList<>();
        BlockPos.MutableBlockPos Probe = new BlockPos.MutableBlockPos();
        for (int Dx = -High; Dx <= High; Dx++) {
            for (int Dz = -High; Dz <= High; Dz++) {
                int Square = Dx * Dx + Dz * Dz;
                if (Square < Low * Low || Square > High * High) {
                    continue;
                }
                for (int Dy = -Vertical; Dy <= Vertical; Dy++) {
                    Probe.set(Centre.getX() + Dx, Centre.getY() + Dy, Centre.getZ() + Dz);
                    if (!World.isLoaded(Probe) || !World.getBlockState(Probe).is(Scp256Registry.Desks)) {
                        continue;
                    }
                    BlockPos Spot = Probe.above().immutable();
                    if (open(World, Spot) && (!Hidden || !seen(World, Reader, Spot))) {
                        Found.add(Spot);
                    }
                }
            }
        }
        return Found.isEmpty() ? Optional.empty() : Optional.of(Found.get(Random.nextInt(Found.size())));
    }

    public static Optional<BlockPos> surfaceSpot(ServerLevel World, Player Reader, int Low, int High, boolean Hidden, RandomSource Random) {
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = Low + Random.nextDouble() * Math.max(0, High - Low);
            BlockPos Column = BlockPos.containing(Reader.getX() + Math.cos(Angle) * Radius, Reader.getY(), Reader.getZ() + Math.sin(Angle) * Radius);
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Spot = Column.above(Dy);
                if (!World.isLoaded(Spot) || !open(World, Spot) || !sturdy(World, Spot.below())) {
                    continue;
                }
                if (Hidden && seen(World, Reader, Spot)) {
                    break;
                }
                return Optional.of(Spot);
            }
        }
        return Optional.empty();
    }

    public static boolean open(ServerLevel World, BlockPos Spot) {
        BlockState Here = World.getBlockState(Spot);
        return Here.isAir() && World.getFluidState(Spot).isEmpty() && World.getWorldBorder().isWithinBounds(Spot);
    }

    public static boolean sturdy(ServerLevel World, BlockPos Below) {
        BlockState Under = World.getBlockState(Below);
        return Under.isFaceSturdy(World, Below, Direction.UP) && World.getFluidState(Below).isEmpty() && !Under.is(BlockTags.LEAVES);
    }

    public static boolean seen(ServerLevel World, Player Reader, BlockPos Spot) {
        Vec3 Eye = Reader.getEyePosition();
        Vec3 Target = Vec3.atCenterOf(Spot);
        Vec3 Toward = Target.subtract(Eye).normalize();
        if (Toward.dot(Reader.getViewVector(1.0f)) < ViewCone) {
            return false;
        }
        HitResult Hit = World.clip(new ClipContext(Eye, Target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader));
        return Hit.getType() == HitResult.Type.MISS;
    }

    public static Direction facingToward(BlockPos Spot, Vec3 Toward) {
        double Dx = Toward.x - (Spot.getX() + 0.5);
        double Dz = Toward.z - (Spot.getZ() + 0.5);
        if (Math.abs(Dx) > Math.abs(Dz)) {
            return Dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return Dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public static boolean place(ServerLevel World, BlockPos Spot, Vec3 Toward) {
        if (!open(World, Spot)) {
            return false;
        }
        BlockState Placed = Scp256Registry.Typewriter.get().defaultBlockState().setValue(Scp256TypewriterBlock.FACING, facingToward(Spot, Toward));
        if (!World.setBlock(Spot, Placed, Block.UPDATE_ALL)) {
            return false;
        }
        Scp256TypewriterBlockEntity Typewriter = Scp256TypewriterBlock.typewriterAt(World, Spot);
        if (Typewriter != null) {
            Typewriter.arrive(World);
        }
        return true;
    }
}
