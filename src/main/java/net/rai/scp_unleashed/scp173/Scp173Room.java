package net.rai.scp_unleashed.scp173;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

public final class Scp173Room {
    public static final int Margin = 1;
    public static final int MaxCells = 16000;
    private static final Direction[] Sides = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private Scp173Room() {
    }

    private record Visit(Scp173Climb.Spot Place, Vec3 Where) {
    }

    private static final class Flood {
        private final Scp173Passage Way;
        private final BlockPos Centre;
        private final int Radius;
        private final int Height;
        private final LongOpenHashSet Seen = new LongOpenHashSet();
        private final LongOpenHashSet Floor = new LongOpenHashSet();
        private final ArrayDeque<Visit> Queue = new ArrayDeque<>();

        private Flood(Scp173Passage Way, BlockPos Centre, int Radius, int Height) {
            this.Way = Way;
            this.Centre = Centre;
            this.Radius = Radius + Margin;
            this.Height = Height + Margin;
        }

        private boolean within(BlockPos Cell) {
            return Math.abs(Cell.getX() - Centre.getX()) <= Radius && Math.abs(Cell.getZ() - Centre.getZ()) <= Radius && Math.abs(Cell.getY() - Centre.getY()) <= Height;
        }

        @Nullable
        private Scp173Climb.Spot spot(BlockPos Cell) {
            return within(Cell) ? Scp173Climb.inspect(Way, Cell, false) : null;
        }

        private void enter(Scp173Climb.Spot Found) {
            if (Seen.size() >= MaxCells || !Seen.add(Found.Cell().asLong())) {
                return;
            }
            if (Found.Supported()) {
                Floor.add(Found.Cell().asLong());
            }
            Queue.add(new Visit(Found, Scp173Climb.centre(Found)));
        }

        private void fall(Vec3 From, BlockPos Over) {
            for (int Depth = 1; Depth <= Scp173Climb.MaxDrop; Depth++) {
                Scp173Climb.Spot Land = spot(Over.below(Depth));
                if (Land == null) {
                    return;
                }
                if (Land.Supported()) {
                    if (Way.passes(From, Scp173Climb.centre(Land))) {
                        enter(Land);
                    }
                    return;
                }
            }
        }

        private void walk(Visit Here) {
            Scp173Climb.Spot From = Here.Place();
            if (!From.Supported()) {
                fall(Here.Where(), From.Cell());
                return;
            }
            for (Direction Side : Sides) {
                BlockPos Next = From.Cell().relative(Side);
                Scp173Climb.Spot Beside = spot(Next);
                if (Beside != null && Way.passes(Here.Where(), Scp173Climb.centre(Beside))) {
                    if (Beside.Supported()) {
                        enter(Beside);
                    } else {
                        fall(Scp173Climb.centre(Beside), Next);
                    }
                }
                Scp173Climb.Spot Up = spot(Next.above());
                if (Up != null && Up.Supported() && Way.passes(Here.Where(), Scp173Climb.centre(Up))) {
                    enter(Up);
                }
            }
        }

        private LongOpenHashSet run(Vec3 Origin) {
            BlockPos Start = BlockPos.containing(Origin.x, Origin.y + 0.01, Origin.z);
            Scp173Climb.Spot First = spot(Start);
            if (First == null) {
                return Floor;
            }
            Seen.add(Start.asLong());
            if (First.Supported()) {
                Floor.add(Start.asLong());
            }
            Queue.add(new Visit(First, Origin));
            while (!Queue.isEmpty()) {
                walk(Queue.poll());
            }
            return Floor;
        }
    }

    public static LongOpenHashSet floor(ServerLevel World, Scp173Statue Statue, int Radius, int Height) {
        return new Flood(Scp173Passage.remembering(World), Statue.blockPosition(), Radius, Height).run(Statue.position());
    }
}
