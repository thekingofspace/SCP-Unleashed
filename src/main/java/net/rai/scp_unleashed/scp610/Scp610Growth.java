package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.faction.FactionState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp610Growth {
    public static final double SurfacePerTick = 3.0 / 40.0;
    public static final double ChamberPerTick = 0.5;
    public static final double CreditCap = 4.0;
    public static final int StepsPerTick = 48;
    public static final int SurfaceRise = 8;
    public static final int SurfaceDepth = 4;
    public static final int SurfaceRiseCap = 24;
    public static final int SurfaceDepthCap = 12;
    public static final double SurfaceBoostFlesh = 1024.0;
    public static final double SurfaceBoostCap = 3.0;
    public static final long RefillTicks = 6000L;
    public static final int BranchEvery = 3;
    public static final int GreatEvery = 4;
    public static final int GreatFlesh = 1024;
    public static final int TunnelGrowth = 12;
    public static final float TendrilChance = 0.15f;
    public static final int TunnelMin = 10;
    public static final int TunnelMax = 20;
    public static final int FloorMargin = 12;
    public static final int Flags = Block.UPDATE_ALL;
    public static final double WeaverSurface = 1.0 / 40.0;
    public static final double WeaverChamber = 0.25;
    public static final double BurrowerChamber = 1.0;

    public enum Step {
        Changed,
        Skipped,
        Idle
    }

    private Scp610Growth() {
    }

    public static boolean halted(MinecraftServer Server, FactionState State) {
        return !Scp610Rules.spreads(Server) || !State.phase().active() || Scp610Pact.active(State);
    }

    public static int budget(MinecraftServer Server, FactionState State) {
        return (int) Math.round(Scp610Config.blocksPerTick() * Scp610Pact.surgeScale(State, Server.overworld().getGameTime()));
    }

    public static void tick(MinecraftServer Server) {
        FactionState State = Scp610Hives.state(Server);
        if (halted(Server, State)) {
            return;
        }
        List<Scp610Hive> All = Scp610Hives.list(Server);
        if (All.isEmpty()) {
            return;
        }
        double Surge = Scp610Pact.surgeScale(State, Server.overworld().getGameTime());
        int Budget = budget(Server, State);
        int Start = Math.floorMod(Server.getTickCount(), All.size());
        for (int Offset = 0; Offset < All.size() && Budget > 0; Offset++) {
            Scp610Hive Hive = All.get((Start + Offset) % All.size());
            ServerLevel World = Server.getLevel(Hive.Site.dimension());
            if (World == null || !World.isLoaded(Hive.anchor()) || !World.isPositionEntityTicking(Hive.anchor())) {
                continue;
            }
            if (Server.getTickCount() % 20 == 0) {
                Hive.Weavers = Scp610Biomass.countAt(Hive.id(), Scp610Form.Weaver);
                Hive.Burrowers = Scp610Biomass.countAt(Hive.id(), Scp610Form.Burrower);
            }
            Hive.SurfaceCredit = Math.min(CreditCap, Hive.SurfaceCredit + SurfacePerTick * Surge * surfaceBoost(Hive) + Hive.Weavers * WeaverSurface);
            Hive.ChamberCredit = Math.min(CreditCap, Hive.ChamberCredit + ChamberPerTick * Surge * (1.0 + WeaverChamber * Hive.Weavers) + Hive.Burrowers * BurrowerChamber);
            Budget -= grow(World, State, Hive, Budget);
        }
    }

    public static int grow(ServerLevel World, FactionState State, Scp610Hive Hive, int Budget) {
        int Used = 0;
        while (Used < Budget && Hive.SurfaceCredit >= 1.0 && Scp610Biomass.affordsGrowth(Hive)) {
            Hive.SurfaceCredit -= 1.0;
            if (surface(World, Hive)) {
                Scp610Biomass.spend(Hive, Scp610Biomass.GrowthCost);
                Used++;
            }
        }
        int Steps = 0;
        while (Used < Budget && Hive.ChamberCredit >= 1.0 && Steps++ < StepsPerTick) {
            Scp610Chambers.Chamber Room = Hive.current();
            boolean Paid = Room == null || !Room.deep();
            if (Paid && !Scp610Biomass.affordsGrowth(Hive)) {
                break;
            }
            Step Result = chamberStep(World, State, Hive);
            if (Result == Step.Changed) {
                Used++;
                Hive.ChamberCredit -= 1.0;
                if (Paid) {
                    Scp610Biomass.spend(Hive, Scp610Biomass.GrowthCost);
                }
            } else if (Result == Step.Idle) {
                Hive.ChamberCredit = 0.0;
                break;
            }
        }
        return Used;
    }

    public static boolean fleshCapped(Scp610Hive Hive) {
        return Hive.flesh() >= Scp610Limits.fleshCap();
    }

    public static boolean chamberCapped(Scp610Hive Hive) {
        return Hive.carved() >= Scp610Limits.chamberCap();
    }

    public static double surfaceBoost(Scp610Hive Hive) {
        return 1.0 + Math.min(SurfaceBoostCap, Hive.flesh() / SurfaceBoostFlesh);
    }

    public static int surfaceRadius(Scp610Hive Hive) {
        return (int) Math.min(Scp610Limits.radiusCap(), 3L + (long) Math.sqrt(Hive.flesh()));
    }

    public static int surfaceRise(Scp610Hive Hive) {
        return Math.min(SurfaceRiseCap, SurfaceRise + surfaceRadius(Hive) / 4);
    }

    public static int surfaceDepth(Scp610Hive Hive) {
        return Math.min(SurfaceDepthCap, SurfaceDepth + surfaceRadius(Hive) / 8);
    }

    public static BlockPos from(ServerLevel World, Scp610Hive Hive) {
        if (Hive.Frontier.size() == 0 && Hive.flesh() > 0 && World.getGameTime() >= Hive.NextRefill) {
            Hive.NextRefill = World.getGameTime() + RefillTicks;
            Hive.survey();
        }
        return Hive.Frontier.size() > 0 ? BlockPos.of(Hive.Frontier.random(World.random)) : Hive.anchor().below();
    }

    public static boolean ready(ServerLevel World, BlockPos Pos) {
        int X = Pos.getX() >> 4;
        int Z = Pos.getZ() >> 4;
        int Left = (Pos.getX() - 1) >> 4;
        int Right = (Pos.getX() + 1) >> 4;
        int Back = (Pos.getZ() - 1) >> 4;
        int Front = (Pos.getZ() + 1) >> 4;
        return World.getChunkSource().hasChunk(X, Z) && World.getChunkSource().hasChunk(Left, Z) && World.getChunkSource().hasChunk(Right, Z)
                && World.getChunkSource().hasChunk(X, Back) && World.getChunkSource().hasChunk(X, Front);
    }

    @Nullable
    public static BlockPos target(ServerLevel World, BlockPos From, Direction Way) {
        BlockPos Next = From.relative(Way);
        if (!ready(World, Next)) {
            return null;
        }
        if (convertible(World, Next) && exposed(World, Next)) {
            return Next;
        }
        if (open(World, Next) && convertible(World, Next.below())) {
            return Next.below();
        }
        if (convertible(World, Next.above()) && open(World, Next.above().above())) {
            return Next.above();
        }
        return null;
    }

    public static boolean spreadable(ServerLevel World, Scp610Hive Hive, BlockPos From) {
        for (Direction Way : Direction.values()) {
            if (!ready(World, From.relative(Way))) {
                return true;
            }
            BlockPos Target = target(World, From, Way);
            if (Target != null && withinEnvelope(Hive, Target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean withinEnvelope(Scp610Hive Hive, BlockPos Target) {
        BlockPos Anchor = Hive.anchor();
        double Dx = Target.getX() - Anchor.getX();
        double Dz = Target.getZ() - Anchor.getZ();
        double Radius = Scp610Limits.radiusCap();
        return Dx * Dx + Dz * Dz <= Radius * Radius && Target.getY() <= Anchor.getY() + SurfaceRiseCap && Target.getY() >= Anchor.getY() - SurfaceDepthCap - 2;
    }

    public static boolean surface(ServerLevel World, Scp610Hive Hive) {
        if (fleshCapped(Hive)) {
            return false;
        }
        RandomSource Random = World.random;
        BlockPos From = from(World, Hive);
        Direction Way = Random.nextFloat() < 0.8f ? Direction.Plane.HORIZONTAL.getRandomDirection(Random) : (Random.nextBoolean() ? Direction.UP : Direction.DOWN);
        BlockPos Target = target(World, From, Way);
        if (Target == null || !withinSurface(Hive, Target)) {
            if (Hive.Frontier.contains(From.asLong()) && !spreadable(World, Hive, From)) {
                Hive.Frontier.remove(From.asLong());
            }
            return false;
        }
        place(World, Hive, Target, pick(Random), false);
        BlockPos Above = Target.above();
        if (open(World, Above) && Random.nextFloat() < TendrilChance && Scp610Registry.Tendrils.get().defaultBlockState().canSurvive(World, Above)) {
            World.setBlock(Above, Scp610Registry.Tendrils.get().defaultBlockState(), Flags);
        } else if (!World.getBlockState(Above).isAir() && World.getBlockState(Above).canBeReplaced() && World.getBlockState(Above).getFluidState().isEmpty()) {
            World.setBlock(Above, Blocks.AIR.defaultBlockState(), Flags);
        }
        if (Random.nextInt(6) == 0) {
            World.playSound(null, Target, Scp610Sounds.Grow.get(), SoundSource.BLOCKS, 0.5f, 0.8f + Random.nextFloat() * 0.3f);
        }
        return true;
    }

    public static boolean withinSurface(Scp610Hive Hive, BlockPos Target) {
        BlockPos Anchor = Hive.anchor();
        double Dx = Target.getX() - Anchor.getX();
        double Dz = Target.getZ() - Anchor.getZ();
        double Radius = surfaceRadius(Hive);
        return Dx * Dx + Dz * Dz <= Radius * Radius && Target.getY() <= Anchor.getY() + surfaceRise(Hive) && Target.getY() >= Anchor.getY() - surfaceDepth(Hive) - 2;
    }

    public static boolean open(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.isAir() || State.canBeReplaced() && State.getFluidState().isEmpty();
    }

    public static boolean exposed(ServerLevel World, BlockPos Pos) {
        for (Direction Side : Direction.values()) {
            if (open(World, Pos.relative(Side))) {
                return true;
            }
        }
        return false;
    }

    public static boolean protectedBlock(ServerLevel World, BlockPos Pos, BlockState State) {
        if (World.isOutsideBuildHeight(Pos) || State.hasBlockEntity() || State.is(Scp610Registry.Unspreadable)) {
            return true;
        }
        float Hardness = State.getDestroySpeed(World, Pos);
        return Hardness < 0.0f || Hardness >= 50.0f;
    }

    public static boolean isFlesh(BlockState State) {
        return State.getBlock() instanceof Scp610FleshBlock || State.getBlock() instanceof Scp610SinewBlock || State.getBlock() instanceof Scp610TendrilsBlock
                || State.is(Scp610Registry.FleshTag);
    }

    public static boolean convertible(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (State.isAir() || !State.getFluidState().isEmpty() || isFlesh(State) || protectedBlock(World, Pos, State)) {
            return false;
        }
        return State.isCollisionShapeFullBlock(World, Pos);
    }

    public static BlockState pick(RandomSource Random) {
        int Roll = Random.nextInt(100);
        if (Roll < 55) {
            return Scp610Registry.Flesh.get().defaultBlockState();
        }
        if (Roll < 75) {
            return Scp610Registry.VeinedFlesh.get().defaultBlockState();
        }
        if (Roll < 85) {
            return Scp610Registry.Sinew.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        }
        if (Roll < 95) {
            return Scp610Registry.Pustule.get().defaultBlockState();
        }
        return Scp610Registry.Wound.get().defaultBlockState();
    }

    public static BlockState wild(BlockState State) {
        return State.hasProperty(Scp610FleshBlock.Wild) ? State.setValue(Scp610FleshBlock.Wild, true) : State;
    }

    public static void place(ServerLevel World, Scp610Hive Hive, BlockPos Pos, BlockState State, boolean Silent) {
        World.setBlock(Pos, wild(State), Flags);
        Scp610Hives.own(World, Hive, Pos);
    }

    public static Step chamberStep(ServerLevel World, FactionState State, Scp610Hive Hive) {
        Scp610Chambers.Chamber Room = Hive.current();
        if (Room == null) {
            if (!extend(World, Hive)) {
                return Step.Idle;
            }
            Room = Hive.current();
            if (Room == null) {
                return Step.Idle;
            }
        }
        boolean Lord = Room.deep();
        if (!Lord && (fleshCapped(Hive) || chamberCapped(Hive))) {
            return Step.Idle;
        }
        List<Scp610Chambers.Op> Ops = Hive.ops(Room);
        if (Room.Progress >= Ops.size()) {
            Hive.finishRoom();
            if (Room.lord()) {
                Scp610Lord.chamberDone(World, State, Hive, Room);
            }
            return Step.Skipped;
        }
        Scp610Chambers.Op Next = Ops.get(Room.Progress);
        if (!ready(World, BlockPos.of(Next.Pos()))) {
            return Step.Idle;
        }
        Room.Progress++;
        Hive.Dirty = true;
        return apply(World, Hive, Next, Lord) ? Step.Changed : Step.Skipped;
    }

    public static boolean apply(ServerLevel World, Scp610Hive Hive, Scp610Chambers.Op Next, boolean Lord) {
        BlockPos Pos = BlockPos.of(Next.Pos());
        if (World.isOutsideBuildHeight(Pos) || Pos.getY() <= World.getMinBuildHeight() + 4 || !World.isLoaded(Pos)) {
            return false;
        }
        BlockState State = World.getBlockState(Pos);
        if (!State.getFluidState().isEmpty() || protectedBlock(World, Pos, State)) {
            return false;
        }
        RandomSource Random = World.random;
        return switch (Next.Kind()) {
            case Scp610Chambers.Carve -> carve(World, Hive, Pos, State, Lord);
            case Scp610Chambers.Pillar -> line(World, Hive, Pos, State, Scp610Registry.Sinew.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), Lord);
            case Scp610Chambers.Floor -> line(World, Hive, Pos, State, Random.nextInt(10) < 3 ? Scp610Registry.Pustule.get().defaultBlockState() : wall(Random), Lord);
            case Scp610Chambers.Light -> line(World, Hive, Pos, State, Scp610Registry.Pustule.get().defaultBlockState(), Lord);
            default -> line(World, Hive, Pos, State, wall(Random), Lord);
        };
    }

    public static BlockState wall(RandomSource Random) {
        int Roll = Random.nextInt(100);
        if (Roll < 50) {
            return Scp610Registry.Flesh.get().defaultBlockState();
        }
        if (Roll < 75) {
            return Scp610Registry.VeinedFlesh.get().defaultBlockState();
        }
        if (Roll < 90) {
            return Scp610Registry.Sinew.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        }
        return Scp610Registry.Wound.get().defaultBlockState();
    }

    private static boolean nearFluid(ServerLevel World, BlockPos Pos) {
        for (Direction Side : Direction.values()) {
            if (!World.getFluidState(Pos.relative(Side)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean carve(ServerLevel World, Scp610Hive Hive, BlockPos Pos, BlockState State, boolean Lord) {
        if (State.isAir()) {
            return false;
        }
        if (nearFluid(World, Pos)) {
            return line(World, Hive, Pos, State, wall(World.random), Lord);
        }
        if (isFlesh(State)) {
            Scp610Hives.released(World, Pos, false);
        }
        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Flags);
        Hive.addCarved();
        return true;
    }

    private static boolean line(ServerLevel World, Scp610Hive Hive, BlockPos Pos, BlockState State, BlockState Lining, boolean Lord) {
        if (isFlesh(State)) {
            if (Scp610Hives.ownerOf(World, Pos) == null) {
                Scp610Hives.own(World, Hive, Pos);
            }
            return false;
        }
        if (!Lord && fleshCapped(Hive)) {
            return false;
        }
        if (!State.isAir() && !State.canBeReplaced() && !State.isCollisionShapeFullBlock(World, Pos)) {
            return false;
        }
        place(World, Hive, Pos, Lining, true);
        return true;
    }

    public static boolean extend(ServerLevel World, Scp610Hive Hive) {
        if (chamberCapped(Hive) || fleshCapped(Hive)) {
            return false;
        }
        int Room = Hive.rooms();
        RandomSource Random = RandomSource.create(Hive.Site.seed() * 31L + Room * 7919L);
        BlockPos Start = Hive.tail();
        List<Long> Junctions = Hive.junctions();
        if (Room > 0 && Room % BranchEvery == 0 && !Junctions.isEmpty()) {
            Start = BlockPos.of(Junctions.get(Random.nextInt(Junctions.size())));
        }
        double Heading = Random.nextDouble() * Mth.TWO_PI;
        int Length = TunnelMin + Random.nextInt(TunnelMax - TunnelMin + 1) + Math.min(TunnelGrowth, Room / 2);
        int Floor = World.getMinBuildHeight() + FloorMargin;
        int Drop = Math.min(Length * 2 / 3, Math.max(0, Start.getY() - Floor - 8));
        BlockPos End = Start.offset(Mth.floor(Math.cos(Heading) * Length), -Drop, Mth.floor(Math.sin(Heading) * Length));
        String Kind = roomKind(Hive, Room);
        double Reach = switch (Kind) {
            case Scp610Chambers.Cavern -> Scp610Chambers.CavernWide;
            case Scp610Chambers.GreatCavern -> Scp610Chambers.GreatWide;
            default -> Scp610Chambers.NurseryHalf;
        } + 1.0;
        BlockPos Centre = End.offset(Mth.floor(Math.cos(Heading) * Reach), -1, Mth.floor(Math.sin(Heading) * Reach));
        Hive.Plan.add(new Scp610Chambers.Chamber(Scp610Chambers.Tunnel, Start, End, 0));
        Hive.Plan.add(new Scp610Chambers.Chamber(Kind, End, Centre, 0));
        Hive.setTail(Centre.above(1));
        Hive.addJunction(Centre.above(1));
        Hive.addRoom();
        Hive.Dirty = true;
        return true;
    }

    public static String roomKind(Scp610Hive Hive, int Room) {
        if (Room > 0 && Room % GreatEvery == GreatEvery - 1 && Hive.flesh() >= GreatFlesh) {
            return Scp610Chambers.GreatCavern;
        }
        return Room % 2 == 0 ? Scp610Chambers.Cavern : Scp610Chambers.Nursery;
    }

    public static int runAll(ServerLevel World, FactionState State, Scp610Hive Hive, int Limit) {
        int Changed = 0;
        for (int Step0 = 0; Step0 < Limit; Step0++) {
            Step Result = chamberStep(World, State, Hive);
            if (Result == Step.Changed) {
                Changed++;
            } else if (Result == Step.Idle) {
                break;
            }
        }
        return Changed;
    }
}
