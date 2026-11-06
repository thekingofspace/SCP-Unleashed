package net.rai.scp_unleashed.scp529.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.scp458.Scp458Pizza;
import net.rai.scp_unleashed.scp458.Scp458Registry;
import net.rai.scp_unleashed.scp529.Scp529Cat;
import net.rai.scp_unleashed.scp529.Scp529Cheese;
import net.rai.scp_unleashed.scp529.Scp529Registry;
import net.rai.scp_unleashed.scp529.Scp529StrayCondition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp529GameTests {
    public static final String Arena = "scp529_arena";
    public static final double Floor = 2.0;
    public static final String GeoPath = "/assets/scp_unleashed/geo/entity/scp529/scp529_cat.geo.json";

    private Scp529GameTests() {
    }

    private static Scp529Cat spawn(GameTestHelper Helper, double X, double Z) {
        Scp529Cat Josie = Helper.spawn(Scp529Registry.Josie.get(), new Vec3(X, Floor, Z));
        Josie.setPersistenceRequired();
        return Josie;
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

    private static String state(Scp529Cat Josie) {
        return " [tame " + Josie.isTame() + ", action " + Josie.action() + ", rest " + Josie.rest() + ", sitting " + Josie.isInSittingPose() + ", ordered " + Josie.isOrderedToSit()
                + ", distressed " + Josie.isDistressed() + ", craving " + Josie.isCraving() + ", health " + Josie.getHealth() + ", at " + Josie.position() + "]";
    }

    private static void face(Scp529Cat Josie, float Yaw) {
        Josie.setYRot(Yaw);
        Josie.setYBodyRot(Yaw);
        Josie.setYHeadRot(Yaw);
    }

    private static boolean named(Entity Josie) {
        Component Name = Josie.getCustomName();
        return Name != null && Name.getContents() instanceof TranslatableContents Key && Key.getKey().equals(Scp529Cat.NameKey);
    }

    private static ItemStack cheeseSlice(Scp458Pizza Kind) {
        ItemStack Slice = new ItemStack(Scp458Registry.Slice.get());
        Slice.set(Scp458Registry.Pizza.get(), Kind);
        return Slice;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp529_name")
    public static void sheIsCalledJosie(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Helper.assertTrue(named(Josie), "a fresh SCP-529 is not named Josie: " + Josie.getCustomName());
        Helper.assertTrue(!Josie.isCustomNameVisible(), "Josie's name floats overhead all the time");
        Scp529Cat Fresh = Scp529Registry.Josie.get().create(World);
        Helper.assertTrue(Fresh != null, "SCP-529 could not be created");
        Fresh.moveTo(Helper.absoluteVec(new Vec3(12.5, Floor, 12.5)));
        Fresh.finalizeSpawn(World, World.getCurrentDifficultyAt(Fresh.blockPosition()), MobSpawnType.SPAWN_EGG, null);
        Helper.assertTrue(named(Fresh), "an SCP-529 from the egg path is not named Josie");
        Fresh.setCustomName(Component.literal("Biscuit"));
        CompoundTag Saved = new CompoundTag();
        Fresh.save(Saved);
        Optional<Entity> Copy = EntityType.create(Saved, World);
        Helper.assertTrue(Copy.isPresent() && "Biscuit".equals(Copy.get().getCustomName().getString()), "a name tag rename did not survive a reload");
        Copy.get().discard();
        Fresh.discard();
        tidy(Helper, List.of(Josie));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp529_arrival")
    public static void arrivalPlacesAStray(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Reader = person(Helper, 16.5, 16.5, "scp529_reader");
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp529Registry.DocumentId);
        Helper.assertTrue(Document != null, "the scp_529 dossier did not load");
        Optional<BlockPos> Found = Scp529StrayCondition.findSpot(World, Reader, 6, 12, false, false, World.getRandom());
        Helper.assertTrue(Found.isPresent(), "the arrival found no spot on open ground");
        BlockPos Spot = Found.get();
        double Away = Vec3.atBottomCenterOf(Spot).distanceTo(Reader.position());
        Helper.assertTrue(Away >= 5.0 && Away <= 13.5, "the arrival spot is " + Away + " blocks from the reader");
        boolean Placed = Scp529StrayCondition.manifest(new AnomalySpawners.Discovery(World, Spot, Reader, Scp529Registry.DocumentId, Document, true));
        Helper.assertTrue(Placed, "the arrival placed nothing");
        List<Scp529Cat> Cats = World.getEntitiesOfClass(Scp529Cat.class, new AABB(Spot).inflate(2.0));
        Helper.assertTrue(Cats.size() == 1, "the arrival placed " + Cats.size() + " cats");
        Scp529Cat Josie = Cats.get(0);
        Helper.assertTrue(!Josie.isTame(), "Josie arrived already tamed" + state(Josie));
        Helper.assertTrue(Josie.isPersistenceRequired(), "the arriving Josie could despawn" + state(Josie));
        Helper.assertTrue(named(Josie), "the arriving cat is not named Josie");
        Helper.assertTrue(Reader.getUUID().equals(Josie.sought()), "Josie is not heading for her reader");
        tidy(Helper, Cats, Reader);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp529_seek")
    public static void strayWalksUpToTheReader(GameTestHelper Helper) {
        Scp529Cat Josie = spawn(Helper, 8.5, 16.5);
        ServerPlayer Reader = person(Helper, 24.5, 16.5, "scp529_seeker");
        Josie.seek(Reader.getUUID());
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Josie.distanceTo(Reader) < 4.0, "Josie did not come up to the reader (" + Josie.distanceTo(Reader) + ")" + state(Josie));
            Helper.assertTrue(Josie.sought() == null, "Josie kept looking after she arrived");
            tidy(Helper, List.of(Josie), Reader);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp529_fish")
    public static void fishNeverTamesHer(GameTestHelper Helper) {
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Josie.setNoAi(true);
        Josie.setOnGround(true);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp529_fisher");
        Josie.setHealth(4.0f);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 40));
        for (int Try = 0; Try < 30; Try++) {
            Josie.mobInteract(User, InteractionHand.MAIN_HAND);
        }
        Helper.assertTrue(!Josie.isTame() && Josie.getOwnerUUID() == null, "thirty fish tamed Josie" + state(Josie));
        Helper.assertTrue(User.getMainHandItem().getCount() == 10, "Josie ate " + (40 - User.getMainHandItem().getCount()) + " of thirty fish");
        Helper.assertTrue(Josie.getHealth() > 4.0f, "the fish did not heal Josie" + state(Josie));
        Helper.assertTrue(Josie.likes(User.getUUID()), "fish did not make Josie fond of her feeder (" + Josie.fondness(User.getUUID()) + ")");
        Helper.assertTrue(!Josie.isCraving(), "fish gave Josie a craving" + state(Josie));
        Helper.assertTrue(Josie.buryLeft() > 0, "eating did not lead to the later scratching");
        Player Creative = Helper.makeMockPlayer(GameType.CREATIVE);
        Creative.getAbilities().instabuild = true;
        Creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SALMON, 3));
        Josie.mobInteract(Creative, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Creative.getMainHandItem().getCount() == 3, "a creative player's fish was used up");
        Helper.assertTrue(!Josie.isTame(), "a creative feed tamed Josie");
        tidy(Helper, List.of(Josie), User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp529_noOrders")
    public static void sheCannotBeOrderedToSit(GameTestHelper Helper) {
        ServerPlayer User = person(Helper, 8.5, 8.5, "scp529_bossy");
        Scp529Cat Josie = spawn(Helper, 10.5, 8.5);
        Josie.tame(User);
        Helper.assertTrue(!Josie.isTame() && Josie.getOwnerUUID() == null, "Josie accepted an owner" + state(Josie));
        Josie.setTame(true, false);
        Helper.assertTrue(!Josie.isTame(), "Josie could be marked tame" + state(Josie));
        User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Josie.mobInteract(User, InteractionHand.MAIN_HAND);
        Josie.setOrderedToSit(true);
        Helper.assertTrue(!Josie.isOrderedToSit(), "Josie took an order to sit" + state(Josie));
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(!Josie.isOrderedToSit() && !Josie.isTame(), "an order or an owner stuck" + state(Josie));
            tidy(Helper, List.of(Josie), User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp529_noCollar")
    public static void sheWearsNoCollar(GameTestHelper Helper) {
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Josie.setNoAi(true);
        ServerPlayer User = person(Helper, 10.5, 8.5, "scp529_dyer");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLUE_DYE, 4));
        Josie.mobInteract(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(User.getMainHandItem().getCount() == 4, "dye was used on Josie");
        Helper.assertTrue(Josie.getCollarColor() != DyeColor.BLUE, "Josie's collar took the dye");
        String Model = "";
        try (InputStream Stream = Scp529GameTests.class.getResourceAsStream(GeoPath)) {
            Helper.assertTrue(Stream != null, "the SCP-529 model is missing");
            Model = new String(Stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException Problem) {
            Helper.fail("the SCP-529 model could not be read: " + Problem.getMessage());
        }
        Helper.assertTrue(Model.contains("\"name\": \"head\""), "the model check read the wrong file");
        Helper.assertTrue(!Model.contains("collar"), "the SCP-529 model still has a collar");
        tidy(Helper, List.of(Josie), User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp529_visit")
    public static void sheVisitsPeopleSheLikes(GameTestHelper Helper) {
        ServerPlayer Friend = person(Helper, 9.5, 8.5, "scp529_friend");
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Friend.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 4));
        Josie.mobInteract(Friend, InteractionHand.MAIN_HAND);
        Josie.mobInteract(Friend, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Josie.likes(Friend.getUUID()), "two fish did not make Josie fond of her friend");
        Friend.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        moveTo(Helper, Friend, 20.5, 18.5);
        Josie.hurryVisit(1);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Josie.distanceTo(Friend) < 4.5, "Josie did not trot over to her friend (" + Josie.distanceTo(Friend) + ")" + state(Josie));
            Helper.assertTrue(!Josie.isTame(), "a visit made Josie tame");
            tidy(Helper, List.of(Josie), Friend);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp529_strokeFront")
    public static void aStrokeOnTheHeadMakesHerPurr(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Josie.setNoAi(true);
        Josie.setOnGround(true);
        face(Josie, 0.0f);
        ServerPlayer User = person(Helper, 8.5, 9.8, "scp529_petter");
        float Before = User.getHealth();
        for (float Roll : new float[]{0.0f, 0.5f, 0.99f}) {
            Josie.hurryIdle(1000);
            int Result = Josie.stroke(World, User, Roll);
            Helper.assertTrue(Result == Scp529Cat.StrokePurr || Result == Scp529Cat.StrokeIgnored, "stroking Josie's head gave " + Result + state(Josie));
        }
        Helper.assertTrue(Josie.action() == Scp529Cat.ActionKnead, "a purring Josie did not knead" + state(Josie));
        Helper.assertTrue(User.getHealth() >= Before, "Josie scratched a head stroke");
        tidy(Helper, List.of(Josie), User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp529_strokeBack")
    public static void theVoidIsNotForStroking(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Josie.setNoAi(true);
        Josie.setOnGround(true);
        face(Josie, 0.0f);
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 Behind = Helper.absoluteVec(new Vec3(8.5, Floor, 7.2));
        User.moveTo(Behind.x, Behind.y, Behind.z, 0.0f, 0.0f);
        Helper.assertTrue(Josie.behind(User.position()), "a person behind Josie was not counted as behind");
        float Before = User.getHealth();
        int First = Josie.stroke(World, User, 0.95f);
        Helper.assertTrue(First == Scp529Cat.StrokeSwat, "a stroke on the cut did not get a swat (" + First + ")" + state(Josie));
        if (World.getDifficulty() != Difficulty.PEACEFUL) {
            Helper.assertTrue(User.getHealth() < Before, "the swat did not scratch");
        }
        Helper.assertTrue(User.getHealth() > Before - 2.0f, "the scratch was more than a scratch");
        Helper.runAfterDelay(Scp529Cat.StrokeCooldown + 15, () -> {
            face(Josie, 0.0f);
            int Second = Josie.stroke(World, User, 0.05f);
            Helper.assertTrue(Second == Scp529Cat.StrokePurr, "a lucky stroke on the cut did not purr (" + Second + ")" + state(Josie));
            tidy(Helper, List.of(Josie));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp529_cheese")
    public static void cheeseStartsTrouble(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Feeder = person(Helper, 8.5, 8.5, "scp529_cheesemonger");
        Scp529Cat Josie = spawn(Helper, 10.5, 8.5);
        Helper.assertTrue(Scp529Cheese.is(cheeseSlice(Scp458Pizza.FourCheese)), "a four cheese slice does not count as cheese");
        Helper.assertTrue(!Scp529Cheese.is(cheeseSlice(Scp458Pizza.Pepperoni)), "a pepperoni slice counts as cheese");
        Feeder.setItemInHand(InteractionHand.MAIN_HAND, cheeseSlice(Scp458Pizza.Cheese));
        Josie.mobInteract(Feeder, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Josie.cheeseFed() == 1 && Josie.isCraving(), "the cheese did not take" + state(Josie));
        Helper.assertTrue(Feeder.getMainHandItem().isEmpty(), "Josie did not eat the slice");
        Helper.assertTrue(!Josie.isDistressed(), "Josie was distressed straight away" + state(Josie));
        Helper.assertTrue(!Josie.isTame() && Josie.getOwnerUUID() == null, "cheese tamed Josie" + state(Josie));
        Josie.setInSittingPose(true);
        Josie.hurryCraving(2);
        Helper.runAfterDelay(10, () -> {
            Helper.assertTrue(Josie.isDistressed(), "going without cheese did not distress Josie" + state(Josie));
            Helper.assertTrue(Josie.rest() == Scp529Cat.RestNone, "a distressed Josie is resting" + state(Josie));
        });
        Helper.runAfterDelay(60, () -> {
            Helper.assertTrue(!Josie.isInSittingPose(), "a distressed Josie stayed seated" + state(Josie));
            Feeder.setItemInHand(InteractionHand.MAIN_HAND, cheeseSlice(Scp458Pizza.FourCheese));
            Josie.mobInteract(Feeder, InteractionHand.MAIN_HAND);
            Helper.assertTrue(!Josie.isDistressed() && Josie.isCraving() && Josie.cheeseFed() == 2, "more cheese did not settle her" + state(Josie));
            Josie.hurryCraving(1);
        });
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Josie.isDistressed(), "the craving did not come back" + state(Josie));
            Josie.hurryDistress(2);
        });
        Helper.runAfterDelay(100, () -> {
            Helper.assertTrue(!Josie.isDistressed() && !Josie.isCraving(), "the distress never wore off" + state(Josie));
            tidy(Helper, List.of(Josie), Feeder);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp529_bury")
    public static void afterEatingSheScratchesTheFloor(GameTestHelper Helper) {
        ServerPlayer Feeder = person(Helper, 8.5, 8.5, "scp529_feeder");
        Scp529Cat Josie = spawn(Helper, 10.5, 8.5);
        Josie.setNoAi(true);
        Josie.setOnGround(true);
        Josie.setHealth(4.0f);
        Feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SALMON, 2));
        Josie.mobInteract(Feeder, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Josie.getHealth() > 4.0f, "a fish did not heal Josie" + state(Josie));
        Helper.assertTrue(Josie.buryLeft() > 0, "eating did not schedule the scratching");
        Josie.hurryIdle(1000);
        Josie.hurryBury(3);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Josie.action() == Scp529Cat.ActionBury, "Josie never scratched at the floor" + state(Josie));
            tidy(Helper, List.of(Josie), Feeder);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp529_nap")
    public static void napsSettleAndWake(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Josie.setNoAi(true);
        Josie.setOnGround(true);
        Josie.hurryIdle(1000);
        Helper.runAfterDelay(5, () -> Josie.setNapping(true));
        Helper.runAfterDelay(10, () -> {
            Helper.assertTrue(Josie.rest() == Scp529Cat.RestLoaf, "a napping Josie did not settle into a loaf" + state(Josie));
            Helper.assertTrue(Josie.action() == Scp529Cat.ActionLieDown, "Josie did not lie down first" + state(Josie));
            for (int Step = 0; Step <= Scp529Cat.NapDoze; Step++) {
                Josie.napTick();
            }
        });
        Helper.runAfterDelay(15, () -> Helper.assertTrue(Josie.rest() == Scp529Cat.RestSleep, "a long nap did not turn into sleep" + state(Josie)));
        Helper.runAfterDelay(30, () -> {
            Josie.hurt(World.damageSources().generic(), 1.0f);
            Helper.assertTrue(!Josie.isNapping(), "a hurt Josie kept napping");
        });
        Helper.runAfterDelay(34, () -> {
            Helper.assertTrue(Josie.rest() == Scp529Cat.RestNone, "a hurt Josie stayed asleep" + state(Josie));
            tidy(Helper, List.of(Josie));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp529_sitLoaf")
    public static void aLongSitBecomesALoaf(GameTestHelper Helper) {
        Scp529Cat Josie = spawn(Helper, 10.5, 8.5);
        Josie.setNoAi(true);
        Josie.setOnGround(true);
        Josie.hurryIdle(1000);
        Helper.runAfterDelay(5, () -> Josie.setInSittingPose(true));
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Josie.isInSittingPose() && Josie.rest() == Scp529Cat.RestNone, "Josie is not plainly sitting" + state(Josie));
            Josie.hurrySitting(Scp529Cat.SitSettleTicks + 1);
        });
        Helper.runAfterDelay(34, () -> {
            Helper.assertTrue(Josie.rest() == Scp529Cat.RestLoaf, "a long sit did not become a loaf" + state(Josie));
            Josie.setInSittingPose(false);
            Helper.assertTrue(Josie.rest() == Scp529Cat.RestNone && Josie.action() == Scp529Cat.ActionGetUp, "getting up from the loaf did not play" + state(Josie));
        });
        Helper.runAfterDelay(60, () -> {
            Helper.assertTrue(!Josie.isInSittingPose() && Josie.rest() == Scp529Cat.RestNone, "Josie did not stay up" + state(Josie));
            tidy(Helper, List.of(Josie));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp529_bed")
    public static void sheNapsOnBedsByHerself(GameTestHelper Helper) {
        BlockPos Foot = new BlockPos(12, 2, 8);
        BlockState Bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        Helper.setBlock(Foot, Bed.setValue(BedBlock.PART, BedPart.FOOT));
        Helper.setBlock(Foot.east(), Bed.setValue(BedBlock.PART, BedPart.HEAD));
        Scp529Cat Josie = spawn(Helper, 9.5, 8.5);
        Josie.hurryIdle(1000);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Josie.isLying(), "Josie never curled up on the bed" + state(Josie));
            Helper.assertTrue(Josie.rest() == Scp529Cat.RestSleep, "lying on the bed did not put her to sleep" + state(Josie));
            Helper.assertTrue(!Josie.isTame(), "a bed made Josie tame");
            tidy(Helper, List.of(Josie));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp529_lying")
    public static void lyingOnABedIsSleep(GameTestHelper Helper) {
        Scp529Cat Josie = spawn(Helper, 9.5, 8.5);
        Josie.hurryIdle(1000);
        Helper.runAfterDelay(5, () -> Josie.setLying(true));
        Helper.runAfterDelay(8, () -> {
            Helper.assertTrue(Josie.rest() == Scp529Cat.RestSleep, "lying down did not put Josie to sleep" + state(Josie));
            Helper.assertTrue(Josie.action() == Scp529Cat.ActionLieDown, "Josie did not lie down first" + state(Josie));
            Josie.setLying(false);
        });
        Helper.runAfterDelay(11, () -> {
            Helper.assertTrue(Josie.rest() == Scp529Cat.RestNone && Josie.action() == Scp529Cat.ActionGetUp, "Josie did not get up" + state(Josie));
            tidy(Helper, List.of(Josie));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp529_creeper")
    public static void creepersStillFearHer(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Josie.setNoAi(true);
        Josie.setOnGround(true);
        Creeper Hiss = Helper.spawn(EntityType.CREEPER, new Vec3(11.5, Floor, 8.5));
        Hiss.setPersistenceRequired();
        List<Cat> Cats = World.getEntitiesOfClass(Cat.class, Hiss.getBoundingBox().inflate(6.0, 3.0, 6.0));
        Helper.assertTrue(Cats.contains(Josie), "a creeper looking for cats does not find Josie");
        Helper.succeedWhen(() -> {
            boolean Wary = false;
            for (WrappedGoal Each : Hiss.goalSelector.getAvailableGoals()) {
                if (Each.getGoal() instanceof AvoidEntityGoal<?> Avoid && Avoid.canUse()) {
                    Wary = true;
                }
            }
            Helper.assertTrue(Wary, "a creeper three blocks from Josie is not keen to back off");
            tidy(Helper, List.of(Josie, Hiss));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp529_hiss")
    public static void phantomsGetHissedAt(GameTestHelper Helper) {
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Josie.setNoAi(true);
        Josie.setOnGround(true);
        Helper.runAfterDelay(5, () -> {
            Josie.hiss();
            Helper.assertTrue(Josie.action() == Scp529Cat.ActionHiss, "a hiss did not arch her back" + state(Josie));
            Helper.assertTrue(Scp529Registry.voice(SoundEvents.CAT_HISS) == Scp529Registry.HissLayers, "the hiss is not her own layered hiss");
            tidy(Helper, List.of(Josie));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp529_voice")
    public static void everyCatSoundIsHers(GameTestHelper Helper) {
        Helper.assertTrue(Scp529Registry.voice(SoundEvents.CAT_AMBIENT) == Scp529Registry.MeowLayers, "meows are not hers");
        Helper.assertTrue(Scp529Registry.voice(SoundEvents.CAT_PURR) == Scp529Registry.PurrLayers, "purrs are not hers");
        Helper.assertTrue(Scp529Registry.voice(SoundEvents.CAT_STRAY_AMBIENT) == Scp529Registry.MewLayers, "stray mews are not hers");
        Helper.assertTrue(Scp529Registry.voice(SoundEvents.CAT_PURREOW) == Scp529Registry.TrillLayers, "trills are not hers");
        Helper.assertTrue(Scp529Registry.voice(SoundEvents.CAT_HURT) == Scp529Registry.HurtLayers, "hurt sounds are not hers");
        Helper.assertTrue(Scp529Registry.voice(SoundEvents.CAT_DEATH) == Scp529Registry.DeathLayers, "the death sound is not hers");
        Helper.assertTrue(Scp529Registry.voice(SoundEvents.CAT_EAT) == Scp529Registry.EatLayers, "eating is not hers");
        Helper.assertTrue(Scp529Registry.voice(SoundEvents.CAT_BEG_FOR_FOOD) == Scp529Registry.BegLayers, "begging is not hers");
        Helper.assertTrue(Scp529Registry.voice(SoundEvents.COW_AMBIENT) == null, "a cow sound was claimed");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp529_hunt")
    public static void sheSwatsWhatSheHunts(GameTestHelper Helper) {
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Josie.setNoAi(true);
        Josie.setOnGround(true);
        Rabbit Prey = Helper.spawn(EntityType.RABBIT, new Vec3(9.3, Floor, 8.5));
        Prey.setNoAi(true);
        Helper.runAfterDelay(5, () -> {
            float Before = Prey.getHealth();
            boolean Hit = Josie.doHurtTarget(Prey);
            Helper.assertTrue(Hit && Prey.getHealth() < Before, "Josie's swipe did not land");
            Helper.assertTrue(Josie.action() == Scp529Cat.ActionSwat, "the attack was not a swat" + state(Josie));
            tidy(Helper, List.of(Josie, Prey));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp529_idle")
    public static void sheGroomsAndStretches(GameTestHelper Helper) {
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Josie.setNoAi(true);
        Josie.setOnGround(true);
        Helper.runAfterDelay(10, () -> Josie.hurryIdle(1));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Josie.action() == Scp529Cat.ActionGroom || Josie.action() == Scp529Cat.ActionStretch, "an idle Josie never groomed or stretched" + state(Josie));
            tidy(Helper, List.of(Josie));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp529_noKittens")
    public static void thereIsOnlyOneJosie(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Matchmaker = person(Helper, 8.5, 8.5, "scp529_matchmaker");
        Scp529Cat First = spawn(Helper, 10.5, 8.5);
        Scp529Cat Second = spawn(Helper, 11.5, 8.5);
        First.setInLove(Matchmaker);
        Second.setInLove(Matchmaker);
        Helper.assertTrue(!First.canMate(Second), "two Josies could mate");
        Helper.assertTrue(First.getBreedOffspring(World, Second) == null, "Josie produced offspring");
        Helper.runAfterDelay(120, () -> {
            int Count = World.getEntitiesOfClass(Cat.class, new AABB(Helper.absolutePos(new BlockPos(8, 2, 8))).inflate(8.0)).size();
            Helper.assertTrue(Count == 2, "kittens appeared (" + Count + " cats)");
            tidy(Helper, List.of(First, Second), Matchmaker);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp529_reload")
    public static void stateSurvivesReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Friend = person(Helper, 6.5, 6.5, "scp529_saver");
        Scp529Cat Josie = spawn(Helper, 8.5, 6.5);
        Josie.fond(Friend.getUUID(), 5);
        Josie.distress(World);
        Helper.assertTrue(Josie.isDistressed(), "Josie was not distressed before saving");
        CompoundTag Saved = new CompoundTag();
        Helper.assertTrue(Josie.save(Saved), "Josie refused to save");
        Saved.putUUID("Owner", Friend.getUUID());
        Saved.putBoolean("Sitting", true);
        Optional<Entity> Copy = EntityType.create(Saved, World);
        Helper.assertTrue(Copy.isPresent() && Copy.get() instanceof Scp529Cat, "Josie could not be rebuilt");
        Scp529Cat Loaded = (Scp529Cat) Copy.get();
        Helper.assertTrue(Loaded.isDistressed() && Loaded.isCraving() && Loaded.distressLeft() > 0, "the craving was lost on reload" + state(Loaded));
        Helper.assertTrue(Loaded.fondness(Friend.getUUID()) == 5, "her fondness was lost on reload (" + Loaded.fondness(Friend.getUUID()) + ")");
        Helper.assertTrue(!Loaded.isTame() && Loaded.getOwnerUUID() == null && !Loaded.isOrderedToSit(), "an old owner or order came back on reload" + state(Loaded));
        Helper.assertTrue(named(Loaded), "the name was lost on reload");
        Helper.assertTrue(Loaded.isPersistenceRequired() && !Loaded.removeWhenFarAway(128.0), "the reloaded Josie could despawn");
        Helper.assertTrue(Loaded.action() == Scp529Cat.ActionNone && Loaded.rest() == Scp529Cat.RestNone, "a reload left a stale action" + state(Loaded));
        Loaded.discard();
        tidy(Helper, List.of(Josie), Friend);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp529_document")
    public static void documentIsSafe(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp529Registry.DocumentId);
        Helper.assertTrue(Document != null, "the scp_529 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Safe, "SCP-529 is not filed as Safe");
        Helper.assertTrue(Document.Danger() == 1, "SCP-529 has threat tier " + Document.Danger());
        Helper.assertTrue(Document.Condition() instanceof Scp529StrayCondition, "SCP-529 does not arrive as a stray");
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp529Registry.DocumentId).isPresent(), "SCP-529 has no spawner");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp529_egg")
    public static void spawnEggMakesJosie(GameTestHelper Helper) {
        Helper.assertTrue(Scp529Registry.Egg.get().getType(new ItemStack(Scp529Registry.Egg.get())) == Scp529Registry.Josie.get(), "the spawn egg makes the wrong entity");
        Scp529Cat Josie = spawn(Helper, 8.5, 8.5);
        Helper.assertTrue(Josie instanceof Cat, "Josie is not a cat underneath");
        Helper.assertTrue(Josie.getBbHeight() <= 0.75f && Josie.getBbWidth() <= 0.65f, "Josie is " + Josie.getBbWidth() + " by " + Josie.getBbHeight());
        Helper.assertTrue(!Josie.removeWhenFarAway(128.0), "Josie would despawn");
        tidy(Helper, List.of(Josie));
        Helper.succeed();
    }
}
