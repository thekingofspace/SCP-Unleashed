package net.rai.scp_unleashed.scp492.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.scp492.Scp492Figure;
import net.rai.scp_unleashed.scp492.Scp492OutskirtsCondition;
import net.rai.scp_unleashed.scp492.Scp492Registry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp492GameTests {
    public static final String Arena = "scp492_arena";
    public static final double Floor = 2.0;

    private Scp492GameTests() {
    }

    private static Scp492Figure spawn(GameTestHelper Helper, double X, double Z) {
        Scp492Figure Figure = Helper.spawn(Scp492Registry.Figure.get(), new Vec3(X, Floor, Z));
        Figure.setPersistenceRequired();
        return Figure;
    }

    private static ServerPlayer person(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        whenDone(Helper, () -> {
            if (!User.isRemoved()) {
                World.removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        });
        return User;
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

    private static void moveTo(GameTestHelper Helper, ServerPlayer User, double X, double Z) {
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
    }

    private static void tidy(GameTestHelper Helper, List<? extends Entity> Made, ServerPlayer... Users) {
        for (Entity Each : Made) {
            Each.discard();
        }
        for (ServerPlayer User : Users) {
            if (!User.isRemoved()) {
                Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private static String state(Scp492Figure Figure) {
        return " [tame " + Figure.isTame() + ", torn " + Figure.isTorn() + ", afraid " + Figure.isAfraid() + ", fire " + Figure.isOnFire() + ", action " + Figure.action()
                + ", health " + Figure.getHealth() + ", sitting " + Figure.isOrderedToSit() + ", at " + Figure.position() + "]";
    }

    private static void befriend(Scp492Figure Figure, ServerPlayer User) {
        Figure.tame(User);
        Figure.setOrderedToSit(false);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp492_arrival")
    public static void arrivalPlacesAWaryFigure(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Reader = person(Helper, 16.5, 16.5, "scp492_reader");
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp492Registry.DocumentId);
        Helper.assertTrue(Document != null, "the scp_492 dossier did not load");
        Optional<BlockPos> Found = Scp492OutskirtsCondition.findSpot(World, Reader, 6, 12, false, false, World.getRandom());
        Helper.assertTrue(Found.isPresent(), "the arrival found no spot on open ground");
        BlockPos Spot = Found.get();
        double Away = Vec3.atBottomCenterOf(Spot).distanceTo(Reader.position());
        Helper.assertTrue(Away >= 5.0 && Away <= 13.5, "the arrival spot is " + Away + " blocks from the reader");
        boolean Placed = Scp492OutskirtsCondition.manifest(new AnomalySpawners.Discovery(World, Spot, Reader, Scp492Registry.DocumentId, Document, true));
        Helper.assertTrue(Placed, "the arrival placed nothing");
        List<Scp492Figure> Figures = World.getEntitiesOfClass(Scp492Figure.class, new AABB(Spot).inflate(2.0));
        Helper.assertTrue(Figures.size() == 1, "the arrival placed " + Figures.size() + " figures");
        Scp492Figure Figure = Figures.get(0);
        Helper.assertTrue(!Figure.isTame(), "the figure arrived already befriended" + state(Figure));
        Helper.assertTrue(Figure.isPersistenceRequired(), "the arriving figure could despawn" + state(Figure));
        Helper.assertTrue(Reader.getUUID().equals(Figure.sought()), "the figure is not looking for its reader");
        tidy(Helper, Figures, Reader);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp492_curious")
    public static void waryFigureComesCloser(GameTestHelper Helper) {
        Scp492Figure Figure = spawn(Helper, 4.5, 16.5);
        ServerPlayer User = person(Helper, 26.5, 16.5, "scp492_watcher");
        double Start = Figure.distanceTo(User);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Figure.distanceTo(User) < Start - 8.0, "the wary figure did not come closer (" + Figure.distanceTo(User) + " of " + Start + ")" + state(Figure));
            Helper.assertTrue(Figure.distanceTo(User) > 2.5, "the wary figure walked right up to a stranger");
            tidy(Helper, List.of(Figure), User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp492_persuade")
    public static void emptyHandsWinItOver(GameTestHelper Helper) {
        Scp492Figure Figure = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp492_agent");
        User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Figure.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Figure.isTame(), "empty hands did not win the figure over" + state(Figure));
        Helper.assertTrue(User.getUUID().equals(Figure.getOwnerUUID()), "the figure trusts the wrong person");
        Helper.assertTrue(Figure.action() == Scp492Figure.ActionTip, "the figure did not tip its hat to its new friend" + state(Figure));
        tidy(Helper, List.of(Figure), User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp492_flame")
    public static void aFlameInHandScaresItOff(GameTestHelper Helper) {
        Scp492Figure Figure = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp492_torchbearer");
        User.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
        Figure.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(!Figure.isTame(), "the figure trusted someone holding a torch" + state(Figure));
        Helper.assertTrue(Figure.fearSource() != null, "a torch close by did not frighten the figure" + state(Figure));
        tidy(Helper, List.of(Figure), User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp492_grudge")
    public static void itRemembersWhoHitIt(GameTestHelper Helper) {
        Scp492Figure Figure = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp492_bully");
        Figure.hurt(Helper.getLevel().damageSources().playerAttack(User), 1.0f);
        Helper.assertTrue(Figure.holdsGrudge(User), "the figure forgot being hit straight away");
        User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Figure.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(!Figure.isTame(), "the figure befriended the person who just hit it" + state(Figure));
        tidy(Helper, List.of(Figure), User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp492_fear")
    public static void fireSendsItRunning(GameTestHelper Helper) {
        Helper.setBlock(new BlockPos(14, 2, 16), Blocks.CAMPFIRE.defaultBlockState());
        Scp492Figure Figure = spawn(Helper, 16.5, 16.5);
        Vec3 Fire = Helper.absoluteVec(new Vec3(14.5, Floor, 16.5));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Figure.position().distanceTo(Fire) > 6.0, "the figure stayed by the campfire (" + Figure.position().distanceTo(Fire) + ")" + state(Figure));
            Helper.assertTrue(!Figure.isOnFire(), "the figure caught fire while fleeing" + state(Figure));
            tidy(Helper, List.of(Figure));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp492_douse")
    public static void burningFigureRunsForWater(GameTestHelper Helper) {
        for (int X = 19; X <= 20; X++) {
            for (int Z = 16; Z <= 17; Z++) {
                Helper.setBlock(new BlockPos(X, 1, Z), Blocks.WATER.defaultBlockState());
            }
        }
        Scp492Figure Figure = spawn(Helper, 16.5, 16.5);
        Figure.setRemainingFireTicks(160);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Figure.isAlive(), "the figure burned up before it found water" + state(Figure));
            Helper.assertTrue(!Figure.isOnFire(), "the figure is still burning" + state(Figure));
            Helper.assertTrue(Figure.isInWater(), "the fire went out, but not in the water" + state(Figure));
            tidy(Helper, List.of(Figure));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp492_fireDamage")
    public static void fireHurtsTwiceOver(GameTestHelper Helper) {
        Scp492Figure Figure = spawn(Helper, 8.5, 8.5);
        float Before = Figure.getHealth();
        Figure.hurt(Helper.getLevel().damageSources().onFire(), 1.0f);
        float Lost = Before - Figure.getHealth();
        Helper.assertTrue(Math.abs(Lost - Scp492Figure.FireScale) < 0.01f, "one point of fire took " + Lost + " health" + state(Figure));
        Figure.invulnerableTime = 0;
        Figure.hurt(Helper.getLevel().damageSources().lava(), 40.0f);
        Helper.assertTrue(Figure.isDeadOrDying(), "lava did not burn the figure up" + state(Figure));
        Helper.assertTrue(!Figure.isTorn(), "a burnt figure was left torn instead" + state(Figure));
        tidy(Helper, List.of(Figure));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp492_torn")
    public static void bluntForceOnlyKnocksItFlat(GameTestHelper Helper) {
        Scp492Figure Figure = spawn(Helper, 8.5, 8.5);
        Figure.hurt(Helper.getLevel().damageSources().generic(), 200.0f);
        Helper.assertTrue(Figure.isAlive(), "blunt force killed the figure" + state(Figure));
        Helper.assertTrue(Figure.isTorn(), "a crushing blow did not leave the figure torn" + state(Figure));
        Helper.assertTrue(Math.abs(Figure.getHealth() - Scp492Figure.TornHealth) < 0.01f, "a torn figure kept " + Figure.getHealth() + " health");
        Figure.invulnerableTime = 0;
        Helper.assertTrue(!Figure.hurt(Helper.getLevel().damageSources().generic(), 50.0f), "a torn figure could still be hurt by blows");
        Vec3 Lying = Figure.position();
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Figure.isAlive() && Figure.isTorn(), "the torn figure did not stay down" + state(Figure));
            Helper.assertTrue(Figure.position().distanceTo(Lying) < 0.2, "the torn figure wandered off" + state(Figure));
            tidy(Helper, List.of(Figure));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp492_sew")
    public static void stringMendsItsSeams(GameTestHelper Helper) {
        Scp492Figure Figure = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp492_tailor");
        Figure.hurt(Helper.getLevel().damageSources().generic(), 200.0f);
        Helper.assertTrue(Figure.isTorn(), "the figure was not torn before sewing");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STRING, 5));
        Figure.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Figure.isTorn(), "one stitch was enough to stand back up" + state(Figure));
        Helper.assertTrue(User.getMainHandItem().getCount() == 4, "sewing did not use up the string");
        Figure.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(!Figure.isTorn(), "two stitches did not mend the figure" + state(Figure));
        Helper.assertTrue(Figure.getHealth() >= Scp492Figure.RiseHealth, "the mended figure has only " + Figure.getHealth() + " health");
        Helper.assertTrue(Figure.action() == Scp492Figure.ActionRise, "the mended figure did not get back up" + state(Figure));
        Helper.assertTrue(User.getMainHandItem().getCount() == 3, "the second stitch did not use up string");
        tidy(Helper, List.of(Figure), User);
        Helper.succeed();
    }

    private static boolean upright(Scp492Figure Figure) {
        byte Action = Figure.action();
        return Action == Scp492Figure.ActionNone || Action == Scp492Figure.ActionTip || Action == Scp492Figure.ActionPoint;
    }

    private static void grounded(GameTestHelper Helper, Scp492Figure Figure, String When) {
        double Floor0 = Helper.absoluteVec(new Vec3(0.0, Floor, 0.0)).y;
        Helper.assertTrue(Figure.onGround(), When + ": the figure is not on the ground" + state(Figure));
        Helper.assertTrue(Math.abs(Figure.getY() - Floor0) < 0.01, When + ": the figure stands at " + Figure.getY() + " instead of the floor top " + Floor0 + state(Figure));
        Helper.assertTrue(!Figure.isInWater(), When + ": the figure is in water" + state(Figure));
        Helper.assertTrue(!Figure.isNoGravity(), When + ": the figure ignores gravity" + state(Figure));
        Helper.assertTrue(Figure.getPose() == Pose.STANDING, When + ": the figure is in pose " + Figure.getPose());
        Helper.assertTrue(Math.abs(Figure.getBbHeight() - Scp492Figure.Height) < 0.001 && Math.abs(Figure.getBbWidth() - Scp492Figure.Width) < 0.001,
                When + ": the figure is " + Figure.getBbWidth() + " by " + Figure.getBbHeight() + " instead of its standing size");
        Helper.assertTrue(Math.abs(Figure.getDeltaMovement().y) < 0.1, When + ": the figure is still moving vertically " + Figure.getDeltaMovement());
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp492_riseGround")
    public static void mendedFigureStandsOnTheGround(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp492Figure Figure = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp492_mender");
        Figure.hurt(World.damageSources().generic(), 200.0f);
        Helper.assertTrue(Figure.isTorn(), "the figure was not torn");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STRING, 4));
        Figure.mobInteract(User, InteractionHand.MAIN_HAND);
        Figure.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(!Figure.isTorn() && Figure.action() == Scp492Figure.ActionRise, "sewing to the threshold did not start the rise" + state(Figure));
        Helper.runAfterDelay(Scp492Figure.RiseTicks + 20, () -> {
            Helper.assertTrue(!Figure.isTorn() && upright(Figure), "the rise never finished" + state(Figure));
            grounded(Helper, Figure, "after rising");
            CompoundTag Saved = new CompoundTag();
            Helper.assertTrue(Figure.save(Saved), "the risen figure refused to save");
            Optional<Entity> Copy = EntityType.create(Saved, World);
            Helper.assertTrue(Copy.isPresent() && Copy.get() instanceof Scp492Figure, "the risen figure could not be rebuilt");
            Scp492Figure Loaded = (Scp492Figure) Copy.get();
            Helper.assertTrue(!Loaded.isTorn() && Loaded.action() == Scp492Figure.ActionNone && !Loaded.isInSittingPose() && Loaded.getPose() == Pose.STANDING,
                    "the risen figure came back from a reload in the wrong state" + state(Loaded));
            Loaded.discard();
            tidy(Helper, List.of(Figure), User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp492_sitGround")
    public static void sittingFigureRestsOnTheGround(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Owner = person(Helper, 6.5, 8.5, "scp492_sitter");
        Scp492Figure Figure = spawn(Helper, 9.5, 8.5);
        befriend(Figure, Owner);
        Owner.setShiftKeyDown(true);
        Owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Figure.mobInteract(Owner, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Figure.isInSittingPose() && Figure.action() == Scp492Figure.ActionSitDown, "the figure did not start sitting down" + state(Figure));
        Helper.runAfterDelay(Scp492Figure.SitDownTicks + 40, () -> {
            Helper.assertTrue(Figure.isInSittingPose() && Figure.isOrderedToSit(), "the figure did not stay seated" + state(Figure));
            Helper.assertTrue(Figure.action() == Scp492Figure.ActionNone, "the seated figure is stuck in action " + Figure.action() + state(Figure));
            grounded(Helper, Figure, "while sitting");
            CompoundTag Saved = new CompoundTag();
            Helper.assertTrue(Figure.save(Saved), "the seated figure refused to save");
            Optional<Entity> Copy = EntityType.create(Saved, World);
            Helper.assertTrue(Copy.isPresent() && Copy.get() instanceof Scp492Figure, "the seated figure could not be rebuilt");
            Scp492Figure Loaded = (Scp492Figure) Copy.get();
            Helper.assertTrue(Loaded.isOrderedToSit() && Loaded.isInSittingPose() && Loaded.action() == Scp492Figure.ActionNone,
                    "the seated figure came back from a reload in the wrong state" + state(Loaded));
            Loaded.discard();
            Figure.mobInteract(Owner, InteractionHand.MAIN_HAND);
            Helper.assertTrue(!Figure.isInSittingPose() && Figure.action() == Scp492Figure.ActionStandUp, "the figure did not start standing up" + state(Figure));
        });
        Helper.runAfterDelay(Scp492Figure.SitDownTicks + 40 + Scp492Figure.StandUpTicks + 20, () -> {
            Helper.assertTrue(!Figure.isInSittingPose() && upright(Figure), "the figure did not finish standing up" + state(Figure));
            grounded(Helper, Figure, "after standing up");
            tidy(Helper, List.of(Figure), Owner);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp492_slump")
    public static void tornWhileSittingSlumpsOver(GameTestHelper Helper) {
        ServerPlayer Owner = person(Helper, 6.5, 12.5, "scp492_slumper");
        Scp492Figure Figure = spawn(Helper, 9.5, 12.5);
        befriend(Figure, Owner);
        Figure.setOrderedToSit(true);
        Figure.setInSittingPose(true);
        Figure.hurt(Helper.getLevel().damageSources().generic(), 200.0f);
        Helper.assertTrue(Figure.isTorn() && Figure.action() == Scp492Figure.ActionSlump, "a figure torn while sitting did not slump over" + state(Figure));
        Helper.assertTrue(!Figure.isInSittingPose() && !Figure.isOrderedToSit(), "the torn figure is still sitting" + state(Figure));
        Helper.runAfterDelay(Scp492Figure.SlumpTicks + 20, () -> {
            Helper.assertTrue(Figure.isTorn() && Figure.action() == Scp492Figure.ActionNone, "the slumped figure did not settle into a heap" + state(Figure));
            grounded(Helper, Figure, "after slumping");
            tidy(Helper, List.of(Figure), Owner);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp492_noOrgans")
    public static void noOrgansNoAilments(GameTestHelper Helper) {
        Scp492Figure Figure = spawn(Helper, 8.5, 8.5);
        Helper.assertTrue(!Figure.addEffect(new MobEffectInstance(MobEffects.POISON, 200)), "poison took hold of the figure");
        Helper.assertTrue(!Figure.addEffect(new MobEffectInstance(MobEffects.WITHER, 200)), "wither took hold of the figure");
        Helper.assertTrue(Figure.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40)), "a harmless glow would not stick to the figure");
        Helper.assertTrue(!Figure.canDrownInFluidType(NeoForgeMod.WATER_TYPE.value()), "the figure can drown");
        Vec3 High = Helper.absoluteVec(new Vec3(20.5, Floor + 6.0, 20.5));
        Scp492Figure Faller = spawn(Helper, 20.5, 20.5);
        Faller.moveTo(High.x, High.y, High.z);
        float Before = Faller.getHealth();
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Faller.onGround(), "the dropped figure has not landed");
            Helper.assertTrue(Faller.getHealth() >= Before, "the fall hurt the figure" + state(Faller));
            Helper.assertTrue(!Figure.hasEffect(MobEffects.POISON), "poison is on the figure");
            tidy(Helper, List.of(Figure, Faller));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp492_lookout")
    public static void lookoutPointsOutDanger(GameTestHelper Helper) {
        ServerPlayer Owner = person(Helper, 6.5, 10.5, "scp492_captain");
        Scp492Figure Figure = spawn(Helper, 12.5, 10.5);
        befriend(Figure, Owner);
        Spider Raider = Helper.spawn(EntityType.SPIDER, new Vec3(15.5, Floor, 10.5));
        Raider.setNoAi(true);
        Raider.setInvulnerable(true);
        Raider.setPersistenceRequired();
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Raider.hasEffect(MobEffects.GLOWING), "the spider was never pointed out" + state(Figure));
            Helper.assertTrue(Figure.action() == Scp492Figure.ActionPoint, "the figure did not point at the spider" + state(Figure));
            Helper.assertTrue(Figure.spotted() == Raider, "the figure pointed at the wrong thing");
            tidy(Helper, List.of(Figure, Raider), Owner);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp492_greet")
    public static void itTipsItsHatOnce(GameTestHelper Helper) {
        Scp492Figure Figure = spawn(Helper, 10.5, 10.5);
        ServerPlayer User = person(Helper, 12.5, 10.5, "scp492_visitor");
        Figure.setNoAi(false);
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Figure.greetedCount() == 1, "the figure did not greet the visitor" + state(Figure));
            Helper.assertTrue(Figure.action() == Scp492Figure.ActionTip, "the greeting was not a tip of the hat" + state(Figure));
        });
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(Figure.action() == Scp492Figure.ActionNone, "the figure kept greeting the same visitor" + state(Figure));
            Helper.assertTrue(Figure.greetedCount() == 1, "the figure greeted more than once");
            tidy(Helper, List.of(Figure), User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp492_sit")
    public static void standWatchWhenTold(GameTestHelper Helper) {
        ServerPlayer Owner = person(Helper, 8.5, 8.5, "scp492_keeper");
        Scp492Figure Figure = spawn(Helper, 10.5, 8.5);
        befriend(Figure, Owner);
        Owner.setShiftKeyDown(true);
        Owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Figure.mobInteract(Owner, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Figure.isOrderedToSit(), "sneaking and asking did not make the figure stay" + state(Figure));
        Vec3 Post = Figure.position();
        moveTo(Helper, Owner, 22.5, 22.5);
        Owner.setShiftKeyDown(false);
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Figure.position().distanceTo(Post) < 0.6, "the figure left its post" + state(Figure));
            Owner.setShiftKeyDown(true);
            Figure.mobInteract(Owner, InteractionHand.MAIN_HAND);
            Helper.assertTrue(!Figure.isOrderedToSit(), "asking again did not release the figure" + state(Figure));
            tidy(Helper, List.of(Figure), Owner);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp492_follow")
    public static void crewmateFollows(GameTestHelper Helper) {
        ServerPlayer Owner = person(Helper, 6.5, 6.5, "scp492_skipper");
        Scp492Figure Figure = spawn(Helper, 7.5, 6.5);
        befriend(Figure, Owner);
        moveTo(Helper, Owner, 16.5, 14.5);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Figure.distanceTo(Owner) < 4.5, "the crewmate did not follow (" + Figure.distanceTo(Owner) + ")" + state(Figure));
            tidy(Helper, List.of(Figure), Owner);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp492_reload")
    public static void stateSurvivesReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Owner = person(Helper, 6.5, 6.5, "scp492_saver");
        Scp492Figure Figure = spawn(Helper, 8.5, 6.5);
        befriend(Figure, Owner);
        Figure.hurt(World.damageSources().playerAttack(Owner), 200.0f);
        Helper.assertTrue(Figure.isTorn(), "the figure was not torn before saving");
        CompoundTag Saved = new CompoundTag();
        Helper.assertTrue(Figure.save(Saved), "the figure refused to save");
        Optional<Entity> Copy = EntityType.create(Saved, World);
        Helper.assertTrue(Copy.isPresent() && Copy.get() instanceof Scp492Figure, "the figure could not be rebuilt");
        Scp492Figure Loaded = (Scp492Figure) Copy.get();
        Helper.assertTrue(Loaded.isTorn(), "the torn state was lost on reload" + state(Loaded));
        Helper.assertTrue(Loaded.isTame() && Owner.getUUID().equals(Loaded.getOwnerUUID()), "the friendship was lost on reload" + state(Loaded));
        Helper.assertTrue(Loaded.holdsGrudge(Owner), "the grudge was lost on reload");
        Helper.assertTrue(Loaded.isPersistenceRequired(), "the reloaded figure could despawn");
        Loaded.discard();
        tidy(Helper, List.of(Figure), Owner);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp492_document")
    public static void documentIsSafe(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp492Registry.DocumentId);
        Helper.assertTrue(Document != null, "the scp_492 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Safe, "SCP-492 is not filed as Safe");
        Helper.assertTrue(Document.Danger() == 1, "SCP-492 has threat tier " + Document.Danger());
        Helper.assertTrue(Document.Condition() instanceof Scp492OutskirtsCondition, "SCP-492 does not arrive from the outskirts");
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp492Registry.DocumentId).isPresent(), "SCP-492 has no spawner");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp492_egg")
    public static void spawnEggMakesAFigure(GameTestHelper Helper) {
        Helper.assertTrue(Scp492Registry.Egg.get().getType(new ItemStack(Scp492Registry.Egg.get())) == Scp492Registry.Figure.get(), "the spawn egg makes the wrong entity");
        Scp492Figure Figure = spawn(Helper, 8.5, 8.5);
        Helper.assertTrue(Figure.getBbHeight() < 0.8f && Figure.getBbHeight() > 0.5f, "the figure is " + Figure.getBbHeight() + " blocks tall");
        Helper.assertTrue(!Figure.removeWhenFarAway(128.0), "the figure would despawn");
        tidy(Helper, List.of(Figure));
        Helper.succeed();
    }
}
