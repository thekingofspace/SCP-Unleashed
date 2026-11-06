package net.rai.scp_unleashed.scp465;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Scp465Layout {
    public static final int WideBooth = 9;
    public static final int WideDance = 7;
    public static final int BarMin = 2;
    public static final int BarMax = 5;
    public static final int StreamerSpacing = 5;
    public static final double DanceShare = 0.28;
    public static final double DanceLean = 0.3;
    private static final Direction[] Walls = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public record Plan(Map<BlockPos, BlockState> Placed, BlockPos DjStand, BlockPos Booth, Direction BoothFacing, List<BlockPos> Bar, List<BlockPos> BarFront,
                       List<BlockPos> Dance, List<BlockPos> Floor, int FloorArea) {
    }

    private Scp465Layout() {
    }

    private static long key(int X, int Z) {
        return BlockPos.asLong(X, 0, Z);
    }

    public static Plan plan(Scp465Room.Scan Found, BlockPos Box) {
        LongOpenHashSet Cells = Found.Cells();
        int FloorY = Found.FloorY();
        LongOpenHashSet Grid = new LongOpenHashSet();
        Map<Long, Integer> Heights = new LinkedHashMap<>();
        int MinX = Integer.MAX_VALUE;
        int MaxX = Integer.MIN_VALUE;
        int MinZ = Integer.MAX_VALUE;
        int MaxZ = Integer.MIN_VALUE;
        for (int X = Found.Low().getX(); X <= Found.High().getX(); X++) {
            for (int Z = Found.Low().getZ(); Z <= Found.High().getZ(); Z++) {
                if (!Scp465Room.floorCell(Cells, X, FloorY, Z)) {
                    continue;
                }
                int Height = Scp465Room.headroom(Cells, X, FloorY, Z);
                if (Height < 2) {
                    continue;
                }
                Grid.add(key(X, Z));
                Heights.put(key(X, Z), Height);
                MinX = Math.min(MinX, X);
                MaxX = Math.max(MaxX, X);
                MinZ = Math.min(MinZ, Z);
                MaxZ = Math.max(MaxZ, Z);
            }
        }
        LongOpenHashSet Used = new LongOpenHashSet();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (long Packed : Found.Kept()) {
            Cursor.set(Packed);
            if (Cursor.getY() >= FloorY - 1 && Cursor.getY() <= FloorY + 1) {
                Used.add(key(Cursor.getX(), Cursor.getZ()));
            }
        }
        Map<BlockPos, BlockState> Placed = new LinkedHashMap<>();
        int SpanX = MaxX - MinX + 1;
        int SpanZ = MaxZ - MinZ + 1;
        BlockPos BoxFloor = new BlockPos(Box.getX(), FloorY, Box.getZ());

        BoothSpot Chosen = booth(Grid, Used, MinX, MaxX, MinZ, MaxZ, BoxFloor, FloorY);
        BlockPos DjStand;
        BlockPos Centre;
        Direction Facing;
        List<BlockPos> Decks = new ArrayList<>();
        List<BlockPos> Speakers = new ArrayList<>();
        if (Chosen != null) {
            DjStand = Chosen.Stand();
            Facing = Chosen.Inward();
            Direction Along = Facing.getClockWise();
            BlockPos Deck = DjStand.relative(Facing);
            Centre = Deck;
            int Span = Along.getAxis() == Direction.Axis.X ? SpanX : SpanZ;
            boolean Wide = Span >= WideBooth && free(Grid, Used, Deck.relative(Along)) && free(Grid, Used, Deck.relative(Along.getOpposite()));
            Decks.add(Deck);
            if (Wide) {
                Decks.add(Deck.relative(Along));
                Decks.add(Deck.relative(Along.getOpposite()));
                Speakers.add(Deck.relative(Along, 2));
                Speakers.add(Deck.relative(Along.getOpposite(), 2));
            } else {
                Speakers.add(Deck.relative(Along));
                Speakers.add(Deck.relative(Along.getOpposite()));
            }
            Used.add(key(DjStand.getX(), DjStand.getZ()));
            for (BlockPos Back : List.of(DjStand.relative(Along), DjStand.relative(Along.getOpposite()))) {
                if (Grid.contains(key(Back.getX(), Back.getZ()))) {
                    Used.add(key(Back.getX(), Back.getZ()));
                }
            }
            for (BlockPos Each : Decks) {
                Placed.put(Each, Scp465Registry.decor(Scp465Decor.DjDeck).defaultBlockState().setValue(Scp465PartyBlock.FACING, Facing)
                        .setValue(Scp465PartyBlock.Tint, Each.equals(Deck) ? 0 : 1));
                Used.add(key(Each.getX(), Each.getZ()));
            }
            for (BlockPos Each : Speakers) {
                if (!free(Grid, Used, Each)) {
                    continue;
                }
                Placed.put(Each, Scp465Registry.decor(Scp465Decor.Speaker).defaultBlockState().setValue(Scp465PartyBlock.FACING, Facing)
                        .setValue(Scp465PartyBlock.Tint, Math.floorMod(Each.getX() + Each.getZ(), Scp465PartyBlock.Tints)));
                Used.add(key(Each.getX(), Each.getZ()));
                BlockPos Above = Each.above();
                if (Cells.contains(Above.asLong()) && Heights.getOrDefault(key(Each.getX(), Each.getZ()), 0) >= Scp465Room.MinHeight) {
                    Placed.put(Above, Scp465Registry.decor(Scp465Decor.StageLight).defaultBlockState().setValue(Scp465PartyBlock.FACING, Facing)
                            .setValue(Scp465PartyBlock.Tint, Math.floorMod(Each.getX() * 3 + Each.getZ(), Scp465PartyBlock.Tints)));
                }
            }
        } else {
            DjStand = farthest(Grid, Used, BoxFloor, FloorY);
            Centre = DjStand;
            Facing = Direction.SOUTH;
            Used.add(key(DjStand.getX(), DjStand.getZ()));
        }

        List<BlockPos> Bar = new ArrayList<>();
        List<BlockPos> BarFront = new ArrayList<>();
        bar(Grid, Used, Cells, FloorY, Facing, SpanX, SpanZ, BoxFloor, Placed, Bar, BarFront);

        List<BlockPos> Floor = new ArrayList<>();
        List<BlockPos> Dance = new ArrayList<>();
        boolean Roomy = SpanX >= WideDance && SpanZ >= WideDance;
        double DanceX = (MinX + MaxX) / 2.0 * (1.0 - DanceLean) + Centre.getX() * DanceLean;
        double DanceZ = (MinZ + MaxZ) / 2.0 * (1.0 - DanceLean) + Centre.getZ() * DanceLean;
        double HalfX = Math.max(1.0, Math.round(SpanX * DanceShare));
        double HalfZ = Math.max(1.0, Math.round(SpanZ * DanceShare));
        for (long Packed : Grid) {
            if (Used.contains(Packed)) {
                continue;
            }
            Cursor.set(Packed);
            BlockPos Spot = new BlockPos(Cursor.getX(), FloorY, Cursor.getZ());
            Floor.add(Spot);
            boolean Edge = edge(Grid, Spot.getX(), Spot.getZ());
            if (Roomy && Edge || BarFront.contains(Spot)) {
                continue;
            }
            if (Roomy && (Math.abs(Spot.getX() - DanceX) > HalfX + 0.01 || Math.abs(Spot.getZ() - DanceZ) > HalfZ + 0.01)) {
                continue;
            }
            Dance.add(Spot);
        }
        Floor.sort(Comparator.comparingLong(BlockPos::asLong));
        Dance.sort(Comparator.comparingLong(BlockPos::asLong));
        for (BlockPos Spot : Dance) {
            int Shade = (Spot.getX() & 1) + 2 * (Spot.getZ() & 1);
            Placed.put(Spot, Scp465Registry.decor(Scp465Decor.DanceFloor).defaultBlockState().setValue(Scp465PartyBlock.Tint, Shade));
        }

        ceiling(Grid, Heights, Dance, FloorY, Placed, Centre, Speakers);
        return new Plan(Placed, DjStand, Centre, Facing, Bar, BarFront, Dance, Floor, Grid.size());
    }

    private record BoothSpot(BlockPos Stand, Direction Inward, double Score) {
    }

    private static boolean free(LongOpenHashSet Grid, LongOpenHashSet Used, BlockPos Pos) {
        long Packed = key(Pos.getX(), Pos.getZ());
        return Grid.contains(Packed) && !Used.contains(Packed);
    }

    private static boolean edge(LongOpenHashSet Grid, int X, int Z) {
        for (Direction Side : Walls) {
            if (!Grid.contains(key(X + Side.getStepX(), Z + Side.getStepZ()))) {
                return true;
            }
        }
        return false;
    }

    private static BoothSpot booth(LongOpenHashSet Grid, LongOpenHashSet Used, int MinX, int MaxX, int MinZ, int MaxZ, BlockPos Box, int FloorY) {
        BoothSpot Best = null;
        double MidX = (MinX + MaxX) / 2.0;
        double MidZ = (MinZ + MaxZ) / 2.0;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (long Packed : Grid) {
            Cursor.set(Packed);
            int X = Cursor.getX();
            int Z = Cursor.getZ();
            for (Direction Wall : Walls) {
                if (Grid.contains(key(X + Wall.getStepX(), Z + Wall.getStepZ()))) {
                    continue;
                }
                Direction Inward = Wall.getOpposite();
                BlockPos Stand = new BlockPos(X, FloorY, Z);
                BlockPos Deck = Stand.relative(Inward);
                if (!free(Grid, Used, Stand) || !free(Grid, Used, Deck) || !Grid.contains(key(Deck.getX() + Inward.getStepX(), Deck.getZ() + Inward.getStepZ()))) {
                    continue;
                }
                double Off = Wall.getAxis() == Direction.Axis.Z ? Math.abs(X - MidX) : Math.abs(Z - MidZ);
                double Away = Math.sqrt(Stand.distSqr(Box));
                double Score = Away - Off * 1.6 + (Wall == Direction.NORTH ? 0.05 : 0.0);
                if (Best == null || Score > Best.Score() + 1e-6) {
                    Best = new BoothSpot(Stand, Inward, Score);
                }
            }
        }
        return Best;
    }

    private static BlockPos farthest(LongOpenHashSet Grid, LongOpenHashSet Used, BlockPos Box, int FloorY) {
        BlockPos Best = Box;
        double Far = -1.0;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (long Packed : Grid) {
            if (Used.contains(Packed)) {
                continue;
            }
            Cursor.set(Packed);
            BlockPos Spot = new BlockPos(Cursor.getX(), FloorY, Cursor.getZ());
            double Distance = Spot.distSqr(Box);
            if (Distance > Far) {
                Far = Distance;
                Best = Spot;
            }
        }
        return Best;
    }

    private static void bar(LongOpenHashSet Grid, LongOpenHashSet Used, LongOpenHashSet Cells, int FloorY, Direction BoothFacing, int SpanX, int SpanZ, BlockPos Box,
                            Map<BlockPos, BlockState> Placed, List<BlockPos> Bar, List<BlockPos> BarFront) {
        Direction BoothWall = BoothFacing.getOpposite();
        int Depth = BoothFacing.getAxis() == Direction.Axis.Z ? SpanZ : SpanX;
        List<Direction> Order = new ArrayList<>();
        if (Depth >= 6) {
            Order.add(BoothWall.getOpposite());
        }
        Direction Left = BoothWall.getClockWise();
        Direction Right = BoothWall.getCounterClockWise();
        Order.add(Left);
        Order.add(Right);
        if (!Order.contains(BoothWall.getOpposite())) {
            Order.add(BoothWall.getOpposite());
        }
        for (Direction Wall : Order) {
            Direction Inward = Wall.getOpposite();
            Direction Along = Wall.getClockWise();
            List<BlockPos> Line = new ArrayList<>();
            for (long Packed : Grid) {
                BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
                Cursor.set(Packed);
                int X = Cursor.getX();
                int Z = Cursor.getZ();
                if (Grid.contains(key(X + Wall.getStepX(), Z + Wall.getStepZ()))) {
                    continue;
                }
                BlockPos Spot = new BlockPos(X, FloorY, Z);
                BlockPos Front = Spot.relative(Inward);
                if (!free(Grid, Used, Spot) || !free(Grid, Used, Front) || !Cells.contains(Spot.above().asLong())) {
                    continue;
                }
                Line.add(Spot);
            }
            if (Line.size() < BarMin) {
                continue;
            }
            Line.sort(Comparator.comparingInt(Spot -> Along.getAxis() == Direction.Axis.X ? Spot.getX() : Spot.getZ()));
            List<BlockPos> Run = longestRun(Line, Along);
            if (Run.size() < BarMin) {
                continue;
            }
            int Length = Math.max(BarMin, Math.min(BarMax, Math.min(Run.size(), Math.round(Line.size() / 3.0f))));
            int Start = (Run.size() - Length) / 2;
            List<BlockPos> Picked = Run.subList(Start, Start + Length);
            Scp465Decor[] Foods = foods(Length);
            for (int Index = 0; Index < Picked.size(); Index++) {
                BlockPos Counter = Picked.get(Index);
                Placed.put(Counter, Scp465Registry.decor(Scp465Decor.SnackCounter).defaultBlockState().setValue(Scp465PartyBlock.FACING, Inward)
                        .setValue(Scp465PartyBlock.Tint, Index % Scp465PartyBlock.Tints));
                BlockPos Top = Counter.above();
                Placed.put(Top, Scp465Registry.decor(Foods[Index]).defaultBlockState().setValue(Scp465PartyBlock.FACING, Inward)
                        .setValue(Scp465PartyBlock.Tint, Math.floorMod(Counter.getX() + Counter.getZ(), Scp465PartyBlock.Tints)));
                Used.add(key(Counter.getX(), Counter.getZ()));
                if (Foods[Index].serves()) {
                    Bar.add(Top);
                }
                BarFront.add(Counter.relative(Inward));
            }
            return;
        }
    }

    private static List<BlockPos> longestRun(List<BlockPos> Line, Direction Along) {
        List<BlockPos> Best = new ArrayList<>();
        List<BlockPos> Current = new ArrayList<>();
        for (BlockPos Spot : Line) {
            if (!Current.isEmpty()) {
                BlockPos Last = Current.get(Current.size() - 1);
                boolean Next = Last.relative(Along).equals(Spot) || Last.relative(Along.getOpposite()).equals(Spot);
                if (!Next) {
                    if (Current.size() > Best.size()) {
                        Best = new ArrayList<>(Current);
                    }
                    Current.clear();
                }
            }
            Current.add(Spot);
        }
        if (Current.size() > Best.size()) {
            Best = new ArrayList<>(Current);
        }
        return Best;
    }

    public static Scp465Decor[] foods(int Length) {
        return switch (Length) {
            case 2 -> new Scp465Decor[]{Scp465Decor.PunchBowl, Scp465Decor.CupcakeStand};
            case 3 -> new Scp465Decor[]{Scp465Decor.PunchBowl, Scp465Decor.SnackBowl, Scp465Decor.CupcakeStand};
            case 4 -> new Scp465Decor[]{Scp465Decor.SoloCups, Scp465Decor.PunchBowl, Scp465Decor.SnackBowl, Scp465Decor.CupcakeStand};
            default -> new Scp465Decor[]{Scp465Decor.SoloCups, Scp465Decor.PunchBowl, Scp465Decor.SnackBowl, Scp465Decor.CupcakeStand, Scp465Decor.SoloCups};
        };
    }

    private static void ceiling(LongOpenHashSet Grid, Map<Long, Integer> Heights, List<BlockPos> Dance, int FloorY, Map<BlockPos, BlockState> Placed, BlockPos Centre, List<BlockPos> Speakers) {
        BlockPos Disco = null;
        if (!Dance.isEmpty()) {
            double SumX = 0.0;
            double SumZ = 0.0;
            for (BlockPos Spot : Dance) {
                SumX += Spot.getX();
                SumZ += Spot.getZ();
            }
            double CentreX = SumX / Dance.size();
            double CentreZ = SumZ / Dance.size();
            Disco = Dance.stream().min(Comparator.comparingDouble(Spot -> (Spot.getX() - CentreX) * (Spot.getX() - CentreX) + (Spot.getZ() - CentreZ) * (Spot.getZ() - CentreZ))).orElse(null);
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        List<Long> Columns = new ArrayList<>(Grid);
        Columns.sort(Long::compare);
        for (long Packed : Columns) {
            Cursor.set(Packed);
            int X = Cursor.getX();
            int Z = Cursor.getZ();
            int Height = Heights.getOrDefault(Packed, 0);
            if (Height < Scp465Room.MinHeight) {
                continue;
            }
            BlockPos Top = new BlockPos(X, FloorY + Height - 1, Z);
            if (Placed.containsKey(Top)) {
                continue;
            }
            int Walled = 0;
            Direction Wall = null;
            for (Direction Side : Walls) {
                if (!Grid.contains(key(X + Side.getStepX(), Z + Side.getStepZ()))) {
                    Walled++;
                    if (Wall == null) {
                        Wall = Side;
                    }
                }
            }
            int Shade = Math.floorMod(X * 7 + Z * 3, Scp465PartyBlock.Tints);
            if (Disco != null && X == Disco.getX() && Z == Disco.getZ()) {
                Placed.put(Top, Scp465Registry.decor(Scp465Decor.DiscoBall).defaultBlockState());
                continue;
            }
            boolean OverSpeaker = Speakers.stream().anyMatch(Spot -> Spot.getX() == X && Spot.getZ() == Z);
            if (Walled >= 2 || OverSpeaker) {
                Placed.put(Top, Scp465Registry.decor(Scp465Decor.Balloons).defaultBlockState().setValue(Scp465PartyBlock.Tint, Shade));
                continue;
            }
            if (Walled == 1) {
                Placed.put(Top, Scp465Registry.decor(Scp465Decor.PartyLights).defaultBlockState().setValue(Scp465PartyBlock.FACING, Wall.getOpposite())
                        .setValue(Scp465PartyBlock.Tint, Math.floorMod(X + Z, Scp465PartyBlock.Tints)));
                continue;
            }
            if (Math.floorMod(X + 2 * Z, StreamerSpacing) == 0) {
                Placed.put(Top, Scp465Registry.decor(Scp465Decor.Streamers).defaultBlockState().setValue(Scp465PartyBlock.FACING, Math.floorMod(X + Z, 2) == 0 ? Direction.NORTH : Direction.EAST)
                        .setValue(Scp465PartyBlock.Tint, Shade));
            }
        }
    }
}
