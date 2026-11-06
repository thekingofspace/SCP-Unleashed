package net.rai.scp_unleashed.scp407;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class Scp407Growth {
    public static final int MaxSpread = 3000;
    public static final float StartRate = 0.08f;
    public static final float PeakRate = 1.0f;
    public static final int RampTicks = 3000;
    public static final int MaxStepsPerTick = 16;
    public static final float LandingChance = 0.08f;
    public static final float TipChance = 0.3f;
    public static final float TrunkStart = 0.025f;
    public static final float TrunkPeak = 0.09f;
    public static final float SprigChance = 0.3f;
    public static final float HangingChance = 0.07f;
    public static final float WallBloomChance = 0.035f;
    public static final float BranchChance = 0.3f;
    public static final int SeedRadius = 3;
    public static final int MaxTrunk = 7;
    public static final int SoundGap = 5;
    public static final float PuffChancePerBloom = 0.0012f;
    public static final float WrapChance = 0.6f;
    public static final float FloorPlantChance = 0.55f;
    public static final float UpgradeChance = 0.18f;
    public static final float UpgradeTake = 0.55f;
    public static final float CeilingPlantChance = 0.34f;
    public static final float WallPlantChance = 0.045f;
    public static final float CeilingUpgrade = 0.7f;
    public static final float WallUpgrade = 0.08f;
    public static final float BulbShare = 0.38f;
    public static final float HangUnderBranch = 0.45f;
    public static final int TrunkSpacing = 2;
    public static final int ClearingRadius = 2;

    private Scp407Growth() {
    }

    public static boolean free(BlockState State) {
        if (State.isAir()) {
            return true;
        }
        return State.is(BlockTags.REPLACEABLE) && State.getFluidState().isEmpty() && !State.is(BlockTags.FIRE) && !State.is(Blocks.LIGHT) && !State.is(Blocks.SNOW)
                && !State.is(Blocks.STRUCTURE_VOID) && !State.hasBlockEntity();
    }

    public static boolean carpet(BlockState State) {
        return State.getBlock() instanceof Scp407MatBlock || State.getBlock() instanceof Scp407SprigBlock;
    }

    public static boolean soft(BlockState State) {
        return State.getBlock() instanceof Scp407GrowthBlock Growth && Growth.soft(State);
    }

    public static boolean roomFor(BlockState State) {
        return free(State) || soft(State);
    }

    public static boolean openCarpet(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return carpet(State) && Scp407GrowthBlock.alive(State) && !Scp407Faces.open(World, Pos, State).isEmpty();
    }

    public static boolean place(ServerLevel World, Scp407Session Session, BlockPos Pos, BlockState Growth) {
        long Key = Pos.asLong();
        if (!Session.Region.contains(Key) || !World.isLoaded(Pos)) {
            return false;
        }
        BlockState Current = World.getBlockState(Pos);
        boolean Carpet = soft(Current);
        if (!free(Current) && !(Carpet && !soft(Growth))) {
            return false;
        }
        if (Growth.getBlock() instanceof Scp407GrowthBlock Kind) {
            Growth = Kind.dress(World, Pos, Growth);
        }
        if (!Growth.getCollisionShape(World, Pos).isEmpty()) {
            AABB Space = Growth.getCollisionShape(World, Pos).bounds().move(Pos);
            for (LivingEntity Inside : World.getEntitiesOfClass(LivingEntity.class, Space)) {
                if (Inside.isAlive()) {
                    return false;
                }
            }
        }
        if (!Current.isAir() && !Carpet) {
            Session.Replaced.put(Key, Current);
        }
        if (!World.setBlock(Pos, Growth, Block.UPDATE_ALL)) {
            if (!Carpet) {
                Session.Replaced.remove(Key);
            }
            return false;
        }
        Session.Grown.add(Key);
        Session.Frontier.remove(Key);
        return true;
    }

    public static boolean sturdy(ServerLevel World, BlockPos Pos, Direction Toward) {
        BlockPos Support = Pos.relative(Toward);
        return World.getBlockState(Support).isFaceSturdy(World, Support, Toward.getOpposite());
    }

    public static void ensureSurface(ServerLevel World, Scp407Session Session) {
        if (!Session.Surface.isEmpty() || Session.Region.isEmpty()) {
            return;
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        LongIterator Cells = Session.Region.iterator();
        while (Cells.hasNext()) {
            long Key = Cells.nextLong();
            Cursor.set(Key);
            for (Direction Side : Direction.values()) {
                BlockPos Next = Cursor.relative(Side);
                if (!Session.Region.contains(Next.asLong()) && sturdy(World, Cursor, Side)) {
                    Session.Surface.add(Key);
                    break;
                }
            }
        }
        LongIterator Grown = Session.Grown.iterator();
        while (Grown.hasNext()) {
            spreadFrom(World, Session, BlockPos.of(Grown.nextLong()));
        }
        if (Session.FrontierList.isEmpty()) {
            seed(Session);
        }
    }

    private static void seed(Scp407Session Session) {
        for (int Index = 0; Index < Session.Surface.size(); Index++) {
            long Key = Session.Surface.getLong(Index);
            if (BlockPos.of(Key).distManhattan(Session.Origin) <= SeedRadius * 2) {
                addFrontier(Session, Key);
            }
        }
    }

    private static void addFrontier(Scp407Session Session, long Key) {
        if (Session.Frontier.add(Key)) {
            Session.FrontierList.add(Key);
        }
    }

    private static void spreadFrom(ServerLevel World, Scp407Session Session, BlockPos Pos) {
        LongOpenHashSet Surface = Session.SurfaceSet;
        if (Surface.size() != Session.Surface.size()) {
            Surface.clear();
            Surface.addAll(Session.Surface);
        }
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dy = -1; Dy <= 1; Dy++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    long Key = BlockPos.asLong(Pos.getX() + Dx, Pos.getY() + Dy, Pos.getZ() + Dz);
                    if (Surface.contains(Key) && (!Session.Grown.contains(Key) || openCarpet(World, BlockPos.of(Key)))) {
                        addFrontier(Session, Key);
                    }
                }
            }
        }
    }

    public static void tick(ServerLevel World, Scp407Session Session, long Position) {
        if (!World.isLoaded(Session.Origin)) {
            return;
        }
        ensureSurface(World, Session);
        float Share = Mth.clamp((Position - Session.ActTick) / (float) RampTicks, 0.0f, 1.0f);
        Session.Debt += StartRate + (PeakRate - StartRate) * Share;
        int Steps = 0;
        while (Session.Debt >= 1.0f && Steps < MaxStepsPerTick) {
            Session.Debt -= 1.0f;
            Steps++;
            if (Session.Spread >= Session.Cap) {
                Session.Debt = 0.0f;
                break;
            }
            step(World, Session, Share);
        }
        puff(World, Session);
    }

    private static void step(ServerLevel World, Scp407Session Session, float Share) {
        RandomSource Random = Session.random();
        if (!Session.Tips.isEmpty() && Random.nextFloat() < TipChance) {
            extendTip(World, Session, Random.nextInt(Session.Tips.size()));
            return;
        }
        if (!Session.Surface.isEmpty() && Random.nextFloat() < UpgradeChance) {
            upgrade(World, Session, BlockPos.of(Session.Surface.getLong(Random.nextInt(Session.Surface.size()))), Random);
            return;
        }
        BlockPos Cell = pick(World, Session, Random);
        if (Cell == null) {
            return;
        }
        BlockState Current = World.getBlockState(Cell);
        if (carpet(Current)) {
            cover(World, Session, Cell, Current, Random);
            return;
        }
        int Near = clearing(Session, Cell);
        boolean Plain = supports(World, Cell) == 1 && Near > 1;
        boolean Tall = Near > ClearingRadius;
        if (sturdy(World, Cell, Direction.DOWN)) {
            float TrunkChance = TrunkStart + (TrunkPeak - TrunkStart) * Share;
            if (Tall && Random.nextFloat() < TrunkChance && startTrunk(World, Session, Cell)) {
                return;
            }
            BlockState Growth = Plain && Random.nextFloat() < FloorPlantChance ? floorPlant(World, Cell, Random, !Tall)
                    : wrap(World, Cell, Scp407MatBlock.covering(List.of(Direction.DOWN)), Random);
            grow(World, Session, Cell, Growth, false);
            return;
        }
        if (sturdy(World, Cell, Direction.UP)) {
            BlockState Growth;
            if (Random.nextFloat() < HangingChance) {
                Growth = Scp407Registry.Bloom.get().defaultBlockState().setValue(Scp407BloomBlock.Attached, Direction.UP);
            } else if (Plain && Random.nextFloat() < CeilingPlantChance) {
                Growth = hanging(Random);
            } else {
                Growth = wrap(World, Cell, Scp407MatBlock.covering(List.of(Direction.UP)), Random);
            }
            grow(World, Session, Cell, Growth, false);
            return;
        }
        for (Direction Side : Direction.Plane.HORIZONTAL.shuffledCopy(Random)) {
            if (sturdy(World, Cell, Side)) {
                BlockState Growth;
                if (Random.nextFloat() < WallBloomChance) {
                    Growth = Scp407Registry.Bloom.get().defaultBlockState().setValue(Scp407BloomBlock.Attached, Side);
                } else if (Plain && Random.nextFloat() < WallPlantChance) {
                    Growth = Scp407Registry.Shelf.get().defaultBlockState().setValue(Scp407ShelfBlock.Attached, Side);
                } else {
                    Growth = wrap(World, Cell, Scp407MatBlock.covering(List.of(Side)), Random);
                }
                grow(World, Session, Cell, Growth, false);
                return;
            }
        }
    }

    private static void upgrade(ServerLevel World, Scp407Session Session, BlockPos Cell, RandomSource Random) {
        if (!World.isLoaded(Cell) || !Session.Grown.contains(Cell.asLong())) {
            return;
        }
        BlockState Current = World.getBlockState(Cell);
        if (!(Current.getBlock() instanceof Scp407MatBlock) || !Scp407GrowthBlock.alive(Current) || Scp407Faces.count(Current) != 1 || supports(World, Cell) != 1) {
            return;
        }
        int Near = clearing(Session, Cell);
        if (Near <= 1 || Random.nextFloat() > UpgradeTake) {
            return;
        }
        BlockState Plant;
        if (Scp407Faces.has(Current, Direction.DOWN)) {
            Plant = floorPlant(World, Cell, Random, Near <= ClearingRadius);
        } else if (Scp407Faces.has(Current, Direction.UP)) {
            if (Random.nextFloat() > CeilingUpgrade) {
                return;
            }
            Plant = hanging(Random);
        } else {
            Direction Side = null;
            for (Direction Each : Direction.Plane.HORIZONTAL) {
                if (Scp407Faces.has(Current, Each)) {
                    Side = Each;
                }
            }
            if (Side == null || Random.nextFloat() > WallUpgrade) {
                return;
            }
            Plant = Scp407Registry.Shelf.get().defaultBlockState().setValue(Scp407ShelfBlock.Attached, Side);
        }
        if (Plant.getBlock() instanceof Scp407GrowthBlock Kind) {
            Plant = Kind.dress(World, Cell, Plant);
        }
        if (!Plant.getCollisionShape(World, Cell).isEmpty()) {
            AABB Space = Plant.getCollisionShape(World, Cell).bounds().move(Cell);
            if (!World.getEntitiesOfClass(LivingEntity.class, Space, LivingEntity::isAlive).isEmpty()) {
                return;
            }
        }
        World.setBlock(Cell, Plant, Block.UPDATE_ALL);
    }

    public static int supports(ServerLevel World, BlockPos Cell) {
        int Count = 0;
        for (Direction Side : Direction.values()) {
            if (sturdy(World, Cell, Side)) {
                Count++;
            }
        }
        return Count;
    }

    public static int clearing(Scp407Session Session, BlockPos Cell) {
        int Best = reach(Cell, Session.Origin);
        for (Scp407Session.Listener Entry : Session.Listeners.values()) {
            if (Entry.Anchor != null) {
                Best = Math.min(Best, reach(Cell, Entry.Anchor));
            }
        }
        return Best;
    }

    private static int reach(BlockPos First, BlockPos Second) {
        if (Math.abs(First.getY() - Second.getY()) > 3) {
            return Integer.MAX_VALUE / 2;
        }
        return Math.max(Math.abs(First.getX() - Second.getX()), Math.abs(First.getZ() - Second.getZ()));
    }

    private static BlockState floorPlant(ServerLevel World, BlockPos Cell, RandomSource Random, boolean LowOnly) {
        int Total = 0;
        for (int Index = 0; Index < FloorWeights.length; Index++) {
            if (!LowOnly || !TallFloor[Index]) {
                Total += FloorWeights[Index];
            }
        }
        int Roll = Random.nextInt(Total);
        for (int Index = 0; Index < FloorWeights.length; Index++) {
            if (LowOnly && TallFloor[Index]) {
                continue;
            }
            Roll -= FloorWeights[Index];
            if (Roll < 0) {
                BlockState Plant = floorBlock(Index).defaultBlockState();
                return Plant.getBlock() instanceof Scp407SprigBlock ? wrap(World, Cell, Plant, Random) : Plant;
            }
        }
        return Scp407Registry.Fern.get().defaultBlockState();
    }

    private static final int[] FloorWeights = {14, 20, 12, 14, 12, 16, 12};
    private static final boolean[] TallFloor = {false, false, false, false, false, true, true};

    private static Block floorBlock(int Index) {
        return switch (Index) {
            case 0 -> Scp407Registry.Sprig.get();
            case 1 -> Scp407Registry.Fern.get();
            case 2 -> Scp407Registry.Roots.get();
            case 3 -> Scp407Registry.Creeper.get();
            case 4 -> Scp407Registry.Puffball.get();
            case 5 -> Scp407Registry.Reeds.get();
            default -> Scp407Registry.Coral.get();
        };
    }

    private static BlockState hanging(RandomSource Random) {
        return Random.nextFloat() < BulbShare ? Scp407Registry.Bulbs.get().defaultBlockState() : Scp407Registry.Tendril.get().defaultBlockState();
    }

    public static BlockState wrap(ServerLevel World, BlockPos Cell, BlockState Carpet, RandomSource Random) {
        BlockState Out = Carpet;
        for (Direction Side : Scp407Faces.open(World, Cell, Carpet)) {
            if (Random.nextFloat() < WrapChance) {
                Out = Scp407Faces.with(Out, Side, true);
            }
        }
        return Out;
    }

    private static void cover(ServerLevel World, Scp407Session Session, BlockPos Cell, BlockState Current, RandomSource Random) {
        List<Direction> Open = Scp407Faces.open(World, Cell, Current);
        if (Open.isEmpty() || !Scp407GrowthBlock.alive(Current) || !Session.Grown.contains(Cell.asLong())) {
            return;
        }
        BlockState Covered = Scp407Faces.with(Current, Open.get(Random.nextInt(Open.size())), true);
        for (Direction Side : Open) {
            if (!Scp407Faces.has(Covered, Side) && Random.nextFloat() < WrapChance * 0.5f) {
                Covered = Scp407Faces.with(Covered, Side, true);
            }
        }
        if (World.setBlock(Cell, Covered, Block.UPDATE_ALL)) {
            spreadFrom(World, Session, Cell);
        }
    }

    private static BlockPos pick(ServerLevel World, Scp407Session Session, RandomSource Random) {
        for (int Attempt = 0; Attempt < 10; Attempt++) {
            long Key;
            if (Session.FrontierList.isEmpty() || Random.nextFloat() < LandingChance) {
                if (Session.Surface.isEmpty()) {
                    return null;
                }
                Key = Session.Surface.getLong(Random.nextInt(Session.Surface.size()));
            } else {
                int Index = Random.nextInt(Session.FrontierList.size());
                Key = Session.FrontierList.getLong(Index);
                int Last = Session.FrontierList.size() - 1;
                Session.FrontierList.set(Index, Session.FrontierList.getLong(Last));
                Session.FrontierList.removeLong(Last);
                if (!Session.Frontier.remove(Key)) {
                    continue;
                }
            }
            BlockPos Cell = BlockPos.of(Key);
            if (World.isLoaded(Cell) && (free(World.getBlockState(Cell)) || openCarpet(World, Cell))) {
                return Cell;
            }
        }
        return null;
    }

    private static boolean grow(ServerLevel World, Scp407Session Session, BlockPos Cell, BlockState Growth, boolean Trunk) {
        boolean Fresh = !Session.Grown.contains(Cell.asLong());
        if (Fresh && Session.Spread >= Session.Cap) {
            return false;
        }
        if (!place(World, Session, Cell, Growth)) {
            return false;
        }
        if (Fresh) {
            Session.Spread++;
        }
        spreadFrom(World, Session, Cell);
        RandomSource Random = World.getRandom();
        if (Random.nextFloat() < 0.35f) {
            World.sendParticles(Scp407Registry.Leaf.get(), Cell.getX() + 0.5, Cell.getY() + 0.3, Cell.getZ() + 0.5, 2, 0.3, 0.2, 0.3, 0.01);
        }
        long Now = World.getGameTime();
        if (Now - Session.LastSound >= SoundGap) {
            Session.LastSound = Now;
            World.playSound(null, Cell, Trunk ? Scp407Registry.GrowthCreak.get() : Scp407Registry.GrowthRustle.get(), SoundSource.BLOCKS,
                    Trunk ? 0.7f : 0.45f, 0.85f + Random.nextFloat() * 0.3f);
        }
        return true;
    }

    private static int headroom(ServerLevel World, Scp407Session Session, BlockPos Base) {
        int Height = 0;
        BlockPos.MutableBlockPos Cursor = Base.mutable();
        while (Height < MaxTrunk + 1) {
            Cursor.move(Direction.UP);
            if (!Session.Region.contains(Cursor.asLong()) || !roomFor(World.getBlockState(Cursor))) {
                break;
            }
            Height++;
        }
        return Height;
    }

    public static boolean trunk(BlockState State) {
        return State.getBlock() instanceof Scp407TrunkBlock;
    }

    private static boolean crowded(ServerLevel World, BlockPos Cell) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -TrunkSpacing; Dx <= TrunkSpacing; Dx++) {
            for (int Dz = -TrunkSpacing; Dz <= TrunkSpacing; Dz++) {
                for (int Dy = -1; Dy <= MaxTrunk; Dy++) {
                    Cursor.set(Cell.getX() + Dx, Cell.getY() + Dy, Cell.getZ() + Dz);
                    if ((Dx != 0 || Dz != 0 || Dy != 0) && World.isLoaded(Cursor) && trunk(World.getBlockState(Cursor))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean startTrunk(ServerLevel World, Scp407Session Session, BlockPos Cell) {
        RandomSource Random = Session.random();
        if (crowded(World, Cell)) {
            return false;
        }
        int Room = headroom(World, Session, Cell);
        if (Room < 1) {
            return false;
        }
        if (!grow(World, Session, Cell, Scp407Registry.Trunk.get().defaultBlockState(), true)) {
            return false;
        }
        int Goal = Math.min(Room, 1 + Random.nextInt(4));
        if (Room <= 4 && Random.nextFloat() < 0.5f) {
            Goal = Room;
        }
        Session.Tips.add(Cell.above().asLong());
        Session.TipGoals.add(Goal * 4L + (Goal >= Room ? 1L : 0L));
        return true;
    }

    private static void extendTip(ServerLevel World, Scp407Session Session, int Index) {
        BlockPos Tip = BlockPos.of(Session.Tips.getLong(Index));
        long Packed = Session.TipGoals.getLong(Index);
        int Left = (int) (Packed / 4L);
        boolean Column = (Packed & 1L) != 0L;
        Session.Tips.removeLong(Index);
        Session.TipGoals.removeLong(Index);
        RandomSource Random = Session.random();
        if (!Session.Region.contains(Tip.asLong()) || !World.isLoaded(Tip) || !roomFor(World.getBlockState(Tip)) || !trunk(World.getBlockState(Tip.below()))) {
            return;
        }
        if (Left <= 1 && !Column) {
            grow(World, Session, Tip, Scp407Registry.Bloom.get().defaultBlockState().setValue(Scp407BloomBlock.Attached, Direction.DOWN), false);
            return;
        }
        if (!grow(World, Session, Tip, Scp407Registry.Trunk.get().defaultBlockState(), true)) {
            return;
        }
        if (Random.nextFloat() < BranchChance) {
            branch(World, Session, Tip, Random);
        }
        if (Left > 1) {
            Session.Tips.add(Tip.above().asLong());
            Session.TipGoals.add((Left - 1) * 4L + (Column ? 1L : 0L));
        }
    }

    private static boolean lonely(ServerLevel World, BlockPos Cell, BlockPos Parent) {
        for (Direction Side : Direction.values()) {
            BlockPos Next = Cell.relative(Side);
            if (!Next.equals(Parent) && trunk(World.getBlockState(Next))) {
                return false;
            }
        }
        return true;
    }

    private static void branch(ServerLevel World, Scp407Session Session, BlockPos Tip, RandomSource Random) {
        Direction Side = Direction.Plane.HORIZONTAL.getRandomDirection(Random);
        BlockPos Arm = Tip.relative(Side);
        BlockPos End = Arm.relative(Side);
        if (!trunk(World.getBlockState(Tip)) || !Session.Region.contains(Arm.asLong()) || !Session.Region.contains(End.asLong()) || sturdy(World, Arm, Direction.DOWN)) {
            return;
        }
        if (!roomFor(World.getBlockState(Arm)) || !roomFor(World.getBlockState(End)) || !lonely(World, Arm, Tip) || !lonely(World, End, Arm)
                || clearing(Session, Arm) <= ClearingRadius) {
            return;
        }
        if (!grow(World, Session, Arm, Scp407Registry.Trunk.get().defaultBlockState(), true)) {
            return;
        }
        grow(World, Session, End, Scp407Registry.Bloom.get().defaultBlockState().setValue(Scp407BloomBlock.Attached, Side.getOpposite()), false);
        BlockPos Under = Arm.below();
        if (Random.nextFloat() < HangUnderBranch && Session.Region.contains(Under.asLong()) && free(World.getBlockState(Under)) && lonely(World, Under, Arm)) {
            grow(World, Session, Under, hanging(Random), false);
        }
    }

    public static int blooms(ServerLevel World, Scp407Session Session) {
        int Count = 0;
        LongIterator Cells = Session.Grown.iterator();
        while (Cells.hasNext()) {
            if (World.getBlockState(BlockPos.of(Cells.nextLong())).is(Scp407Registry.Bloom.get())) {
                Count++;
            }
        }
        return Count;
    }

    public static BlockPos randomBloom(ServerLevel World, Scp407Session Session, RandomSource Dice) {
        if (Session.Grown.isEmpty()) {
            return null;
        }
        long[] Cells = Session.Grown.toLongArray();
        for (int Attempt = 0; Attempt < 24; Attempt++) {
            BlockPos Pos = BlockPos.of(Cells[Dice.nextInt(Cells.length)]);
            BlockState State = World.getBlockState(Pos);
            if (State.is(Scp407Registry.Bloom.get()) && Scp407GrowthBlock.alive(State)) {
                return Pos;
            }
        }
        return null;
    }

    public static List<BlockPos> livingBlooms(ServerLevel World, Scp407Session Session) {
        List<BlockPos> Found = new ArrayList<>();
        LongIterator Cells = Session.Grown.iterator();
        while (Cells.hasNext()) {
            BlockPos Pos = BlockPos.of(Cells.nextLong());
            if (World.isLoaded(Pos)) {
                BlockState State = World.getBlockState(Pos);
                if (State.is(Scp407Registry.Bloom.get()) && Scp407GrowthBlock.alive(State)) {
                    Found.add(Pos);
                }
            }
        }
        return Found;
    }

    private static void puff(ServerLevel World, Scp407Session Session) {
        RandomSource Random = World.getRandom();
        if (Session.Grown.isEmpty() || World.getGameTime() % 10L != Session.Id % 10L) {
            return;
        }
        int Estimate = Math.max(1, Session.Spread / 12);
        if (Random.nextFloat() > Math.min(0.8f, Estimate * PuffChancePerBloom * 10.0f)) {
            return;
        }
        BlockPos Bloom = randomBloom(World, Session, Random);
        if (Bloom == null) {
            return;
        }
        World.sendParticles(Scp407Registry.Spore.get(), Bloom.getX() + 0.5, Bloom.getY() + 0.6, Bloom.getZ() + 0.5, 10 + Random.nextInt(8), 0.35, 0.3, 0.35, 0.012);
        World.playSound(null, Bloom, Scp407Registry.SporePuff.get(), SoundSource.BLOCKS, 0.5f, 0.9f + Random.nextFloat() * 0.2f);
    }
}
