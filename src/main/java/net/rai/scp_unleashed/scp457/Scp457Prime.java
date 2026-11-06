package net.rai.scp_unleashed.scp457;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public final class Scp457Prime {
    public static final int SearchRadius = 3;
    public static final int DropDepth = 4;
    private static final Set<MobSpawnType> PrimeReasons = EnumSet.of(MobSpawnType.SPAWN_EGG, MobSpawnType.DISPENSER, MobSpawnType.COMMAND, MobSpawnType.EVENT);

    private Scp457Prime() {
    }

    public static boolean makesPrime(MobSpawnType Reason) {
        return PrimeReasons.contains(Reason);
    }

    public static boolean open(ServerLevel World, BlockPos Pos) {
        if (!World.isInWorldBounds(Pos)) {
            return false;
        }
        BlockState State = World.getBlockState(Pos);
        return State.canBeReplaced() && !State.hasBlockEntity() && !State.is(Scp457Registry.AshPile.get());
    }

    public static boolean dry(ServerLevel World, BlockPos Pos) {
        return open(World, Pos) && World.getFluidState(Pos).isEmpty();
    }

    public static boolean floored(ServerLevel World, BlockPos Pos) {
        BlockPos Below = Pos.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    @Nullable
    public static BlockPos spot(ServerLevel World, BlockPos From) {
        BlockPos Sunken = null;
        for (int Drop = 0; Drop <= DropDepth; Drop++) {
            BlockPos Pos = From.below(Drop);
            if (!open(World, Pos)) {
                break;
            }
            if (floored(World, Pos)) {
                if (dry(World, Pos)) {
                    return Pos;
                }
                if (Sunken == null) {
                    Sunken = Pos;
                }
            }
        }
        for (int Radius = 1; Radius <= SearchRadius; Radius++) {
            for (int Dx = -Radius; Dx <= Radius; Dx++) {
                for (int Dz = -Radius; Dz <= Radius; Dz++) {
                    if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Radius) {
                        continue;
                    }
                    for (int Dy = 1; Dy >= -2; Dy--) {
                        BlockPos Pos = From.offset(Dx, Dy, Dz);
                        if (dry(World, Pos) && floored(World, Pos)) {
                            return Pos;
                        }
                    }
                }
            }
        }
        if (Sunken != null) {
            return Sunken;
        }
        return open(World, From) ? From : null;
    }

    @Nullable
    public static BlockPos settle(ServerLevel World, Scp457 Burning) {
        BlockPos Spot = spot(World, Burning.blockPosition());
        if (Spot == null) {
            return null;
        }
        CompoundTag Remains = Burning.saveWithoutId(new CompoundTag());
        BlockState State = Scp457Registry.AshPile.get().defaultBlockState().setValue(Scp457AshPileBlock.Waterlogged, World.getFluidState(Spot).getType() == Fluids.WATER)
                .setValue(Scp457AshPileBlock.Frosted, Burning.deathKind() == Scp457.DeathFrost);
        if (!World.setBlock(Spot, State, Block.UPDATE_ALL)) {
            return null;
        }
        if (World.getBlockEntity(Spot) instanceof Scp457AshPileBlockEntity Pile) {
            Pile.setRemains(Remains);
        }
        World.gameEvent(GameEvent.BLOCK_PLACE, Spot, GameEvent.Context.of(State));
        return Spot;
    }

    @Nullable
    public static Scp457 relight(ServerLevel World, BlockPos Pos, BlockState State, @Nullable CompoundTag Remains) {
        if (State.getValue(Scp457AshPileBlock.Waterlogged) || State.getValue(Scp457AshPileBlock.Frosted) || Scp457Spread.count(World) >= Scp457Spread.MaxPerDimension) {
            return null;
        }
        Scp457 Made = Scp457Registry.Burning.get().create(World);
        if (Made == null) {
            return null;
        }
        if (Remains != null) {
            Made.load(Remains);
        }
        if (World.getEntity(Made.getUUID()) != null) {
            Made.setUUID(Mth.createInsecureUUID(World.getRandom()));
        }
        float Yaw = Made.getYRot();
        Vec3 Spot = Scp457Spread.room(World, Made, Vec3.atBottomCenterOf(Pos), Yaw);
        if (Spot == null) {
            return null;
        }
        Made.rekindle(Spot, Yaw);
        if (Scp457Water.touching(Made)) {
            return null;
        }
        World.removeBlock(Pos, false);
        if (!World.addFreshEntity(Made)) {
            World.setBlock(Pos, State, Block.UPDATE_ALL);
            if (World.getBlockEntity(Pos) instanceof Scp457AshPileBlockEntity Pile) {
                Pile.setRemains(Remains);
            }
            return null;
        }
        Scp457Effects.ignition(World, Made);
        return Made;
    }
}
