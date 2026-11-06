package net.rai.scp_unleashed.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.DuctBlock;
import net.rai.scp_unleashed.block.entity.DuctBlockEntity;
import net.rai.scp_unleashed.gas.Gas;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.particle.PipeLiquidOptions;
import net.rai.scp_unleashed.registry.ModParticles;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PressureNetwork {
    public static final double FlowPerKpa = 0.25;
    public static final double CostPerMillibucket = 2.0;
    public static final double GasFlowPerKpa = 0.5;
    public static final double GasCostPerUnit = 0.5;
    public static final double GasPressureMin = 10.0;
    public static final double IntakePressure = 1.0;
    public static final int WaterPoolSide = 2;
    public static final int LavaPoolSide = 6;
    public static final int DropTicks = 2;
    public static final int VentTicks = 4;
    public static final int HissTicks = 8;
    public static final int SuctionCount = 2;
    public static final double PipeMouth = 0.52;
    public static final double VentMouth = 0.55;
    public static final double ReleaseReach = 1.0;
    public static final double BoreSpread = 0.09;
    public static final TagKey<Fluid> FinitePool = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "finite_pool"));

    private final DuctKind Kind;
    private final Set<BlockPos> Members;
    private final List<Opening> Ends;
    private final List<Opening> Leaks = new ArrayList<>();
    private final List<Opening> Intakes = new ArrayList<>();
    private final List<Opening> Drawing = new ArrayList<>();
    private final Set<BlockPos> Endless = new HashSet<>();
    private final List<Port> Ports;
    private final long ScannedAt;
    private double Pressure;
    private double PendingInput;
    private double LastWritten;
    private boolean Hissing;
    private boolean Pushed;
    private int Turn;
    private FluidStack Carrying = FluidStack.EMPTY;

    private PressureNetwork(DuctKind Kind, Set<BlockPos> Members, List<Opening> Ends, List<Port> Ports, long ScannedAt, double Pressure) {
        this.Kind = Kind;
        this.Members = Members;
        this.Ends = Ends;
        this.Ports = Ports;
        this.ScannedAt = ScannedAt;
        this.Pressure = Pressure;
        this.LastWritten = Pressure;
    }

    public static PressureNetwork scan(ServerLevel World, BlockPos Start, DuctKind Kind) {
        Set<BlockPos> Members = new LinkedHashSet<>();
        List<Opening> Ends = new ArrayList<>();
        List<Port> Ports = new ArrayList<>();
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        BlockPos First = Start.immutable();
        Members.add(First);
        Queue.add(First);
        double PressureSum = 0.0;
        int Sampled = 0;
        while (!Queue.isEmpty()) {
            BlockPos Current = Queue.poll();
            BlockState State = World.getBlockState(Current);
            if (!isDuct(State, Kind)) {
                continue;
            }
            if (World.getBlockEntity(Current) instanceof DuctBlockEntity Duct) {
                PressureSum += Duct.getPressure();
                Sampled++;
            }
            int Linked = 0;
            Direction Only = null;
            for (Map.Entry<Direction, BooleanProperty> Entry : DuctBlock.Connections.entrySet()) {
                if (!State.getValue(Entry.getValue())) {
                    continue;
                }
                Linked++;
                Only = Entry.getKey();
                BlockPos Next = Current.relative(Entry.getKey());
                if (Members.contains(Next) || !World.isLoaded(Next)) {
                    continue;
                }
                BlockState NextState = World.getBlockState(Next);
                if (isDuct(NextState, Kind)) {
                    if (Members.size() < DuctKind.MaxNetworkSize) {
                        Members.add(Next);
                        Queue.add(Next);
                    }
                    continue;
                }
                PortRole Role = null;
                if (Kind == DuctKind.Pipe && NextState.getBlock() instanceof FluidPort Connection) {
                    Role = Connection.portRole(NextState, Entry.getKey().getOpposite());
                } else if (Kind == DuctKind.Vent && NextState.getBlock() instanceof GasPort Connection) {
                    Role = Connection.gasRole(NextState, Entry.getKey().getOpposite());
                }
                if (Role != null) {
                    Port Found = new Port(Next.immutable(), Role);
                    if (!Ports.contains(Found)) {
                        Ports.add(Found);
                    }
                }
            }
            if (Linked == 1) {
                Ends.add(new Opening(Current, Only.getOpposite()));
            }
        }
        double Pressure = Sampled == 0 ? 0.0 : PressureSum / Sampled;
        PressureNetwork Network = new PressureNetwork(Kind, Collections.unmodifiableSet(Members), Ends, Ports, World.getGameTime(), Pressure);
        Network.classify(World);
        return Network;
    }

    private void classify(ServerLevel World) {
        Leaks.clear();
        Intakes.clear();
        for (Opening End : Ends) {
            BlockPos Far = End.Pos().relative(End.Out());
            if (!World.isLoaded(Far)) {
                continue;
            }
            BlockState FarState = World.getBlockState(Far);
            if (Kind == DuctKind.Pipe) {
                if (!FarState.getFluidState().isEmpty()) {
                    Intakes.add(End);
                }
            } else if (!FarState.isFaceSturdy(World, Far, End.Out().getOpposite())) {
                Leaks.add(End);
            }
        }
    }

    private static boolean isDuct(BlockState State, DuctKind Kind) {
        return State.getBlock() instanceof DuctBlock Duct && Duct.getKind() == Kind;
    }

    public void pump(double Amount) {
        PendingInput += Math.max(0.0, Amount);
        Pushed = true;
    }

    public void markPushed() {
        Pushed = true;
    }

    public double headroom(double Target) {
        return Math.max(0.0, (Target - Pressure) * Members.size() - PendingInput);
    }

    public boolean isOpen() {
        return Kind == DuctKind.Pipe && !Leaks.isEmpty();
    }

    public List<OpenEnd> getOpenEnds() {
        List<OpenEnd> Found = new ArrayList<>(Leaks.size());
        for (Opening Hole : Leaks) {
            Found.add(new OpenEnd(Hole.Pos(), Hole.Out()));
        }
        return Found;
    }

    void simulate(ServerLevel World, Map<BlockPos, FluidTank> Buffers) {
        classify(World);
        double Volume = Math.max(1, Members.size());
        if (isOpen()) {
            Hissing = Pushed;
            Pushed = false;
            PendingInput = 0.0;
            Pressure = 0.0;
            Carrying = FluidStack.EMPTY;
            Drawing.clear();
            return;
        }
        Hissing = false;
        Pushed = false;
        double Charge = Pressure * Volume + PendingInput;
        PendingInput = 0.0;
        Charge -= Kind.getLeakRate() * Pressure * Leaks.size();
        if (Kind == DuctKind.Pipe) {
            Charge -= moveLiquid(World, Math.max(0.0, Charge / Volume), Volume, Buffers) * CostPerMillibucket;
        } else {
            Charge -= moveGas(World, Math.max(0.0, Charge / Volume)) * GasCostPerUnit;
        }
        Pressure = Mth.clamp(Charge / Volume, 0.0, Kind.getRating());
        Turn++;
    }

    private List<FluidEnd> fluidEnds(ServerLevel World) {
        List<FluidEnd> Found = new ArrayList<>();
        for (Port Valve : Ports) {
            if (!World.isLoaded(Valve.Pos())) {
                continue;
            }
            BlockState State = World.getBlockState(Valve.Pos());
            if (!(State.getBlock() instanceof FluidPort Connection)) {
                continue;
            }
            IFluidHandler Handler = Connection.portHandler(World, Valve.Pos(), State);
            if (Handler != null) {
                Found.add(new FluidEnd(Handler, Connection.portOwner(World, Valve.Pos(), State), Valve.Role()));
            }
        }
        return Found;
    }

    private List<GasEnd> gasEnds(ServerLevel World) {
        List<GasEnd> Found = new ArrayList<>();
        for (Port Valve : Ports) {
            if (!World.isLoaded(Valve.Pos())) {
                continue;
            }
            BlockState State = World.getBlockState(Valve.Pos());
            if (!(State.getBlock() instanceof GasPort Connection)) {
                continue;
            }
            GasHandler Handler = Connection.gasHandler(World, Valve.Pos(), State);
            if (Handler != null) {
                Found.add(new GasEnd(Handler, Connection.gasOwner(World, Valve.Pos(), State), Valve.Role(), Connection.exhausts(State)));
            }
        }
        return Found;
    }

    private <T> List<T> rotated(List<T> Items) {
        if (Items.size() < 2) {
            return Items;
        }
        List<T> Turned = new ArrayList<>(Items);
        Collections.rotate(Turned, Turn % Items.size());
        return Turned;
    }

    public static int fairShare(int Amount, List<Acceptor> Sinks, boolean Execute) {
        int Count = Sinks.size();
        if (Amount <= 0 || Count == 0) {
            return 0;
        }
        int[] Plan = new int[Count];
        List<Integer> Open = new ArrayList<>();
        for (int Index = 0; Index < Count; Index++) {
            if (Sinks.get(Index).take(1, false) > 0) {
                Open.add(Index);
            }
        }
        int Remaining = Amount;
        while (Remaining > 0 && !Open.isEmpty()) {
            int Share = Math.max(1, Remaining / Open.size());
            List<Integer> Still = new ArrayList<>();
            boolean Progress = false;
            for (int Index : Open) {
                if (Remaining <= 0) {
                    break;
                }
                int Want = Math.min(Share, Remaining);
                int Got = Math.max(0, Sinks.get(Index).take(Plan[Index] + Want, false) - Plan[Index]);
                if (Got > 0) {
                    Plan[Index] += Got;
                    Remaining -= Got;
                    Progress = true;
                }
                if (Got >= Want) {
                    Still.add(Index);
                }
            }
            if (!Progress) {
                break;
            }
            Open = Still;
        }
        int Total = Amount - Remaining;
        if (!Execute || Total <= 0) {
            return Total;
        }
        int Filled = 0;
        for (int Index = 0; Index < Count; Index++) {
            if (Plan[Index] > 0) {
                Filled += Sinks.get(Index).take(Plan[Index], true);
            }
        }
        return Filled;
    }

    private int moveLiquid(ServerLevel World, double Current, double Volume, Map<BlockPos, FluidTank> Buffers) {
        Carrying = FluidStack.EMPTY;
        Drawing.clear();
        if (Current < 0.5) {
            return 0;
        }
        List<FluidEnd> Valves = fluidEnds(World);
        List<FluidEnd> Sources = new ArrayList<>();
        List<FluidEnd> Sinks = new ArrayList<>();
        for (FluidEnd End : Valves) {
            if (End.Role().canDrain()) {
                Sources.add(End);
            }
            if (End.Role().canFill()) {
                Sinks.add(End);
            }
        }
        for (Opening Intake : Intakes) {
            FluidTank Buffer = Buffers.computeIfAbsent(Intake.Pos(), Key -> new FluidTank(FluidType.BUCKET_VOLUME));
            if (Buffer.isEmpty() && Current >= IntakePressure) {
                if (refill(World, Intake, Buffer, Sinks)) {
                    Endless.add(Intake.Pos());
                } else {
                    Endless.remove(Intake.Pos());
                }
            }
        }
        for (Map.Entry<BlockPos, FluidTank> Held : Buffers.entrySet()) {
            if (!Held.getValue().isEmpty() && Members.contains(Held.getKey())) {
                Sources.addFirst(new FluidEnd(Held.getValue(), Held.getKey(), PortRole.Output));
            }
        }
        int Budget = (int) Math.min(FlowPerKpa * Current, Current * Volume / CostPerMillibucket);
        List<FluidEnd> Order = rotated(Sinks);
        int Moved = 0;
        for (FluidEnd Source : Sources) {
            if (Moved >= Budget) {
                break;
            }
            FluidStack Offer = Source.Handler().drain(Budget - Moved, IFluidHandler.FluidAction.SIMULATE);
            if (Offer.isEmpty()) {
                continue;
            }
            int Got = deliverFluid(Source, Offer, Order);
            if (Got > 0 && Carrying.isEmpty()) {
                Carrying = Offer.copyWithAmount(1);
            }
            Moved += Got;
        }
        if (Moved > 0) {
            for (Opening Intake : Intakes) {
                if (Endless.contains(Intake.Pos())) {
                    Drawing.add(Intake);
                }
            }
        }
        return Moved;
    }

    private static int deliverFluid(FluidEnd Source, FluidStack Offer, List<FluidEnd> Sinks) {
        List<Acceptor> Takers = new ArrayList<>();
        for (FluidEnd Sink : Sinks) {
            if (!Sink.Owner().equals(Source.Owner())) {
                Takers.add((Amount, Execute) -> Sink.Handler().fill(Offer.copyWithAmount(Amount), Execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE));
            }
        }
        int Planned = fairShare(Offer.getAmount(), Takers, false);
        if (Planned <= 0) {
            return 0;
        }
        FluidStack Drained = Source.Handler().drain(Offer.copyWithAmount(Planned), IFluidHandler.FluidAction.EXECUTE);
        if (Drained.isEmpty()) {
            return 0;
        }
        List<Acceptor> Fillers = new ArrayList<>();
        for (FluidEnd Sink : Sinks) {
            if (!Sink.Owner().equals(Source.Owner())) {
                Fillers.add((Amount, Execute) -> Sink.Handler().fill(Drained.copyWithAmount(Amount), Execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE));
            }
        }
        int Filled = fairShare(Drained.getAmount(), Fillers, true);
        if (Filled < Drained.getAmount()) {
            Source.Handler().fill(Drained.copyWithAmount(Drained.getAmount() - Filled), IFluidHandler.FluidAction.EXECUTE);
        }
        return Filled;
    }

    private int moveGas(ServerLevel World, double Current) {
        if (Current < GasPressureMin) {
            return 0;
        }
        List<GasEnd> Valves = gasEnds(World);
        List<GasEnd> Sources = new ArrayList<>();
        List<GasEnd> Sinks = new ArrayList<>();
        for (GasEnd End : Valves) {
            if (End.Role().canDrain()) {
                Sources.add(End);
            }
            if (End.Role().canFill()) {
                Sinks.add(End);
            }
        }
        if (Sources.isEmpty()) {
            return 0;
        }
        int Budget = (int) (GasFlowPerKpa * Current);
        List<GasEnd> Order = rotated(Sinks);
        int Moved = 0;
        for (GasEnd Source : Sources) {
            if (Moved >= Budget) {
                break;
            }
            GasStack Offer = Source.Handler().drain(Budget - Moved, true);
            if (Offer.isEmpty()) {
                continue;
            }
            int Got = 0;
            int Planned = shareGas(Offer, Source.Owner(), Order, false);
            if (Planned > 0) {
                GasStack Drained = Source.Handler().drain(Planned, false);
                Got = shareGas(Drained, Source.Owner(), Order, true);
                if (Got < Drained.getAmount()) {
                    Source.Handler().fill(Drained.copyWithAmount(Drained.getAmount() - Got), false);
                }
            }
            Moved += Got;
            int Spare = Math.min(Offer.getAmount() - Got, Budget - Moved);
            if (Spare > 0 && !Leaks.isEmpty()) {
                GasStack Vented = Source.Handler().drain(Spare, false);
                release(World, Vented);
                Moved += Vented.getAmount();
            }
        }
        return Moved;
    }

    private void release(ServerLevel World, GasStack Vented) {
        if (Vented.isEmpty() || Leaks.isEmpty()) {
            return;
        }
        int Share = Math.max(1, Vented.getAmount() / Leaks.size());
        int Left = Vented.getAmount();
        for (Opening Hole : Leaks) {
            if (Left <= 0) {
                break;
            }
            int Amount = Math.min(Share, Left);
            Left -= Amount;
            Vented.getGas().release(World, mouthOf(Hole, ReleaseReach), Amount);
        }
    }

    private static int shareGas(GasStack Offer, @Nullable BlockPos Owner, List<GasEnd> Sinks, boolean Execute) {
        List<Acceptor> Takers = new ArrayList<>();
        for (GasEnd Sink : Sinks) {
            if (Owner == null || !Sink.Owner().equals(Owner)) {
                Takers.add((Amount, Fill) -> Sink.Handler().fill(Offer.copyWithAmount(Amount), !Fill));
            }
        }
        return fairShare(Offer.getAmount(), Takers, Execute);
    }

    public boolean canTakeGas(ServerLevel World, Gas Kind, @Nullable BlockPos Owner) {
        if (this.Kind != DuctKind.Vent || Pressure < GasPressureMin) {
            return false;
        }
        for (GasEnd End : gasEnds(World)) {
            if (End.Role().canFill() && (Owner == null || !End.Owner().equals(Owner)) && End.Handler().fill(new GasStack(Kind, 1), true) > 0) {
                return true;
            }
        }
        return false;
    }

    public int offerGas(ServerLevel World, GasStack Offer, @Nullable BlockPos Owner) {
        if (Kind != DuctKind.Vent || Pressure < GasPressureMin || Offer.isEmpty()) {
            return 0;
        }
        List<GasEnd> Sinks = new ArrayList<>();
        for (GasEnd End : gasEnds(World)) {
            if (End.Role().canFill()) {
                Sinks.add(End);
            }
        }
        int Taken = shareGas(Offer, Owner, rotated(Sinks), true);
        if (Taken > 0) {
            Pressure = Math.max(0.0, Pressure - Taken * GasCostPerUnit / Math.max(1, Members.size()));
        }
        return Taken;
    }

    private static boolean refill(ServerLevel World, Opening Intake, FluidTank Buffer, List<FluidEnd> Sinks) {
        BlockPos Far = Intake.Pos().relative(Intake.Out());
        FluidState Liquid = World.getFluidState(Far);
        if (!Liquid.isSource()) {
            return false;
        }
        FluidStack Bucket = new FluidStack(Liquid.getType(), FluidType.BUCKET_VOLUME);
        int Space = 0;
        for (FluidEnd Sink : Sinks) {
            Space += Sink.Handler().fill(Bucket, IFluidHandler.FluidAction.SIMULATE);
            if (Space >= FluidType.BUCKET_VOLUME) {
                break;
            }
        }
        Space = Math.min(Space, FluidType.BUCKET_VOLUME);
        if (Space <= 0) {
            return false;
        }
        if (isEndless(World, Far, Liquid)) {
            Buffer.fill(Bucket.copyWithAmount(Space), IFluidHandler.FluidAction.EXECUTE);
            return true;
        }
        if (Space < FluidType.BUCKET_VOLUME) {
            return false;
        }
        BlockState FarState = World.getBlockState(Far);
        if (!(FarState.getBlock() instanceof BucketPickup Pickup) || Pickup.pickupBlock(null, World, Far, FarState).isEmpty()) {
            return false;
        }
        Buffer.fill(Bucket, IFluidHandler.FluidAction.EXECUTE);
        SoundEvent Sound = Liquid.getType().getFluidType().getSound(SoundActions.BUCKET_FILL);
        if (Sound != null) {
            World.playSound(null, Far, Sound, SoundSource.BLOCKS, 0.4f, 1.0f);
        }
        return false;
    }

    private static boolean isEndless(ServerLevel World, BlockPos Start, FluidState Liquid) {
        if (Liquid.is(FinitePool)) {
            return false;
        }
        int Side = Liquid.is(FluidTags.WATER) ? WaterPoolSide : LavaPoolSide;
        int Needed = Side * Side;
        Set<BlockPos> Seen = new HashSet<>();
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        Seen.add(Start);
        Queue.add(Start);
        int Found = 0;
        while (!Queue.isEmpty()) {
            BlockPos Current = Queue.poll();
            FluidState State = World.getFluidState(Current);
            if (!State.isSource() || !State.getType().isSame(Liquid.getType())) {
                continue;
            }
            if (++Found >= Needed) {
                return true;
            }
            for (Direction Way : Direction.Plane.HORIZONTAL) {
                BlockPos Next = Current.relative(Way);
                if (World.isLoaded(Next) && Seen.add(Next)) {
                    Queue.add(Next);
                }
            }
        }
        return false;
    }

    void emitParticles(ServerLevel World) {
        long Now = World.getGameTime();
        if (Kind == DuctKind.Vent) {
            if (Now % VentTicks == 0) {
                emitVentAir(World);
            }
        } else {
            if (Hissing && Now % HissTicks == 0) {
                emitHiss(World);
            }
            if (Now % DropTicks == 0) {
                emitSuction(World);
            }
        }
    }

    private void emitVentAir(ServerLevel World) {
        if (Pressure < 0.5 || Leaks.isEmpty()) {
            return;
        }
        double Strength = Math.min(1.0, Pressure / Kind.getRating());
        int Count = Math.max(1, (int) Math.round(Strength * 6.0));
        double Speed = 0.06 + Strength * 0.3;
        RandomSource Random = World.getRandom();
        for (Opening Hole : Leaks) {
            if (!World.isLoaded(Hole.Pos())) {
                continue;
            }
            Direction Out = Hole.Out();
            Vec3 Mouth = mouthOf(Hole, VentMouth);
            for (int Puff = 0; Puff < Count; Puff++) {
                double DirectionX = Out.getStepX() + (Random.nextDouble() - 0.5) * 0.5;
                double DirectionY = Out.getStepY() + (Random.nextDouble() - 0.5) * 0.5;
                double DirectionZ = Out.getStepZ() + (Random.nextDouble() - 0.5) * 0.5;
                World.sendParticles(ModParticles.VentAir.get(), Mouth.x, Mouth.y, Mouth.z, 0, DirectionX, DirectionY, DirectionZ, Speed);
            }
        }
    }

    private void emitHiss(ServerLevel World) {
        RandomSource Random = World.getRandom();
        for (Opening Hole : Leaks) {
            if (!World.isLoaded(Hole.Pos())) {
                continue;
            }
            Direction Out = Hole.Out();
            Vec3 Mouth = mouthOf(Hole, PipeMouth);
            for (int Puff = 0; Puff < 2; Puff++) {
                Vec3 Spot = Mouth.add(across(Out, Random, BoreSpread));
                World.sendParticles(ModParticles.VentAir.get(), Spot.x, Spot.y, Spot.z, 0, Out.getStepX(), Out.getStepY(), Out.getStepZ(), 0.08);
            }
        }
    }

    private void emitSuction(ServerLevel World) {
        if (Drawing.isEmpty()) {
            return;
        }
        RandomSource Random = World.getRandom();
        double Ticks = PipeLiquidOptions.SuctionTicks;
        for (Opening Intake : Drawing) {
            if (!World.isLoaded(Intake.Pos())) {
                continue;
            }
            Direction Out = Intake.Out();
            FluidState Pool = World.getFluidState(Intake.Pos().relative(Out));
            if (Pool.isEmpty()) {
                continue;
            }
            PipeLiquidOptions Pull = new PipeLiquidOptions(Pool.getType(), true);
            Vec3 Mouth = mouthOf(Intake, PipeMouth);
            for (int Index = 0; Index < SuctionCount; Index++) {
                double Along = 0.3 + Random.nextDouble() * 1.1;
                Vec3 Spot = surfaced(World, Mouth.add(Out.getStepX() * Along, Out.getStepY() * Along, Out.getStepZ() * Along)
                        .add(across(Out, Random, 0.3 + Along * 0.4)));
                if (Spot == null) {
                    continue;
                }
                World.sendParticles(Pull, Spot.x, Spot.y, Spot.z, 0, (Mouth.x - Spot.x) / Ticks, (Mouth.y - Spot.y) / Ticks, (Mouth.z - Spot.z) / Ticks, 1.0);
            }
        }
    }

    @Nullable
    private static Vec3 surfaced(ServerLevel World, Vec3 Spot) {
        BlockPos Cell = BlockPos.containing(Spot);
        if (!World.isLoaded(Cell)) {
            return null;
        }
        FluidState Inside = World.getFluidState(Cell);
        if (Inside.isEmpty()) {
            return null;
        }
        if (!World.getFluidState(Cell.above()).isEmpty()) {
            return Spot;
        }
        return new Vec3(Spot.x, Math.max(Spot.y, Cell.getY() + Inside.getHeight(World, Cell) + 0.02), Spot.z);
    }

    private static Vec3 mouthOf(Opening End, double Reach) {
        Direction Out = End.Out();
        return Vec3.atCenterOf(End.Pos()).add(Out.getStepX() * Reach, Out.getStepY() * Reach, Out.getStepZ() * Reach);
    }

    private static Vec3 across(Direction Out, RandomSource Random, double Spread) {
        double First = (Random.nextDouble() * 2.0 - 1.0) * Spread;
        double Second = (Random.nextDouble() * 2.0 - 1.0) * Spread;
        return switch (Out.getAxis()) {
            case X -> new Vec3(0.0, First, Second);
            case Y -> new Vec3(First, 0.0, Second);
            case Z -> new Vec3(First, Second, 0.0);
        };
    }

    void writeBack(ServerLevel World, boolean Force) {
        if (!Force && Math.abs(Pressure - LastWritten) < 0.5) {
            return;
        }
        LastWritten = Pressure;
        for (BlockPos Member : Members) {
            if (World.isLoaded(Member) && World.getBlockEntity(Member) instanceof DuctBlockEntity Duct) {
                Duct.setPressure((float) Pressure);
            }
        }
    }

    public DuctKind getKind() {
        return Kind;
    }

    public Set<BlockPos> getMembers() {
        return Members;
    }

    public long getScannedAt() {
        return ScannedAt;
    }

    public double getPressure() {
        return Pressure;
    }

    public FluidStack getCarrying() {
        return Carrying;
    }

    public record OpenEnd(BlockPos Pos, Direction Out) {
        public Vec3 point(double Reach) {
            return Vec3.atCenterOf(Pos).add(Out.getStepX() * Reach, Out.getStepY() * Reach, Out.getStepZ() * Reach);
        }
    }

    @FunctionalInterface
    public interface Acceptor {
        int take(int Amount, boolean Execute);
    }

    private record Opening(BlockPos Pos, Direction Out) {
    }

    private record Port(BlockPos Pos, PortRole Role) {
    }

    private record FluidEnd(IFluidHandler Handler, BlockPos Owner, PortRole Role) {
    }

    private record GasEnd(GasHandler Handler, BlockPos Owner, PortRole Role, boolean Exhausts) {
    }
}
