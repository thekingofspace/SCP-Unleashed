package net.rai.scp_unleashed.scp4845.staff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;
import net.rai.scp_unleashed.scp4845.Scp4845Restaurant;
import net.rai.scp_unleashed.scp4845.Scp4845Site;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp4845Brain extends Goal {
    public static double DurationScale = 1.0;
    public static final double WalkSpeed = 0.8;
    public static final double ArriveDistance = 0.45;
    public static final double SnapDistance = 1.4;
    public static final int TravelTimeout = 400;
    public static final int RepathTicks = 20;
    public static final int StrayCheck = 40;
    public static final double StrayTeleport = 6.0;
    public static final int[] GrillTicks = {400, 800};
    public static final int[] FryTicks = {300, 600};
    public static final int[] PrepTicks = {400, 700};
    public static final int[] WashTicks = {300, 600};
    public static final int[] RestockTicks = {300, 500};
    public static final int[] MopSpotTicks = {80, 120};
    public static final int[] MopSpots = {3, 5};
    public static final int[] WipeTicks = {120, 200};
    public static final int[] BreakTicks = {500, 800};
    public static final int[] ChatTicks = {160, 280};
    public static final int[] TypeTicks = {160, 400};
    public static final int SighTicks = 50;
    public static final int StretchTicks = 60;
    public static final int GlanceTicks = 50;
    public static final int[] CounterWipeGap = {1200, 2400};
    public static final int CounterWipeTicks = 120;
    public static final int WaveTicks = 24;
    public static final int GreetGap = 1200;
    public static final double GreetRange = 6.0;
    public static final int GreetCheck = 10;
    public static final int[] DrifterTicks = {120, 300};
    public static final int DrifterRoam = 5;
    public static final int[] SeatWaitTicks = {60, 120};
    public static final int NoticeTicks = 40;
    public static final int BreakLoop = 140;
    public static final int BiteTick = 24;
    public static final int SipTick = 68;
    public static final String[] KitchenJobs = {"grill", "fry", "prep", "wash", "restock", "mop", "wipe", "break", "chat"};
    public static final int[] KitchenWeights = {3, 2, 3, 2, 1, 2, 2, 1, 1};

    private enum Phase {
        Choose,
        Travel,
        Work
    }

    private record Step(Vec3 Stand, Direction Facing, Vec3 Look, int Ticks, @Nullable BlockPos Sound) {
    }

    private final Scp4845Employee Owner;
    private Phase Current = Phase.Choose;
    private Scp4845Task Job = Scp4845Task.None;
    private final List<Step> Queue = new ArrayList<>();
    @Nullable
    private Step Active;
    @Nullable
    private BlockPos Seat;
    @Nullable
    private Vec3 SeatExit;
    private boolean Sitting;
    @Nullable
    private Vec3 SittingExit;
    private long Until;
    private long TravelSince;
    private long NextRepath;
    private String Reserved = "";
    private Scp4845Task FlashTask = Scp4845Task.None;
    private long FlashUntil;
    @Nullable
    private UUID Partner;
    private long NextStray;
    private long NextGreet;
    private long NextCounterWipe;
    private long NextSound;
    @Nullable
    private Entity Noticed;
    private long NoticeUntil;
    private final Map<UUID, Long> Greeted = new HashMap<>();
    private final List<Scp4845Task> History = new ArrayList<>();

    public Scp4845Brain(Scp4845Employee Owner) {
        this.Owner = Owner;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return Owner.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return Owner.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void reset() {
        Current = Phase.Choose;
        Job = Scp4845Task.None;
        Queue.clear();
        Active = null;
        Seat = null;
        SeatExit = null;
        Sitting = false;
        SittingExit = null;
        Reserved = "";
        Partner = null;
        FlashTask = Scp4845Task.None;
    }

    public String reserved() {
        return Reserved;
    }

    public boolean working() {
        return Current == Phase.Work;
    }

    public Scp4845Task job() {
        return Job;
    }

    public List<Scp4845Task> history() {
        return History;
    }

    @Nullable
    public BlockPos seat() {
        return Seat;
    }

    public static boolean seatedAt(Level World, BlockPos Pos) {
        return !World.getEntitiesOfClass(Scp4845Employee.class, new AABB(Pos).inflate(1.0), Worker -> Worker.isAlive() && Worker.brain().sitting()
                && Pos.equals(Worker.brain().seat())).isEmpty();
    }

    public void flash(Scp4845Task Task, int Ticks) {
        FlashTask = Task;
        FlashUntil = Owner.level().getGameTime() + Ticks;
    }

    public void notice(Entity Who) {
        Noticed = Who;
        NoticeUntil = Owner.level().getGameTime() + NoticeTicks;
    }

    private static int scaled(RandomSource Random, int[] Range) {
        int Ticks = Range[0] + Random.nextInt(Math.max(1, Range[1] - Range[0] + 1));
        return Math.max(10, (int) Math.round(Ticks * DurationScale));
    }

    @Override
    public void tick() {
        if (!(Owner.level() instanceof ServerLevel World)) {
            return;
        }
        long Now = World.getGameTime();
        Scp4845Site Site = Owner.isBound() ? Scp4845Restaurant.in(World) : null;
        if (Site != null && Now >= NextStray) {
            NextStray = Now + StrayCheck;
            if (!Site.inside(World, Owner.position()) && Owner.position().distanceTo(Vec3.atBottomCenterOf(Site.Restaff)) > StrayTeleport && Current != Phase.Travel) {
                unseat();
                Owner.getNavigation().stop();
                Owner.teleportTo(Site.Restaff.getX() + 0.5, Site.Restaff.getY(), Site.Restaff.getZ() + 0.5);
                abandon();
            }
        }
        if (FlashTask != Scp4845Task.None) {
            if (Now < FlashUntil) {
                Owner.setTask(FlashTask);
                holdPose();
                return;
            }
            FlashTask = Scp4845Task.None;
            if (Current == Phase.Work) {
                Owner.setTask(Job);
            }
        }
        if (Site == null) {
            drifter(World, Now);
            return;
        }
        switch (Owner.role()) {
            case Scp4845Employee.RoleCounter -> counter(World, Site, Now);
            case Scp4845Employee.RoleManager -> manager(World, Site, Now);
            default -> kitchen(World, Site, Now);
        }
    }

    private void abandon() {
        releasePartner();
        Queue.clear();
        Active = null;
        Reserved = "";
        Current = Phase.Choose;
        Owner.setTask(Scp4845Task.None);
    }

    private void releasePartner() {
        if (Partner != null && Owner.level() instanceof ServerLevel World && World.getEntity(Partner) instanceof Scp4845Employee Other && Other.brain().Partner != null
                && Other.brain().Partner.equals(Owner.getUUID())) {
            Other.brain().Partner = null;
            Other.brain().abandon();
        }
        Partner = null;
    }

    private void begin(Scp4845Task Task, String Key, List<Step> Steps) {
        Job = Task;
        Reserved = Key;
        Queue.clear();
        Queue.addAll(Steps);
        record(Task);
        next();
    }

    private void record(Scp4845Task Task) {
        History.add(Task);
        if (History.size() > 16) {
            History.removeFirst();
        }
    }

    private void next() {
        if (Queue.isEmpty()) {
            abandon();
            return;
        }
        Active = Queue.removeFirst();
        Vec3 Goal = SeatExit != null ? SeatExit : Active.Stand();
        travel(Goal);
    }

    private void travel(Vec3 Goal) {
        unseat();
        Current = Phase.Travel;
        TravelSince = Owner.level().getGameTime();
        NextRepath = TravelSince + RepathTicks;
        Owner.setTask(Scp4845Task.None);
        Owner.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, WalkSpeed);
    }

    private void travelling(long Now) {
        if (Active == null) {
            abandon();
            return;
        }
        Vec3 Goal = SeatExit != null ? SeatExit : Active.Stand();
        double Distance = horizontal(Owner.position(), Goal);
        boolean Done = Owner.getNavigation().isDone();
        if (Distance <= ArriveDistance || Done && Distance <= SnapDistance || Now - TravelSince > TravelTimeout) {
            arrive(Now);
            return;
        }
        if (Done && Now >= NextRepath) {
            NextRepath = Now + RepathTicks;
            Owner.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, WalkSpeed);
        }
    }

    private void arrive(long Now) {
        Owner.getNavigation().stop();
        if (Seat != null && PropSeatEntity.occupied(Owner.level(), Seat)) {
            Seat = null;
            SeatExit = null;
            abandon();
            return;
        }
        Vec3 Spot = Active.Stand();
        if (Seat != null && SeatExit != null) {
            Sitting = true;
            SittingExit = SeatExit;
        }
        Owner.teleportTo(Spot.x, Spot.y, Spot.z);
        Owner.setDeltaMovement(Vec3.ZERO);
        Current = Phase.Work;
        Until = Now + Active.Ticks();
        Owner.setTask(Job);
        face();
        if (Job == Scp4845Task.Chat && Partner != null && Owner.level() instanceof ServerLevel World && World.getEntity(Partner) instanceof Scp4845Employee Other
                && Other.isAlive()) {
            Other.brain().joinChat(Owner, Until);
        }
    }

    private void unseat() {
        if (Sitting && SittingExit != null) {
            Owner.teleportTo(SittingExit.x, SittingExit.y, SittingExit.z);
        }
        Sitting = false;
        SittingExit = null;
    }

    public boolean sitting() {
        return Sitting;
    }

    private void face() {
        if (Active == null) {
            return;
        }
        float Yaw = Active.Facing().toYRot();
        Owner.setYRot(Yaw);
        Owner.setYBodyRot(Yaw);
        Owner.setYHeadRot(Yaw);
    }

    private void holdPose() {
        if (Current == Phase.Work && Active != null) {
            Vec3 Spot = Active.Stand();
            if (horizontal(Owner.position(), Spot) > 0.05) {
                Owner.teleportTo(Spot.x, Spot.y, Spot.z);
            }
            Owner.getNavigation().stop();
            face();
            Vec3 Look = Noticed != null && Owner.level().getGameTime() < NoticeUntil && !Owner.task().Seated ? Noticed.getEyePosition() : Active.Look();
            Owner.getLookControl().setLookAt(Look.x, Look.y, Look.z, 30.0f, 30.0f);
        }
    }

    private static double horizontal(Vec3 First, Vec3 Second) {
        double Dx = First.x - Second.x;
        double Dz = First.z - Second.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    private static Vec3 lookAt(BlockPos Block, double Height) {
        return new Vec3(Block.getX() + 0.5, Block.getY() + Height, Block.getZ() + 0.5);
    }

    private List<Scp4845Employee> colleagues(ServerLevel World, Scp4845Site Site) {
        return World.getEntitiesOfClass(Scp4845Employee.class, Site.Protected, Other -> Other != Owner && Other.isAlive() && Other.isBound());
    }

    private boolean taken(ServerLevel World, Scp4845Site Site, String Key) {
        for (Scp4845Employee Other : colleagues(World, Site)) {
            if (Other.brain().Reserved.equals(Key)) {
                return true;
            }
        }
        return false;
    }

    private void kitchen(ServerLevel World, Scp4845Site Site, long Now) {
        switch (Current) {
            case Choose -> chooseKitchen(World, Site, Now);
            case Travel -> travelling(Now);
            case Work -> {
                holdPose();
                workSounds(World, Now);
                if (Now >= Until) {
                    if (Job == Scp4845Task.Chat) {
                        releasePartner();
                    }
                    if (Queue.isEmpty()) {
                        abandon();
                    } else {
                        next();
                    }
                }
            }
        }
    }

    private String pickJob(RandomSource Random) {
        int Total = 0;
        for (int Weight : KitchenWeights) {
            Total += Weight;
        }
        int Roll = Random.nextInt(Total);
        for (int Index = 0; Index < KitchenJobs.length; Index++) {
            Roll -= KitchenWeights[Index];
            if (Roll < 0) {
                return KitchenJobs[Index];
            }
        }
        return KitchenJobs[0];
    }

    private void chooseKitchen(ServerLevel World, Scp4845Site Site, long Now) {
        RandomSource Random = Owner.getRandom();
        Scp4845Task Last = History.isEmpty() ? Scp4845Task.None : History.getLast();
        for (int Attempt = 0; Attempt < 8; Attempt++) {
            String Kind = pickJob(Random);
            if (tryJob(World, Site, Kind, Random, Last)) {
                return;
            }
        }
        tryJob(World, Site, "mop", Random, Scp4845Task.None);
    }

    private boolean tryJob(ServerLevel World, Scp4845Site Site, String Kind, RandomSource Random, Scp4845Task Last) {
        SeatExit = null;
        Seat = null;
        switch (Kind) {
            case "grill", "fry", "prep", "wash", "restock" -> {
                Scp4845Task Task = switch (Kind) {
                    case "grill" -> Scp4845Task.Grill;
                    case "fry" -> Scp4845Task.Fry;
                    case "prep" -> Scp4845Task.Prep;
                    case "wash" -> Scp4845Task.Wash;
                    default -> Scp4845Task.Restock;
                };
                if (Task == Last) {
                    return false;
                }
                int[] Range = switch (Kind) {
                    case "grill" -> GrillTicks;
                    case "fry" -> FryTicks;
                    case "prep" -> PrepTicks;
                    case "wash" -> WashTicks;
                    default -> RestockTicks;
                };
                List<Scp4845Site.Station> Options = new ArrayList<>(Site.stations(Kind));
                while (!Options.isEmpty()) {
                    Scp4845Site.Station Pick = Options.remove(Random.nextInt(Options.size()));
                    String Key = "station:" + Pick.Block().toShortString();
                    if (taken(World, Site, Key)) {
                        continue;
                    }
                    double Height = Task == Scp4845Task.Restock ? 1.2 : 0.9;
                    begin(Task, Key, List.of(new Step(Pick.centre(), Pick.Facing(), lookAt(Pick.Block(), Height), scaled(Random, Range), Pick.Block())));
                    return true;
                }
                return false;
            }
            case "wipe" -> {
                if (Last == Scp4845Task.Wipe) {
                    return false;
                }
                List<Scp4845Site.Station> Options = new ArrayList<>(Site.stations("wipe"));
                List<Step> Steps = new ArrayList<>();
                int Count = 1 + Random.nextInt(2);
                while (!Options.isEmpty() && Steps.size() < Count) {
                    Scp4845Site.Station Pick = Options.remove(Random.nextInt(Options.size()));
                    if (taken(World, Site, "station:" + Pick.Block().toShortString())) {
                        continue;
                    }
                    Steps.add(new Step(Pick.centre(), Pick.Facing(), lookAt(Pick.Block(), 0.85), scaled(Random, WipeTicks), Pick.Block()));
                }
                if (Steps.isEmpty()) {
                    return false;
                }
                begin(Scp4845Task.Wipe, "wipe", Steps);
                return true;
            }
            case "mop" -> {
                if (Last == Scp4845Task.Mop && Random.nextBoolean()) {
                    return false;
                }
                List<BlockPos> Options = new ArrayList<>(Site.Mop);
                List<Step> Steps = new ArrayList<>();
                int Count = MopSpots[0] + Random.nextInt(MopSpots[1] - MopSpots[0] + 1);
                while (!Options.isEmpty() && Steps.size() < Count) {
                    BlockPos Pick = Options.remove(Random.nextInt(Options.size()));
                    Direction Facing = Direction.Plane.HORIZONTAL.getRandomDirection(Random);
                    Steps.add(new Step(Vec3.atBottomCenterOf(Pick), Facing, lookAt(Pick.relative(Facing), 0.1), scaled(Random, MopSpotTicks), Pick));
                }
                begin(Scp4845Task.Mop, "mop", Steps);
                return true;
            }
            case "break" -> {
                if (Last == Scp4845Task.Break) {
                    return false;
                }
                for (Scp4845Site.Spot Option : Site.BreakSeats) {
                    String Key = "seat:" + Option.Pos().toShortString();
                    if (taken(World, Site, Key) || PropSeatEntity.occupied(World, Option.Pos())) {
                        continue;
                    }
                    Vec3 Exit = exitOf(World, Option.Pos(), Option.Facing());
                    if (Exit == null) {
                        continue;
                    }
                    Seat = Option.Pos();
                    SeatExit = Exit;
                    begin(Scp4845Task.Break, Key, List.of(new Step(Option.centre(), Option.Facing(), lookAt(Option.Pos().relative(Option.Facing()), 0.6), scaled(Random, BreakTicks),
                            Option.Pos())));
                    return true;
                }
                return false;
            }
            case "chat" -> {
                if (Last == Scp4845Task.Chat) {
                    return false;
                }
                for (Scp4845Employee Other : colleagues(World, Site)) {
                    Scp4845Brain Mind = Other.brain();
                    if (Other.role() != Scp4845Employee.RoleKitchen || Mind.Current != Phase.Work || Mind.Job.Seated || Mind.Partner != null || Mind.Job == Scp4845Task.Chat
                            || Mind.FlashTask != Scp4845Task.None) {
                        continue;
                    }
                    Vec3 Spot = besideOf(World, Other);
                    if (Spot == null) {
                        continue;
                    }
                    Vec3 Toward = Other.position().subtract(Spot);
                    Direction Facing = Direction.getNearest(Toward.x, 0.0, Toward.z);
                    Partner = Other.getUUID();
                    begin(Scp4845Task.Chat, "chat", List.of(new Step(Spot, Facing, Other.getEyePosition(), scaled(Random, ChatTicks), null)));
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    public void joinChat(Scp4845Employee Speaker, long End) {
        releasePartner();
        Queue.clear();
        Partner = Speaker.getUUID();
        Vec3 Toward = Speaker.position().subtract(Owner.position());
        Direction Facing = Direction.getNearest(Toward.x, 0.0, Toward.z);
        Active = new Step(Owner.position(), Facing, Speaker.getEyePosition(), (int) (End - Owner.level().getGameTime()), null);
        Job = Scp4845Task.Chat;
        Reserved = "";
        record(Scp4845Task.Chat);
        Current = Phase.Work;
        Until = End;
        Owner.setTask(Scp4845Task.Chat);
        face();
    }

    @Nullable
    private static Vec3 exitOf(Level World, BlockPos Seat, Direction Facing) {
        for (Direction Side : new Direction[]{Facing.getOpposite(), Facing.getClockWise(), Facing.getCounterClockWise()}) {
            BlockPos Cell = Seat.relative(Side);
            if (open(World, Cell)) {
                return Vec3.atBottomCenterOf(Cell);
            }
        }
        return null;
    }

    @Nullable
    private Vec3 besideOf(Level World, Scp4845Employee Other) {
        BlockPos Base = Other.blockPosition();
        Vec3 Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            BlockPos Cell = Base.relative(Side);
            if (!open(World, Cell)) {
                continue;
            }
            Vec3 Spot = Vec3.atBottomCenterOf(Cell);
            double Distance = Spot.distanceToSqr(Owner.position());
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Spot;
            }
        }
        return Best;
    }

    public static boolean open(Level World, BlockPos Cell) {
        BlockState Feet = World.getBlockState(Cell);
        BlockState Head = World.getBlockState(Cell.above());
        return Feet.getCollisionShape(World, Cell).isEmpty() && Head.getCollisionShape(World, Cell.above()).isEmpty()
                && World.getBlockState(Cell.below()).isFaceSturdy(World, Cell.below(), Direction.UP);
    }

    private void counter(ServerLevel World, Scp4845Site Site, long Now) {
        Scp4845Site.Spot Spot = Site.CounterWorker;
        if (Current == Phase.Choose) {
            SeatExit = null;
            Seat = null;
            BlockPos Register = Spot.Pos().relative(Spot.Facing());
            begin(Scp4845Task.Register, "counter", List.of(new Step(Spot.centre(), Spot.Facing(), lookAt(Register, 1.3), Integer.MAX_VALUE / 4, Register)));
            NextCounterWipe = Now + scaled(Owner.getRandom(), CounterWipeGap);
            return;
        }
        if (Current == Phase.Travel) {
            travelling(Now);
            return;
        }
        holdPose();
        if (Now >= NextGreet) {
            NextGreet = Now + GreetCheck;
            greet(World, Spot, Now);
        }
        if (Now >= NextCounterWipe) {
            NextCounterWipe = Now + scaled(Owner.getRandom(), CounterWipeGap);
            if (World.getNearestPlayer(Owner, 8.0) == null) {
                flash(Scp4845Task.Wipe, CounterWipeTicks);
                record(Scp4845Task.Wipe);
            }
        }
        if (Now >= NextSound && Owner.task() == Scp4845Task.Register) {
            NextSound = Now + 200 + Owner.getRandom().nextInt(200);
            play(World, Spot.Pos().relative(Spot.Facing()), List.of(Scp4845Registry.BeepClick), 0.25f, 1.0f);
        }
    }

    private void greet(ServerLevel World, Scp4845Site.Spot Spot, long Now) {
        Vec3 Front = Spot.centre().add(Spot.Facing().getStepX() * 2.0, 0.0, Spot.Facing().getStepZ() * 2.0);
        for (ServerPlayer Customer : World.getEntitiesOfClass(ServerPlayer.class, new AABB(Front, Front).inflate(GreetRange, 2.0, GreetRange))) {
            if (Customer.isSpectator()) {
                continue;
            }
            Vec3 Offset = Customer.position().subtract(Spot.centre());
            if (Offset.x * Spot.Facing().getStepX() + Offset.z * Spot.Facing().getStepZ() < 0.5) {
                continue;
            }
            Long Last = Greeted.get(Customer.getUUID());
            if (Last != null && Now - Last < GreetGap) {
                continue;
            }
            Greeted.put(Customer.getUUID(), Now);
            flash(Scp4845Task.Wave, WaveTicks);
            notice(Customer);
            Scp4845Lines.say(Owner, Customer, Scp4845Lines.Greet);
            return;
        }
    }

    private void manager(ServerLevel World, Scp4845Site Site, long Now) {
        Scp4845Site.Spot Spot = Site.Manager;
        if (Current == Phase.Choose) {
            Vec3 Exit = exitOf(World, Spot.Pos(), Spot.Facing());
            if (PropSeatEntity.occupied(World, Spot.Pos())) {
                Seat = null;
                SeatExit = null;
                begin(Scp4845Task.None, "manager", List.of(new Step(Exit != null ? Exit : Owner.position(), Spot.Facing(), lookAt(Spot.Pos(), 1.2),
                        scaled(Owner.getRandom(), SeatWaitTicks), null)));
                return;
            }
            Seat = Spot.Pos();
            SeatExit = Exit;
            begin(Scp4845Task.Type, "manager", List.of(new Step(Spot.centre(), Spot.Facing(), lookAt(Site.Desk, 1.4), scaled(Owner.getRandom(), TypeTicks), Site.Desk)));
            return;
        }
        if (Current == Phase.Travel) {
            travelling(Now);
            return;
        }
        holdPose();
        if (Seat == null) {
            if (Now >= Until) {
                abandon();
            }
            return;
        }
        if (Now >= Until) {
            RandomSource Random = Owner.getRandom();
            if (Job == Scp4845Task.Type) {
                int Roll = Random.nextInt(3);
                Job = Roll == 0 ? Scp4845Task.Sigh : Roll == 1 ? Scp4845Task.Stretch : Scp4845Task.Glance;
                Until = Now + (Job == Scp4845Task.Sigh ? SighTicks : Job == Scp4845Task.Stretch ? StretchTicks : GlanceTicks);
                if (Job == Scp4845Task.Sigh) {
                    play(World, Owner.blockPosition().above(), Scp4845Registry.SighLayers, 0.5f, 0.9f + Random.nextFloat() * 0.1f);
                }
            } else {
                Job = Scp4845Task.Type;
                Until = Now + scaled(Random, TypeTicks);
            }
            record(Job);
            Owner.setTask(Job);
        }
        workSounds(World, Now);
    }

    private void drifter(ServerLevel World, long Now) {
        if (Current == Phase.Travel) {
            travelling(Now);
            return;
        }
        if (Current == Phase.Work) {
            holdPose();
            if (Now >= Until) {
                abandon();
            }
            return;
        }
        RandomSource Random = Owner.getRandom();
        BlockPos Home = Owner.home() != null ? Owner.home() : Owner.blockPosition();
        BlockPos Spot = Home.offset(Random.nextInt(DrifterRoam * 2 + 1) - DrifterRoam, 0, Random.nextInt(DrifterRoam * 2 + 1) - DrifterRoam);
        for (int Dy = 2; Dy >= -2; Dy--) {
            if (open(World, Spot.above(Dy))) {
                Spot = Spot.above(Dy);
                break;
            }
        }
        Direction Facing = Direction.Plane.HORIZONTAL.getRandomDirection(Random);
        Scp4845Task[] Choices = {Scp4845Task.None, Scp4845Task.None, Scp4845Task.Mop, Scp4845Task.Wipe, Scp4845Task.Chat};
        Scp4845Task Task = Choices[Random.nextInt(Choices.length)];
        SeatExit = null;
        Seat = null;
        begin(Task, "", List.of(new Step(open(World, Spot) ? Vec3.atBottomCenterOf(Spot) : Owner.position(), Facing, lookAt(Spot.relative(Facing), 1.2),
                scaled(Random, DrifterTicks), null)));
    }

    private void workSounds(ServerLevel World, long Now) {
        if (Active == null) {
            return;
        }
        RandomSource Random = Owner.getRandom();
        BlockPos Where = Active.Sound() != null ? Active.Sound() : Owner.blockPosition();
        Scp4845Task Task = Owner.task();
        if (Task == Scp4845Task.Type) {
            if (Now >= NextSound) {
                NextSound = Now + 3 + Random.nextInt(5);
                play(World, Where, List.of(Scp4845Registry.Keyboard), 0.22f, 0.9f + Random.nextFloat() * 0.2f);
                if (Random.nextInt(9) == 0) {
                    play(World, Where, List.of(Scp4845Registry.KeyboardClack), 0.3f, 1.0f);
                }
            }
            return;
        }
        if (Task == Scp4845Task.Break) {
            int Beat = (int) Math.floorMod(Now - Owner.taskStart(), (long) BreakLoop);
            if (Beat == BiteTick) {
                play(World, Owner.blockPosition().above(), Scp4845Registry.CrunchLayers, 0.3f, 1.1f);
            } else if (Beat == SipTick) {
                play(World, Owner.blockPosition().above(), List.of(Scp4845Registry.Fizz), 0.25f, 1.3f);
            }
            return;
        }
        if (Now < NextSound) {
            if (Task == Scp4845Task.Grill && Now % 8 == 0) {
                World.sendParticles(ParticleTypes.SMOKE, Where.getX() + 0.5, Where.getY() + 1.0, Where.getZ() + 0.5, 2, 0.25, 0.05, 0.25, 0.01);
            }
            return;
        }
        switch (Task) {
            case Grill -> {
                NextSound = Now + 50 + Random.nextInt(30);
                play(World, Where, Scp4845Registry.SizzleLayers, 0.55f, 0.9f + Random.nextFloat() * 0.2f);
            }
            case Fry -> {
                NextSound = Now + 60 + Random.nextInt(30);
                play(World, Where, Scp4845Registry.BubbleLayers, 0.5f, 0.9f + Random.nextFloat() * 0.2f);
            }
            case Wash -> {
                NextSound = Now + 40 + Random.nextInt(30);
                play(World, Where, Scp4845Registry.WashLayers, 0.45f, 0.9f + Random.nextFloat() * 0.2f);
            }
            case Mop -> {
                NextSound = Now + 30 + Random.nextInt(20);
                play(World, Where, Scp4845Registry.MopLayers, 0.35f, 0.9f + Random.nextFloat() * 0.2f);
            }
            case Restock -> {
                NextSound = Now + 70 + Random.nextInt(20);
                play(World, Where, Scp4845Registry.RestockLayers, 0.5f, 0.9f + Random.nextFloat() * 0.2f);
            }
            case Prep -> {
                NextSound = Now + 60 + Random.nextInt(30);
                play(World, Where, List.of(Scp4845Registry.Bag), 0.25f, 1.1f + Random.nextFloat() * 0.2f);
            }
            default -> NextSound = Now + 20;
        }
    }

    private static void play(ServerLevel World, BlockPos Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        Scp4845Registry.playLayers(World, null, Where.getX() + 0.5, Where.getY() + 0.8, Where.getZ() + 0.5, Layers, SoundSource.NEUTRAL, Volume, Mth.clamp(Pitch, 0.5f, 2.0f));
    }
}
