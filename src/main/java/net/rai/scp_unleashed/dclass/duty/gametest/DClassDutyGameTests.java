package net.rai.scp_unleashed.dclass.duty.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.BodyAttachments;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.dclass.duty.DClassCleaning;
import net.rai.scp_unleashed.dclass.duty.DClassDuties;
import net.rai.scp_unleashed.dclass.duty.DClassDuty;
import net.rai.scp_unleashed.dclass.duty.DClassDutyGoals;
import net.rai.scp_unleashed.dclass.duty.DClassDutyRegistry;
import net.rai.scp_unleashed.dclass.duty.DClassGaze;
import net.rai.scp_unleashed.dclass.duty.DClassManagerMenu;
import net.rai.scp_unleashed.dclass.duty.DClassRole;
import net.rai.scp_unleashed.dclass.duty.DClassTask;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.scp096.Scp096ShyGuy;
import net.rai.scp_unleashed.scp096.Scp096Sight;
import net.rai.scp_unleashed.scp106.Scp106OldMan;
import net.rai.scp_unleashed.scp106.Scp106Pocket;
import net.rai.scp_unleashed.scp106.Scp106Registry;
import net.rai.scp_unleashed.scp106.Scp106Targeting;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.scp173.Scp173Residue;
import net.rai.scp_unleashed.scp173.Scp173ResidueIndex;
import net.rai.scp_unleashed.scp173.Scp173Sight;
import net.rai.scp_unleashed.scp173.Scp173Statue;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class DClassDutyGameTests {
    public static final String Arena = "dclassroles_arena";
    public static final double Floor = 2.0;
    public static final int Ground = 2;
    public static final int Held = 100000;
    public static final int OperativeHold = 40;

    private DClassDutyGameTests() {
    }

    private static void quiet(GameTestHelper Helper) {
        Helper.onEachTick(() -> {
            for (Monster Stray : Helper.getLevel().getEntitiesOfClass(Monster.class, Helper.getBounds().inflate(8.0))) {
                if ("minecraft".equals(EntityType.getKey(Stray.getType()).getNamespace())) {
                    Stray.discard();
                }
            }
        });
    }

    private static DClass person(GameTestHelper Helper, double X, double Z, boolean Thinking, DClassTrait Trait) {
        Vec3 Spot = new Vec3(X, Floor, Z);
        DClass Made = Thinking ? Helper.spawn(DClassRegistry.DClassType.get(), Spot) : Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), Spot);
        Made.identify(Trait, DClassCrime.Smuggling, 0);
        Made.assignNumber();
        return Made;
    }

    private static MtfOperative operative(GameTestHelper Helper, double X, double Z) {
        MtfOperative Made = Helper.spawnWithNoFreeWill(MtfRegistry.OperativeType.get(), new Vec3(X, Floor, Z));
        Made.setState(MtfOperative.StateActive);
        return Made;
    }

    private static Scp173Statue statue(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        Scp173Statue Made = Scp173Registry.Statue.get().create(World);
        if (Made == null) {
            throw new IllegalStateException("SCP-173 could not be created");
        }
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addFreshEntity(Made);
        return Made;
    }

    private static void face(LivingEntity Body, Vec3 Target) {
        Vec3 Eye = Body.getEyePosition();
        double Dx = Target.x - Eye.x;
        double Dy = Target.y - Eye.y;
        double Dz = Target.z - Eye.z;
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        float Pitch = (float) -(Mth.atan2(Dy, Math.sqrt(Dx * Dx + Dz * Dz)) * Mth.RAD_TO_DEG);
        Body.setYRot(Yaw);
        Body.yRotO = Yaw;
        Body.setYHeadRot(Yaw);
        Body.yHeadRotO = Yaw;
        Body.setYBodyRot(Yaw);
        Body.yBodyRotO = Yaw;
        Body.setXRot(Pitch);
        Body.xRotO = Pitch;
    }

    private static Vec3 sightOf(Scp173Statue Statue) {
        return Statue.sightBox().getCenter();
    }

    private static DClassDutyTestPlayer player(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        DClassDutyTestPlayer User = new DClassDutyTestPlayer(World, "dclassroles_test");
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, DClassDutyTestPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static List<BlockPos> residue(GameTestHelper Helper, int... Coordinates) {
        List<BlockPos> Placed = new ArrayList<>();
        for (int Index = 0; Index + 1 < Coordinates.length; Index += 2) {
            BlockPos Spot = Helper.absolutePos(new BlockPos(Coordinates[Index], Ground, Coordinates[Index + 1]));
            if (!Scp173Residue.place(Helper.getLevel(), Spot)) {
                throw new IllegalStateException("Residue could not be placed at " + Spot);
            }
            Placed.add(Spot);
        }
        return Placed;
    }

    private static boolean anyLeft(GameTestHelper Helper, List<BlockPos> Spots) {
        for (BlockPos Spot : Spots) {
            if (DClassCleaning.cleanable(Helper.getLevel().getBlockState(Spot))) {
                return true;
            }
        }
        return false;
    }

    private static int packCount(DClass Subject, ItemStack Kind) {
        int Count = 0;
        for (ItemStack Held : Subject.main()) {
            if (ItemStack.isSameItemSameComponents(Held, Kind)) {
                Count += Held.getCount();
            }
        }
        return Count;
    }

    private static void hunger(DClass Subject, int Food) {
        CompoundTag Saved = new CompoundTag();
        Saved.putInt("foodLevel", Food);
        Saved.putFloat("foodSaturationLevel", 0.0f);
        Subject.food().load(Saved);
    }

    private static String brief(DClass Body) {
        StringBuilder Text = new StringBuilder();
        Text.append(" [").append(Body.getName().getString()).append(" at ").append(Body.blockPosition().toShortString());
        Text.append(" action ").append(Body.action()).append(" duty ").append(DClassDuties.of(Body).Role()).append("/").append(DClassDuties.of(Body).Task());
        Text.append(" blink ").append(DClassGaze.state(Body).blinking()).append(" clock ").append(DClassGaze.state(Body).clock()).append(" watching ").append(DClassGaze.state(Body).watching());
        Text.append(" goals");
        Body.goalSelector.getAvailableGoals().stream().filter(Wrapped -> Wrapped.isRunning()).forEach(Wrapped -> {
            Text.append(" ").append(Wrapped.getGoal().getClass().getSimpleName());
            if (Wrapped.getGoal() instanceof DClassDutyGoals.Janitor Work) {
                Text.append(" target ").append(Work.worker().target()).append(" scrubbed ").append(Work.worker().scrubbed()).append(" walked ").append(Work.worker().walked());
            }
        });
        Text.append("]");
        return Text.toString();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "dclassroles_173KillsUnwatched")
    public static void scp173KillsUnwatchedDClass(GameTestHelper Helper) {
        quiet(Helper);
        Scp173Statue Statue = statue(Helper, 12.5, 16.5);
        DClass Victim = person(Helper, 12.5, 11.5, false, DClassTrait.Stoic);
        face(Victim, Helper.absoluteVec(new Vec3(12.5, 3.6, 2.5)));
        Helper.runAtTickTime(2, () -> Helper.assertTrue(Scp173Sight.personObservers(Helper.getLevel(), Statue).isEmpty(), "a D-Class facing away counts as watching"));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 has not killed the unwatched D-Class yet"))
                .thenExecute(Statue::discard)
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "dclassroles_gazeHolds173")
    public static void dclassGazeHolds173(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 6.5);
        DClass Victim = person(Helper, 12.5, 9.0, false, DClassTrait.Stoic);
        DClass Watcher = person(Helper, 17.5, 13.5, false, DClassTrait.Stoic);
        face(Victim, Helper.absoluteVec(new Vec3(12.5, 3.6, 22.5)));
        face(Watcher, sightOf(Statue));
        DClassGaze.holdEyes(Watcher, Held);
        Vec3 Start = Statue.position();
        boolean[] Blinking = {false};
        Helper.onEachTick(() -> {
            if (Blinking[0] && Victim.isAlive() && Helper.getTick() % 10 == 0 && !DClassGaze.blinking(Watcher)) {
                DClassGaze.blinkNow(Watcher);
            }
        });
        Helper.startSequence()
                .thenExecuteFor(120, () -> {
                    Helper.assertTrue(Victim.isAlive(), "SCP-173 killed a D-Class while another D-Class watched it");
                    Helper.assertTrue(Statue.position().distanceTo(Start) < 1.0e-3, "SCP-173 moved while a D-Class watched it");
                })
                .thenExecute(() -> {
                    Helper.assertTrue(Scp173Sight.isObserved(World, Statue), "the watching D-Class does not hold SCP-173");
                    Helper.assertTrue(Scp173Sight.personObservers(World, Statue).contains(Watcher), "the watching D-Class is not listed as an observer");
                    DClassGaze.blinkNow(Watcher);
                    Helper.assertTrue(!Scp173Sight.isObserved(World, Statue), "SCP-173 is still observed while its only watcher blinks");
                    Blinking[0] = true;
                })
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 never used the watcher's blinks to reach the D-Class"))
                .thenExecute(() -> Helper.assertTrue(Statue.position().distanceTo(Start) > 0.5, "SCP-173 killed without moving"))
                .thenExecute(Statue::discard)
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 2400, batch = "dclassroles_janitorWatches173")
    public static void janitorCleansWhileWatching173(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 19.5);
        DClass Janitor = person(Helper, 12.5, 10.5, true, DClassTrait.Insomniac);
        face(Janitor, sightOf(Statue));
        DClassGaze.holdEyes(Janitor, Held);
        Helper.assertTrue(DClassDuties.assignRole(Janitor, DClassRole.Janitor), "the D-Class refused the janitor role");
        List<BlockPos> Spots = residue(Helper, 9, 12, 15, 11, 12, 14, 10, 8);
        Vec3 Start = Statue.position();
        boolean[] Watched = {false};
        List<String> Trace = new ArrayList<>();
        Helper.onEachTick(() -> {
            boolean Sees = Scp173Sight.personObservers(World, Statue).contains(Janitor);
            if (Sees) {
                Watched[0] = true;
            } else {
                Trace.add("t" + Helper.getTick() + " yaw " + Math.round(Janitor.getYRot()) + " head " + Math.round(Janitor.getYHeadRot()) + " body " + Math.round(Janitor.yBodyRot) + " pitch " + Math.round(Janitor.getXRot()) + brief(Janitor));
                if (Trace.size() > 6) {
                    Trace.remove(0);
                }
            }
            if (Statue.position().distanceTo(Start) > 1.0e-3) {
                Helper.fail("SCP-173 moved while the janitor worked near it at tick " + Helper.getTick() + " unseen " + String.join(" | ", Trace));
            }
        });
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(Janitor.isAlive(), "the janitor died");
            Helper.assertTrue(!anyLeft(Helper, Spots), "the janitor has not cleaned all the residue yet" + brief(Janitor) + " home " + DClassDuties.home(Janitor));
            Helper.assertTrue(Watched[0], "the janitor never watched SCP-173");
            Helper.assertTrue(DClassDuties.role(Janitor) == DClassRole.Janitor, "the janitor lost its role");
        }).thenExecute(Statue::discard).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 800, batch = "dclassroles_janitorPairBlinks")
    public static void janitorPairTakesTurnsBlinking(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 6.5);
        DClass Victim = person(Helper, 12.5, 9.0, false, DClassTrait.Stoic);
        face(Victim, Helper.absoluteVec(new Vec3(12.5, 3.6, 22.5)));
        DClass First = person(Helper, 8.5, 14.5, false, DClassTrait.Stoic);
        DClass Second = person(Helper, 16.5, 14.5, false, DClassTrait.Stoic);
        DClassDuties.assignRole(First, DClassRole.Janitor);
        DClassDuties.assignRole(Second, DClassRole.Janitor);
        face(First, sightOf(Statue));
        face(Second, sightOf(Statue));
        int[] Blinks = {0, 0};
        int[] Last = {0, 0};
        Helper.onEachTick(() -> {
            if (Helper.getTick() < 25 || Helper.getTick() >= 700) {
                return;
            }
            int FirstNow = DClassGaze.state(First).blinking();
            int SecondNow = DClassGaze.state(Second).blinking();
            if (FirstNow == DClassGaze.BlinkTicks && Last[0] != DClassGaze.BlinkTicks) {
                Blinks[0]++;
            }
            if (SecondNow == DClassGaze.BlinkTicks && Last[1] != DClassGaze.BlinkTicks) {
                Blinks[1]++;
            }
            Last[0] = FirstNow;
            Last[1] = SecondNow;
            Helper.assertTrue(FirstNow == 0 || SecondNow == 0, "both janitors blinked at the same time");
            Helper.assertTrue(Scp173Sight.isObserved(World, Statue), "SCP-173 slipped out of sight between the janitors' blinks at tick " + Helper.getTick() + brief(First) + brief(Second)
                    + " sees " + DClassGaze.couldSee(First, Statue.sightBox()) + "/" + DClassGaze.couldSee(Second, Statue.sightBox()) + " statue " + Statue.position());
        });
        Helper.runAtTickTime(700, () -> {
            Helper.assertTrue(Victim.isAlive(), "SCP-173 reached the D-Class while the janitors took turns, last damage " + Victim.getLastDamageSource());
            Helper.assertTrue(Blinks[0] >= 2 && Blinks[1] >= 2, "the janitors did not blink enough to test turn taking: " + Blinks[0] + " and " + Blinks[1]);
            Statue.discard();
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "dclassroles_seeing096")
    public static void dclassSeeing096FaceTriggersIt(GameTestHelper Helper) {
        quiet(Helper);
        Scp096ShyGuy Guy = Helper.spawn(Scp096Registry.ShyGuy.get(), new Vec3(12.5, Floor, 17.5));
        DClass Viewer = person(Helper, 12.5, 9.5, false, DClassTrait.Stoic);
        Helper.onEachTick(() -> {
            if (!Guy.isCalm()) {
                return;
            }
            face(Viewer, Scp096Sight.faceOf(Guy));
            face(Guy, Viewer.getEyePosition());
        });
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(Guy.targetKeys().contains(Viewer.getUUID()), "SCP-096 has not taken the D-Class who saw its face as a target");
            Helper.assertTrue(Guy.isDistressed() || Guy.isRaging(), "SCP-096 was not triggered by the D-Class");
        }).thenExecute(Guy::discard).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "dclassroles_106TakesDClass")
    public static void scp106TargetsLoneDClass(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        DClass Victim = person(Helper, 12.5, 8.5, false, DClassTrait.Stoic);
        Scp106OldMan Old = Helper.spawn(Scp106Registry.OldMan.get(), new Vec3(12.5, Floor, 16.5));
        Helper.assertTrue(Scp106Targeting.rank(World, Victim) == Scp106Targeting.RankPerson, "SCP-106 does not rank a D-Class as a person");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Old.target() == Victim, "SCP-106 has not picked the lone D-Class as its target"))
                .thenWaitUntil(() -> Helper.assertTrue(Victim.getHealth() < Victim.getMaxHealth() || !Victim.isAlive() || Victim.isRemoved() || Victim.level() != World, "SCP-106 has not taken the D-Class: pocket " + (Scp106Pocket.level(World.getServer()) != null) + " health " + Victim.getHealth() + " mode " + Old.mode()
                        + " at " + Old.position() + " target " + Old.target() + " victim at " + Victim.position()))
                .thenWaitUntil(() -> Helper.assertTrue(Scp106Pocket.level(World.getServer()) == null || Victim.isRemoved() || Victim.level() != World,
                        "SCP-106 caught the D-Class but did not take it to the pocket dimension"))
                .thenExecute(Old::erase)
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "dclassroles_managerButtons")
    public static void managerButtonsAssignDuties(GameTestHelper Helper) {
        quiet(Helper);
        DClass Subject = person(Helper, 12.5, 12.5, true, DClassTrait.Insomniac);
        DClassDutyTestPlayer User = player(Helper, 12.5, 4.5);
        DClassManagerMenu Menu = new DClassManagerMenu(1, User.getInventory(), Subject);
        Helper.assertTrue(Menu.stillValid(User), "the duty sheet closed although the D-Class is in reach");
        Helper.assertTrue(Menu.clickMenuButton(User, DClassManagerMenu.RoleButton + DClassRole.Janitor.ordinal()), "the janitor button did nothing");
        DClassDuty Duty = DClassDuties.of(Subject);
        Helper.assertTrue(Duty.Role() == DClassRole.Janitor && Duty.Home().isPresent(), "the janitor role was not stored with a home");
        Helper.assertTrue(DClassDuties.busy(Subject) && DClassDuties.hasRole(Subject) && !DClassDuties.hasTask(Subject), "the duty queries disagree with the janitor role");
        Helper.assertTrue(Menu.role() == DClassRole.Janitor, "the duty sheet does not show the janitor role");
        Helper.assertTrue(Menu.clickMenuButton(User, DClassManagerMenu.CleanButton), "the clean area button did nothing");
        Helper.assertTrue(DClassDuties.task(Subject) == DClassTask.CleanArea && DClassDuties.of(Subject).Spot().isPresent(), "the clean area order was not stored");
        Helper.assertTrue(DClassDuties.role(Subject) == DClassRole.Janitor, "a one off order replaced the role");
        Helper.assertTrue(Menu.clickMenuButton(User, DClassManagerMenu.CancelButton), "the cancel button did nothing");
        Helper.assertTrue(DClassDuties.task(Subject) == DClassTask.None && DClassDuties.role(Subject) == DClassRole.Janitor, "cancel did not clear only the order");
        Menu.getSlot(DClassManagerMenu.MealSlot).set(new ItemStack(Items.COBBLESTONE));
        Helper.assertTrue(!Menu.clickMenuButton(User, DClassManagerMenu.EatButton), "an inedible item was accepted");
        Helper.assertTrue(Menu.meal().isEmpty() && User.getInventory().countItem(Items.COBBLESTONE) == 1, "the inedible item was not handed back");
        Helper.assertTrue(DClassDuties.task(Subject) == DClassTask.None, "an inedible item created an order");
        Helper.assertTrue(Menu.clickMenuButton(User, DClassManagerMenu.RoleButton + DClassRole.None.ordinal()), "the no role button did nothing");
        Helper.assertTrue(!DClassDuties.busy(Subject), "the D-Class still has a duty after clearing its role");
        Helper.assertTrue(Menu.clickMenuButton(User, DClassManagerMenu.FollowButton), "the follow button did nothing");
        Helper.assertTrue(DClassDuties.task(Subject) == DClassTask.FollowMe && DClassDuties.leader(Subject) == User, "the follow order does not lead to the player");
        MtfOperative Operative = operative(Helper, 16.5, 4.5);
        ItemStack Tool = new ItemStack(DClassDutyRegistry.Manager.get());
        Helper.assertTrue(Tool.interactLivingEntity(User, Operative, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "the D-Class Manager opened on an MTF operative");
        Helper.assertTrue(!DClassDuties.busy(Operative) && DClassDuties.role(Operative) == DClassRole.None, "an MTF operative reports a D-Class duty");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Subject.distanceTo(User) < 3.5, "the D-Class is not following the player"))
                .thenExecute(() -> {
                    Helper.assertTrue(DClassDuties.clear(Subject), "clearing a busy D-Class reported nothing to clear");
                    Helper.assertTrue(!DClassDuties.busy(Subject), "the D-Class is still busy after clearing");
                    Menu.removed(User);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "dclassroles_eatFood")
    public static void eatThisFood(GameTestHelper Helper) {
        quiet(Helper);
        DClass Subject = person(Helper, 12.5, 12.5, true, DClassTrait.Insomniac);
        hunger(Subject, 10);
        DClassDutyTestPlayer User = player(Helper, 12.5, 9.5);
        DClassManagerMenu Menu = new DClassManagerMenu(1, User.getInventory(), Subject);
        ItemStack Bread = new ItemStack(Items.BREAD);
        Menu.getSlot(DClassManagerMenu.MealSlot).set(Bread.copy());
        Helper.assertTrue(Menu.clickMenuButton(User, DClassManagerMenu.EatButton), "the eat button refused bread");
        Helper.assertTrue(Menu.meal().isEmpty(), "the bread stayed in the slot");
        Helper.assertTrue(DClassDuties.task(Subject) == DClassTask.EatThis, "no eat order was stored");
        Helper.assertTrue(packCount(Subject, Bread) == 1, "the bread was not moved into the D-Class");
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    Helper.assertTrue(DClassDuties.task(Subject) == DClassTask.None, "the D-Class is still busy eating");
                    Helper.assertTrue(packCount(Subject, Bread) == 0, "the bread was not eaten");
                    Helper.assertTrue(Subject.food().food() > 10, "eating the bread did not feed the D-Class");
                })
                .thenExecute(() -> {
                    Menu.removed(User);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "dclassroles_eatPotion")
    public static void eatThisPotion(GameTestHelper Helper) {
        quiet(Helper);
        DClass Subject = person(Helper, 12.5, 12.5, true, DClassTrait.Insomniac);
        DClassDutyTestPlayer User = player(Helper, 12.5, 9.5);
        DClassManagerMenu Menu = new DClassManagerMenu(1, User.getInventory(), Subject);
        ItemStack Potion = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
        Menu.getSlot(DClassManagerMenu.MealSlot).set(Potion.copy());
        Helper.assertTrue(Menu.clickMenuButton(User, DClassManagerMenu.EatButton), "the eat button refused a potion");
        Helper.assertTrue(DClassDuties.task(Subject) == DClassTask.EatThis, "no drink order was stored");
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    Helper.assertTrue(DClassDuties.task(Subject) == DClassTask.None, "the D-Class is still drinking");
                    Helper.assertTrue(Subject.hasEffect(MobEffects.MOVEMENT_SPEED), "the potion had no effect on the D-Class, holding " + Subject.getMainHandItem() + " with " + Subject.getActiveEffects());
                    Helper.assertTrue(packCount(Subject, new ItemStack(Items.GLASS_BOTTLE)) == 1, "the empty bottle was not kept");
                })
                .thenExecute(() -> {
                    Menu.removed(User);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "dclassroles_cleanArea")
    public static void cleanAreaRemovesResidue(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        DClass Subject = person(Helper, 12.5, 12.5, true, DClassTrait.Insomniac);
        Scp173ResidueIndex Index = Scp173ResidueIndex.get(World);
        int Before = Index.total();
        List<BlockPos> Spots = residue(Helper, 9, 10, 15, 14, 11, 16, 16, 9);
        List<BlockPos> Far = residue(Helper, 23, 23);
        Helper.assertTrue(Index.total() == Before + Spots.size() + Far.size(), "placing residue did not update the index");
        BlockPos Centre = Helper.absolutePos(new BlockPos(12, Ground, 12));
        Helper.assertTrue(DClassDuties.orderClean(Subject, null), "the clean area order was refused");
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    Helper.assertTrue(DClassDuties.task(Subject) == DClassTask.None, "the D-Class is still cleaning");
                    Helper.assertTrue(!anyLeft(Helper, Spots), "residue in the area is left");
                })
                .thenExecute(() -> {
                    Helper.assertTrue(Index.total() == Before + Far.size(), "the residue index still counts cleaned residue: " + Index.total() + " vs " + (Before + Far.size()));
                    Helper.assertTrue(anyLeft(Helper, Far), "residue outside the area was cleaned");
                    Helper.assertTrue(Scp173Residue.find(World, Centre, 8, 3).isEmpty(), "the residue index still lists residue in the area");
                    Helper.getLevel().removeBlock(Far.get(0), false);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "dclassroles_173KillsOperative")
    public static void scp173KillsUnwatchedOperative(GameTestHelper Helper) {
        quiet(Helper);
        Scp173Statue Statue = statue(Helper, 12.5, 16.5);
        MtfOperative Victim = operative(Helper, 12.5, 11.5);
        face(Victim, Helper.absoluteVec(new Vec3(12.5, 3.6, 2.5)));
        Helper.runAtTickTime(2, () -> Helper.assertTrue(Scp173Sight.personObservers(Helper.getLevel(), Statue).isEmpty(), "an operative facing away counts as watching"));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Victim.isAlive(), "SCP-173 has not killed the unwatched operative yet"))
                .thenExecute(Statue::discard)
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "dclassroles_operativeHolds173")
    public static void operativeGazeHolds173(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        Scp173Statue Statue = statue(Helper, 12.5, 6.5);
        DClass Victim = person(Helper, 12.5, 9.0, false, DClassTrait.Stoic);
        MtfOperative Watcher = operative(Helper, 17.5, 13.5);
        face(Victim, Helper.absoluteVec(new Vec3(12.5, 3.6, 22.5)));
        face(Watcher, sightOf(Statue));
        DClassGaze.holdEyes(Watcher, Held);
        Vec3 Start = Statue.position();
        Helper.startSequence()
                .thenExecuteFor(OperativeHold, () -> {
                    Helper.assertTrue(Victim.isAlive(), "SCP-173 killed a D-Class while an operative watched it");
                    Helper.assertTrue(Statue.position().distanceTo(Start) < 1.0e-3, "SCP-173 moved while an operative watched it at tick " + Helper.getTick() + " head " + Watcher.getYHeadRot()
                            + " yaw " + Watcher.getYRot() + " pitch " + Watcher.getXRot() + " watcher " + DClassGaze.watcher(Watcher) + " sees " + DClassGaze.couldSee(Watcher, Statue.sightBox())
                            + " shut " + Watcher.eyesShut() + " blink " + DClassGaze.blinking(Watcher) + " alive " + Watcher.isAlive() + " at " + Watcher.position() + " statue " + Statue.position());
                })
                .thenExecute(() -> {
                    Helper.assertTrue(Scp173Sight.personObservers(World, Statue).contains(Watcher), "the operative is not listed as an observer");
                    DClassGaze.blinkNow(Watcher);
                    Helper.assertTrue(!Scp173Sight.isObserved(World, Statue), "SCP-173 is still observed while the operative blinks");
                    Statue.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "dclassroles_operativeSees096")
    public static void operativeSeeing096FaceTriggersIt(GameTestHelper Helper) {
        quiet(Helper);
        Scp096ShyGuy Guy = Helper.spawn(Scp096Registry.ShyGuy.get(), new Vec3(12.5, Floor, 17.5));
        MtfOperative Viewer = operative(Helper, 12.5, 9.5);
        Helper.onEachTick(() -> {
            if (!Guy.isCalm()) {
                return;
            }
            face(Viewer, Scp096Sight.faceOf(Guy));
            face(Guy, Viewer.getEyePosition());
        });
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(Guy.targetKeys().contains(Viewer.getUUID()), "SCP-096 has not taken the operative who saw its face as a target");
            Helper.assertTrue(Guy.isDistressed() || Guy.isRaging(), "SCP-096 was not triggered by the operative");
        }).thenExecute(Guy::discard).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "dclassroles_106HuntsOperative")
    public static void scp106TargetsLoneOperative(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        MtfOperative Victim = operative(Helper, 12.5, 8.5);
        Scp106OldMan Old = Helper.spawn(Scp106Registry.OldMan.get(), new Vec3(12.5, Floor, 16.5));
        Helper.assertTrue(Scp106Targeting.rank(World, Victim) == Scp106Targeting.RankPerson, "SCP-106 does not rank an operative as a person");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Old.target() == Victim, "SCP-106 has not picked the lone operative as its target"))
                .thenExecute(Old::erase)
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "dclassroles_saveReload")
    public static void dutiesSurviveReload(GameTestHelper Helper) {
        quiet(Helper);
        ServerLevel World = Helper.getLevel();
        DClass Subject = person(Helper, 12.5, 12.5, false, DClassTrait.Stoic);
        DClassDuties.assignRole(Subject, DClassRole.Janitor);
        DClassDuties.orderClean(Subject, null);
        DClassDuty Before = DClassDuties.of(Subject);
        CompoundTag Saved = Subject.saveWithoutId(new CompoundTag());
        DClass Copy = DClassRegistry.DClassType.get().create(World);
        if (Copy == null) {
            throw new IllegalStateException("a D-Class could not be created");
        }
        Copy.load(Saved);
        DClassDuty After = DClassDuties.of(Copy);
        Helper.assertTrue(After.Role() == DClassRole.Janitor, "the janitor role was lost on reload");
        Helper.assertTrue(After.Home().equals(Before.Home()), "the janitor home moved on reload");
        Helper.assertTrue(After.Task() == DClassTask.CleanArea && After.Spot().equals(Before.Spot()), "the clean area order was lost on reload");
        CompoundTag Carried = BodyAttachments.capture(Subject);
        DClass Body = DClassRegistry.DClassType.get().create(World);
        if (Body == null) {
            throw new IllegalStateException("a D-Class could not be created");
        }
        BodyAttachments.apply(Body, Carried);
        Helper.assertTrue(DClassDuties.role(Body) == DClassRole.Janitor && DClassDuties.task(Body) == DClassTask.CleanArea, "the duty did not travel with the body through control");
        DClassDuties.clear(Subject);
        Helper.assertTrue(!Subject.saveWithoutId(new CompoundTag()).toString().contains("dclass_duty"), "an idle D-Class still saves a duty");
        Helper.succeed();
    }
}
