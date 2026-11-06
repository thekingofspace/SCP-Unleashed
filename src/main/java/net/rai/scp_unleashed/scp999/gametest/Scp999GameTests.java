package net.rai.scp_unleashed.scp999.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp999.PinkDogBowlBlock;
import net.rai.scp_unleashed.scp999.Scp999ArrivalCondition;
import net.rai.scp_unleashed.scp999.Scp999Blob;
import net.rai.scp_unleashed.scp999.Scp999Danger;
import net.rai.scp_unleashed.scp999.Scp999Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp999GameTests {
    public static final String Arena = "scp999_arena";
    public static final double Floor = 2.0;

    private Scp999GameTests() {
    }

    private static Scp999Blob spawn(GameTestHelper Helper, double X, double Z) {
        Scp999Blob Blob = Helper.spawn(Scp999Registry.Blob.get(), new Vec3(X, Floor, Z));
        Blob.setPersistenceRequired();
        return Blob;
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

    private static String state(Scp999Blob Blob) {
        return " [action " + Blob.action() + ", mood " + Blob.mood() + ", meals " + Blob.meals() + ", health " + Blob.getHealth() + ", at " + Blob.position() + "]";
    }

    private static BlockHitResult hit(GameTestHelper Helper, BlockPos Relative) {
        BlockPos Pos = Helper.absolutePos(Relative);
        return new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false);
    }

    private static BlockPos bowl(GameTestHelper Helper, BlockPos Relative, int Candy) {
        Helper.setBlock(Relative, Scp999Registry.Bowl.get().defaultBlockState().setValue(PinkDogBowlBlock.Candy, Candy));
        return Helper.absolutePos(Relative);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp999_arrival")
    public static void scp999ArrivalSendsItToTheReader(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Reader = person(Helper, 16.5, 16.5, "scp999_reader");
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp999Registry.DocumentId);
        Helper.assertTrue(Document != null, "the scp_999 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Safe, "SCP-999 is not filed as Safe");
        Optional<BlockPos> Found = Scp999ArrivalCondition.findSpot(World, Reader, 6, 12, false, false, World.getRandom());
        Helper.assertTrue(Found.isPresent(), "the arrival found no spot on open ground");
        BlockPos Spot = Found.get();
        boolean Placed = Scp999ArrivalCondition.manifest(new AnomalySpawners.Discovery(World, Spot, Reader, Scp999Registry.DocumentId, Document, true));
        Helper.assertTrue(Placed, "the arrival placed nothing");
        List<Scp999Blob> Blobs = World.getEntitiesOfClass(Scp999Blob.class, new AABB(Spot).inflate(2.0));
        Helper.assertTrue(Blobs.size() == 1, "the arrival placed " + Blobs.size() + " blobs");
        Scp999Blob Blob = Blobs.get(0);
        Helper.assertTrue(Blob.isPersistenceRequired(), "the arriving blob could despawn");
        Helper.assertTrue(Reader.getUUID().equals(Blob.sought()), "the blob is not heading for its reader");
        tidy(Helper, Blobs, Reader);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp999_seek")
    public static void scp999HopsOverToGreetItsReader(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 6.5, 16.5);
        ServerPlayer Reader = person(Helper, 22.5, 16.5, "scp999_greeted");
        Blob.seek(Reader.getUUID());
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Blob.sought() == null, "the blob never reached its reader (" + Blob.distanceTo(Reader) + ")" + state(Blob));
            Helper.assertTrue(Blob.distanceTo(Reader) < 4.0, "the blob greeted from too far away" + state(Blob));
            Helper.assertTrue(Blob.affection(Reader.getUUID()) > 0, "the arrival greeting left no fondness");
            tidy(Helper, List.of(Blob), Reader);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp999_pet")
    public static void scp999PettingSquashesItHappily(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp999_petter");
        User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult Result = Blob.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Result.consumesAction(), "petting was not accepted");
        Helper.assertTrue(Blob.action() == Scp999Blob.ActionPet, "an empty hand did not start the petting squash" + state(Blob));
        Helper.assertTrue(Blob.affection(User.getUUID()) == Scp999Blob.PetAffection, "petting did not make it fonder");
        Blob.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Blob.affection(User.getUUID()) == Scp999Blob.PetAffection, "a second pat right away counted again");
        Helper.runAfterDelay(Scp999Blob.PetTicks + 2, () -> {
            Helper.assertTrue(Blob.action() != Scp999Blob.ActionPet, "the petting squash never sprang back" + state(Blob));
            tidy(Helper, List.of(Blob), User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp999_wake")
    public static void scp999PettingWakesANapAndItGetsCurious(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 16.5, 8.5, "scp999_waker");
        Blob.nap(Helper.getLevel(), 600);
        Helper.assertTrue(Blob.isNapping(), "the blob did not settle into a nap" + state(Blob));
        User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Blob.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(!Blob.isNapping() && Blob.action() == Scp999Blob.ActionWake, "a pat did not wake the napping blob" + state(Blob));
        boolean[] Curious = new boolean[1];
        Helper.onEachTick(() -> {
            if (Blob.curiosity() > 0 && User.getUUID().equals(Blob.curiousAbout())) {
                Curious[0] = true;
            }
        });
        Helper.runAfterDelay(Scp999Blob.WakeTicks + 6, () -> {
            Helper.assertTrue(Curious[0], "the blob woke up without any curiosity" + state(Blob));
            tidy(Helper, List.of(Blob), User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp999_feed")
    public static void scp999EatsCandyFromAHand(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 10.0, 8.5, "scp999_feeder");
        Blob.setHealth(10.0f);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp999Registry.Candy.get(), 3));
        Blob.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Blob.action() == Scp999Blob.ActionEat, "held candy did not start a meal" + state(Blob));
        Helper.assertTrue(User.getMainHandItem().getCount() == 2, "feeding did not use up one serving");
        Helper.assertTrue(Blob.affection(User.getUUID()) == Scp999Blob.FeedAffection, "feeding did not make it fonder");
        Helper.runAfterDelay(Scp999Blob.BiteTick + 2, () -> {
            Helper.assertTrue(Blob.meals() == 1, "the blob never took its bite" + state(Blob));
            Helper.assertTrue(Blob.getHealth() >= 10.0f + Scp999Blob.EatHeal - 0.01f, "the candy did not do it any good" + state(Blob));
            Helper.assertTrue(Blob.treatCooldown() > 0, "the blob is still hungry straight after a treat");
        });
        Helper.runAfterDelay(Scp999Blob.EatTicks + 3, () -> {
            Helper.assertTrue(Blob.action() == Scp999Blob.ActionWobble, "the meal did not end in a contented wobble" + state(Blob));
            tidy(Helper, List.of(Blob), User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp999_bowlFill")
    public static void scp999BowlFillsAndEmptiesByHand(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Relative = new BlockPos(8, 2, 8);
        BlockPos Pos = bowl(Helper, Relative, 0);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp999_filler");
        ItemStack Candy = new ItemStack(Scp999Registry.Candy.get(), 12);
        User.setItemInHand(InteractionHand.MAIN_HAND, Candy);
        for (int Index = 0; Index < PinkDogBowlBlock.Capacity; Index++) {
            BlockState Current = World.getBlockState(Pos);
            ItemInteractionResult Result = Current.useItemOn(Candy, World, User, InteractionHand.MAIN_HAND, hit(Helper, Relative));
            Helper.assertTrue(Result.consumesAction(), "the bowl refused candy at " + Index);
        }
        Helper.assertTrue(PinkDogBowlBlock.candy(World.getBlockState(Pos)) == PinkDogBowlBlock.Capacity, "the bowl did not fill up");
        Helper.assertTrue(User.getMainHandItem().getCount() == 12 - PinkDogBowlBlock.Capacity, "filling did not use up the candy");
        World.getBlockState(Pos).useItemOn(User.getMainHandItem(), World, User, InteractionHand.MAIN_HAND, hit(Helper, Relative));
        Helper.assertTrue(User.getMainHandItem().getCount() == 12 - PinkDogBowlBlock.Capacity, "a full bowl still took candy");
        Helper.assertTrue(World.getBlockState(Pos).getAnalogOutputSignal(World, Pos) == 15, "a full bowl does not read as full");
        User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult Taken = World.getBlockState(Pos).useWithoutItem(World, User, hit(Helper, Relative));
        Helper.assertTrue(Taken.consumesAction(), "an empty hand could not take candy back");
        Helper.assertTrue(PinkDogBowlBlock.candy(World.getBlockState(Pos)) == PinkDogBowlBlock.Capacity - 1, "taking candy did not lower the bowl");
        Helper.assertTrue(User.getInventory().countItem(Scp999Registry.Candy.get()) == 1, "the candy taken back did not reach the hand");
        Helper.assertTrue(PinkDogBowlBlock.level(PinkDogBowlBlock.Capacity) == 3 && PinkDogBowlBlock.level(1) == 1 && PinkDogBowlBlock.level(0) == 0,
                "bowl fill levels are out of step with the models");
        tidy(Helper, List.of(), User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp999_bowlBreak")
    public static void scp999BrokenBowlSpillsItsCandy(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = bowl(Helper, new BlockPos(8, 2, 8), 5);
        World.destroyBlock(Pos, true);
        Helper.runAfterDelay(2, () -> {
            int Spilled = 0;
            int Bowls = 0;
            for (ItemEntity Item : World.getEntitiesOfClass(ItemEntity.class, new AABB(Pos).inflate(2.0))) {
                if (Item.getItem().is(Scp999Registry.Candy.get())) {
                    Spilled += Item.getItem().getCount();
                } else if (Item.getItem().is(Scp999Registry.BowlItem.get())) {
                    Bowls += Item.getItem().getCount();
                }
                Item.discard();
            }
            Helper.assertTrue(Spilled == 5, "the broken bowl spilled " + Spilled + " servings instead of 5");
            Helper.assertTrue(Bowls == 1, "the broken bowl dropped " + Bowls + " bowls");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp999_bowlEat")
    public static void scp999HelpsItselfFromTheBowl(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = bowl(Helper, new BlockPos(16, 2, 16), 3);
        Scp999Blob Blob = spawn(Helper, 9.5, 16.5);
        Blob.crave();
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Blob.meals() >= 1, "the blob never ate from the bowl (" + Blob.position().distanceTo(Vec3.atCenterOf(Pos)) + ")" + state(Blob));
            Helper.assertTrue(PinkDogBowlBlock.candy(World.getBlockState(Pos)) == 2, "eating did not take one serving from the bowl");
            tidy(Helper, List.of(Blob));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp999_groundEat")
    public static void scp999EatsCandyOffTheFloor(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Vec3 Spot = Helper.absoluteVec(new Vec3(16.5, Floor, 16.5));
        ItemEntity Candy = new ItemEntity(World, Spot.x, Spot.y, Spot.z, new ItemStack(Scp999Registry.Candy.get(), 2));
        Candy.setDeltaMovement(Vec3.ZERO);
        Candy.setPickUpDelay(32767);
        World.addFreshEntity(Candy);
        Scp999Blob Blob = spawn(Helper, 9.5, 16.5);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Blob.meals() >= 1, "the blob never ate the dropped candy" + state(Blob));
            Helper.assertTrue(Candy.isAlive() && Candy.getItem().getCount() == 1, "eating did not take exactly one serving off the floor");
            tidy(Helper, List.of(Blob, Candy));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp999_regen")
    public static void scp999SoothesPeopleCloseBy(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp999Blob Blob = spawn(Helper, 16.5, 16.5);
        Scp999Blob Second = spawn(Helper, 17.5, 15.5);
        ServerPlayer Near = person(Helper, 19.5, 16.5, "scp999_near");
        ServerPlayer Far = person(Helper, 16.5 + Scp999Blob.RegenRadius + 2.5, 16.5, "scp999_far");
        ServerPlayer Healthy = person(Helper, 16.5, 19.5, "scp999_healthy");
        LivingEntity Person = DClassRegistry.DClassType.get().create(World);
        Helper.assertTrue(Person != null, "no D-Class could be made");
        Vec3 Spot = Helper.absoluteVec(new Vec3(14.5, Floor, 16.5));
        Person.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addFreshEntity(Person);
        Near.setHealth(8.0f);
        Far.setHealth(8.0f);
        Person.setHealth(Person.getMaxHealth() * 0.4f);
        Helper.runAfterDelay(Scp999Blob.RegenInterval * 2 + 2, () -> {
            MobEffectInstance Effect = Near.getEffect(MobEffects.REGENERATION);
            Helper.assertTrue(Effect != null, "a hurt person within reach got no regeneration");
            Helper.assertTrue(Effect.getAmplifier() == 0 && Effect.getDuration() <= Scp999Blob.RegenTicks, "two blobs stacked their regeneration " + Effect);
            Helper.assertTrue(!Far.hasEffect(MobEffects.REGENERATION), "a person beyond the reach was soothed");
            Helper.assertTrue(!Healthy.hasEffect(MobEffects.REGENERATION), "a person at full health was given regeneration");
            Helper.assertTrue(Person.hasEffect(MobEffects.REGENERATION), "a hurt D-Class within reach got no regeneration");
            tidy(Helper, List.of(Blob, Second, Person), Near, Far, Healthy);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp999_danger")
    public static void scp999FlattensNearAHostileAndPeeksOutAfter(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp999Blob Blob = spawn(Helper, 16.5, 16.5);
        Spider Threat = EntityType.SPIDER.create(World);
        Helper.assertTrue(Threat != null, "no spider could be made");
        Vec3 Spot = Helper.absoluteVec(new Vec3(20.5, Floor, 16.5));
        Threat.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Threat.setNoAi(true);
        Threat.setSilent(true);
        World.addFreshEntity(Threat);
        boolean[] Seen = new boolean[3];
        Helper.onEachTick(() -> {
            if (Blob.isFlat()) {
                Seen[0] = true;
            }
            if (Seen[0] && Threat.isAlive() && Helper.getTick() > 40) {
                Helper.assertTrue(Blob.isFlat(), "the blob got up while the hostile was still there" + state(Blob));
                Threat.discard();
            }
            if (Seen[0] && !Threat.isAlive() && Blob.action() == Scp999Blob.ActionPeek) {
                Seen[1] = true;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Seen[0], "a hostile close by did not flatten the blob" + state(Blob));
            Helper.assertTrue(Seen[1], "the blob never peeked out once it was safe" + state(Blob));
            Helper.assertTrue(!Blob.isFlat() && Blob.action() == Scp999Blob.ActionNone, "the blob is still hiding" + state(Blob));
            tidy(Helper, List.of(Blob));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp999_fire")
    public static void scp999HidesFromFire(GameTestHelper Helper) {
        Helper.setBlock(new BlockPos(14, 2, 16), Blocks.CAMPFIRE.defaultBlockState());
        Scp999Blob Blob = spawn(Helper, 16.5, 16.5);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Blob.isFlat(), "a lit campfire close by did not flatten the blob" + state(Blob));
            Helper.assertTrue(Blob.dangerSource() != null, "the blob does not know where the danger is");
            tidy(Helper, List.of(Blob));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp999_explosion")
    public static void scp999HidesFromAnExplosion(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp999Blob Blob = spawn(Helper, 16.5, 16.5);
        Helper.runAfterDelay(5, () -> {
            Vec3 Blast = Helper.absoluteVec(new Vec3(16.5, Floor + 0.5, 16.5 + Scp999Danger.ExplosionRadius - 6.0));
            World.explode(null, Blast.x, Blast.y, Blast.z, 0.5f, Level.ExplosionInteraction.NONE);
            Helper.assertTrue(Blob.isFlat(), "a nearby explosion did not flatten the blob" + state(Blob));
            Helper.assertTrue(Blob.isAlive() && Blob.getHealth() == Blob.getMaxHealth(), "the far explosion hurt the blob");
            tidy(Helper, List.of(Blob));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp999_hurt")
    public static void scp999HidesFromWhoeverHitsIt(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp999_bully");
        Blob.adjustAffection(User.getUUID(), 10);
        Blob.hurt(Helper.getLevel().damageSources().playerAttack(User), 1.0f);
        Helper.assertTrue(Blob.isFlat(), "being hit did not flatten the blob" + state(Blob));
        Helper.assertTrue(Blob.affection(User.getUUID()) < Scp999Blob.LikeThreshold, "the blob still adores the person who hit it");
        tidy(Helper, List.of(Blob), User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp999_greet")
    public static void scp999BouncesBackAtACrouchGreeting(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 11.5, 8.5, "scp999_bobber");
        boolean[] Greeted = new boolean[1];
        int Start = Scp999Blob.GreetTicks + 40;
        Helper.runAfterDelay(Start - 2, () -> Blob.adjustAffection(User.getUUID(), -Blob.affection(User.getUUID())));
        for (int Index = 0; Index < Scp999Blob.CrouchToggles; Index++) {
            boolean Down = Index % 2 == 0;
            Helper.runAfterDelay(Start + Index * 3, () -> User.setShiftKeyDown(Down));
        }
        Helper.onEachTick(() -> {
            if (Helper.getTick() >= Start && Blob.action() == Scp999Blob.ActionGreet) {
                Greeted[0] = true;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Greeted[0], "a crouch greeting got no bounce back" + state(Blob));
            Helper.assertTrue(Blob.affection(User.getUUID()) > 0, "the greeting left no fondness");
            tidy(Helper, List.of(Blob), User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp999_candySight")
    public static void scp999GetsExcitedAboutHeldCandy(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 8.5, 16.5);
        ServerPlayer User = person(Helper, 15.5, 16.5, "scp999_tease");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp999Registry.Candy.get()));
        double Start = Blob.distanceTo(User);
        boolean[] Excited = new boolean[1];
        Helper.onEachTick(() -> {
            if (Blob.action() == Scp999Blob.ActionExcited) {
                Excited[0] = true;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Excited[0], "the blob showed no excitement at the candy" + state(Blob));
            Helper.assertTrue(Blob.distanceTo(User) < Math.min(Start - 3.0, 3.0), "the blob did not come begging (" + Blob.distanceTo(User) + ")" + state(Blob));
            tidy(Helper, List.of(Blob), User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp999_comfort")
    public static void scp999HopsToAHurtPerson(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 6.5, 16.5);
        ServerPlayer User = person(Helper, 18.5, 16.5, "scp999_patient");
        User.setHealth(6.0f);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Blob.distanceTo(User) < 2.6, "the blob did not come to the hurt person (" + Blob.distanceTo(User) + ")" + state(Blob));
            tidy(Helper, List.of(Blob), User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp999_follow")
    public static void scp999FollowsAFriend(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 4.5, 16.5);
        ServerPlayer User = person(Helper, 20.5, 16.5, "scp999_friend");
        Blob.adjustAffection(User.getUUID(), Scp999Blob.LikeThreshold + 4);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Blob.distanceTo(User) < 5.5, "the blob did not follow its friend (" + Blob.distanceTo(User) + ")" + state(Blob));
            tidy(Helper, List.of(Blob), User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp999_hug")
    public static void scp999TickleHugsAFriendWithoutHarm(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 9.8, 8.5, "scp999_hugged");
        float Before = User.getHealth();
        Blob.hug(Helper.getLevel(), User);
        Helper.assertTrue(Blob.action() == Scp999Blob.ActionHug, "the tickle hug did not start" + state(Blob));
        Helper.assertTrue(Blob.affection(User.getUUID()) == Scp999Blob.HugAffection, "the hug made it no fonder");
        Helper.runAfterDelay(Scp999Blob.HugTicks + 2, () -> {
            Helper.assertTrue(Blob.action() != Scp999Blob.ActionHug, "the hug never let go" + state(Blob));
            Helper.assertTrue(User.getHealth() >= Before, "the tickle hug hurt someone");
            tidy(Helper, List.of(Blob), User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp999_cuddle")
    public static void scp999OffersHugsToAStillFriend(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp999_still");
        Blob.adjustAffection(User.getUUID(), Scp999Blob.LikeThreshold + 4);
        boolean[] Hugged = new boolean[1];
        Helper.onEachTick(() -> {
            if (Blob.action() == Scp999Blob.ActionHug) {
                Hugged[0] = true;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Hugged[0], "the blob never hugged a friend standing still nearby (" + Blob.distanceTo(User) + ")" + state(Blob));
            Helper.assertTrue(Blob.distanceTo(User) <= Scp999Blob.HugRange + 0.5, "the blob hugged from too far away");
            tidy(Helper, List.of(Blob), User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp999_play")
    public static void scp999PlaysWithDroppedThings(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Vec3 Spot = Helper.absoluteVec(new Vec3(14.5, Floor, 16.5));
        ItemEntity Toy = new ItemEntity(World, Spot.x, Spot.y, Spot.z, new ItemStack(Items.SLIME_BALL));
        Toy.setDeltaMovement(Vec3.ZERO);
        Toy.setPickUpDelay(32767);
        World.addFreshEntity(Toy);
        Scp999Blob Blob = spawn(Helper, 9.5, 16.5);
        boolean[] Played = new boolean[1];
        Helper.onEachTick(() -> {
            if (Blob.action() == Scp999Blob.ActionPlay) {
                Played[0] = true;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Played[0], "the blob never played with the dropped item" + state(Blob));
            Helper.assertTrue(Toy.position().distanceTo(Spot) > 0.3, "the blob played but the toy never moved");
            tidy(Helper, List.of(Blob, Toy));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp999_playMob")
    public static void scp999PlaysWithAFriendlyAnimal(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Sheep Friend = EntityType.SHEEP.create(World);
        Helper.assertTrue(Friend != null, "no sheep could be made");
        Vec3 Spot = Helper.absoluteVec(new Vec3(14.5, Floor, 16.5));
        Friend.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Friend.setNoAi(true);
        World.addFreshEntity(Friend);
        Scp999Blob Blob = spawn(Helper, 9.5, 16.5);
        boolean[] Played = new boolean[1];
        Helper.onEachTick(() -> {
            if (Blob.action() == Scp999Blob.ActionPlay) {
                Played[0] = true;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Played[0], "the blob never played with the sheep" + state(Blob));
            Helper.assertTrue(!Blob.isFlat(), "a sheep frightened the blob");
            tidy(Helper, List.of(Blob, Friend));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp999_shapes")
    public static void scp999ShiftsBetweenShapes(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        byte[] Shapes = {Scp999Blob.ActionDrop, Scp999Blob.ActionPuddle, Scp999Blob.ActionLumpy, Scp999Blob.ActionHeart};
        List<String> Seen = new ArrayList<>();
        for (int Index = 0; Index < Shapes.length; Index++) {
            byte Shape = Shapes[Index];
            Helper.runAfterDelay(2 + Index * (Scp999Blob.ShapeTicks + 4), () -> {
                Blob.morph(Helper.getLevel(), Shape);
                Helper.assertTrue(Blob.action() == Shape, "shape " + Shape + " did not start" + state(Blob));
                Seen.add(Scp999Blob.clipOf(Shape));
            });
            Helper.runAfterDelay(2 + Index * (Scp999Blob.ShapeTicks + 4) + Scp999Blob.ShapeTicks + 1, () ->
                    Helper.assertTrue(Blob.action() != Shape, "shape " + Shape + " never melted back" + state(Blob)));
        }
        Helper.runAfterDelay(2 + Shapes.length * (Scp999Blob.ShapeTicks + 4), () -> {
            Helper.assertTrue(Seen.equals(List.of("shape_drop", "shape_puddle", "shape_lumpy", "shape_heart")), "the shapes played were " + Seen);
            tidy(Helper, List.of(Blob));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp999_hum")
    public static void scp999HumsAndNapsGrounded(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        Helper.runAfterDelay(30, () -> {
            grounded(Helper, Blob, "standing");
            Blob.hum(World);
            Helper.assertTrue(Blob.action() == Scp999Blob.ActionHum, "the night hum did not start" + state(Blob));
        });
        Helper.runAfterDelay(30 + Scp999Blob.HumTicks + 2, () -> {
            Helper.assertTrue(Blob.action() != Scp999Blob.ActionHum, "the hum never finished" + state(Blob));
            Blob.nap(World, 200);
            grounded(Helper, Blob, "napping");
            tidy(Helper, List.of(Blob));
            Helper.succeed();
        });
    }

    private static void grounded(GameTestHelper Helper, Scp999Blob Blob, String When) {
        double Floor0 = Helper.absoluteVec(new Vec3(0.0, Floor, 0.0)).y;
        Helper.assertTrue(Blob.onGround(), When + ": the blob is not on the ground" + state(Blob));
        Helper.assertTrue(Math.abs(Blob.getY() - Floor0) < 0.01, When + ": the blob rests at " + Blob.getY() + " instead of the floor top " + Floor0 + state(Blob));
        Helper.assertTrue(Blob.getPose() == Pose.STANDING, When + ": the blob is in pose " + Blob.getPose());
        Helper.assertTrue(Math.abs(Blob.getBbHeight() - Scp999Blob.Height) < 0.001 && Math.abs(Blob.getBbWidth() - Scp999Blob.Width) < 0.001,
                When + ": the blob is " + Blob.getBbWidth() + " by " + Blob.getBbHeight());
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp999_flatGround")
    public static void scp999StaysOnTheFloorWhileHiding(GameTestHelper Helper) {
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        Helper.runAfterDelay(20, () -> Blob.frighten(Helper.getLevel(), Blob.position(), 40));
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Blob.isFlat(), "the blob is not hiding" + state(Blob));
            grounded(Helper, Blob, "hiding");
            Helper.assertTrue(Blob.getNavigation().isDone(), "the hiding blob is still wandering");
        });
        Helper.runAfterDelay(110, () -> {
            Helper.assertTrue(!Blob.isFlat(), "the blob never came out of hiding" + state(Blob));
            grounded(Helper, Blob, "after hiding");
            tidy(Helper, List.of(Blob));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp999_save")
    public static void scp999RemembersFriendsAndNapsAcrossReloads(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp999Blob Blob = spawn(Helper, 8.5, 8.5);
        UUID Friend = UUID.randomUUID();
        Blob.adjustAffection(Friend, 9);
        Blob.nap(World, 300);
        CompoundTag Saved = new CompoundTag();
        Helper.assertTrue(Blob.save(Saved), "the blob refused to save");
        Optional<Entity> Copy = EntityType.create(Saved, World);
        Helper.assertTrue(Copy.isPresent() && Copy.get() instanceof Scp999Blob, "the blob could not be rebuilt");
        Scp999Blob Loaded = (Scp999Blob) Copy.get();
        Helper.assertTrue(Loaded.affection(Friend) == 9, "the blob forgot its friend");
        Helper.assertTrue(Loaded.isNapping(), "the blob woke up from a reload");
        Helper.assertTrue(Loaded.action() == Scp999Blob.ActionNone && !Loaded.isFlat(), "the blob came back from a reload mid-action" + state(Loaded));
        Loaded.discard();
        tidy(Helper, List.of(Blob));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp999_candyItem")
    public static void scp999ChocolateOrbsAreALightSnack(GameTestHelper Helper) {
        ItemStack Candy = new ItemStack(Scp999Registry.Candy.get());
        var Food = Candy.getFoodProperties(null);
        Helper.assertTrue(Food != null, "chocolate orbs cannot be eaten");
        Helper.assertTrue(Food.nutrition() == Scp999Registry.CandyNutrition && Food.eatDurationTicks() < 32, "chocolate orbs are not a quick light snack " + Food);
        Helper.assertTrue(Candy.getMaxStackSize() == 64, "chocolate orbs do not stack");
        Helper.succeed();
    }
}
