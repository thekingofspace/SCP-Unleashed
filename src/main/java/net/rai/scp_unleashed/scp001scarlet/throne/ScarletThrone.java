package net.rai.scp_unleashed.scp001scarlet.throne;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.rai.scp_unleashed.scp001scarlet.Scp001ScarletRegistry;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScarletThrone {
    public static final int FloorRadius = 40;
    public static final int FlatRadius = 45;
    public static final int RimRadius = 52;
    public static final int ThroneRadius = 43;
    public static final int PillarRadius = 34;
    public static final int PillarHeight = 24;
    public static final int Pillars = 7;
    public static final int DaisRadius = 7;
    public static final int DaisHeight = 2;
    public static final int ClearAbove = 60;
    public static final int Sink = 3;
    public static final double PillarWidth = 2.6;
    public static final int ChainClearance = 9;
    private static final Map<Long, Plan> Cache = new HashMap<>();

    public static final int DaisGroup = 0;
    public static final int ThroneGroup = Pillars + 1;
    public static final int RuinGroup = Pillars + 2;

    public record Piece(int X, int Y, int Z, BlockState State, int Group) {
        public Piece(int X, int Y, int Z, BlockState State) {
            this(X, Y, Z, State, DaisGroup);
        }
    }

    public record Plan(long Seed, Map<Long, List<Piece>> ByChunk, List<Piece> All) {
    }

    public record Anchor(int Index, BlockPos Bracket, BlockPos Arm, Direction Facing, int Length) {
        public Anchor offset(BlockPos Throne) {
            return new Anchor(Index, Bracket.offset(Throne), Arm.offset(Throne), Facing, Length);
        }
    }

    private ScarletThrone() {
    }

    public static BlockPos dais(ServerLevel World, BlockPos Throne) {
        return Throne.above(DaisHeight);
    }

    public static BlockPos dais(MinecraftServer Server) {
        return dais(Server.overworld(), ScarletState.get(Server).throne());
    }

    public static double angle(int Index) {
        return Index * Mth.TWO_PI / Pillars;
    }

    public static int[] pillarOffset(int Index) {
        double Angle = angle(Index);
        return new int[]{(int) Math.round(Math.sin(Angle) * PillarRadius), (int) Math.round(-Math.cos(Angle) * PillarRadius)};
    }

    public static List<BlockPos> pillars(ServerLevel World, BlockPos Throne) {
        List<BlockPos> Found = new ArrayList<>();
        for (int Index = 0; Index < Pillars; Index++) {
            int[] Offset = pillarOffset(Index);
            Found.add(Throne.offset(Offset[0], PillarHeight, Offset[1]));
        }
        return Found;
    }

    public static List<BlockPos> pillars(MinecraftServer Server) {
        return pillars(Server.overworld(), ScarletState.get(Server).throne());
    }

    @Nullable
    public static Long seedFor(ServerLevel World, BlockPos Throne) {
        if (World != World.getServer().overworld()) {
            return null;
        }
        ScarletThroneSite Site = ScarletThroneSite.get(World.getServer());
        return Site.planned() && Site.centre().equals(Throne) ? Site.seed() : null;
    }

    public static List<BlockPos> anchorBlocks(ServerLevel World, BlockPos Throne) {
        Long Seed = seedFor(World, Throne);
        if (Seed == null) {
            return pillars(World, Throne);
        }
        List<BlockPos> Found = new ArrayList<>();
        for (Anchor Each : anchors(Seed)) {
            BlockPos At = Throne.offset(Each.Arm());
            if (World.isLoaded(At) && World.getBlockState(At).getBlock() instanceof ScarletAnchorBlock) {
                Found.add(At);
            } else {
                int[] Centre = pillarOffset(Each.Index());
                Found.add(Throne.offset(Centre[0], brokenTop(Each.Index(), Seed), Centre[1]));
            }
        }
        return Found;
    }

    public static Vec3 anchorPoint(ServerLevel World, BlockPos Throne, int Index) {
        BlockPos At = anchorBlocks(World, Throne).get(Index);
        BlockState Here = World.isLoaded(At) ? World.getBlockState(At) : Blocks.AIR.defaultBlockState();
        return Here.getBlock() instanceof ScarletAnchorBlock ? ScarletAnchorBlock.ring(At, Here.getValue(ScarletAnchorBlock.FACING)) : Vec3.atCenterOf(At);
    }

    public static void anchorState(ServerLevel World, BlockPos At, boolean Snapped) {
        if (!World.isLoaded(At)) {
            return;
        }
        BlockState Here = World.getBlockState(At);
        if (!(Here.getBlock() instanceof ScarletAnchorBlock) || Here.getValue(ScarletAnchorBlock.Broken) == Snapped) {
            return;
        }
        World.setBlock(At, Here.setValue(ScarletAnchorBlock.Broken, Snapped), Block.UPDATE_CLIENTS);
        if (Snapped) {
            Vec3 Ring = ScarletAnchorBlock.ring(At, Here.getValue(ScarletAnchorBlock.FACING));
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CHAIN.defaultBlockState()), Ring.x, Ring.y, Ring.z, 16, 0.25, 0.25, 0.25, 0.15);
            World.sendParticles(ParticleTypes.LAVA, Ring.x, Ring.y, Ring.z, 3, 0.1, 0.1, 0.1, 0.0);
        }
    }

    public static void restoreAnchors(ServerLevel World, BlockPos Throne) {
        Long Seed = seedFor(World, Throne);
        if (Seed == null) {
            return;
        }
        for (Anchor Each : anchors(Seed)) {
            anchorState(World, Throne.offset(Each.Arm()), false);
        }
    }

    public static int refit(ServerLevel World, BlockPos Throne) {
        Long Seed = seedFor(World, Throne);
        if (Seed == null) {
            return 0;
        }
        int Changed = 0;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Index = 0; Index < Pillars; Index++) {
            int[] Centre = pillarOffset(Index);
            double Inward = Math.atan2(-Centre[0], -Centre[1]);
            int Hx = (int) Math.round(Centre[0] + Math.sin(Inward) * (PillarWidth + 1.0));
            int Hz = (int) Math.round(Centre[1] + Math.cos(Inward) * (PillarWidth + 1.0));
            int Lx = (int) Math.round(Centre[0] + Math.sin(Inward) * PillarWidth);
            int Lz = (int) Math.round(Centre[1] + Math.cos(Inward) * PillarWidth);
            int Hang = 20 - hash(Index, 9, 0, Seed) % 3;
            int Length = 6 + hash(Index, 11, 0, Seed) % 5;
            Anchor Mount = anchor(Index, Seed);
            BlockPos Arm = Mount.Arm();
            List<int[]> Old = new ArrayList<>();
            Old.add(new int[]{Lx, Hang + 1, Lz});
            Old.add(new int[]{Hx, Hang + 1, Hz});
            for (int Step = 0; Step < Length; Step++) {
                Old.add(new int[]{Hx, Hang - Step, Hz});
            }
            for (int[] Cell : Old) {
                boolean Mine = Cell[0] == Arm.getX() && Cell[2] == Arm.getZ();
                boolean Stale = Mine ? Cell[1] > Arm.getY() || Cell[1] < Arm.getY() - Mount.Length() : columnTop(Index, Cell[0], Cell[2], Seed) < Cell[1];
                if (!Stale) {
                    continue;
                }
                Cursor.set(Throne.getX() + Cell[0], Throne.getY() + Cell[1], Throne.getZ() + Cell[2]);
                BlockState Here = World.getBlockState(Cursor);
                if (Here.is(Blocks.CHAIN) || Here.is(Blocks.POLISHED_BLACKSTONE_BRICK_WALL) || Here.is(Blocks.POLISHED_BLACKSTONE_BRICKS)) {
                    World.setBlock(Cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    Changed++;
                }
            }
            BlockPos Bracket = Mount.Bracket();
            Cursor.set(Throne.getX() + Bracket.getX(), Throne.getY() + Bracket.getY(), Throne.getZ() + Bracket.getZ());
            if (!World.getBlockState(Cursor).isAir() && !World.getBlockState(Cursor).is(Blocks.CHISELED_POLISHED_BLACKSTONE)) {
                World.setBlock(Cursor, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                Changed++;
            }
            Cursor.set(Throne.getX() + Arm.getX(), Throne.getY() + Arm.getY(), Throne.getZ() + Arm.getZ());
            BlockState Here = World.getBlockState(Cursor);
            if (!(Here.getBlock() instanceof ScarletAnchorBlock) && (Here.isAir() || Here.is(Blocks.CHAIN) || Here.is(Blocks.POLISHED_BLACKSTONE_BRICK_WALL))) {
                World.setBlock(Cursor, Scp001ScarletRegistry.ChainAnchor.get().defaultBlockState().setValue(ScarletAnchorBlock.FACING, Mount.Facing()), Block.UPDATE_CLIENTS);
                Changed++;
            }
            for (int Step = 1; Step <= Mount.Length(); Step++) {
                Cursor.set(Throne.getX() + Arm.getX(), Throne.getY() + Arm.getY() - Step, Throne.getZ() + Arm.getZ());
                if (World.getBlockState(Cursor).isAir()) {
                    World.setBlock(Cursor, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y), Block.UPDATE_CLIENTS);
                    Changed++;
                }
            }
        }
        return Changed;
    }

    public static boolean inArena(BlockPos Throne, BlockPos Pos) {
        double Dx = Pos.getX() - Throne.getX();
        double Dz = Pos.getZ() - Throne.getZ();
        return Dx * Dx + Dz * Dz <= (double) RimRadius * RimRadius && Pos.getY() >= Throne.getY() - 8 && Pos.getY() <= Throne.getY() + ClearAbove;
    }

    public static int hash(int X, int Y, int Z, long Seed) {
        long Value = X * 73856093L ^ Y * 19349663L ^ Z * 83492791L ^ Seed * 2654435761L;
        Value ^= Value >>> 17;
        Value *= 0xED5AD4BBL;
        Value ^= Value >>> 11;
        return (int) (Value & 0x7FFFFFFF);
    }

    public static float chance(int X, int Y, int Z, long Seed) {
        return (hash(X, Y, Z, Seed) % 10000) / 10000.0f;
    }

    public static BlockState floor(int Dx, int Dz, long Seed) {
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        double Angle = Math.atan2(Dx, -Dz);
        double Slice = Mth.TWO_PI / Pillars;
        double Spoke = Math.abs(Mth.wrapDegrees(Math.toDegrees(Angle - Math.round(Angle / Slice) * Slice)));
        float Roll = chance(Dx, 0, Dz, Seed);
        if (Distance > DaisRadius + 2 && Distance < PillarRadius - 3 && Spoke * Distance * Mth.DEG_TO_RAD < 1.0) {
            return Roll < 0.12f ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (Math.abs(Distance - (DaisRadius + 3)) < 0.5 || Math.abs(Distance - (PillarRadius - 4)) < 0.5) {
            return Roll < 0.85f ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
        }
        if (Distance > FloorRadius - 3) {
            return Roll < 0.45f ? Blocks.BLACKSTONE.defaultBlockState() : Roll < 0.75f ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (Roll < 0.08f) {
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        if (Roll < 0.3f) {
            return Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
        }
        if (Roll < 0.38f) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        return Blocks.DEEPSLATE_TILES.defaultBlockState();
    }

    public static BlockState rim(int Dx, int Dz, int Depth, long Seed) {
        float Roll = chance(Dx, Depth, Dz, Seed + 5);
        if (Depth > 0) {
            return Roll < 0.6f ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
        }
        return Roll < 0.35f ? Blocks.BLACKSTONE.defaultBlockState() : Roll < 0.65f ? Blocks.COARSE_DIRT.defaultBlockState() : Roll < 0.85f ? Blocks.GRAVEL.defaultBlockState()
                : Blocks.TUFF.defaultBlockState();
    }

    public static int surface(int Dx, int Dz, int Natural, int Floor) {
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Distance <= FlatRadius) {
            return Floor;
        }
        double Share = Mth.clamp((Distance - FlatRadius) / (RimRadius - FlatRadius), 0.0, 1.0);
        double Eased = Share * Share * (3.0 - 2.0 * Share);
        int Lifted = Floor + (int) Math.round(Math.max(0, Natural - Floor) * Eased);
        return Math.min(Math.max(Floor, Natural), Math.max(Floor, Lifted));
    }

    public static synchronized Plan plan(long Seed) {
        Plan Known = Cache.get(Seed);
        if (Known != null) {
            return Known;
        }
        List<Piece> All = new ArrayList<>();
        dais(All, Seed);
        for (int Index = 0; Index < Pillars; Index++) {
            List<Piece> Column = new ArrayList<>();
            pillar(Column, Index, Seed);
            for (Piece Each : Column) {
                All.add(new Piece(Each.X(), Each.Y(), Each.Z(), Each.State(), Index + 1));
            }
            List<Piece> Seat = new ArrayList<>();
            throne(Seat, Index, Seed);
            for (Piece Each : Seat) {
                All.add(new Piece(Each.X(), Each.Y(), Each.Z(), Each.State(), ThroneGroup));
            }
        }
        List<Piece> Fallen = new ArrayList<>();
        ruins(Fallen, Seed);
        for (Piece Each : Fallen) {
            All.add(new Piece(Each.X(), Each.Y(), Each.Z(), Each.State(), RuinGroup));
        }
        Map<Long, List<Piece>> ByChunk = new HashMap<>();
        for (Piece Each : All) {
            ByChunk.computeIfAbsent(chunkKey(Each.X(), Each.Z()), Key -> new ArrayList<>()).add(Each);
        }
        Plan Made = new Plan(Seed, ByChunk, List.copyOf(All));
        if (Cache.size() > 4) {
            Cache.clear();
        }
        Cache.put(Seed, Made);
        return Made;
    }

    public static long chunkKey(int Dx, int Dz) {
        return ((long) (Dx >> 4) << 32) ^ ((Dz >> 4) & 0xFFFFFFFFL);
    }

    private static void dais(List<Piece> All, long Seed) {
        for (int Dx = -DaisRadius; Dx <= DaisRadius; Dx++) {
            for (int Dz = -DaisRadius; Dz <= DaisRadius; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                if (Distance > DaisRadius + 0.4) {
                    continue;
                }
                boolean Ring = Distance > DaisRadius - 1.2;
                All.add(new Piece(Dx, 0, Dz, Ring && hash(Dx, 0, Dz, Seed) % 7 == 0 ? Scp001ScarletRegistry.GlyphStone.get().defaultBlockState()
                        : Ring ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState()));
                if (Distance <= DaisRadius - 2.5) {
                    BlockState Top;
                    if (Distance < 1.0) {
                        Top = Scp001ScarletRegistry.GlyphStone.get().defaultBlockState();
                    } else if (Math.abs(Distance - 3.0) < 0.6) {
                        Top = Blocks.BONE_BLOCK.defaultBlockState();
                    } else {
                        Top = chance(Dx, 1, Dz, Seed) < 0.15f ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                    }
                    All.add(new Piece(Dx, 1, Dz, Top));
                } else if (Distance <= DaisRadius - 1.2) {
                    All.add(new Piece(Dx, 1, Dz, Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)));
                }
            }
        }
    }

    public static int brokenTop(int Index, long Seed) {
        return PillarHeight - 1 - hash(Index, 3, 0, Seed) % 3;
    }

    public static int columnTop(int Index, int X, int Z, long Seed) {
        int[] Centre = pillarOffset(Index);
        double Distance = Math.sqrt((X - Centre[0]) * (X - Centre[0]) + (Z - Centre[1]) * (Z - Centre[1]));
        if (Distance > PillarWidth) {
            return -1;
        }
        return brokenTop(Index, Seed) - (Distance > PillarWidth - 1.0 ? hash(X, 7, Z, Seed) % 3 : 0);
    }

    public static Direction inward(int Index) {
        int[] Centre = pillarOffset(Index);
        return facing(Centre[0], Centre[1]);
    }

    public static Anchor anchor(int Index, long Seed) {
        int[] Centre = pillarOffset(Index);
        Direction Toward = inward(Index);
        int Bx = Centre[0];
        int Bz = Centre[1];
        for (int Step = 1; Step <= (int) Math.ceil(PillarWidth) + 1; Step++) {
            int X = Centre[0] + Toward.getStepX() * Step;
            int Z = Centre[1] + Toward.getStepZ() * Step;
            if (columnTop(Index, X, Z, Seed) < 0) {
                break;
            }
            Bx = X;
            Bz = Z;
        }
        int Hang = 20 - hash(Index, 9, 0, Seed) % 3;
        int Y = Math.min(Hang + 1, columnTop(Index, Bx, Bz, Seed) - 2);
        int Length = Math.max(2, Math.min(6 + hash(Index, 11, 0, Seed) % 5, Y - ChainClearance));
        BlockPos Bracket = new BlockPos(Bx, Y, Bz);
        return new Anchor(Index, Bracket, Bracket.relative(Toward), Toward, Length);
    }

    public static List<Anchor> anchors(long Seed) {
        List<Anchor> Found = new ArrayList<>();
        for (int Index = 0; Index < Pillars; Index++) {
            Found.add(anchor(Index, Seed));
        }
        return Found;
    }

    private static void pillar(List<Piece> All, int Index, long Seed) {
        int[] Centre = pillarOffset(Index);
        int Reach = (int) Math.ceil(PillarWidth) + 1;
        int Broken = brokenTop(Index, Seed);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                int X = Centre[0] + Dx;
                int Z = Centre[1] + Dz;
                if (Distance <= PillarWidth + 1.2 && Distance > PillarWidth) {
                    All.add(new Piece(X, 0, Z, chance(X, 0, Z, Seed) < 0.7f ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                            : Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()));
                    continue;
                }
                if (Distance > PillarWidth) {
                    continue;
                }
                int Edge = Distance > PillarWidth - 1.0 ? 1 : 0;
                int Top = Broken - (Edge == 1 ? hash(X, 7, Z, Seed) % 3 : 0);
                for (int Y = 0; Y <= Top; Y++) {
                    All.add(new Piece(X, Y, Z, pillarBlock(X, Y, Z, Edge == 1, Index, Seed)));
                }
            }
        }
        Anchor Mount = anchor(Index, Seed);
        BlockPos Bracket = Mount.Bracket();
        BlockPos Arm = Mount.Arm();
        All.add(new Piece(Bracket.getX(), Bracket.getY(), Bracket.getZ(), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()));
        All.add(new Piece(Bracket.getX(), Bracket.getY() + 1, Bracket.getZ(), Blocks.POLISHED_BLACKSTONE.defaultBlockState()));
        All.add(new Piece(Arm.getX(), Arm.getY(), Arm.getZ(), Scp001ScarletRegistry.ChainAnchor.get().defaultBlockState().setValue(ScarletAnchorBlock.FACING, Mount.Facing())));
        for (int Step = 1; Step <= Mount.Length(); Step++) {
            All.add(new Piece(Arm.getX(), Arm.getY() - Step, Arm.getZ(), Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y)));
        }
    }

    private static BlockState pillarBlock(int X, int Y, int Z, boolean Edge, int Index, long Seed) {
        float Roll = chance(X, Y, Z, Seed + Index);
        if (Y == 0 || Y == PillarHeight - 6) {
            return Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (Edge && (Y == 6 || Y == 12 || Y == 13)) {
            return Roll < 0.55f ? Scp001ScarletRegistry.GlyphStone.get().defaultBlockState() : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (Y == 5 || Y == 7 || Y == 11 || Y == 14) {
            return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (Roll < 0.22f) {
            return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (Roll < 0.3f) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (Roll < 0.34f && Y > PillarHeight / 2) {
            return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        }
        return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    public static Direction facing(int Dx, int Dz) {
        if (Math.abs(Dx) > Math.abs(Dz)) {
            return Dx > 0 ? Direction.WEST : Direction.EAST;
        }
        return Dz > 0 ? Direction.NORTH : Direction.SOUTH;
    }

    private static void throne(List<Piece> All, int Index, long Seed) {
        double Angle = angle(Index) + Mth.PI / Pillars;
        int Cx = (int) Math.round(Math.sin(Angle) * ThroneRadius);
        int Cz = (int) Math.round(-Math.cos(Angle) * ThroneRadius);
        Direction Front = facing(Cx, Cz);
        Direction Side = Front.getClockWise();
        Direction Back = Front.getOpposite();
        int Ruin = hash(Index, 13, 0, Seed) % 4;
        for (int Across = -2; Across <= 2; Across++) {
            for (int Deep = -1; Deep <= 2; Deep++) {
                int X = Cx + Side.getStepX() * Across + Back.getStepX() * Deep;
                int Z = Cz + Side.getStepZ() * Across + Back.getStepZ() * Deep;
                All.add(new Piece(X, 0, Z, Blocks.POLISHED_BLACKSTONE.defaultBlockState()));
            }
        }
        for (int Across = -1; Across <= 1; Across++) {
            int X = Cx + Side.getStepX() * Across;
            int Z = Cz + Side.getStepZ() * Across;
            All.add(new Piece(X, 1, Z, Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Back)));
            int Bx = X + Back.getStepX();
            int Bz = Z + Back.getStepZ();
            All.add(new Piece(Bx, 1, Bz, Blocks.RED_NETHER_BRICKS.defaultBlockState()));
            int Height = 5 - (Across == 0 ? 0 : 1) - (Across == Ruin - 2 ? 2 + Ruin % 2 : 0);
            for (int Y = 2; Y <= Height; Y++) {
                BlockState Made = Y == Height && Across == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()
                        : Y == 3 ? Blocks.RED_NETHER_BRICKS.defaultBlockState() : chance(Bx, Y, Bz, Seed) < 0.3f ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                All.add(new Piece(Bx, Y, Bz, Made));
            }
            if (Across == 0 && Ruin != 2) {
                All.add(new Piece(Bx, Height + 1, Bz, Blocks.BONE_BLOCK.defaultBlockState()));
            }
        }
        for (int Sign = -1; Sign <= 1; Sign += 2) {
            int X = Cx + Side.getStepX() * 2 * Sign;
            int Z = Cz + Side.getStepZ() * 2 * Sign;
            if (Sign * 2 == Ruin - 2) {
                continue;
            }
            All.add(new Piece(X, 1, Z, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState()));
            All.add(new Piece(X + Back.getStepX(), 1, Z + Back.getStepZ(), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()));
            All.add(new Piece(X + Back.getStepX(), 2, Z + Back.getStepZ(), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState()));
        }
    }

    private static void ruins(List<Piece> All, long Seed) {
        int Count = 8;
        for (int Index = 0; Index < Count; Index++) {
            double Angle = (Index + 0.5) * Mth.TWO_PI / Count + (chance(Index, 1, 0, Seed) - 0.5) * 0.5;
            double Radius = 16.0 + chance(Index, 2, 0, Seed) * 12.0;
            int Cx = (int) Math.round(Math.sin(Angle) * Radius);
            int Cz = (int) Math.round(-Math.cos(Angle) * Radius);
            boolean AlongX = hash(Index, 3, 0, Seed) % 2 == 0;
            int Length = 3 + hash(Index, 4, 0, Seed) % 3;
            for (int Step = 0; Step < Length; Step++) {
                int X = Cx + (AlongX ? Step - Length / 2 : 0);
                int Z = Cz + (AlongX ? 0 : Step - Length / 2);
                int Tall = 1 + hash(X, 5, Z, Seed) % 3;
                for (int Y = 0; Y < Tall; Y++) {
                    BlockState Made = chance(X, Y, Z, Seed) < 0.4f ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                            : Y == Tall - 1 && chance(X, Y, Z, Seed + 1) < 0.3f ? Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState()
                            : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                    All.add(new Piece(X, Y, Z, Made));
                }
            }
        }
    }
}
