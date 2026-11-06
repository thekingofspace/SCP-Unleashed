package net.rai.scp_unleashed.scp337.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp337.Scp337Arrival;
import net.rai.scp_unleashed.scp337.Scp337Lash;
import net.rai.scp_unleashed.scp337.Scp337Mass;
import net.rai.scp_unleashed.scp337.Scp337Registry;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp337GameTests {
    public static final String Arena = "scp337_arena";

    private Scp337GameTests() {
    }

    private static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static Scp337Mass mass(GameTestHelper Helper, double X, double Z) {
        Scp337Mass Mass = Helper.spawn(Scp337Registry.Mass.get(), new Vec3(X, stand(Helper), Z));
        Mass.setYRot(0.0f);
        return Mass;
    }

    private static Victim victim(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        Victim User = new Victim(World, new GameProfile(UUID.randomUUID(), "scp337_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, stand(Helper), Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static FakePlayer watcher(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp337_watcher"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, stand(Helper), Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static void tidy(GameTestHelper Helper) {
        Helper.killAllEntitiesOfClass(Scp337Mass.class);
        Helper.killAllEntitiesOfClass(DClass.class);
        Helper.killAllEntitiesOfClass(Villager.class);
        Helper.killAllEntitiesOfClass(Pig.class);
    }

    private static String state(Scp337Mass Mass) {
        return " [target " + (Mass.getTarget() == null ? "none" : Mass.getTarget().getName().getString()) + ", lashes " + Mass.lashes().size() + ", taken " + Mass.taken()
                + ", health " + Mass.getHealth() + ", at " + Mass.position() + "]";
    }

    private static void wall(GameTestHelper Helper, int X, int FromZ, int ToZ, int Height) {
        int Base = stand(Helper);
        for (int Z = FromZ; Z <= ToZ; Z++) {
            for (int Y = Base; Y < Base + Height; Y++) {
                Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE);
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp337_targetsNearest")
    public static void targetsNearest(GameTestHelper Helper) {
        Scp337Mass Mass = mass(Helper, 8.5, 16.5);
        FakePlayer Far = watcher(Helper, 8.5, 26.5);
        DClass Near = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(8.5, stand(Helper), 11.5));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Mass.getTarget() == Near, "SCP-337 did not pick the nearer D-Class" + state(Mass)))
                .thenExecute(() -> {
                    Near.discard();
                    FakePlayer Closer = watcher(Helper, 14.5, 16.5);
                    Helper.runAfterDelay(Scp337Mass.RetargetInterval * 3, () -> {
                        Helper.assertTrue(Mass.getTarget() == Closer, "SCP-337 did not move to the nearest player" + state(Mass));
                        dismiss(Helper, Closer);
                        dismiss(Helper, Far);
                        tidy(Helper);
                        Helper.succeed();
                    });
                });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp337_ignoresVillager")
    public static void ignoresVillager(GameTestHelper Helper) {
        Scp337Mass Mass = mass(Helper, 8.5, 16.5);
        Villager Bystander = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(11.5, stand(Helper), 16.5));
        float Full = Bystander.getHealth();
        Mass.setTarget(Bystander);
        Helper.assertTrue(Mass.getTarget() == null, "SCP-337 accepted a villager as a target");
        Helper.onEachTick(() -> {
            Helper.assertTrue(Mass.getTarget() != Bystander, "SCP-337 targeted the villager" + state(Mass));
            for (Scp337Lash Lash : Mass.lashes()) {
                Helper.assertTrue(Lash.Target() != Bystander.getId(), "SCP-337 lashed at the villager" + state(Mass));
            }
        });
        for (int Delay = 5; Delay <= 125; Delay += 20) {
            Helper.runAfterDelay(Delay, () -> Mass.hurt(Helper.getLevel().damageSources().mobAttack(Bystander), 1.0f));
        }
        Helper.runAfterDelay(150, () -> {
            Helper.assertTrue(Mass.getHealth() < Mass.getMaxHealth(), "the villager's blows never landed");
            Helper.assertTrue(Mass.getTarget() == null, "SCP-337 picked a target with only a villager near" + state(Mass));
            Helper.assertTrue(Bystander.getHealth() >= Full, "the villager was hurt");
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp337_lashHitsInRange")
    public static void lashHitsInRange(GameTestHelper Helper) {
        Scp337Mass Mass = mass(Helper, 8.5, 16.5);
        Victim User = victim(Helper, 14.5, 16.5);
        float Full = User.getHealth();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(User.getHealth() < Full, "no lash landed on a player six blocks away" + state(Mass)))
                .thenExecute(() -> {
                    Vec3 Pull = User.getDeltaMovement();
                    Helper.assertTrue(Pull.x < -0.1, "the lash did not pull the player toward SCP-337, motion " + Pull);
                    float Dealt = Full - User.getHealth();
                    Helper.assertTrue(Dealt >= Scp337Mass.LashDamage * 0.5f && Dealt <= Scp337Mass.LashDamage * 1.5f + 0.01f, "a lash dealt " + Dealt);
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp337_wallBlocksLash")
    public static void wallBlocksLash(GameTestHelper Helper) {
        Scp337Mass Mass = mass(Helper, 8.5, 16.5);
        wall(Helper, 11, 4, 28, 4);
        Victim User = victim(Helper, 13.5, 16.5);
        float Full = User.getHealth();
        Helper.onEachTick(() -> Helper.assertTrue(Mass.lashes().isEmpty(), "SCP-337 lashed through a solid wall" + state(Mass)));
        Helper.runAfterDelay(150, () -> {
            Helper.assertTrue(User.getHealth() >= Full, "a player behind a wall was hurt");
            dismiss(Helper, User);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp337_wallStopsLashInFlight")
    public static void wallStopsLashInFlight(GameTestHelper Helper) {
        Scp337Mass Mass = mass(Helper, 8.5, 16.5);
        Victim User = victim(Helper, 14.5, 16.5);
        float Full = User.getHealth();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Mass.lashes().isEmpty(), "SCP-337 never lashed" + state(Mass)))
                .thenExecute(() -> {
                    Helper.assertTrue(User.getHealth() >= Full, "the player was hurt before the wall went up");
                    wall(Helper, 12, 4, 28, 4);
                })
                .thenExecuteAfter(Scp337Lash.Ticks + 2, () -> {
                    Helper.assertTrue(User.getHealth() >= Full, "a lash struck through a wall raised while it was in the air");
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 360, batch = "scp337_cellHolds")
    public static void cellHolds(GameTestHelper Helper) {
        int Base = stand(Helper);
        for (int X = 5; X <= 11; X++) {
            for (int Z = 13; Z <= 19; Z++) {
                if (X != 5 && X != 11 && Z != 13 && Z != 19) {
                    continue;
                }
                for (int Y = Base; Y < Base + 2; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE_BRICKS);
                }
            }
        }
        BlockState Door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST).setValue(DoorBlock.OPEN, false);
        Helper.setBlock(new BlockPos(11, Base, 16), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        Helper.setBlock(new BlockPos(11, Base + 1, 16), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        Scp337Mass Mass = mass(Helper, 8.5, 16.5);
        FakePlayer Bait = watcher(Helper, 22.5, 16.5);
        AABB Cell = new AABB(Helper.absolutePos(new BlockPos(6, Base, 14))).minmax(new AABB(Helper.absolutePos(new BlockPos(10, Base + 1, 18))));
        Helper.onEachTick(() -> Helper.assertTrue(Cell.contains(Mass.position()), "SCP-337 left a closed cell" + state(Mass)));
        Helper.runAfterDelay(320, () -> {
            Helper.assertBlockProperty(new BlockPos(11, Base, 16), DoorBlock.OPEN, false);
            Helper.assertBlockPresent(Blocks.STONE_BRICKS, new BlockPos(5, Base + 1, 16));
            Helper.assertTrue(Mass.getTarget() == Bait, "SCP-337 never noticed the player outside its cell" + state(Mass));
            dismiss(Helper, Bait);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp337_cellarHolds")
    public static void cellarHolds(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Surface = Helper.absolutePos(new BlockPos(8, 7, 16));
        BlockPos Floor = Scp337Arrival.buildCellar(World, Surface, World.getRandom());
        Helper.assertTrue(World.getBlockState(Floor.above(2)).is(Blocks.LADDER), "the cellar has no ladder");
        Helper.assertTrue(World.getBlockState(Surface.below()).is(Blocks.SPRUCE_TRAPDOOR), "the cellar has no hatch");
        Helper.assertTrue(Scp337Arrival.place(World, new Vec3(Floor.getX() + 2.5, Floor.getY(), Floor.getZ() + 0.5), 90.0f), "SCP-337 did not fit in its cellar");
        List<Scp337Mass> Found = World.getEntitiesOfClass(Scp337Mass.class, new AABB(Floor).inflate(8.0), Scp337Mass::isAlive);
        Helper.assertTrue(Found.size() == 1, "expected one SCP-337 in the cellar, found " + Found.size());
        Scp337Mass Mass = Found.get(0);
        FakePlayer Bait = watcher(Helper, 22.5, 16.5);
        AABB Room = new AABB(Floor.getX(), Floor.getY(), Floor.getZ() - 2, Floor.getX() + 5, Floor.getY() + 3, Floor.getZ() + 3);
        Helper.onEachTick(() -> Helper.assertTrue(Room.contains(Mass.position()), "SCP-337 got out of the cellar" + state(Mass)));
        Helper.runAfterDelay(220, () -> {
            Helper.assertTrue(Mass.getTarget() == Bait, "SCP-337 never noticed the player above" + state(Mass));
            dismiss(Helper, Bait);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp337_stasisRoundTrip")
    public static void stasisRoundTrip(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp337Mass Mass = mass(Helper, 8.5, 16.5);
        Pig Stand = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(3.5, stand(Helper), 3.5));
        Mass.hurt(World.damageSources().generic(), 13.0f);
        Mass.recordTaken(Stand);
        Mass.recordTaken(Stand);
        float Health = Mass.getHealth();
        FakePlayer User = watcher(Helper, 10.5, 16.5);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.refusal(User, Pod, Mass) == null, "a stasis pod refuses SCP-337");
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Mass), "the stasis pod failed to capture SCP-337");
        Helper.assertTrue(Mass.isRemoved() && StasisPodItem.isOccupied(Pod), "the pod does not hold SCP-337");
        Helper.assertTrue(StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(16.5, stand(Helper), 16.5)), null), "SCP-337 could not be released");
        List<Scp337Mass> Released = World.getEntitiesOfClass(Scp337Mass.class, new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(40.0), Scp337Mass::isAlive);
        Helper.assertTrue(Released.size() == 1, "expected one released SCP-337, found " + Released.size());
        Scp337Mass Back = Released.get(0);
        Helper.assertTrue(Math.abs(Back.getHealth() - Health) < 0.01f, "health changed in stasis: " + Health + " became " + Back.getHealth());
        Helper.assertTrue(Back.taken() == 2, "SCP-337 forgot what it had taken: " + Back.taken());
        Helper.assertTrue(Back.sated() > 0, "SCP-337 lost its settled state in stasis");
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(Back.isAlive() && !Back.isRemoved(), "the released SCP-337 did not stay in the world");
            dismiss(Helper, User);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp337_controlledDeathOnce")
    public static void controlledDeathOnce(GameTestHelper Helper) {
        Scp337Mass Mass = mass(Helper, 8.5, 16.5);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(14.5, stand(Helper), 16.5));
        Victim User = victim(Helper, 16.5, 16.5);
        Helper.assertTrue(ControlManager.possess(User, Body), "the test player could not take control of the D-Class");
        Helper.assertTrue(ControlSessions.isControlling(User), "no control session started");
        User.setHealth(2.0f);
        boolean[] Pending = new boolean[1];
        Helper.onEachTick(() -> {
            if (ControlManager.hasPendingDeath(User)) {
                Pending[0] = true;
                Helper.assertTrue(Mass.taken() == 0, "the controlled death was counted before the deferred tick" + state(Mass));
                ControlManager.processDeath(User);
            }
            Helper.assertTrue(Mass.taken() <= 1, "the controlled D-Class death was counted twice" + state(Mass));
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Mass.taken() == 1, "the controlled D-Class death was never counted" + state(Mass) + " [player health " + User.getHealth()
                        + ", pending " + Pending[0] + ", controlling " + ControlSessions.isControlling(User) + ", deaths " + User.Deaths + "]"))
                .thenExecute(() -> {
                    Helper.assertTrue(Pending[0], "the death did not go through the deferred control path");
                    Helper.assertTrue(!ControlSessions.isControlling(User), "the player is still controlling a dead body");
                    Helper.assertTrue(User.Deaths == 0, "the controller died for real");
                })
                .thenExecuteAfter(40, () -> {
                    Helper.assertTrue(Mass.taken() == 1, "the count changed after the death" + state(Mass));
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    public static final class Victim extends FakePlayer {
        public int Deaths;

        public Victim(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
            ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, this, 0, "spawnInvulnerableTime");
        }

        @Override
        public boolean isInvulnerableTo(DamageSource Source) {
            return isRemoved() || getAbilities().invulnerable && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        }

        @Override
        public void die(DamageSource Source) {
            if (CommonHooks.onLivingDeath(this, Source)) {
                return;
            }
            Deaths++;
        }
    }
}
