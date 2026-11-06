package net.rai.scp_unleashed.scp428;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.rai.scp_unleashed.door.DoorButtonBlock;
import net.rai.scp_unleashed.door.DoorLeverBlock;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.npc.doors.NpcDoors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class Scp428Doorwork {
    public static final int Search = 2;
    public static final double Reach = 1.6;
    public static final double ReachUp = 1.4;
    public static final double CordReach = 2.2;
    public static final double CordReachUp = 2.8;
    public static final double WaitNear = 1.4;
    public static final int HurryNodes = 5;
    public static final int HurryTicks = 6;
    public static final int ButtonMemory = 100;
    public static final int LeverMemory = 400;
    public static final int ScannerMemory = 120;
    public static final int DoorMemory = 100;
    public static final int HandMemory = 40;
    public static final int WallMemory = 300;
    public static final int OpenWait = 40;
    public static final int SwipeWait = 300;
    public static final int SwipeHold = 100;
    public static final double ReadyNear = 1.5;
    public static final int Reopen = 20;
    public static final int CloseDelay = 30;
    public static final int Forget = 600;
    public static final int Boost = 50;
    public static final double BoostSpeed = 2.2;
    public static final int GiveUp = 200;
    public static final int RepathTicks = 10;
    public static final int ScanTicks = 20;
    public static final double Arrived = 2.5;
    public static final double PassageReach = 2.2;
    public static final int Radius = 8;

    private Scp428Doorwork() {
    }

    public enum Kind {
        Door, Gate, Hatch, Panel, Button, Lever, Scanner, None
    }

    public record Doorway(List<BlockPos> Cells, BlockPos Origin, boolean Hand) {
    }

    public static Kind kindOf(BlockState State) {
        if (State.getBlock() instanceof DoorBlock Hinged) {
            return Hinged.type().canOpenByHand() ? Kind.Door : Kind.None;
        }
        if (State.getBlock() instanceof FenceGateBlock) {
            return Kind.Gate;
        }
        if (State.getBlock() instanceof TrapDoorBlock) {
            return State.is(Blocks.IRON_TRAPDOOR) ? Kind.None : Kind.Hatch;
        }
        if (State.getBlock() instanceof PanelDoorBlock Panel) {
            return Panel.getKind().isManual() ? Kind.Panel : Kind.None;
        }
        if (State.getBlock() instanceof ButtonBlock || State.getBlock() instanceof DoorButtonBlock) {
            return Kind.Button;
        }
        if (State.getBlock() instanceof LeverBlock || State.getBlock() instanceof DoorLeverBlock) {
            return Kind.Lever;
        }
        if (State.getBlock() instanceof KeycardScannerBlock) {
            return Kind.Scanner;
        }
        return Kind.None;
    }

    public static boolean isPassage(Kind Found) {
        return Found == Kind.Door || Found == Kind.Gate || Found == Kind.Hatch || Found == Kind.Panel;
    }

    public static boolean isDevice(Kind Found) {
        return Found == Kind.Button || Found == Kind.Lever || Found == Kind.Scanner;
    }

    public static boolean isDoorway(BlockState State) {
        return State.getBlock() instanceof DoorBlock || State.getBlock() instanceof PanelDoorBlock || State.getBlock() instanceof TrapDoorBlock
                || State.getBlock() instanceof FenceGateBlock;
    }

    public static boolean isOpen(BlockState State) {
        return State.hasProperty(BlockStateProperties.OPEN) && State.getValue(BlockStateProperties.OPEN);
    }

    public static boolean isPressed(BlockState State) {
        return State.hasProperty(BlockStateProperties.POWERED) && State.getValue(BlockStateProperties.POWERED);
    }

    public static boolean isClosedPassage(BlockState State) {
        return isPassage(kindOf(State)) && !isOpen(State);
    }

    public static boolean isOpen(BlockGetter World, Doorway Found) {
        for (BlockPos Cell : Found.Cells()) {
            if (isOpen(World.getBlockState(Cell))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Doorway doorwayAt(Level World, BlockPos Pos) {
        List<BlockPos> Cells = NpcDoors.extent(World, Pos);
        if (Cells.isEmpty()) {
            return null;
        }
        BlockPos Origin = Cells.stream().min(Comparator.comparingLong(BlockPos::asLong)).orElse(Pos);
        return new Doorway(Cells, Origin, isPassage(kindOf(World.getBlockState(Pos))));
    }

    public static boolean operate(ServerLevel World, Mob User, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (kindOf(State) == Kind.None) {
            return false;
        }
        Kind Sort = kindOf(State);
        boolean Used;
        if (Sort == Kind.Button || Sort == Kind.Lever) {
            Used = NpcDoors.useOpener(User, Pos);
        } else {
            Used = State.useWithoutItem(World, hand(World, User, Pos), hit(User, Pos)).consumesAction();
        }
        Vec3 Centre = Vec3.atCenterOf(Pos);
        if (Used) {
            if (User instanceof Scp428Core Core) {
                Core.touched(Pos);
            }
            User.swing(InteractionHand.MAIN_HAND, true);
            User.getLookControl().setLookAt(Centre);
            return true;
        }
        return false;
    }

    private static FakePlayer hand(ServerLevel World, Mob User, BlockPos Pos) {
        FakePlayer Hand = FakePlayerFactory.getMinecraft(World);
        Vec3 Eye = User.getEyePosition();
        Vec3 Centre = Vec3.atCenterOf(Pos);
        float Yaw = (float) (Mth.atan2(Centre.z - Eye.z, Centre.x - Eye.x) * Mth.RAD_TO_DEG) - 90.0f;
        Hand.moveTo(Eye.x, Eye.y - Hand.getEyeHeight(), Eye.z, Yaw, 0.0f);
        return Hand;
    }

    private static BlockHitResult hit(Mob User, BlockPos Pos) {
        Vec3 Centre = Vec3.atCenterOf(Pos);
        Vec3 Toward = User.getEyePosition().subtract(Centre);
        return new BlockHitResult(Centre, Direction.getNearest(Toward.x, Toward.y, Toward.z), Pos, false);
    }

    public static boolean inReach(Mob User, BlockPos Pos) {
        boolean Cord = User instanceof Scp428Core;
        return User.getBoundingBox().inflate(Cord ? CordReach : Reach, Cord ? CordReachUp : ReachUp, Cord ? CordReach : Reach).contains(Vec3.atCenterOf(Pos));
    }

    public static final class Agent {
        private final Map<BlockPos, Long> Tried = new HashMap<>();
        private final Map<BlockPos, Long> Doors = new HashMap<>();
        private final List<Opened> Passages = new ArrayList<>();
        @Nullable
        private BlockPos Device;
        @Nullable
        private Doorway DeviceDoor;
        @Nullable
        private Doorway WaitDoor;
        @Nullable
        private BlockPos LastUsed;
        @Nullable
        private BlockPos WaitDevice;
        private boolean WaitPulse;
        private int DeviceTicks;
        private int Waiting;
        private int BoostLeft;
        private int RepathReady;
        private int ScanReady;
        private int Operations;

        public boolean busy() {
            return Device != null || WaitDoor != null;
        }

        @Nullable
        public BlockPos device() {
            return Device;
        }

        @Nullable
        public BlockPos lastUsed() {
            return LastUsed;
        }

        public int operations() {
            return Operations;
        }

        public boolean ready(Mob User) {
            if (WaitDoor == null || SwipeWait - Waiting >= SwipeHold) {
                return true;
            }
            double Nearest = WaitDoor.Cells().stream().mapToDouble(Cell -> Vec3.atBottomCenterOf(Cell).distanceTo(User.position())).min().orElse(0.0);
            return Nearest - User.getBbWidth() * 0.5 <= ReadyNear;
        }

        public double speedFactor() {
            return BoostLeft > 0 ? BoostSpeed : 1.0;
        }

        public void reset() {
            Device = null;
            DeviceDoor = null;
            DeviceTicks = 0;
            RepathReady = 0;
        }

        public void tick(Mob User, ServerLevel World) {
            if (BoostLeft > 0) {
                BoostLeft--;
            }
            long Now = World.getGameTime();
            openAhead(User, World, Now);
            hurryThrough(User, World);
            closeBehind(User, World, Now);
            if (Tried.size() > 24) {
                Tried.values().removeIf(Until -> Until <= Now);
            }
            if (Doors.size() > 24) {
                Doors.values().removeIf(Until -> Until <= Now);
            }
        }

        private void openAhead(Mob User, ServerLevel World, long Now) {
            Path Route = User.getNavigation().getPath();
            if (Route == null || Route.isDone()) {
                return;
            }
            int From = Route.getNextNodeIndex();
            int To = Math.min(Route.getNodeCount(), From + 3);
            for (int Index = Math.max(0, From - 1); Index < To; Index++) {
                Node Step = Route.getNode(Index);
                for (int Lift = 0; Lift <= 1; Lift++) {
                    BlockPos Pos = new BlockPos(Step.x, Step.y + Lift, Step.z);
                    BlockState State = World.getBlockState(Pos);
                    if (!isClosedPassage(State) || !inReach(User, Pos) && User.position().distanceTo(Vec3.atBottomCenterOf(Pos)) > PassageReach) {
                        continue;
                    }
                    if (operate(World, User, Pos)) {
                        Operations++;
                        LastUsed = Pos.immutable();
                        Passages.add(new Opened(Pos.immutable(), Now));
                        return;
                    }
                }
            }
        }

        private void hurryThrough(Mob User, ServerLevel World) {
            Path Route = User.getNavigation().getPath();
            if (Route == null || Route.isDone()) {
                return;
            }
            int From = Route.getNextNodeIndex();
            int To = Math.min(Route.getNodeCount(), From + HurryNodes);
            for (int Index = Math.max(0, From - 1); Index < To; Index++) {
                Node Step = Route.getNode(Index);
                for (int Lift = 0; Lift <= 1; Lift++) {
                    BlockPos Pos = new BlockPos(Step.x, Step.y + Lift, Step.z);
                    BlockState State = World.getBlockState(Pos);
                    if (isDoorway(State) && isOpen(State) && !isPassage(kindOf(State))) {
                        if (BoostLeft < HurryTicks) {
                            BoostLeft = HurryTicks;
                            if (User.getNavigation() instanceof Scp428Navigation Walker) {
                                Walker.setSpeedModifier(Math.max(Walker.speed(), BoostSpeed));
                            }
                        }
                        return;
                    }
                }
            }
        }

        private void closeBehind(Mob User, ServerLevel World, long Now) {
            Iterator<Opened> Each = Passages.iterator();
            while (Each.hasNext()) {
                Opened Entry = Each.next();
                if (Now - Entry.At() > Forget) {
                    Each.remove();
                    continue;
                }
                if (Now - Entry.At() < CloseDelay) {
                    continue;
                }
                BlockState State = World.getBlockState(Entry.Pos());
                Kind Found = kindOf(State);
                if (!isPassage(Found) || !isOpen(State)) {
                    Each.remove();
                    continue;
                }
                if (Found == Kind.Hatch || Found == Kind.Panel) {
                    Each.remove();
                    continue;
                }
                AABB Doorway0 = new AABB(Entry.Pos()).inflate(0.6, 0.0, 0.6).expandTowards(0.0, 1.0, 0.0);
                if (User.getBoundingBox().intersects(Doorway0.inflate(0.4))) {
                    continue;
                }
                if (!World.getEntitiesOfClass(LivingEntity.class, Doorway0, LivingEntity::isAlive).isEmpty()) {
                    continue;
                }
                if (User.position().distanceTo(Vec3.atBottomCenterOf(Entry.Pos())) > Radius) {
                    Each.remove();
                    continue;
                }
                operate(World, User, Entry.Pos());
                Each.remove();
            }
        }

        public boolean route(Mob User, ServerLevel World, Vec3 Destination, double Speed) {
            return route(User, World, Destination, Speed, Arrived);
        }

        public boolean route(Mob User, ServerLevel World, Vec3 Destination, double Speed, double Arrive) {
            PathNavigation Navigation = User.getNavigation();
            if (Device != null) {
                followDevice(User, World);
                return true;
            }
            if (WaitDoor != null) {
                if (isOpen(World, WaitDoor)) {
                    if (WaitPulse) {
                        long Now = World.getGameTime();
                        Doors.put(WaitDoor.Origin(), Now + Reopen);
                        if (WaitDevice != null) {
                            Tried.put(WaitDevice, Now + Reopen);
                        }
                    }
                    WaitDoor = null;
                    RepathReady = 0;
                    BoostLeft = Boost;
                } else if (--Waiting <= 0) {
                    WaitDoor = null;
                    RepathReady = 0;
                } else {
                    approach(User, WaitDoor, Speed);
                    return true;
                }
            }
            if (User.position().distanceTo(Destination) <= Arrive) {
                Navigation.stop();
                return false;
            }
            if (--RepathReady > 0 && Navigation.isInProgress()) {
                Navigation.setSpeedModifier(Speed * speedFactor());
                return true;
            }
            RepathReady = RepathTicks + User.getRandom().nextInt(4);
            Path Current = Navigation.getPath();
            if (Current != null && !Current.canReach()) {
                Navigation.stop();
            }
            Path Route = Navigation.createPath(BlockPos.containing(Destination), 1);
            if (Route != null && Route.canReach()) {
                Navigation.moveTo(Route, Speed * speedFactor());
                return true;
            }
            if (--ScanReady <= 0) {
                ScanReady = ScanTicks / RepathTicks;
                if (plan(User, World, Destination)) {
                    return true;
                }
            }
            if (Route != null) {
                Navigation.moveTo(Route, Speed * speedFactor());
                return true;
            }
            return false;
        }

        private void approach(Mob User, Doorway Found, double Speed) {
            BlockPos Nearest = Found.Cells().stream().min(Comparator.comparingDouble(Cell -> Vec3.atBottomCenterOf(Cell).distanceToSqr(User.position()))).orElse(Found.Origin());
            PathNavigation Navigation = User.getNavigation();
            if (User.position().distanceTo(Vec3.atBottomCenterOf(Nearest)) <= WaitNear) {
                Navigation.stop();
                return;
            }
            if (--RepathReady > 0 && Navigation.isInProgress()) {
                return;
            }
            RepathReady = RepathTicks;
            Path Route = Navigation.createPath(Nearest, 1);
            if (Route != null) {
                Navigation.moveTo(Route, Speed);
            }
        }

        private boolean plan(Mob User, ServerLevel World, Vec3 Destination) {
            if (!(User.getNavigation() instanceof Scp428Navigation Walker)) {
                return false;
            }
            Doorway Blocking = Walker.blockingDoor(BlockPos.containing(Destination));
            if (Blocking == null) {
                return false;
            }
            long Now = World.getGameTime();
            Long Until = Doors.get(Blocking.Origin());
            if (Until != null && Until > Now) {
                return false;
            }
            if (Blocking.Hand()) {
                BlockPos Nearest = Blocking.Cells().stream().min(Comparator.comparingDouble(Cell -> Vec3.atCenterOf(Cell).distanceToSqr(User.position()))).orElse(Blocking.Origin());
                Device = Nearest;
                DeviceDoor = Blocking;
                DeviceTicks = 0;
                return true;
            }
            BlockPos Opener = NpcDoors.findOpener(World, Blocking.Origin()).orElse(null);
            if (Opener != null && !remembered(Opener, Now) && !(kindOf(World.getBlockState(Opener)) == Kind.Button && isPressed(World.getBlockState(Opener)))) {
                Device = Opener;
                DeviceDoor = Blocking;
                DeviceTicks = 0;
                return true;
            }
            BlockPos Scanner = NpcDoors.findScanner(World, Blocking.Origin()).orElse(null);
            if (Scanner != null && !remembered(Scanner, Now)) {
                Scp428Instance Swiper = swiperFor(User, World, Scanner);
                if (Swiper != null) {
                    Swiper.assignSwipe(Scanner);
                    Tried.put(Scanner, Now + ScannerMemory);
                    Doors.put(Blocking.Origin(), Now + SwipeWait);
                    WaitDoor = Blocking;
                    WaitDevice = Scanner;
                    WaitPulse = true;
                    Waiting = SwipeWait;
                    User.getNavigation().stop();
                    return true;
                }
            }
            Doors.put(Blocking.Origin(), Now + WallMemory);
            return false;
        }

        private boolean remembered(BlockPos Pos, long Now) {
            Long Until = Tried.get(Pos);
            return Until != null && Until > Now;
        }

        @Nullable
        private Scp428Instance swiperFor(Mob User, ServerLevel World, BlockPos Scanner) {
            if (User instanceof Scp428Core Core) {
                return Core.cardHolder(World, Scanner);
            }
            if (User instanceof Scp428Instance Body) {
                Scp428Record Entry = Body.record();
                return Entry != null && NpcDoors.pickCard(World, Scanner, Entry.carried()).isPresent() ? Body : null;
            }
            return null;
        }

        private void followDevice(Mob User, ServerLevel World) {
            BlockPos Target = Device;
            if (Target == null) {
                return;
            }
            DeviceTicks++;
            long Now = World.getGameTime();
            BlockState State = World.getBlockState(Target);
            Kind Sort = kindOf(State);
            if (Sort == Kind.None || DeviceTicks > GiveUp || DeviceDoor != null && isOpen(World, DeviceDoor)) {
                remember(Target, Sort, Now);
                Device = null;
                DeviceDoor = null;
                return;
            }
            if (inReach(User, Target)) {
                User.getNavigation().stop();
                boolean Act = switch (Sort) {
                    case Lever -> DeviceDoor == null || !isOpen(World, DeviceDoor);
                    case Button -> !isPressed(State);
                    default -> isClosedPassage(State);
                };
                boolean Done = Act && operate(World, User, Target);
                remember(Target, Sort, Now);
                if (DeviceDoor != null) {
                    Doors.put(DeviceDoor.Origin(), Now + (DeviceDoor.Hand() ? HandMemory : DoorMemory));
                    WaitDoor = DeviceDoor;
                    WaitDevice = Target.immutable();
                    WaitPulse = Sort == Kind.Button;
                    Waiting = OpenWait;
                }
                if (Done) {
                    Operations++;
                    LastUsed = Target.immutable();
                    if (Sort == Kind.Button || Sort == Kind.Lever) {
                        BoostLeft = Boost;
                    } else {
                        Passages.add(new Opened(Target.immutable(), Now));
                    }
                }
                Device = null;
                DeviceDoor = null;
                RepathReady = 0;
                return;
            }
            if (DeviceTicks % RepathTicks == 1 || !User.getNavigation().isInProgress()) {
                Path Route = User.getNavigation().createPath(Target, 1);
                if (Route == null) {
                    remember(Target, Sort, Now);
                    Device = null;
                    DeviceDoor = null;
                    return;
                }
                User.getNavigation().moveTo(Route, 1.0);
            }
        }

        private void remember(BlockPos Pos, Kind Sort, long Now) {
            int Span = switch (Sort) {
                case Button -> ButtonMemory;
                case Lever -> LeverMemory;
                case Scanner -> ScannerMemory;
                default -> HandMemory;
            };
            Tried.put(Pos.immutable(), Now + Span);
        }
    }

    private record Opened(BlockPos Pos, long At) {
    }
}
