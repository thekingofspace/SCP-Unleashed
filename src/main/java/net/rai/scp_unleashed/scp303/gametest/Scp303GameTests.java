package net.rai.scp_unleashed.scp303.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.registry.ModItems;
import net.rai.scp_unleashed.scp303.Scp303Arrival;
import net.rai.scp_unleashed.scp303.Scp303Doorman;
import net.rai.scp_unleashed.scp303.Scp303Doors;
import net.rai.scp_unleashed.scp303.Scp303Sight;
import net.rai.scp_unleashed.scp303.Scp303State;
import net.rai.scp_unleashed.z0003.Z0003Petrify;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp303GameTests {
    public static final String Arena = "npc_doors_arena";
    public static final double Middle = 16.5;
    public static final double Start = 4.5;
    public static final int WallZ = 16;
    public static final int DoorX = 16;
    public static final double WalkFrom = 12.5;
    public static final double WalkStep = 0.25;
    public static final int WalkSteps = 32;
    public static final int Settle = 5;
    public static final double Clearance = 80.0;

    private Scp303GameTests() {
    }

    private static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (!Helper.getBlockState(new BlockPos(1, Y, 1)).isAir() && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static void begin(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp303State.get(World.getServer()).reset();
        AABB Area = Helper.getBounds().inflate(Clearance);
        for (Scp303Doorman Old : World.getEntitiesOfClass(Scp303Doorman.class, Area)) {
            Old.discard();
        }
        for (LivingEntity Stray : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> !(Found instanceof Player) && Scp303Sight.person(Found))) {
            Stray.discard();
        }
    }

    private static void finish(GameTestHelper Helper, List<? extends Entity> Spawned) {
        ServerLevel World = Helper.getLevel();
        for (Entity Each : Spawned) {
            if (Each.isRemoved()) {
                continue;
            }
            if (Each instanceof ServerPlayer User) {
                World.removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            } else {
                Each.discard();
            }
        }
        Scp303State.get(World.getServer()).reset();
    }

    private static void guard(GameTestHelper Helper, int Limit, List<? extends Entity> Spawned) {
        int[] Clock = {0};
        Helper.onEachTick(() -> {
            if (++Clock[0] == Limit - 2) {
                finish(Helper, Spawned);
            }
        });
    }

    private static void face(LivingEntity Body, float Yaw) {
        Body.setYRot(Yaw);
        Body.yRotO = Yaw;
        Body.setYHeadRot(Yaw);
        Body.yHeadRotO = Yaw;
        Body.setYBodyRot(Yaw);
        Body.yBodyRotO = Yaw;
        Body.setXRot(0.0f);
        Body.xRotO = 0.0f;
    }

    private static void put(GameTestHelper Helper, LivingEntity Body, double X, double Y, double Z, float Yaw) {
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Body.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        face(Body, Yaw);
    }

    private static DClass prisoner(GameTestHelper Helper, double X, double Y, double Z, float Yaw) {
        DClass Made = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(X, Y, Z));
        put(Helper, Made, X, Y, Z, Yaw);
        return Made;
    }

    private static Scp303TestPlayer player(GameTestHelper Helper, String Name, double X, double Y, double Z, float Yaw, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Scp303TestPlayer User = new Scp303TestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        face(User, Yaw);
        World.addNewPlayer(User);
        User.setGameMode(Mode);
        return User;
    }

    private static Scp303Doorman doorman(GameTestHelper Helper, double X, double Y, double Z, @Nullable UUID Summoner) {
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Optional<Scp303Doorman> Made = Scp303Arrival.place(Helper.getLevel(), Spot, 0.0f, Summoner);
        Helper.assertTrue(Made.isPresent(), "SCP-303 could not be placed");
        Scp303Doorman Doorman = Made.get();
        Doorman.setNoAi(true);
        return Doorman;
    }

    private static void door(GameTestHelper Helper, BlockPos Lower, Direction Facing, boolean Open) {
        BlockState Door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Facing).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.OPEN, Open);
        Helper.setBlock(Lower, Door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        Helper.setBlock(Lower.above(), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static BlockPos doorway(GameTestHelper Helper, int Base) {
        for (int X = 1; X <= 30; X++) {
            for (int Y = Base; Y < Base + 4; Y++) {
                if (X != DoorX || Y >= Base + 2) {
                    Helper.setBlock(new BlockPos(X, Y, WallZ), Blocks.STONE_BRICKS);
                }
            }
        }
        BlockPos Lower = new BlockPos(DoorX, Base, WallZ);
        door(Helper, Lower, Direction.NORTH, true);
        return Lower;
    }

    private static boolean hunting(Scp303Doorman Doorman, Entity Quarry) {
        return Quarry.getUUID().equals(Doorman.targetId());
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp303_door")
    public static void doormanHoldsDoorOnDClass(GameTestHelper Helper) {
        begin(Helper);
        int Base = stand(Helper);
        BlockPos Door = doorway(Helper, Base);
        DClass Prisoner = prisoner(Helper, Middle, Base, WalkFrom, 0.0f);
        Scp303Doorman Doorman = doorman(Helper, Middle, Base, Start, null);
        guard(Helper, 200, List.of(Prisoner, Doorman));
        int[] Clock = {0};
        boolean[] Chased = {false};
        Helper.onEachTick(() -> {
            Clock[0]++;
            Chased[0] |= hunting(Doorman, Prisoner);
            if (Clock[0] > Settle && Clock[0] <= Settle + WalkSteps) {
                put(Helper, Prisoner, Middle, Base, WalkFrom + (Clock[0] - Settle) * WalkStep, 0.0f);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Chased[0], "SCP-303 never went after the D-Class");
            Helper.assertTrue(Doorman.isAlive() && Doorman.mode() == Scp303Doorman.Mode.Hold, "SCP-303 did not hold the door on the D-Class, mode " + Doorman.mode());
            Helper.assertTrue(Doorman.heldDoor().map(Helper.absolutePos(Door)::equals).orElse(false), "SCP-303 is holding the wrong door");
            Helper.assertFalse(Scp303Doors.isOpen(Helper.getLevel(), Helper.absolutePos(Door)), "the held door is still open");
            Helper.assertTrue(hunting(Doorman, Prisoner), "SCP-303 let go of the D-Class while holding the door");
            finish(Helper, List.of(Prisoner, Doorman));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp303_nearer_player")
    public static void doormanPicksNearerPlayerOverSummoner(GameTestHelper Helper) {
        begin(Helper);
        int Base = stand(Helper);
        Scp303TestPlayer Summoner = player(Helper, "scp303_summoner", Middle, Base, 30.5, 180.0f, GameType.SURVIVAL);
        Scp303TestPlayer Stranger = player(Helper, "scp303_stranger", Middle, Base, 11.5, 0.0f, GameType.SURVIVAL);
        Scp303Doorman Doorman = doorman(Helper, Middle, Base, Start, Summoner.getUUID());
        List<Entity> Spawned = List.of(Summoner, Stranger, Doorman);
        guard(Helper, 120, Spawned);
        Helper.assertTrue(Scp303Doorman.quarry(Summoner, Helper.getLevel()) && Scp303Doorman.quarry(Stranger, Helper.getLevel()), "a survival player is not valid quarry");
        Helper.assertTrue(Summoner.getUUID().equals(Scp303State.get(Helper.getLevel().getServer()).favourite()), "the summoner is not remembered");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(hunting(Doorman, Stranger), "SCP-303 did not go after the nearer player"))
                .thenIdle(Scp303Doorman.RetargetInterval * 2 + 5)
                .thenExecute(() -> {
                    Helper.assertTrue(Doorman.isAlive(), "SCP-303 left");
                    Helper.assertTrue(hunting(Doorman, Stranger), "SCP-303 switched back to the summoner");
                    finish(Helper, Spawned);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp303_nearer_dclass")
    public static void doormanPicksNearerDClassOverSummoner(GameTestHelper Helper) {
        begin(Helper);
        int Base = stand(Helper);
        Scp303TestPlayer Summoner = player(Helper, "scp303_far_summoner", Middle, Base, 30.5, 180.0f, GameType.SURVIVAL);
        DClass Prisoner = prisoner(Helper, Middle, Base, 11.5, 0.0f);
        Scp303Doorman Doorman = doorman(Helper, Middle, Base, Start, Summoner.getUUID());
        List<Entity> Spawned = List.of(Summoner, Prisoner, Doorman);
        guard(Helper, 120, Spawned);
        Helper.assertTrue(Scp303Doorman.quarry(Prisoner, Helper.getLevel()), "a D-Class is not valid quarry");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(hunting(Doorman, Prisoner), "SCP-303 did not go after the nearer D-Class"))
                .thenIdle(Scp303Doorman.RetargetInterval * 2 + 5)
                .thenExecute(() -> {
                    Helper.assertTrue(Doorman.isAlive() && Doorman.mode() == Scp303Doorman.Mode.Stalk, "SCP-303 is not stalking the D-Class, mode " + Doorman.mode());
                    Helper.assertTrue(hunting(Doorman, Prisoner), "SCP-303 switched back to the summoner");
                    finish(Helper, Spawned);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 50, batch = "scp303_operative")
    public static void doormanHuntsOperativesAndRaiders(GameTestHelper Helper) {
        begin(Helper);
        ServerLevel World = Helper.getLevel();
        int Base = stand(Helper);
        MtfOperative Operative = Helper.spawnWithNoFreeWill(MtfRegistry.OperativeType.get(), new Vec3(Middle, Base, 11.5));
        put(Helper, Operative, Middle, Base, 11.5, 0.0f);
        Scp303Doorman Doorman = doorman(Helper, Middle, Base, Start, null);
        List<Entity> Spawned = List.of(Operative, Doorman);
        guard(Helper, 50, Spawned);
        Helper.assertTrue(Scp303Doorman.quarry(Operative, World), "an MTF operative is not valid quarry");
        Helper.assertTrue(Scp303Doorman.quarry(HostileRegistry.AgentType.get().create(World), World), "a Serpent's Hand agent is not valid quarry");
        Helper.assertTrue(Scp303Doorman.quarry(HostileRegistry.InsurgentType.get().create(World), World), "a Chaos Insurgency raider is not valid quarry");
        Helper.assertFalse(Scp303Doorman.quarry(Doorman, World), "SCP-303 counts itself as quarry");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(hunting(Doorman, Operative), "SCP-303 did not go after the MTF operative");
            finish(Helper, Spawned);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp303_ignored")
    public static void doormanIgnoresStatuesCreativeAndSpectators(GameTestHelper Helper) {
        begin(Helper);
        ServerLevel World = Helper.getLevel();
        int Base = stand(Helper);
        DClass Stone = prisoner(Helper, Middle, Base, 10.5, 0.0f);
        Helper.assertTrue(Z0003Petrify.petrify(Stone), "could not turn the D-Class to stone");
        Helper.assertTrue(Targeting.isStatue(Stone), "the D-Class is not a statue");
        Scp303TestPlayer Overseer = player(Helper, "scp303_creative", 4.5, Base, 26.5, 0.0f, GameType.CREATIVE);
        Scp303TestPlayer Ghost = player(Helper, "scp303_spectator", 28.5, Base, 20.5, 0.0f, GameType.SPECTATOR);
        Scp303Doorman Doorman = doorman(Helper, Middle, Base, Start, Overseer.getUUID());
        List<Entity> Spawned = List.of(Stone, Overseer, Ghost, Doorman);
        guard(Helper, 100, Spawned);
        Helper.assertFalse(Scp303Doorman.quarry(Stone, World), "a statue is valid quarry");
        Helper.assertFalse(Scp303Doorman.quarry(Overseer, World), "a creative player is valid quarry");
        Helper.assertFalse(Scp303Doorman.quarry(Ghost, World), "a spectator is valid quarry");
        Helper.startSequence()
                .thenIdle(Scp303Doorman.RetargetInterval * 2 + Settle)
                .thenExecute(() -> {
                    Helper.assertTrue(Doorman.isAlive(), "SCP-303 left");
                    Helper.assertTrue(Doorman.targetId() == null, "SCP-303 went after someone it should ignore");
                    Helper.assertTrue(Doorman.mode() == Scp303Doorman.Mode.Idle, "SCP-303 is not idle, mode " + Doorman.mode());
                    finish(Helper, Spawned);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp303_retarget")
    public static void doormanRetargetsWhenQuarryDies(GameTestHelper Helper) {
        begin(Helper);
        int Base = stand(Helper);
        DClass First = prisoner(Helper, Middle, Base, 10.5, 0.0f);
        DClass Second = prisoner(Helper, Middle, Base, 22.5, 0.0f);
        Scp303Doorman Doorman = doorman(Helper, Middle, Base, Start, null);
        List<Entity> Spawned = List.of(First, Second, Doorman);
        guard(Helper, 100, Spawned);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(hunting(Doorman, First), "SCP-303 did not go after the nearest D-Class"))
                .thenExecute(First::kill)
                .thenWaitUntil(() -> Helper.assertTrue(hunting(Doorman, Second), "SCP-303 did not move on once its quarry died"))
                .thenExecute(() -> {
                    Helper.assertTrue(Doorman.isAlive() && Doorman.mode() == Scp303Doorman.Mode.Stalk, "SCP-303 is not stalking, mode " + Doorman.mode());
                    finish(Helper, Spawned);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp303_bind")
    public static void anyoneCanBindDoorman(GameTestHelper Helper) {
        begin(Helper);
        ServerLevel World = Helper.getLevel();
        Scp303State State = Scp303State.get(World.getServer());
        int Base = stand(Helper);
        BlockPos Lower = new BlockPos(26, Base, 26);
        door(Helper, Lower, Direction.EAST, false);
        Scp303TestPlayer Summoner = player(Helper, "scp303_reader", 4.5, Base, 28.5, 0.0f, GameType.SURVIVAL);
        State.unlock(Summoner.getUUID());
        Scp303TestPlayer Binder = player(Helper, "scp303_binder", 24.5, Base, 26.5, -90.0f, GameType.SURVIVAL);
        Scp303Doorman Doorman = doorman(Helper, Middle, Base, Start, Summoner.getUUID());
        List<Entity> Spawned = List.of(Summoner, Binder, Doorman);
        guard(Helper, 80, Spawned);
        Helper.assertFalse(State.isUnlocked(Binder.getUUID()), "the binder already knows about SCP-303");
        Binder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EnergumDust.get(), 4));
        Helper.startSequence()
                .thenIdle(Settle)
                .thenExecute(() -> {
                    BlockPos Door = Helper.absolutePos(Lower);
                    BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Door), Direction.WEST, Door, false);
                    InteractionResult Result = Binder.gameMode.useItemOn(Binder, World, Binder.getMainHandItem(), InteractionHand.MAIN_HAND, Hit);
                    Helper.assertTrue(Result.consumesAction(), "the door did not take the dust, result " + Result);
                    Helper.assertTrue(State.bound().map(Binding -> Binding.Door().equals(Door)).orElse(false), "SCP-303 was not bound by a player who did not summon it");
                    Helper.assertTrue(Binder.getMainHandItem().getCount() == 3, "the dust was not used up");
                    Helper.assertFalse(Scp303Doors.isOpen(World, Door), "the bound door opened");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Doorman.isAlive() && Doorman.mode() == Scp303Doorman.Mode.Bound, "SCP-303 is not bound, mode " + Doorman.mode()))
                .thenExecute(() -> finish(Helper, Spawned))
                .thenSucceed();
    }
}
