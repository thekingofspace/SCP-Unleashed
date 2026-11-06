package net.rai.scp_unleashed.scp4845.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.PropsRegistry;
import net.rai.scp_unleashed.props.SeatBlock;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;
import net.rai.scp_unleashed.scp4845.Scp4845Restaurant;
import net.rai.scp_unleashed.scp4845.Scp4845Site;
import net.rai.scp_unleashed.scp4845.Scp4845SiteCondition;
import net.rai.scp_unleashed.scp4845.Scp4845Staffing;
import net.rai.scp_unleashed.scp4845.Scp4845State;
import net.rai.scp_unleashed.scp4845.block.Scp4845FacadeBlock;
import net.rai.scp_unleashed.scp4845.item.Scp4845MenuItem;
import net.rai.scp_unleashed.scp4845.order.Scp4845OrderMenu;
import net.rai.scp_unleashed.scp4845.order.Scp4845Orders;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Brain;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Employee;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Task;
import net.rai.scp_unleashed.space.SpaceAir;
import net.rai.scp_unleashed.space.SpaceRegistry;
import net.rai.scp_unleashed.space.SpaceVacuum;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp4845GameTests {
    public static final String Arena = "scp465_arena";
    public static final int TestX = 40016;
    public static final int TestZ = 40016;
    public static final int LoadRadius = 48;
    public static final double FastShift = 0.08;
    public static final int ShiftTicks = 600;
    public static final int DeskTicks = 300;
    public static final int SeatSettle = 10;
    public static final int SeatWatchTicks = 200;
    public static final BlockPos DiningLocal = new BlockPos(17, 1, 19);
    public static final BlockPos LotLocal = new BlockPos(17, 1, 4);
    public static final BlockPos FloorLocal = new BlockPos(17, 0, 18);
    public static final ResourceLocation OtherDocument = Scp4845Registry.location("scp_3008");

    private Scp4845GameTests() {
    }

    private static ServerLevel moon(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        ServerLevel Moon = Server.getLevel(SpaceRegistry.MoonLevel);
        if (Moon == null) {
            Scp4845Restaurant.standIn(Level.NETHER);
            Moon = Server.getLevel(Level.NETHER);
        }
        if (Moon == null) {
            throw new IllegalStateException("neither the moon nor its stand-in is loaded");
        }
        return Moon;
    }

    private static boolean realMoon(GameTestHelper Helper) {
        return Helper.getLevel().getServer().getLevel(SpaceRegistry.MoonLevel) != null;
    }

    private static void fresh(GameTestHelper Helper) {
        Scp4845Brain.DurationScale = 1.0;
        if (!Scp4845Restaurant.standingIn()) {
            return;
        }
        ServerLevel Moon = Helper.getLevel().getServer().getLevel(Scp4845Restaurant.moonKey());
        if (Moon == null) {
            return;
        }
        for (ServerPlayer Left : new ArrayList<>(Moon.players())) {
            if (Left instanceof Scp4845TestPlayer Guest) {
                leave(Guest);
            }
        }
    }

    private static Scp4845Site site(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        fresh(Helper);
        ServerLevel Moon = moon(Helper);
        Scp4845Site Existing = Scp4845Restaurant.current(Server);
        if (Existing != null && Existing.Dimension == Moon.dimension() && Existing.Centre.getX() == TestX && Existing.Centre.getZ() == TestZ) {
            return Existing;
        }
        for (int ChunkX = (TestX - LoadRadius) >> 4; ChunkX <= (TestX + LoadRadius) >> 4; ChunkX++) {
            for (int ChunkZ = (TestZ - LoadRadius) >> 4; ChunkZ <= (TestZ + LoadRadius) >> 4; ChunkZ++) {
                Moon.setChunkForced(ChunkX, ChunkZ, true);
                Moon.getChunk(ChunkX, ChunkZ);
            }
        }
        Scp4845State.get(Server).forget();
        Scp4845Restaurant.forget();
        int Ground = Scp4845Restaurant.ground(Moon, TestX, TestZ);
        return Scp4845Restaurant.place(Moon, new BlockPos(TestX, Ground, TestZ), Direction.NORTH);
    }

    private static Scp4845TestPlayer person(GameTestHelper Helper, ServerLevel World, Vec3 Spot, String Name) {
        Scp4845TestPlayer User = new Scp4845TestPlayer(World, Name);
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void leave(Scp4845TestPlayer User) {
        User.closeContainer();
        if (!User.isRemoved()) {
            User.serverLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static List<Scp4845Employee> crew(ServerLevel Moon, Scp4845Site Site, byte Role) {
        return Scp4845Staffing.crew(Moon, Site).stream().filter(Worker -> Worker.role() == Role).toList();
    }

    private static boolean mentions(Component Line, String Key) {
        if (Line.getContents() instanceof TranslatableContents Contents) {
            if (Contents.getKey().equals(Key)) {
                return true;
            }
            for (Object Argument : Contents.getArgs()) {
                if (Argument instanceof Component Inner && mentions(Inner, Key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4845_lead")
    public static void leadPointsToTheMoon(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        AnomalyDocument Document = AnomalyDocuments.get(World, Scp4845Registry.DocumentId);
        Helper.assertTrue(Document != null, "the SCP-4845 document did not load");
        Scp4845State Data = Scp4845State.get(Server);
        boolean WasBuilt = Data.built();
        BlockPos KeptOrigin = Data.origin();
        Rotation KeptTurn = Data.turn();
        ResourceKey<Level> KeptDimension = Data.dimension();
        Data.forget();
        Scp4845Restaurant.forget();
        Scp4845TestPlayer Reader = person(Helper, World, Helper.absoluteVec(new Vec3(4.5, 2.0, 4.5)), "Reader");
        try {
            Scp4845SiteCondition Condition = new Scp4845SiteCondition(240, 520, 32, 64);
            Investigation Case = new Investigation(Scp4845Registry.DocumentId, World.getGameTime());
            List<Component> Notes = Condition.begin(Reader, Case);
            Helper.assertTrue(Notes.size() >= 3, "the field notes are too short");
            Helper.assertTrue(Notes.stream().anyMatch(Line -> mentions(Line, "archive.scp_unleashed.dimension.scp_unleashed.moon")), "the lead does not point at the Moon");
            BlockPos Planned = Data.planned();
            Helper.assertTrue(Planned != null, "no site was planned");
            double Distance = Math.hypot(Planned.getX() - Reader.getX(), Planned.getZ() - Reader.getZ());
            Helper.assertTrue(Distance >= 200 && Distance <= 1200, "the planned site is " + (int) Distance + " blocks away");
            Helper.assertTrue(Case.getState().getInt("X") == Planned.getX() && Case.getState().getInt("Z") == Planned.getZ(), "the case does not hold the planned site");
            Helper.assertTrue(Condition.tick(Reader, Case).isEmpty(), "the restaurant tried to appear on Earth");
            Scp4845TestPlayer Second = person(Helper, World, Helper.absoluteVec(new Vec3(6.5, 2.0, 6.5)), "Second");
            Investigation Other = new Investigation(Scp4845Registry.DocumentId, World.getGameTime());
            Condition.begin(Second, Other);
            Helper.assertTrue(Other.getState().getInt("X") == Planned.getX() && Other.getState().getInt("Z") == Planned.getZ(), "two readers were sent to different sites");
            leave(Second);
            Helper.assertTrue(AnomalyRealm.exempted(Scp4845Registry.DocumentId, SpaceRegistry.MoonLevel), "SCP-4845 is not allowed on the Moon");
            Helper.assertFalse(AnomalyRealm.exempted(OtherDocument, SpaceRegistry.MoonLevel), "the Moon exemption leaked to another document");
            Helper.assertTrue(AnomalyRealm.allows(World, OtherDocument), "the exemption closed the overworld");
        } finally {
            leave(Reader);
            Data.forget();
            if (WasBuilt && KeptOrigin != null) {
                Data.record(KeptDimension, KeptOrigin, KeptTurn);
            }
            Scp4845Restaurant.forget();
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4845_unique")
    public static void onlyOneRestaurant(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        AnomalyDocument Document = AnomalyDocuments.get(Moon, Scp4845Registry.DocumentId);
        Scp4845TestPlayer Reader = person(Helper, Moon, Vec3.atBottomCenterOf(Site.Doorway), "Visitor");
        try {
            BlockState Built = Moon.getBlockState(Site.Entrance.getFirst());
            Helper.assertTrue(Built.is(Scp4845Registry.Entrance.get()), "no entrance where the restaurant was recorded, found " + Built);
            BlockPos Elsewhere = Site.Centre.offset(300, 0, 300);
            boolean Spawned = Scp4845Restaurant.spawn(new AnomalySpawners.Discovery(Moon, Elsewhere, Reader, Scp4845Registry.DocumentId, Document, false));
            Helper.assertTrue(Spawned, "a second discovery was refused instead of resolving to the existing restaurant");
            Scp4845Site After = Scp4845Restaurant.current(Moon.getServer());
            Helper.assertTrue(After != null && After.Origin.equals(Site.Origin), "a second restaurant replaced the first");
            Helper.assertFalse(Moon.hasChunk(Elsewhere.getX() >> 4, Elsewhere.getZ() >> 4) && Moon.getBlockState(Elsewhere.above(3)).is(Scp4845Registry.Facade.get()),
                    "a second restaurant was built");
            Scp4845SiteCondition Condition = new Scp4845SiteCondition(240, 520, 32, 64);
            Investigation Case = new Investigation(Scp4845Registry.DocumentId, Moon.getGameTime());
            List<Component> Notes = Condition.begin(Reader, Case);
            Helper.assertTrue(Notes.stream().anyMatch(Line -> mentions(Line, "archive.scp_unleashed.notes.scp4845.known")), "a new reader is not pointed at the existing restaurant");
            Helper.assertTrue(Condition.tick(Reader, Case).map(Found -> Found.Pos().equals(Site.Entrance.getFirst())).orElse(false),
                    "the lead does not resolve at the existing restaurant");
        } finally {
            leave(Reader);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4845_sealed")
    public static void restaurantIsUnbreakable(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        Scp4845TestPlayer Vandal = person(Helper, Moon, Vec3.atBottomCenterOf(Site.world(DiningLocal)), "Vandal");
        try {
            BlockPos Wall = Site.Entrance.getFirst().above(4);
            BlockState WallState = Moon.getBlockState(Wall);
            Helper.assertTrue(WallState.getBlock() instanceof Scp4845FacadeBlock, "expected the facade above the entrance, found " + WallState);
            Helper.assertFalse(Vandal.gameMode.destroyBlock(Wall), "a survival player broke the facade");
            Helper.assertTrue(Moon.getBlockState(Wall).equals(WallState), "the facade changed");
            BlockPos Counter = Site.Counters.getFirst();
            BlockState CounterState = Moon.getBlockState(Counter);
            Helper.assertFalse(Vandal.gameMode.destroyBlock(Counter), "a survival player broke the counter");
            Helper.assertTrue(Moon.getBlockState(Counter).equals(CounterState), "the counter changed");
            BlockPos Floor = Site.world(FloorLocal);
            Vandal.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 4));
            Vandal.gameMode.useItemOn(Vandal, Moon, Vandal.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false));
            Helper.assertTrue(Moon.getBlockState(Floor.above()).isAir(), "a block was placed inside the restaurant");
            Vec3 Blast = Vec3.atCenterOf(Site.world(DiningLocal));
            Moon.explode(null, Blast.x, Blast.y, Blast.z, 4.0f, Level.ExplosionInteraction.TNT);
            Helper.assertTrue(Moon.getBlockState(Counter).equals(CounterState), "an explosion broke the counter");
            Helper.assertTrue(Moon.getBlockState(Floor).getBlock() instanceof Scp4845FacadeBlock, "an explosion broke the floor");
            Helper.assertTrue(Moon.getBlockState(Wall).equals(WallState), "an explosion broke the facade");
        } finally {
            leave(Vandal);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4845_air")
    public static void airInsideOnly(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        Scp4845TestPlayer Diner = person(Helper, Moon, Vec3.atBottomCenterOf(Site.world(DiningLocal)), "Diner");
        try {
            Helper.assertTrue(SpaceAir.sheltered(Diner), "the dining room is not breathable");
            Vec3 Kitchen = Vec3.atBottomCenterOf(Site.KitchenSpawns.getFirst());
            Diner.teleportTo(Kitchen.x, Kitchen.y, Kitchen.z);
            Helper.assertTrue(SpaceAir.sheltered(Diner), "the kitchen is not breathable");
            Vec3 Office = Site.Manager.centre();
            Diner.teleportTo(Office.x, Office.y, Office.z);
            Helper.assertTrue(SpaceAir.sheltered(Diner), "the office is not breathable");
            if (realMoon(Helper)) {
                Diner.teleportTo(Kitchen.x, Kitchen.y, Kitchen.z);
                for (int Tick = 0; Tick < 120; Tick++) {
                    SpaceVacuum.tickPlayer(Diner);
                }
                Helper.assertTrue(SpaceVacuum.exposure(Diner) == 0, "a player without a helmet was exposed indoors");
            }
            Vec3 Lot = Vec3.atBottomCenterOf(Site.world(LotLocal));
            Diner.teleportTo(Lot.x, Lot.y, Lot.z);
            Helper.assertFalse(SpaceAir.sheltered(Diner), "the parking lot is breathable");
            Vec3 Roof = Vec3.atBottomCenterOf(Site.world(DiningLocal)).add(0.0, 6.0, 0.0);
            Diner.teleportTo(Roof.x, Roof.y, Roof.z);
            Helper.assertFalse(SpaceAir.sheltered(Diner), "the roof is breathable");
            if (realMoon(Helper)) {
                for (int Tick = 0; Tick < 3; Tick++) {
                    SpaceVacuum.tickPlayer(Diner);
                }
                Helper.assertTrue(SpaceVacuum.exposure(Diner) > 0, "the roof did not expose the player");
            }
        } finally {
            leave(Diner);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4845_counter")
    public static void counterOpensTheMenu(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        BlockPos Register = Site.CounterWorker.Pos().relative(Site.CounterWorker.Facing());
        Vec3 Front = Vec3.atBottomCenterOf(Register.relative(Site.CounterWorker.Facing()));
        staffed(Helper, Site, Moon).thenExecute(() -> {
            Scp4845TestPlayer Customer = person(Helper, Moon, Front, "Customer");
            try {
                Helper.assertTrue(Moon.getBlockState(Register).is(Scp4845Registry.Register.get()), "no register in front of the cashier, found " + Moon.getBlockState(Register));
                Helper.assertTrue(Scp4845Orders.openAt(Customer, Register), "the register refused");
                Helper.assertTrue(Customer.containerMenu instanceof Scp4845OrderMenu, "the register did not open the order menu");
                Customer.closeContainer();
                List<Scp4845Employee> Cashiers = crew(Moon, Site, Scp4845Employee.RoleCounter);
                Helper.assertTrue(Cashiers.size() == 1, "expected one cashier, found " + Cashiers.size());
                Helper.assertTrue(Scp4845Orders.interact(Cashiers.getFirst(), Customer), "talking to the cashier did nothing");
                Helper.assertTrue(Customer.containerMenu instanceof Scp4845OrderMenu, "the cashier did not open the order menu");
            } finally {
                leave(Customer);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4845_order")
    public static void orderingCostsEmeralds(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        BlockPos Register = Site.CounterWorker.Pos().relative(Site.CounterWorker.Facing());
        Vec3 Front = Vec3.atBottomCenterOf(Register.relative(Site.CounterWorker.Facing()));
        Scp4845TestPlayer[] Customer = new Scp4845TestPlayer[1];
        Scp4845OrderMenu[] Menu = new Scp4845OrderMenu[1];
        staffed(Helper, Site, Moon).thenExecute(() -> {
            Customer[0] = person(Helper, Moon, Front, "Hungry");
            Customer[0].getInventory().add(new ItemStack(Items.EMERALD, 3));
            Scp4845Orders.openAt(Customer[0], Register);
            Helper.assertTrue(Customer[0].containerMenu instanceof Scp4845OrderMenu, "the order menu did not open");
            Menu[0] = (Scp4845OrderMenu) Customer[0].containerMenu;
            Helper.assertTrue(Menu[0].clickMenuButton(Customer[0], Scp4845OrderMenu.button(Scp4845MenuItem.Burrito, 1)), "a paid order was refused");
            Helper.assertTrue(Scp4845Orders.emeralds(Customer[0].getInventory()) == 1, "the burrito did not cost two emeralds");
            Helper.assertFalse(Menu[0].clickMenuButton(Customer[0], Scp4845OrderMenu.button(Scp4845MenuItem.Burrito, 1)), "an unaffordable order went through");
            Helper.assertTrue(Scp4845Orders.emeralds(Customer[0].getInventory()) == 1, "a refused order still took emeralds");
            Helper.assertFalse(Menu[0].clickMenuButton(Customer[0], 999), "a bad button id was accepted");
        }).thenIdle(Scp4845Orders.DeliverDelay + 5).thenExecute(() -> {
            try {
                Helper.assertTrue(Customer[0].getInventory().countItem(Scp4845Registry.food(Scp4845MenuItem.Burrito)) == 1, "the burrito was not handed over");
                Helper.assertTrue(Menu[0].clickMenuButton(Customer[0], Scp4845OrderMenu.button(Scp4845MenuItem.SparkSauce, 1)), "a one emerald order was refused");
                Helper.assertTrue(Scp4845Orders.emeralds(Customer[0].getInventory()) == 0, "the sauce did not cost one emerald");
            } finally {
                leave(Customer[0]);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = ShiftTicks + 300, batch = "scp4845_shift")
    public static void staffSwitchTasksAndStayInside(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        Set<Scp4845Employee> Strayed = new HashSet<>();
        Scp4845Brain.DurationScale = FastShift;
        staffed(Helper, Site, Moon).thenExecuteFor(ShiftTicks, () -> {
            for (Scp4845Employee Worker : crew(Moon, Site, Scp4845Employee.RoleKitchen)) {
                if (!Site.Inside.inflate(1.0, 2.0, 1.0).contains(Worker.position())) {
                    Strayed.add(Worker);
                }
            }
        }).thenExecute(() -> {
            Scp4845Brain.DurationScale = 1.0;
            List<Scp4845Employee> Kitchen = crew(Moon, Site, Scp4845Employee.RoleKitchen);
            Helper.assertTrue(Kitchen.size() == Scp4845Staffing.KitchenCrew, "the kitchen crew is " + Kitchen.size() + ": " + Kitchen.stream()
                    .map(Worker -> Worker.speakerName().getString() + "@" + Worker.blockPosition().toShortString() + " age " + Worker.tickCount)
                    .toList());
            Set<Scp4845Task> Seen = EnumSet.noneOf(Scp4845Task.class);
            for (Scp4845Employee Worker : Kitchen) {
                Set<Scp4845Task> Own = EnumSet.noneOf(Scp4845Task.class);
                Own.addAll(Worker.brain().history());
                Helper.assertTrue(Own.size() >= 2, Worker.speakerName().getString() + " only did " + Own);
                Seen.addAll(Own);
            }
            Helper.assertTrue(Seen.size() >= 4, "the crew only did " + Seen);
            Helper.assertTrue(Strayed.isEmpty(), "staff left the restaurant: " + Strayed.size());
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = DeskTicks + 300, batch = "scp4845_manager")
    public static void managerStaysAtTheDesk(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        Scp4845Brain.DurationScale = 0.1;
        staffed(Helper, Site, Moon).thenIdle(DeskTicks).thenExecute(() -> {
            Scp4845Brain.DurationScale = 1.0;
            List<Scp4845Employee> Managers = crew(Moon, Site, Scp4845Employee.RoleManager);
            Helper.assertTrue(Managers.size() == 1, "expected one manager, found " + Managers.size());
            Scp4845Employee Kyle = Managers.getFirst();
            double Distance = Kyle.position().distanceTo(Site.Manager.centre());
            Helper.assertTrue(Distance < 0.75, "the manager wandered " + Distance + " blocks from his chair");
            Helper.assertTrue(Kyle.task().Seated, "the manager is not seated, task " + Kyle.task());
            Set<Scp4845Task> Done = EnumSet.noneOf(Scp4845Task.class);
            Done.addAll(Kyle.brain().history());
            Helper.assertTrue(Done.contains(Scp4845Task.Type), "the manager never typed");
            Helper.assertTrue(Done.size() >= 2, "the manager never took a moment away from the keys: " + Done);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp4845_harm")
    public static void staffTakeNoLastingHarm(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        staffed(Helper, Site, Moon).thenExecute(() -> {
            Scp4845TestPlayer Rude = person(Helper, Moon, Vec3.atBottomCenterOf(Site.world(DiningLocal)), "Rude");
            try {
                List<Scp4845Employee> Kitchen = crew(Moon, Site, Scp4845Employee.RoleKitchen);
                Helper.assertTrue(Kitchen.size() == Scp4845Staffing.KitchenCrew, "the kitchen crew is " + Kitchen.size() + " before the test");
                Scp4845Employee Worker = Kitchen.getFirst();
                Helper.assertFalse(Worker.hurt(Moon.damageSources().playerAttack(Rude), 10.0f), "a punch hurt an employee");
                Helper.assertFalse(Worker.hurt(Moon.damageSources().generic(), 1000.0f), "generic damage hurt an employee");
                Helper.assertTrue(Worker.getHealth() >= Worker.getMaxHealth(), "an employee lost health");
                Helper.assertFalse(SpaceVacuum.susceptible(Worker), "employees should shrug off vacuum");
                Worker.kill();
                Helper.assertFalse(Worker.isAlive(), "kill did not work on an employee");
                int Hired = Scp4845Staffing.restaff(Moon, Site);
                Helper.assertTrue(Hired == 1, "expected one replacement, hired " + Hired);
                Helper.assertTrue(crew(Moon, Site, Scp4845Employee.RoleKitchen).size() == Scp4845Staffing.KitchenCrew, "the kitchen is short-staffed after restaffing");
            } finally {
                leave(Rude);
            }
        }).thenIdle(30).thenExecute(() -> {
            Helper.assertTrue(crew(Moon, Site, Scp4845Employee.RoleKitchen).size() == Scp4845Staffing.KitchenCrew, "a killed employee came back next to its replacement");
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4845_seat_holds")
    public static void seatsHoldRidersAtSeatHeight(GameTestHelper Helper) {
        fresh(Helper);
        ServerLevel World = Helper.getLevel();
        List<Block> Chairs = List.of(Scp4845Registry.Booth.get(), Scp4845Registry.Chair.get(), Scp4845Registry.OfficeChair.get());
        List<PropSeatEntity> Seats = new ArrayList<>();
        List<ArmorStand> Riders = new ArrayList<>();
        for (int Index = 0; Index < Chairs.size(); Index++) {
            BlockPos Pos = Helper.absolutePos(new BlockPos(2 + Index * 2, 1, 2));
            World.setBlockAndUpdate(Pos, Chairs.get(Index).defaultBlockState());
            ArmorStand Rider = EntityType.ARMOR_STAND.create(World);
            Helper.assertTrue(Rider != null, "no armor stand");
            Seats.add(mount(World, Pos, Rider));
            Riders.add(Rider);
        }
        BlockPos TablePos = Helper.absolutePos(new BlockPos(2, 1, 5));
        World.setBlockAndUpdate(TablePos, Scp4845Registry.Table.get().defaultBlockState());
        ArmorStand Diner = EntityType.ARMOR_STAND.create(World);
        Helper.assertTrue(Diner != null, "no armor stand");
        PropSeatEntity Table = mount(World, TablePos, Diner);
        Helper.runAfterDelay(SeatSettle, () -> {
            for (int Index = 0; Index < Seats.size(); Index++) {
                PropSeatEntity Seat = Seats.get(Index);
                String Name = Chairs.get(Index).getDescriptionId();
                Helper.assertTrue(Seat.isAlive() && Seat.isVehicle(), "the seat on " + Name + " threw its rider off");
                double Height = Seat.getPassengerRidingPosition(Riders.get(Index)).y - Seat.blockPosition().getY();
                Helper.assertTrue(Math.abs(Height - Scp4845Registry.SeatTop / 16.0) < 1.0E-4, Name + " seats its rider at " + Height * 16.0 + " px");
            }
            Helper.assertFalse(Table.isAlive(), "a seat on the table stayed");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp4845_seat_player")
    public static void guestsSitInTheRestaurant(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        BlockPos Booth = diningSeat(Moon, Site);
        Helper.startSequence().thenExecute(() -> {
            Helper.assertTrue(Booth != null, "no dining booth or chair inside the restaurant");
            Scp4845TestPlayer Guest = person(Helper, Moon, Vec3.atBottomCenterOf(Booth.above()), "Guest");
            Scp4845TestPlayer Second = person(Helper, Moon, Vec3.atBottomCenterOf(Booth.above()), "Second");
            try {
                sitOn(Guest, Moon, Booth);
                Helper.assertTrue(seatedOn(Guest, Booth), "a guest could not sit on " + Moon.getBlockState(Booth) + " inside the restaurant");
                double Height = Guest.getVehicle().getPassengerRidingPosition(Guest).y - Booth.getY();
                Helper.assertTrue(Math.abs(Height - Scp4845Registry.SeatTop / 16.0) < 1.0E-4, "the guest sits at " + Height * 16.0 + " px");
                sitOn(Second, Moon, Booth);
                Helper.assertFalse(Second.isPassenger(), "two guests sat in the same seat");
                Guest.stopRiding();
                Helper.assertFalse(Guest.isPassenger(), "the guest could not stand up");
            } finally {
                leave(Guest);
                leave(Second);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = DeskTicks + 600, batch = "scp4845_seat_staff")
    public static void managerAndGuestsShareTheChair(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        BlockPos Chair = Site.Manager.Pos();
        List<Scp4845TestPlayer> Guests = new ArrayList<>();
        List<Scp4845Employee> Bosses = new ArrayList<>();
        Set<String> Problems = new HashSet<>();
        Scp4845Brain.DurationScale = 0.1;
        staffed(Helper, Site, Moon).thenExecute(() -> {
            BlockState Held = Moon.getBlockState(Chair);
            Helper.assertTrue(Held.getBlock() instanceof SeatBlock Host && Host.seats(Held), "the manager's chair is not a seat: " + Held);
            for (Scp4845Site.Spot Option : Site.BreakSeats) {
                BlockState Break = Moon.getBlockState(Option.Pos());
                Helper.assertTrue(Break.getBlock() instanceof SeatBlock Host && Host.seats(Break), "a break seat is not a seat: " + Break);
            }
            List<Scp4845Employee> Managers = crew(Moon, Site, Scp4845Employee.RoleManager);
            Helper.assertTrue(Managers.size() == 1, "expected one manager, found " + Managers.size());
            Bosses.add(Managers.getFirst());
            Guests.add(person(Helper, Moon, Site.Manager.centre(), "Guest"));
        }).thenWaitUntil(() -> {
            if (!Bosses.getFirst().brain().sitting()) {
                throw new GameTestAssertException("waiting for the manager to sit down");
            }
        }).thenExecute(() -> {
            Scp4845TestPlayer Guest = Guests.getFirst();
            sitOn(Guest, Moon, Chair);
            Helper.assertFalse(Guest.isPassenger(), "a guest sat down on the manager");
            Bosses.getFirst().brain().reset();
            sitOn(Guest, Moon, Chair);
            Helper.assertTrue(seatedOn(Guest, Chair), "the guest could not take the free office chair");
        }).thenExecuteFor(SeatWatchTicks, () -> {
            if (Bosses.getFirst().brain().sitting()) {
                Problems.add("the manager sat down on the guest");
            }
            if (!seatedOn(Guests.getFirst(), Chair)) {
                Problems.add("the guest lost the office chair");
            }
        }).thenExecute(() -> {
            Helper.assertTrue(Problems.isEmpty(), String.join(", ", Problems));
            Guests.getFirst().stopRiding();
        }).thenWaitUntil(() -> {
            if (!Bosses.getFirst().brain().sitting()) {
                throw new GameTestAssertException("waiting for the manager to take his chair back");
            }
        }).thenExecute(() -> {
            Scp4845Brain.DurationScale = 1.0;
            leave(Guests.getFirst());
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = SeatWatchTicks + 400, batch = "scp4845_seat_break")
    public static void kitchenSkipsTakenBreakSeats(GameTestHelper Helper) {
        Scp4845Site Site = site(Helper);
        ServerLevel Moon = moon(Helper);
        List<Scp4845TestPlayer> Guests = new ArrayList<>();
        Set<String> Problems = new HashSet<>();
        Scp4845Brain.DurationScale = FastShift;
        staffed(Helper, Site, Moon).thenExecute(() -> {
            for (Scp4845Employee Worker : crew(Moon, Site, Scp4845Employee.RoleKitchen)) {
                if (Worker.brain().sitting() || Worker.task() == Scp4845Task.Break) {
                    Worker.brain().reset();
                    Worker.setTask(Scp4845Task.None);
                }
            }
            for (Scp4845Site.Spot Option : Site.BreakSeats) {
                Scp4845TestPlayer Guest = person(Helper, Moon, Option.centre(), "Guest" + Guests.size());
                Guests.add(Guest);
                sitOn(Guest, Moon, Option.Pos());
                Helper.assertTrue(seatedOn(Guest, Option.Pos()), "a guest could not take the break seat at " + Option.Pos().toShortString());
            }
        }).thenExecuteFor(SeatWatchTicks, () -> {
            for (Scp4845Employee Worker : crew(Moon, Site, Scp4845Employee.RoleKitchen)) {
                if (Worker.brain().sitting() || Worker.task() == Scp4845Task.Break) {
                    Problems.add(Worker.speakerName().getString() + " took a break seat a guest was in");
                }
            }
        }).thenExecute(() -> {
            Scp4845Brain.DurationScale = 1.0;
            Guests.forEach(Scp4845GameTests::leave);
            Helper.assertTrue(Problems.isEmpty(), String.join(", ", Problems));
        }).thenSucceed();
    }

    private static PropSeatEntity mount(ServerLevel World, BlockPos Pos, Entity Rider) {
        PropSeatEntity Seat = new PropSeatEntity(PropsRegistry.Seat.get(), World);
        Seat.moveTo(Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5, 0.0f, 0.0f);
        World.addFreshEntity(Seat);
        Rider.moveTo(Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, 0.0f, 0.0f);
        World.addFreshEntity(Rider);
        Rider.startRiding(Seat, true);
        return Seat;
    }

    private static void sitOn(Scp4845TestPlayer User, ServerLevel World, BlockPos Pos) {
        User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        User.gameMode.useItemOn(User, World, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false));
    }

    private static boolean seatedOn(Entity Rider, BlockPos Pos) {
        return Rider.getVehicle() instanceof PropSeatEntity Seat && Seat.blockPosition().equals(Pos);
    }

    @Nullable
    private static BlockPos diningSeat(ServerLevel Moon, Scp4845Site Site) {
        Set<BlockPos> Staff = new HashSet<>();
        Staff.add(Site.Manager.Pos());
        Site.BreakSeats.forEach(Option -> Staff.add(Option.Pos()));
        AABB Box = Site.Inside;
        BlockPos Low = BlockPos.containing(Box.minX, Box.minY, Box.minZ);
        BlockPos High = BlockPos.containing(Box.maxX, Box.maxY, Box.maxZ);
        BlockPos Chair = null;
        for (BlockPos Pos : BlockPos.betweenClosed(Low, High)) {
            BlockState Found = Moon.getBlockState(Pos);
            if (Staff.contains(Pos)) {
                continue;
            }
            if (Found.is(Scp4845Registry.Booth.get())) {
                return Pos.immutable();
            }
            if (Chair == null && Found.is(Scp4845Registry.Chair.get())) {
                Chair = Pos.immutable();
            }
        }
        return Chair;
    }

    private static GameTestSequence staffed(GameTestHelper Helper, Scp4845Site Site, ServerLevel Moon) {
        return Helper.startSequence().thenWaitUntil(() -> {
            if (!Scp4845Staffing.complete(Moon, Site)) {
                Scp4845Staffing.restaff(Moon, Site);
                throw new GameTestAssertException("waiting for the SCP-4845 crew");
            }
        });
    }
}
