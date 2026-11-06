package net.rai.scp_unleashed.z0002.gametest;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.z0002.Z0002BarrierBlock;
import net.rai.scp_unleashed.z0002.Z0002Domain;
import net.rai.scp_unleashed.z0002.Z0002Domains;
import net.rai.scp_unleashed.z0002.Z0002FingerItem;
import net.rai.scp_unleashed.z0002.Z0002Phase;
import net.rai.scp_unleashed.z0002.Z0002Registry;
import net.rai.scp_unleashed.z0002.Z0002Shrine;
import net.rai.scp_unleashed.z0002.Z0002Spot;
import net.rai.scp_unleashed.z0002.Z0002World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Z0002GameTests {
    public static final String Arena = "z0002_arena";
    public static final int Short = 400;
    public static final double Exact = 1.0E-6;

    private Z0002GameTests() {
    }

    private static Z0002TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Z0002TestPlayer User = new Z0002TestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static Z0002TestPlayer holder(GameTestHelper Helper, String Name) {
        Z0002TestPlayer User = player(Helper, 8.5, 8.5, Name);
        User.setItemInHand(InteractionHand.MAIN_HAND, Z0002FingerItem.create());
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer... Users) {
        for (ServerPlayer User : Users) {
            if (!User.isRemoved()) {
                Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private static Z0002Domain open(GameTestHelper Helper, ServerPlayer Holder, int Duration) {
        Z0002Domain Domain = start(Helper, Holder, Duration);
        Z0002Domains.finishBuilding(Holder.server, Domain);
        Helper.assertTrue(Domain.Phase == Z0002Phase.Active, "The domain did not finish building");
        return Domain;
    }

    private static Z0002Domain start(GameTestHelper Helper, ServerPlayer Holder, int Duration) {
        Z0002Domains.Outcome Result = Z0002Domains.open(Holder, Holder.getMainHandItem(), false, Duration);
        Helper.assertTrue(Result == Z0002Domains.Outcome.Opened, "The domain did not open: " + Result);
        Z0002Domain Domain = Z0002Domains.ownedBy(Holder.server, Holder.getUUID());
        Helper.assertTrue(Domain != null, "No domain is recorded for the holder");
        return Domain;
    }

    private static void whenDone(GameTestHelper Helper, Runnable Cleanup) {
        Helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo Info) {
            }

            @Override
            public void testPassed(GameTestInfo Info, GameTestRunner Runner) {
                Cleanup.run();
            }

            @Override
            public void testFailed(GameTestInfo Info, GameTestRunner Runner) {
                Cleanup.run();
            }

            @Override
            public void testAddedForRerun(GameTestInfo Old, GameTestInfo Fresh, GameTestRunner Runner) {
            }
        });
    }

    private static boolean ticking(ServerLevel World, Z0002Domain Domain) {
        for (ChunkPos Chunk : Domain.Region.chunks()) {
            if (!World.isPositionEntityTicking(Chunk.getMiddleBlockPosition(Domain.Region.floorY()))) {
                return false;
            }
        }
        return true;
    }

    private static void ready(LivingEntity Target) {
        Target.invulnerableTime = 0;
        Target.hurtTime = 0;
        Target.setHealth(Target.getMaxHealth());
    }

    private static void close(MinecraftServer Server, Z0002Domain Domain) {
        if (Domain != null && Domain.active()) {
            Z0002Domains.collapse(Server, Domain, false);
        }
    }

    private static String diagnose(ServerLevel World, ServerPlayer User, Vec3 Spot) {
        AABB Box = User.getDimensions(User.getPose()).makeBoundingBox(Spot).deflate(0.01);
        BlockPos Feet = BlockPos.containing(Spot);
        StringBuilder Found = new StringBuilder("feet " + World.getBlockState(Feet) + " head " + World.getBlockState(Feet.above()) + " blocks free " + World.noBlockCollision(User, Box)
                + " free " + World.noCollision(User, Box) + " entities");
        for (Entity Each : World.getEntities(User, Box.inflate(0.5))) {
            Found.append(" ").append(Each.getType().toShortString()).append(Each.canBeCollidedWith() ? "*" : "");
        }
        return Found.toString();
    }

    private static boolean same(Vec3 First, Vec3 Second) {
        return First.distanceTo(Second) < Exact;
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0002_takesWithinRange")
    public static void takesWithinRange(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0002TestPlayer Holder = holder(Helper, "z0002_range_holder");
        Z0002TestPlayer Near = player(Helper, 18.5, 8.5, "z0002_range_near");
        Z0002TestPlayer Edge = player(Helper, 38.5, 8.5, "z0002_range_edge");
        Z0002TestPlayer Far = player(Helper, 48.5, 8.5, "z0002_range_far");
        Z0002TestPlayer Watcher = player(Helper, 8.5, 14.5, "z0002_range_watcher");
        Watcher.setGameMode(GameType.SPECTATOR);
        Vec3 FarBefore = Far.position();
        Vec3 WatcherBefore = Watcher.position();
        Z0002Domain Domain = open(Helper, Holder, Short);
        try {
            Helper.assertTrue(Domain.Members.containsKey(Holder.getUUID()), "The holder was not taken");
            Helper.assertTrue(Domain.Members.containsKey(Near.getUUID()), "A player 10 blocks away was not taken");
            Helper.assertTrue(Domain.Members.containsKey(Edge.getUUID()), "A player 30 blocks away was not taken");
            Helper.assertFalse(Domain.Members.containsKey(Far.getUUID()), "A player 40 blocks away was taken");
            Helper.assertFalse(Domain.Members.containsKey(Watcher.getUUID()), "A spectator was taken");
            Helper.assertTrue(Domain.Region.holds(Near.position(), Near.getBbHeight()), "The near player is not inside the barrier");
            Helper.assertTrue(Domain.Region.holds(Edge.position(), Edge.getBbHeight()), "The player from outside the sphere was not pulled inside");
            Helper.assertTrue(same(Far.position(), FarBefore), "The far player was moved");
            Helper.assertTrue(same(Watcher.position(), WatcherBefore), "The spectator was moved");
            Helper.assertTrue(Domain.ShrineId != null && World.getEntity(Domain.ShrineId) instanceof Z0002Shrine, "No shrine stands in the domain");
            BlockPos Crown = Domain.Region.Centre().above(Domain.Region.Radius());
            Helper.assertTrue(Z0002BarrierBlock.is(World.getBlockState(Crown)), "The barrier does not close overhead");
        } finally {
            close(World.getServer(), Domain);
        }
        dismiss(Helper, Holder, Near, Edge, Far, Watcher);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0002_cannotLeaveEarly")
    public static void cannotLeaveEarly(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0002TestPlayer Holder = holder(Helper, "z0002_leave_holder");
        Z0002TestPlayer Other = player(Helper, 12.5, 8.5, "z0002_leave_other");
        Z0002Domain Domain = open(Helper, Holder, Short);
        Vec3 Away = Domain.Region.floorCentre().add(60.0, 0.0, 0.0);
        EntityTeleportEvent.TeleportCommand Command = NeoForge.EVENT_BUS.post(new EntityTeleportEvent.TeleportCommand(Other, Away.x, Away.y, Away.z));
        Helper.assertTrue(Command.isCanceled(), "A teleport command let a player out");
        EntityTeleportEvent.SpreadPlayersCommand Spread = NeoForge.EVENT_BUS.post(new EntityTeleportEvent.SpreadPlayersCommand(Other, Away.x, Away.y, Away.z));
        Helper.assertTrue(Spread.isCanceled(), "Spreadplayers let a player out");
        EntityTeleportEvent.ChorusFruit Chorus = NeoForge.EVENT_BUS.post(new EntityTeleportEvent.ChorusFruit(Other, Away.x, Away.y, Away.z));
        Helper.assertTrue(Chorus.isCanceled(), "Chorus fruit let a player out");
        ThrownEnderpearl Pearl = new ThrownEnderpearl(World, Other);
        EntityTeleportEvent.EnderPearl Thrown = NeoForge.EVENT_BUS.post(new EntityTeleportEvent.EnderPearl(Other, Away.x, Away.y, Away.z, Pearl, 0.0f,
                BlockHitResult.miss(Away, Direction.UP, BlockPos.containing(Away))));
        Pearl.discard();
        Helper.assertTrue(Thrown.isCanceled(), "An ender pearl let a player out");
        EntityTravelToDimensionEvent Travel = NeoForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(Other, Level.NETHER));
        Helper.assertTrue(Travel.isCanceled(), "A portal let a player out");
        BlockPos Wall = Domain.Region.Centre().offset(Domain.Region.Radius(), 1, 0);
        BlockEvent.BreakEvent Breaking = NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(World, Wall, World.getBlockState(Wall), Other));
        Helper.assertTrue(Breaking.isCanceled(), "The barrier could be broken");
        whenDone(Helper, () -> {
            close(World.getServer(), Domain);
            dismiss(Helper, Holder, Other);
        });
        Other.teleportTo(World, Away.x, Away.y, Away.z, 0.0f, 0.0f);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Domain.Region.holds(Other.position(), Other.getBbHeight()), "A player moved out by other means was not pulled back"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "z0002_slashHurtsOthers")
    public static void slashHurtsOthers(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        boolean Pvp = Server.isPvpAllowed();
        Z0002TestPlayer Holder = holder(Helper, "z0002_slash_holder");
        Z0002TestPlayer Victim = player(Helper, 8.5, 13.5, "z0002_slash_victim");
        Z0002Domain Domain = open(Helper, Holder, Short);
        Villager[] Bystander = new Villager[1];
        float[] Before = new float[3];
        whenDone(Helper, () -> {
            Server.setPvpAllowed(Pvp);
            close(Server, Domain);
            if (Bystander[0] != null) {
                Bystander[0].discard();
            }
            dismiss(Helper, Holder, Victim);
        });
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(Domain.Phase == Z0002Phase.Active, "The domain is not active yet");
            Helper.assertTrue(ticking(World, Domain), "The domain's chunks do not tick entities yet");
        }).thenExecute(() -> {
            Server.setPvpAllowed(true);
            Helper.assertTrue(Domain.Members.containsKey(Victim.getUUID()), "The other player was not taken");
            Vec3 Ahead = Victim.position().subtract(Holder.position()).normalize();
            Vec3 Between = Holder.position().add(Ahead.scale(3.0));
            Villager Made = EntityType.VILLAGER.create(World);
            Made.moveTo(Between.x, Between.y, Between.z, 0.0f, 0.0f);
            Made.setNoAi(true);
            World.addFreshEntity(Made);
            Bystander[0] = Made;
            ready(Holder);
            ready(Victim);
            ready(Made);
            Holder.lookAt(EntityAnchorArgument.Anchor.EYES, Victim.getBoundingBox().getCenter());
            Before[0] = Holder.getHealth();
            Before[1] = Victim.getHealth();
            Before[2] = Made.getHealth();
            Helper.assertTrue(Z0002Domains.slash(Holder, Holder.getMainHandItem(), Domain), "The holder could not slash inside the domain");
            Helper.assertTrue(Holder.getCooldowns().isOnCooldown(Z0002Registry.Finger.get()), "The slash left no cooldown");
            Helper.assertFalse(Z0002Domains.slash(Holder, Holder.getMainHandItem(), Domain), "A second slash ignored the cooldown");
        }).thenWaitUntil(() -> {
            Helper.assertTrue(Victim.getHealth() <= Before[1] - 10.0f, "The slash did not hurt the other player heavily: " + Before[1] + " -> " + Victim.getHealth());
            Helper.assertTrue(Bystander[0].isDeadOrDying() || Bystander[0].getHealth() <= Before[2] - 10.0f, "The slash did not hurt the villager in its path");
        }).thenExecute(() -> Helper.assertTrue(Holder.getHealth() == Before[0], "The slash hurt its holder")).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0002_collapseReturnsEveryone")
    public static void collapseReturnsEveryone(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0002TestPlayer Holder = holder(Helper, "z0002_return_holder");
        Holder.moveTo(Holder.getX(), Holder.getY(), Holder.getZ(), 37.0f, 12.0f);
        Z0002TestPlayer First = player(Helper, 14.2, 3.1, "z0002_return_first");
        First.moveTo(First.getX(), First.getY(), First.getZ(), -20.0f, -5.0f);
        Z0002TestPlayer Second = player(Helper, 33.5, 11.5, "z0002_return_second");
        List<Z0002TestPlayer> Everyone = List.of(Holder, First, Second);
        Map<UUID, Z0002Spot> Before = new HashMap<>();
        for (Z0002TestPlayer Each : Everyone) {
            Before.put(Each.getUUID(), Z0002Spot.of(Each));
        }
        Z0002Domain Domain = open(Helper, Holder, Short);
        Helper.assertFalse(same(First.position(), Before.get(First.getUUID()).Pos()), "The first player was not taken anywhere");
        Z0002Domains.collapse(World.getServer(), Domain, false);
        Helper.assertTrue(Domain.Closed, "The domain is still open");
        for (Z0002TestPlayer Each : Everyone) {
            Z0002Spot Spot = Before.get(Each.getUUID());
            Helper.assertTrue(same(Each.position(), Spot.Pos()), Each.getGameProfile().getName() + " was not returned exactly: " + Each.position() + " vs " + Spot.Pos() + " " + diagnose(World, Each, Spot.Pos()));
            Helper.assertTrue(Math.abs(Each.getYRot() - Spot.Yaw()) < 1.0E-3 && Math.abs(Each.getXRot() - Spot.Pitch()) < 1.0E-3, Each.getGameProfile().getName() + " faces the wrong way");
        }
        Helper.assertTrue(Domain.ShrineId == null || World.getEntity(Domain.ShrineId) == null, "The shrine outlived the domain");
        dismiss(Helper, Holder, First, Second);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0002_dormancyLastsADay")
    public static void dormancyLastsADay(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Z0002TestPlayer Holder = holder(Helper, "z0002_dormant_holder");
        Z0002TestPlayer Other = player(Helper, 8.5, 3.5, "z0002_dormant_other");
        Z0002Domain Domain = open(Helper, Holder, Short);
        Z0002Domains.collapse(Server, Domain, false);
        long Closed = Z0002Domains.now(Server);
        ItemStack Used = Holder.getMainHandItem();
        Helper.assertTrue(Z0002World.get(Server).dormantUntil(Holder.getUUID()) == Closed + Z0002Domains.DayTicks, "The holder is not dormant for exactly one day");
        Helper.assertTrue(Z0002Domains.itemUntil(Used) == Closed + Z0002Domains.DayTicks, "The finger is not dormant for exactly one day");
        Helper.assertTrue(Z0002Domains.dormant(Server, Holder, Used, Closed + Z0002Domains.DayTicks - 1), "Dormancy ends before a full day");
        Helper.assertFalse(Z0002Domains.dormant(Server, Holder, Used, Closed + Z0002Domains.DayTicks), "Dormancy lasts past a full day");
        Helper.assertTrue(Z0002Domains.open(Holder, Used, false, Short) == Z0002Domains.Outcome.Dormant, "The finger opened a second domain the same day");
        Holder.setItemInHand(InteractionHand.MAIN_HAND, Z0002FingerItem.create());
        Helper.assertTrue(Z0002Domains.open(Holder, Holder.getMainHandItem(), false, Short) == Z0002Domains.Outcome.Dormant, "A fresh copy skipped the holder's dormancy");
        Other.setItemInHand(InteractionHand.MAIN_HAND, Used.copy());
        Helper.assertTrue(Z0002Domains.open(Other, Other.getMainHandItem(), false, Short) == Z0002Domains.Outcome.Dormant, "Handing the finger over skipped its dormancy");
        World.setDayTime(World.getDayTime() + Z0002Domains.DayTicks);
        Helper.assertTrue(Z0002Domains.open(Holder, Used, false, Short) == Z0002Domains.Outcome.Dormant, "Skipping the time of day skipped dormancy");
        dismiss(Helper, Holder, Other);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0002_rejoinReturns")
    public static void rejoinReturns(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Z0002TestPlayer Holder = holder(Helper, "z0002_rejoin_holder");
        Z0002TestPlayer Member = player(Helper, 12.5, 12.5, "z0002_rejoin_member");
        UUID Id = Member.getUUID();
        Vec3 Original = Member.position();
        Z0002Domain Domain = open(Helper, Holder, Short);
        Vec3 Inside = Member.position();
        World.removePlayerImmediately(Member, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        Z0002Domains.collapse(Server, Domain, false);
        Helper.assertTrue(Z0002World.get(Server).pendingReturn(Id) != null, "No return was kept for the player who logged out");
        Z0002TestPlayer Back = new Z0002TestPlayer(World, "z0002_rejoin_member", Id);
        Back.moveTo(Inside.x, Inside.y, Inside.z, 0.0f, 0.0f);
        World.addNewPlayer(Back);
        Z0002Domains.loggedIn(Back);
        Helper.assertTrue(same(Back.position(), Original), "The returning player was not put back where they were taken from: " + Back.position() + " vs " + Original + " " + diagnose(World, Back, Original));
        Helper.assertTrue(Z0002World.get(Server).pendingReturn(Id) == null, "The pending return was not cleared");
        dismiss(Helper, Holder, Back);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "z0002_twoDomainsAtOnce")
    public static void twoDomainsAtOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Z0002TestPlayer First = holder(Helper, "z0002_two_first");
        Z0002TestPlayer Caught = player(Helper, 12.5, 8.5, "z0002_two_caught");
        Z0002Domain One = open(Helper, First, Short);
        Vec3 Centre = One.Region.floorCentre();
        Vec3 Edge = Centre.add(16.0, 0.0, 0.0);
        Caught.teleportTo(World, Edge.x, Edge.y, Edge.z, 0.0f, 0.0f);
        Z0002TestPlayer Second = player(Helper, 8.5 + 44.0, 8.5, "z0002_two_second");
        Second.setItemInHand(InteractionHand.MAIN_HAND, Z0002FingerItem.create());
        Z0002TestPlayer Close = player(Helper, 8.5 + 30.0, 8.5 + 6.0, "z0002_two_close");
        Close.setItemInHand(InteractionHand.MAIN_HAND, Z0002FingerItem.create());
        Z0002Domain Two = null;
        try {
            Helper.assertTrue(Second.distanceTo(Caught) <= Z0002Domains.TakeRange, "The test setup does not put the caught player in reach of the second holder");
            Helper.assertTrue(Z0002Domains.open(Close, Close.getMainHandItem(), false, Short) == Z0002Domains.Outcome.Overlap, "A domain opened overlapping another");
            Two = open(Helper, Second, Short);
            Helper.assertTrue(One.active() && Two.active() && One.Id != Two.Id, "Two domains are not open at once");
            Helper.assertFalse(Two.Members.containsKey(Caught.getUUID()), "A player inside one domain was pulled into another");
            Helper.assertTrue(One.Members.containsKey(Caught.getUUID()), "The first domain lost its member");
            Helper.assertTrue(Two.Members.containsKey(Close.getUUID()), "The bystander near the second holder was not taken");
            Helper.assertFalse(One.Region.overlaps(Two.Region, Z0002Domains.OverlapGap), "The two domains overlap");
            Helper.assertTrue(Z0002Domains.slash(First, First.getMainHandItem(), One) && Z0002Domains.slash(Second, Second.getMainHandItem(), Two), "Each holder could not slash in their own domain");
        } finally {
            close(Server, One);
            close(Server, Two);
        }
        dismiss(Helper, First, Caught, Second, Close);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "z0002_roundTripIsPerfect")
    public static void roundTripIsPerfect(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0002TestPlayer Holder = holder(Helper, "z0002_trip_holder");
        UUID FrameId = fixture(Helper);
        Helper.runAfterDelay(4, () -> {
            Expected Before = Expected.take(World, Holder);
            Z0002Domain Domain = open(Helper, Holder, Short);
            try {
                Helper.assertFalse(World.getBlockState(Helper.absolutePos(ChestSpot)).is(Blocks.CHEST), "The chest was not cleared for the domain");
                Helper.assertTrue(World.getEntity(FrameId) == null, "The item frame stayed in the domain");
                Z0002Domains.collapse(World.getServer(), Domain, false);
                Before.compare(Helper, World, FrameId);
            } finally {
                close(World.getServer(), Domain);
            }
            dismiss(Helper, Holder);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 320, batch = "z0002_reloadMidDomain")
    public static void reloadMidDomain(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Z0002TestPlayer Holder = holder(Helper, "z0002_reload_holder");
        Z0002TestPlayer Member = player(Helper, 13.5, 6.5, "z0002_reload_member");
        Vec3 Original = Member.position();
        UUID FrameId = fixture(Helper);
        Helper.runAfterDelay(4, () -> {
            Expected Before = Expected.take(World, Holder);
            Z0002Domain Domain = open(Helper, Holder, Short);
            UUID Id = Domain.Id;
            Z0002World Fresh = Z0002World.reload(Server);
            Helper.assertTrue(Fresh.byId(Id) != null && Fresh.byId(Id) != Domain, "The domain did not survive a reload of its saved data");
            Helper.assertTrue(Fresh.byId(Id).Snapshot.recorded() == Domain.Snapshot.recorded(), "The reloaded snapshot lost blocks");
            Z0002Domains.beginRecovery();
            Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Fresh.byId(Id) == null || Fresh.byId(Id).Closed, "The reloaded domain has not recovered yet")).thenExecute(() -> {
                Before.compare(Helper, World, FrameId);
                Helper.assertTrue(same(Member.position(), Original), "The member was not returned after the reload: " + Member.position() + " vs " + Original + " " + diagnose(World, Member, Original));
                dismiss(Helper, Holder, Member);
            }).thenSucceed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "z0002_lockerKeepsTheFinger")
    public static void lockerKeepsTheFinger(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Spot = new BlockPos(8, 2, 8);
        Helper.setBlock(Spot, StorageRegistry.SmallContainer.get().defaultBlockState());
        if (!(Helper.getBlockEntity(Spot) instanceof StorageContainerBlockEntity Locker)) {
            throw new IllegalStateException("No storage container block entity");
        }
        long Until = World.getGameTime() + 5000L;
        ItemStack Finger = Z0002FingerItem.create();
        Finger.set(Z0002Registry.DormantUntil.get(), Until);
        Helper.assertTrue(Locker.refusalFor(Finger.copy()) == null, "The locker refuses the finger");
        Z0002TestPlayer User = player(Helper, 10.5, 8.5, "z0002_locker_user");
        User.setItemInHand(InteractionHand.MAIN_HAND, Finger.copy());
        Locker.store(User, User.getMainHandItem());
        Helper.assertTrue(User.getMainHandItem().isEmpty(), "The finger stayed in the hand");
        Helper.assertTrue(Z0002FingerItem.is(Locker.peekContents()), "The locker does not hold the finger");
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Z0002Domains.itemUntil(Locker.peekContents()) == Until, "Storage changed the finger's dormancy");
            Helper.assertTrue(Z0002Domains.itemDormant(Locker.peekContents(), World.getGameTime()), "The stored finger stopped counting by game time");
            Helper.assertTrue(Z0002Domains.ownedBy(World.getServer(), User.getUUID()) == null, "A stored finger opened a domain");
            Locker.take(User, InteractionHand.MAIN_HAND);
            Helper.assertTrue(ItemStack.isSameItemSameComponents(User.getMainHandItem(), Finger), "The finger came back changed");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "z0002_windupEscape")
    public static void windupEscape(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Z0002TestPlayer Holder = holder(Helper, "z0002_windup_holder");
        Z0002TestPlayer Near = player(Helper, 18.5, 8.5, "z0002_windup_near");
        Z0002TestPlayer Runner = player(Helper, 26.5, 8.5, "z0002_windup_runner");
        Z0002Domains.Outcome Result = Z0002Domains.begin(Holder, InteractionHand.MAIN_HAND, false, Short);
        Helper.assertTrue(Result == Z0002Domains.Outcome.Channelling, "The finger did not begin to wind up: " + Result);
        Helper.assertTrue(Z0002Domains.ownedBy(Server, Holder.getUUID()) == null, "The domain opened with no warning");
        Vec3 Away = Runner.position().add(50.0, 0.0, 0.0);
        whenDone(Helper, () -> {
            Z0002Domain Left = Z0002Domains.ownedBy(Server, Holder.getUUID());
            if (Left != null) {
                close(Server, Left);
            }
            dismiss(Helper, Holder, Near, Runner);
        });
        Helper.startSequence().thenIdle(12).thenExecute(() -> {
            Helper.assertTrue(Z0002Domains.channelOf(Holder.getUUID()) != null, "The wind-up ended before the runner left");
            Runner.teleportTo(World, Away.x, Away.y, Away.z, 0.0f, 0.0f);
        }).thenWaitUntil(() -> Helper.assertTrue(Z0002Domains.ownedBy(Server, Holder.getUUID()) != null, "The domain has not opened yet")).thenExecute(() -> {
            Z0002Domain Domain = Z0002Domains.ownedBy(Server, Holder.getUUID());
            Helper.assertTrue(Domain.Start - Z0002Domains.now(Server) <= 0 && Z0002Domains.channelOf(Holder.getUUID()) == null, "The wind-up did not end");
            Helper.assertTrue(Domain.Members.containsKey(Near.getUUID()), "A player who stayed in range was not taken");
            Helper.assertFalse(Domain.Members.containsKey(Runner.getUUID()), "A player who ran out of range during the wind-up was taken");
            Helper.assertTrue(Domain.Phase == Z0002Phase.Building, "The domain skipped its construction");
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "z0002_windupFizzles")
    public static void windupFizzles(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Z0002TestPlayer Struck = holder(Helper, "z0002_fizzle_struck");
        Z0002TestPlayer Dropped = player(Helper, 3.5, 3.5, "z0002_fizzle_dropped");
        Dropped.setItemInHand(InteractionHand.MAIN_HAND, Z0002FingerItem.create());
        Helper.assertTrue(Z0002Domains.begin(Struck, InteractionHand.MAIN_HAND, false, Short) == Z0002Domains.Outcome.Channelling, "The first holder did not wind up");
        Helper.assertTrue(Z0002Domains.begin(Dropped, InteractionHand.MAIN_HAND, false, Short) == Z0002Domains.Outcome.Channelling, "The second holder did not wind up");
        whenDone(Helper, () -> {
            for (Z0002TestPlayer Each : List.of(Struck, Dropped)) {
                Z0002Domain Left = Z0002Domains.ownedBy(Server, Each.getUUID());
                if (Left != null) {
                    close(Server, Left);
                }
            }
            dismiss(Helper, Struck, Dropped);
        });
        Helper.startSequence().thenIdle(6).thenExecute(() -> {
            ready(Struck);
            Helper.assertTrue(Struck.hurt(World.damageSources().generic(), 6.0f), "The hard hit did not land on the first holder");
            Dropped.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }).thenWaitUntil(() -> {
            for (Z0002TestPlayer Each : List.of(Struck, Dropped)) {
                Helper.assertTrue(Z0002Domains.channelOf(Each.getUUID()) == null, Each.getGameProfile().getName() + " is still winding up");
            }
        }).thenExecute(() -> {
            long Now = Z0002Domains.now(Server);
            for (Z0002TestPlayer Each : List.of(Struck, Dropped)) {
                String Name = Each.getGameProfile().getName();
                Helper.assertTrue(Z0002Domains.ownedBy(Server, Each.getUUID()) == null, Name + " opened a domain after being interrupted");
                long Rest = Z0002World.get(Server).dormantUntil(Each.getUUID()) - Now;
                Helper.assertTrue(Rest > 0 && Rest <= Z0002Domains.FizzleTicks, Name + " was not given a short rest: " + Rest);
            }
            Helper.assertTrue(Z0002Domains.itemUntil(Struck.getMainHandItem()) == 0L, "A fizzle made the finger dormant for a day");
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "z0002_gradualRoundTrip")
    public static void gradualRoundTrip(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Z0002TestPlayer Holder = holder(Helper, "z0002_gradual_holder");
        UUID FrameId = fixture(Helper);
        Expected[] Before = new Expected[1];
        Z0002Domain[] Made = new Z0002Domain[1];
        whenDone(Helper, () -> {
            close(Server, Made[0]);
            dismiss(Helper, Holder);
        });
        Helper.startSequence().thenIdle(4).thenExecute(() -> {
            Before[0] = Expected.take(World, Holder);
            Made[0] = start(Helper, Holder, 40);
            Helper.assertTrue(Made[0].Phase == Z0002Phase.Building, "The domain did not start by building");
        }).thenIdle(3).thenExecute(() -> {
            BlockPos Crown = Made[0].Region.Centre().above(Made[0].Region.Radius());
            Helper.assertFalse(Z0002BarrierBlock.is(World.getBlockState(Crown)), "The barrier closed overhead all at once");
            Helper.assertFalse(World.getBlockState(Helper.absolutePos(ChestSpot)).is(Blocks.CHEST), "The chest was still standing after the first ticks");
        }).thenWaitUntil(() -> Helper.assertTrue(Made[0].Phase == Z0002Phase.Active, "The construction has not finished")).thenExecute(() -> {
            BlockPos Crown = Made[0].Region.Centre().above(Made[0].Region.Radius());
            Helper.assertTrue(Z0002BarrierBlock.is(World.getBlockState(Crown)), "The finished barrier does not close overhead");
            Z0002Domains.beginCollapse(Server, Made[0]);
            Helper.assertTrue(Made[0].Phase == Z0002Phase.Collapsing, "The collapse did not begin");
        }).thenIdle(8).thenExecute(() -> {
            BlockPos Crown = Made[0].Region.Centre().above(Made[0].Region.Radius());
            Helper.assertFalse(Z0002BarrierBlock.is(World.getBlockState(Crown)), "The barrier top did not break first");
            Helper.assertFalse(Made[0].Closed, "The collapse finished all at once");
        }).thenWaitUntil(() -> Helper.assertTrue(Made[0].Closed, "The collapse has not finished")).thenExecute(() -> Before[0].compare(Helper, World, FrameId)).thenSucceed();
    }

    public static final BlockPos ChestSpot = new BlockPos(3, 2, 3);
    public static final BlockPos SignSpot = new BlockPos(4, 2, 3);
    public static final BlockPos FrameSpot = new BlockPos(11, 3, 10);

    private static UUID fixture(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(ChestSpot, Blocks.CHEST);
        if (Helper.getBlockEntity(ChestSpot) instanceof ChestBlockEntity Chest) {
            Chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
            Chest.setItem(4, new ItemStack(Items.IRON_INGOT, 32));
            ItemStack Named = new ItemStack(Items.PAPER);
            Named.set(DataComponents.CUSTOM_NAME, Component.literal("Inventory list"));
            Chest.setItem(26, Named);
        }
        Helper.setBlock(SignSpot, Blocks.OAK_SIGN);
        if (Helper.getBlockEntity(SignSpot) instanceof SignBlockEntity Sign) {
            Sign.updateText(Text -> Text.setMessage(0, Component.literal("Do not open")), true);
        }
        Helper.setBlock(new BlockPos(5, 1, 5), Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true));
        Helper.setBlock(new BlockPos(6, 1, 5), Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true));
        Helper.setBlock(new BlockPos(29, 2, 8), Blocks.STONE_BRICKS);
        Helper.setBlock(new BlockPos(28, 2, 8), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST));
        Helper.setBlock(new BlockPos(11, 2, 11), Blocks.STONE_BRICKS);
        Helper.setBlock(new BlockPos(11, 3, 11), Blocks.STONE_BRICKS);
        Helper.setBlock(new BlockPos(4, 2, 11), Blocks.REDSTONE_BLOCK);
        Helper.setBlock(new BlockPos(5, 2, 11), Blocks.REDSTONE_WIRE);
        Helper.setBlock(new BlockPos(6, 2, 11), Blocks.REDSTONE_WIRE);
        BlockState Door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        Helper.setBlock(new BlockPos(13, 2, 4), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        Helper.setBlock(new BlockPos(13, 3, 4), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        ItemFrame Frame = new ItemFrame(World, Helper.absolutePos(FrameSpot), Direction.NORTH);
        Frame.setItem(new ItemStack(Items.EMERALD), false);
        World.addFreshEntity(Frame);
        Vec3 CartSpot = Helper.absoluteVec(new Vec3(10.5, 2.0, 3.5));
        Minecart Cart = new Minecart(World, CartSpot.x, CartSpot.y, CartSpot.z);
        World.addFreshEntity(Cart);
        Villager Rider = EntityType.VILLAGER.create(World);
        Rider.moveTo(CartSpot.x, CartSpot.y, CartSpot.z, 0.0f, 0.0f);
        Rider.setNoAi(true);
        World.addFreshEntity(Rider);
        Rider.startRiding(Cart, true);
        Vec3 StandSpot = Helper.absoluteVec(new Vec3(2.5, 2.0, 13.5));
        ArmorStand Stand = new ArmorStand(World, StandSpot.x, StandSpot.y, StandSpot.z);
        Stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        World.addFreshEntity(Stand);
        Riders.put(Frame.getUUID(), new UUID[]{Cart.getUUID(), Rider.getUUID(), Stand.getUUID()});
        return Frame.getUUID();
    }

    private static final Map<UUID, UUID[]> Riders = new HashMap<>();

    private record Expected(Map<BlockPos, BlockState> States, Map<BlockPos, CompoundTag> Data, AABB Area, Set<UUID> Loose, CompoundTag FrameData,
                            BlockPos FramePos, ItemStack FrameItem) {
        static Expected take(ServerLevel World, ServerPlayer Holder) {
            HolderLookup.Provider Lookup = World.registryAccess();
            BlockPos Feet = Holder.blockPosition();
            int Floor = Z0002Domains.floorFor(World, Feet);
            int Reach = Z0002Domains.Radius + 2;
            Map<BlockPos, BlockState> States = new HashMap<>();
            Map<BlockPos, CompoundTag> Data = new HashMap<>();
            for (BlockPos Pos : BlockPos.betweenClosed(Feet.getX() - Reach, Floor - 1, Feet.getZ() - Reach, Feet.getX() + Reach, Floor + Reach, Feet.getZ() + Reach)) {
                BlockPos Fixed = Pos.immutable();
                States.put(Fixed, World.getBlockState(Fixed));
                BlockEntity Found = World.getBlockEntity(Fixed);
                if (Found != null) {
                    Data.put(Fixed, Found.saveWithoutMetadata(Lookup));
                }
            }
            AABB Area = new AABB(Feet).inflate(Reach);
            Set<UUID> Loose = new HashSet<>();
            for (ItemEntity Each : World.getEntitiesOfClass(ItemEntity.class, Area)) {
                Loose.add(Each.getUUID());
            }
            ItemFrame Frame = World.getEntitiesOfClass(ItemFrame.class, Area).stream().findFirst().orElse(null);
            CompoundTag FrameData = new CompoundTag();
            if (Frame != null) {
                Frame.saveWithoutId(FrameData);
            }
            return new Expected(States, Data, Area, Loose, FrameData, Frame == null ? null : Frame.getPos(), Frame == null ? ItemStack.EMPTY : Frame.getItem().copy());
        }

        void compare(GameTestHelper Helper, ServerLevel World, UUID FrameId) {
            HolderLookup.Provider Lookup = World.registryAccess();
            int Wrong = 0;
            String First = "";
            for (Map.Entry<BlockPos, BlockState> Entry : States.entrySet()) {
                BlockState Now = World.getBlockState(Entry.getKey());
                if (Now != Entry.getValue()) {
                    if (Wrong++ == 0) {
                        First = Entry.getKey().toShortString() + " " + Entry.getValue() + " became " + Now;
                    }
                }
            }
            Helper.assertTrue(Wrong == 0, Wrong + " block(s) changed, first " + First);
            for (Map.Entry<BlockPos, CompoundTag> Entry : Data.entrySet()) {
                BlockEntity Found = World.getBlockEntity(Entry.getKey());
                Helper.assertTrue(Found != null && Found.saveWithoutMetadata(Lookup).equals(Entry.getValue()), "Block entity data changed at " + Entry.getKey().toShortString());
            }
            for (ItemEntity Each : World.getEntitiesOfClass(ItemEntity.class, Area)) {
                Helper.assertTrue(Loose.contains(Each.getUUID()), "An item entity appeared: " + Each.getItem());
            }
            Entity Restored = World.getEntity(FrameId);
            Helper.assertTrue(Restored instanceof ItemFrame, "The item frame did not come back");
            ItemFrame Frame = (ItemFrame) Restored;
            Helper.assertTrue(Frame.getPos().equals(FramePos) && ItemStack.isSameItemSameComponents(Frame.getItem(), FrameItem) && Frame.getItem().getCount() == FrameItem.getCount(),
                    "The item frame came back different");
            CompoundTag Now = new CompoundTag();
            Frame.saveWithoutId(Now);
            for (String Key : List.of("Item", "Facing", "TileX", "TileY", "TileZ", "ItemRotation", "Fixed", "Invisible", "UUID")) {
                Helper.assertTrue(String.valueOf(Now.get(Key)).equals(String.valueOf(FrameData.get(Key))), "The item frame's " + Key + " changed");
            }
            UUID[] Kept = Riders.get(FrameId);
            if (Kept != null) {
                Entity Cart = World.getEntity(Kept[0]);
                Entity Rider = World.getEntity(Kept[1]);
                Entity Stand = World.getEntity(Kept[2]);
                Helper.assertTrue(Cart instanceof Minecart && Rider instanceof Villager && Rider.getVehicle() == Cart, "The minecart and its rider did not come back together");
                Helper.assertTrue(Stand instanceof ArmorStand Armour && Armour.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "The armour stand did not come back intact");
            }
        }
    }
}
