package net.rai.scp_unleashed.containmentzone.shutter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.containmentzone.CellShutterBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class ShutterAssembly {
    public static final int MaxCells = 256;
    public static final int HoldTicks = 10;
    public static final int RattleEvery = 6;
    public static final int WarnEvery = 8;
    public static final double CloseReach = 2.5;
    private static final Predicate<Entity> Blocking = Target -> Target.isAlive() && !Target.isSpectator() && (Target instanceof LivingEntity || Target instanceof VehicleEntity);

    private ShutterAssembly() {
    }

    public static Direction[] ways(Direction Facing) {
        return new Direction[]{Direction.UP, Direction.DOWN, Facing.getClockWise(), Facing.getCounterClockWise()};
    }

    public static List<BlockPos> gather(Level World, BlockPos Origin) {
        BlockState Start = World.getBlockState(Origin);
        if (!(Start.getBlock() instanceof CellShutterBlock)) {
            return List.of();
        }
        Direction[] Ways = ways(Start.getValue(CellShutterBlock.FACING));
        Set<BlockPos> Seen = new HashSet<>();
        List<BlockPos> Found = new ArrayList<>();
        ArrayDeque<BlockPos> Frontier = new ArrayDeque<>();
        Seen.add(Origin);
        Frontier.add(Origin);
        while (!Frontier.isEmpty() && Found.size() < MaxCells) {
            BlockPos Cell = Frontier.poll();
            Found.add(Cell);
            for (Direction Way : Ways) {
                BlockPos Next = Cell.relative(Way);
                if (!Seen.contains(Next) && World.isLoaded(Next) && ShutterShapes.joins(Start, World.getBlockState(Next))) {
                    Seen.add(Next);
                    Frontier.add(Next);
                }
            }
        }
        return Found;
    }

    @Nullable
    public static ShutterBlockEntity entity(Level World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof ShutterBlockEntity Shutter ? Shutter : null;
    }

    public static BlockPos anchor(List<BlockPos> Cells) {
        return Cells.stream().min(Comparator.<BlockPos>comparingInt(Cell -> Cell.getY()).thenComparingInt(Cell -> Cell.getX()).thenComparingInt(Cell -> Cell.getZ())).orElseThrow();
    }

    public static boolean isLockdown(Level World, BlockPos Pos) {
        List<BlockPos> Cells = gather(World, Pos);
        if (Cells.isEmpty()) {
            return false;
        }
        return World.getBlockState(anchor(Cells)).getValue(CellShutterBlock.Lockdown);
    }

    public static void evaluate(ServerLevel World, BlockPos Origin) {
        ShutterBlockEntity Self = entity(World, Origin);
        List<BlockPos> Cells = gather(World, Origin);
        if (Self == null || Cells.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        boolean Lockdown = World.getBlockState(anchor(Cells)).getValue(CellShutterBlock.Lockdown);
        boolean Powered = false;
        long LastBlocked = Long.MIN_VALUE / 2;
        List<ShutterBlockEntity> Parts = new ArrayList<>();
        for (BlockPos Cell : Cells) {
            ShutterBlockEntity Part = entity(World, Cell);
            if (Part != null) {
                Part.settle(Now);
                if (Part.consumeSeen()) {
                    Powered = true;
                }
                LastBlocked = Math.max(LastBlocked, Part.blockedAt());
                Parts.add(Part);
            }
            if (World.hasNeighborSignal(Cell)) {
                Powered = true;
            }
        }
        boolean Wanted = Powered != Lockdown;
        boolean Changing = false;
        for (BlockPos Cell : Cells) {
            if (World.getBlockState(Cell).getValue(CellShutterBlock.Open) != Wanted) {
                Changing = true;
                break;
            }
        }
        if (Changing && !Wanted) {
            if (obstructed(World, Cells, 0.0)) {
                Self.markBlocked(Now);
                LastBlocked = Now;
            }
            if (Now - LastBlocked < HoldTicks) {
                Self.request();
                normalise(World, Cells, Lockdown);
                return;
            }
        }
        if (!Changing) {
            normalise(World, Cells, Lockdown);
            return;
        }
        for (BlockPos Cell : Cells) {
            BlockState State = World.getBlockState(Cell);
            BlockState Changed = State.setValue(CellShutterBlock.Open, Wanted).setValue(CellShutterBlock.Lockdown, Lockdown);
            if (Wanted) {
                Changed = Changed.setValue(CellShutterBlock.Shut, false);
            }
            if (Changed != State) {
                World.setBlock(Cell, Changed, Block.UPDATE_CLIENTS);
            }
        }
        for (ShutterBlockEntity Part : Parts) {
            Part.follow();
            Part.adopt(Self.getProgress());
            Part.hold(Now);
        }
        Self.lead(Cells);
        Vec3 Centre = centre(Cells);
        play(World, Centre, ShutterRegistry.Roll.get(), 0.9f, 1.0f);
        play(World, Centre, ShutterRegistry.Gear.get(), 0.6f, 1.0f);
        if (!Wanted) {
            play(World, Centre, ShutterRegistry.Warn.get(), 0.7f, 1.0f);
        }
    }

    private static void normalise(ServerLevel World, List<BlockPos> Cells, boolean Lockdown) {
        for (BlockPos Cell : Cells) {
            BlockState State = World.getBlockState(Cell);
            if (State.getValue(CellShutterBlock.Lockdown) != Lockdown) {
                World.setBlock(Cell, State.setValue(CellShutterBlock.Lockdown, Lockdown), Block.UPDATE_CLIENTS);
            }
        }
    }

    public static boolean toggleMode(ServerLevel World, BlockPos Pos) {
        List<BlockPos> Cells = gather(World, Pos);
        if (Cells.isEmpty()) {
            return false;
        }
        boolean Lockdown = !World.getBlockState(anchor(Cells)).getValue(CellShutterBlock.Lockdown);
        normalise(World, Cells, Lockdown);
        ShutterBlockEntity Self = entity(World, Pos);
        if (Self != null) {
            Self.request();
        }
        return Lockdown;
    }

    public static void touchNeighbours(Level World, BlockPos Pos, BlockState State) {
        for (Direction Way : ways(State.getValue(CellShutterBlock.FACING))) {
            BlockPos Next = Pos.relative(Way);
            if (World.isLoaded(Next) && ShutterShapes.joins(State, World.getBlockState(Next))) {
                ShutterBlockEntity Neighbour = entity(World, Next);
                if (Neighbour != null) {
                    Neighbour.request();
                }
            }
        }
    }

    public static boolean obstructed(Level World, List<BlockPos> Cells, double Reach) {
        for (BlockPos Cell : Cells) {
            if (!World.isLoaded(Cell)) {
                continue;
            }
            BlockState State = World.getBlockState(Cell);
            ShutterBlockEntity Part = entity(World, Cell);
            if (!(State.getBlock() instanceof CellShutterBlock) || Part == null || State.getValue(CellShutterBlock.Shut)) {
                continue;
            }
            ShutterShapes.Column Place = Part.place();
            int Top = ShutterShapes.localTop(Place);
            if (Top <= 0) {
                continue;
            }
            double Bar = ShutterShapes.barBottom(Place.Height(), Part.getProgress()) - 16.0 * Place.Row();
            double Height = Math.min(Top, Bar + Reach);
            if (Height <= 0.0) {
                continue;
            }
            AABB Space = ShutterShapes.opening(Cell, State.getValue(CellShutterBlock.FACING), 0, Height);
            if (!World.getEntitiesOfClass(Entity.class, Space, Blocking).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static void reverse(ServerLevel World, ShutterBlockEntity Lead) {
        long Now = World.getGameTime();
        for (BlockPos Cell : Lead.members()) {
            if (!World.isLoaded(Cell)) {
                continue;
            }
            BlockState State = World.getBlockState(Cell);
            if (State.getBlock() instanceof CellShutterBlock && !State.getValue(CellShutterBlock.Open)) {
                World.setBlock(Cell, State.setValue(CellShutterBlock.Open, true).setValue(CellShutterBlock.Shut, false), Block.UPDATE_CLIENTS);
            }
            ShutterBlockEntity Part = entity(World, Cell);
            if (Part != null) {
                Part.adopt(Lead.getProgress());
                Part.hold(Now);
            }
        }
        Lead.request();
        Lead.lead(Lead.members());
        Vec3 Centre = centre(Lead.members());
        play(World, Centre, ShutterRegistry.Clunk.get(), 0.6f, 1.15f);
        play(World, Centre, ShutterRegistry.Roll.get(), 0.8f, 1.05f);
    }

    public static void travel(ServerLevel World, ShutterBlockEntity Lead, boolean Opening, int Clock) {
        Vec3 Centre = centre(Lead.members());
        if (Clock % RattleEvery == 0) {
            play(World, Centre, ShutterRegistry.Rattle.get(), 0.45f, 1.0f);
        }
        if (!Opening && Clock % WarnEvery == 0) {
            play(World, Centre, ShutterRegistry.Warn.get(), 0.7f, (Clock / WarnEvery) % 2 == 0 ? 1.0f : 0.84f);
        }
    }

    public static void arrive(ServerLevel World, ShutterBlockEntity Lead, boolean Opened) {
        Vec3 Centre = centre(Lead.members());
        if (Opened) {
            play(World, Centre, ShutterRegistry.Clunk.get(), 0.45f, 1.25f);
        } else {
            play(World, Centre, ShutterRegistry.Clunk.get(), 1.0f, 1.0f);
            play(World, Centre, ShutterRegistry.Thud.get(), 0.8f, 1.0f);
        }
    }

    public static Vec3 centre(List<BlockPos> Cells) {
        if (Cells.isEmpty()) {
            return Vec3.ZERO;
        }
        double X = 0.0;
        double Y = 0.0;
        double Z = 0.0;
        for (BlockPos Cell : Cells) {
            X += Cell.getX() + 0.5;
            Y += Cell.getY() + 0.5;
            Z += Cell.getZ() + 0.5;
        }
        return new Vec3(X / Cells.size(), Y / Cells.size(), Z / Cells.size());
    }

    private static void play(ServerLevel World, Vec3 Centre, SoundEvent Sound, float Volume, float Pitch) {
        float Jitter = 0.96f + World.getRandom().nextFloat() * 0.08f;
        World.playSound(null, Centre.x, Centre.y, Centre.z, Sound, SoundSource.BLOCKS, Volume, Pitch * Jitter);
    }
}
