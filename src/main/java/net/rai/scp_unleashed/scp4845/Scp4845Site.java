package net.rai.scp_unleashed.scp4845;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Scp4845Site {
    public static final int FoundationDepth = 16;
    public static final int Headroom = 4;

    public record Spot(BlockPos Pos, Direction Facing) {
        public Vec3 centre() {
            return Vec3.atBottomCenterOf(Pos);
        }
    }

    public record Station(String Kind, BlockPos Block, BlockPos Stand, Direction Facing) {
        public Vec3 centre() {
            return Vec3.atBottomCenterOf(Stand);
        }
    }

    public final ResourceKey<Level> Dimension;
    public final BlockPos Origin;
    public final Rotation Turn;
    public final Scp4845Layout Layout;
    public final StructurePlaceSettings Settings;
    public final AABB Protected;
    public final AABB Building;
    public final AABB Inside;
    public final List<AABB> Air;
    public final Spot CounterWorker;
    public final Spot Manager;
    public final BlockPos Desk;
    public final List<Spot> BreakSeats;
    public final List<Station> Stations;
    public final List<BlockPos> Registers;
    public final List<BlockPos> Counters;
    public final List<BlockPos> Mop;
    public final List<BlockPos> Chat;
    public final List<BlockPos> KitchenSpawns;
    public final List<BlockPos> Entrance;
    public final BlockPos Doorway;
    public final BlockPos Restaff;
    public final BlockPos Centre;

    public Scp4845Site(ResourceKey<Level> Dimension, BlockPos Origin, Rotation Turn, Scp4845Layout Layout) {
        this.Dimension = Dimension;
        this.Origin = Origin.immutable();
        this.Turn = Turn;
        this.Layout = Layout;
        this.Settings = settings(Turn);
        int[] Whole = {0, 0, 0, Layout.Size.getX() - 1, Layout.Size.getY() - 1, Layout.Size.getZ() - 1};
        Protected = box(Whole).expandTowards(0.0, -FoundationDepth, 0.0).expandTowards(0.0, Headroom, 0.0);
        Building = box(Layout.Building);
        Inside = box(Layout.Inside);
        List<AABB> Boxes = new ArrayList<>();
        for (int[] Entry : Layout.Air) {
            Boxes.add(box(Entry));
        }
        Air = Collections.unmodifiableList(Boxes);
        CounterWorker = spot(Layout.CounterWorker);
        Manager = spot(Layout.Manager);
        Desk = world(Layout.Desk);
        List<Spot> Seats = new ArrayList<>();
        for (Scp4845Layout.Seat Entry : Layout.BreakSeats) {
            Seats.add(spot(Entry));
        }
        BreakSeats = Collections.unmodifiableList(Seats);
        List<Station> Found = new ArrayList<>();
        for (Scp4845Layout.Station Entry : Layout.Stations) {
            Found.add(new Station(Entry.Kind(), world(Entry.Block()), world(Entry.Stand()), Turn.rotate(Entry.Facing())));
        }
        Stations = Collections.unmodifiableList(Found);
        Registers = worlds(Layout.Registers);
        Counters = worlds(Layout.Counters);
        Mop = worlds(Layout.Mop);
        Chat = worlds(Layout.Chat);
        KitchenSpawns = worlds(Layout.KitchenSpawns);
        Entrance = worlds(Layout.Entrance);
        Doorway = world(Layout.Doorway);
        Restaff = world(Layout.Restaff);
        Centre = world(Layout.Anchor);
    }

    public static StructurePlaceSettings settings(Rotation Turn) {
        return new StructurePlaceSettings().setRotation(Turn).setKnownShape(true);
    }

    public static Rotation rotationFor(Direction Front) {
        return switch (Front) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos originFor(BlockPos Centre, Rotation Turn, Scp4845Layout Layout) {
        BlockPos Relative = StructureTemplate.calculateRelativePosition(settings(Turn), Layout.Anchor);
        return Centre.subtract(Relative);
    }

    public BlockPos world(BlockPos Local) {
        return Origin.offset(StructureTemplate.calculateRelativePosition(Settings, Local));
    }

    private List<BlockPos> worlds(List<BlockPos> Locals) {
        List<BlockPos> Made = new ArrayList<>();
        for (BlockPos Local : Locals) {
            Made.add(world(Local));
        }
        return Collections.unmodifiableList(Made);
    }

    private Spot spot(Scp4845Layout.Seat Entry) {
        return new Spot(world(Entry.Pos()), Turn.rotate(Entry.Facing()));
    }

    public AABB box(int[] Local) {
        BlockPos First = world(new BlockPos(Local[0], Local[1], Local[2]));
        BlockPos Second = world(new BlockPos(Local[3], Local[4], Local[5]));
        return new AABB(Math.min(First.getX(), Second.getX()), Math.min(First.getY(), Second.getY()), Math.min(First.getZ(), Second.getZ()),
                Math.max(First.getX(), Second.getX()) + 1, Math.max(First.getY(), Second.getY()) + 1, Math.max(First.getZ(), Second.getZ()) + 1);
    }

    public Direction front() {
        return Turn.rotate(Direction.NORTH);
    }

    public boolean in(Level World) {
        return World.dimension() == Dimension;
    }

    public boolean protects(Level World, BlockPos Pos) {
        return in(World) && Protected.contains(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5);
    }

    public boolean breathable(Level World, Vec3 Point) {
        if (!in(World) || !Building.contains(Point)) {
            return false;
        }
        for (AABB Entry : Air) {
            if (Entry.contains(Point)) {
                return true;
            }
        }
        return false;
    }

    public boolean inside(Level World, Vec3 Point) {
        return in(World) && Inside.inflate(0.0, 1.0, 0.0).contains(Point);
    }

    public List<Station> stations(String Kind) {
        List<Station> Found = new ArrayList<>();
        for (Station Entry : Stations) {
            if (Entry.Kind().equals(Kind)) {
                Found.add(Entry);
            }
        }
        return Found;
    }
}
