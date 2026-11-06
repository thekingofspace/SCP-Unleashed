package net.rai.scp_unleashed.scp539.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp539.Scp539Chat;
import net.rai.scp_unleashed.scp539.Scp539Compulsion;
import net.rai.scp_unleashed.scp539.Scp539Crash;
import net.rai.scp_unleashed.scp539.Scp539Helicopter;
import net.rai.scp_unleashed.scp539.Scp539Plan;
import net.rai.scp_unleashed.scp539.Scp539Registry;
import net.rai.scp_unleashed.scp539.Scp539Stone;
import net.rai.scp_unleashed.scp539.Scp539StoneItem;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.storage.StorageTags;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp539GameTests {
    public static final String Arena = "scp539_arena";
    public static final String DeathKey = "death.attack.scp_unleashed.scp539_helicopter";
    public static final String DeathText = "Dammit charles";
    public static final String BannerKey = "gui.scp_unleashed.scp539.greatest_plan";
    public static final String BannerText = "THIS IS THE GREATEST PLANNNNNN";
    public static final double Floor = 2.0;
    public static final BlockPos LockerSpot = new BlockPos(20, 2, 20);
    public static final BlockPos Centre = new BlockPos(12, 2, 12);

    private Scp539GameTests() {
    }

    private static FakePlayer player(GameTestHelper Helper, String Name, double X, double Z, float Yaw, float Pitch) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Where = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Where.x, Where.y, Where.z, Yaw, Pitch);
        User.setGameMode(GameType.SURVIVAL);
        World.addNewPlayer(User);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer... Users) {
        for (ServerPlayer User : Users) {
            Scp539Compulsion.clear(User.getUUID());
            Scp539Plan.clear(User.getUUID());
            if (!User.isRemoved()) {
                Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private static AABB area(GameTestHelper Helper) {
        return new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(24.0, 10.0, 24.0).inflate(4.0);
    }

    private static List<ItemEntity> droppedStones(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, area(Helper), Dropped -> Scp539StoneItem.is(Dropped.getItem()));
    }

    private static List<Scp539Stone> flyingStones(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp539Stone.class, area(Helper), Stone -> !Stone.isRemoved());
    }

    private static List<Scp539Helicopter> helicopters(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp539Helicopter.class, area(Helper).inflate(48.0), Craft -> !Craft.isRemoved());
    }

    private static void tidy(GameTestHelper Helper) {
        for (Entity Each : new ArrayList<Entity>(droppedStones(Helper))) {
            Each.discard();
        }
        for (Entity Each : new ArrayList<Entity>(flyingStones(Helper))) {
            Each.discard();
        }
        for (Entity Each : new ArrayList<Entity>(helicopters(Helper))) {
            Each.discard();
        }
    }

    private static Scp539Stone looseStone(GameTestHelper Helper, ServerPlayer Thrower, double X, double Y, double Z) {
        Scp539Stone Stone = new Scp539Stone(Helper.getLevel(), Thrower);
        Stone.setItem(new ItemStack(Scp539Registry.Stone.get()));
        Vec3 Where = Helper.absoluteVec(new Vec3(X, Y, Z));
        Stone.setPos(Where.x, Where.y, Where.z);
        return Stone;
    }

    private static long now(GameTestHelper Helper) {
        return Scp539Compulsion.now(Helper.getLevel());
    }

    private static String key(Component Text) {
        return Text.getContents() instanceof TranslatableContents Contents ? Contents.getKey() : Text.getString();
    }

    private static boolean postChat(ServerPlayer User, String Raw) {
        ServerChatEvent Event = new ServerChatEvent(User, Raw, Component.literal(Raw));
        return NeoForge.EVENT_BUS.post(Event).isCanceled();
    }

    private static void fill(GameTestHelper Helper, Block Kind, int X0, int Y0, int Z0, int X1, int Y1, int Z1) {
        for (BlockPos Each : BlockPos.betweenClosed(X0, Y0, Z0, X1, Y1, Z1)) {
            Helper.setBlock(Each, Kind.defaultBlockState());
        }
    }

    private static int count(GameTestHelper Helper, Block Kind, int X0, int Y0, int Z0, int X1, int Y1, int Z1) {
        int Found = 0;
        for (BlockPos Each : BlockPos.betweenClosed(X0, Y0, Z0, X1, Y1, Z1)) {
            if (Helper.getBlockState(Each).is(Kind)) {
                Found++;
            }
        }
        return Found;
    }

    private static Pig pig(GameTestHelper Helper, double X, double Y, double Z) {
        return Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(X, Y, Z));
    }

    private static GameRules.BooleanValue griefing(GameTestHelper Helper) {
        return Helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp539_throwLandsAsItem")
    public static void throwLandsAsItem(GameTestHelper Helper) {
        tidy(Helper);
        ServerPlayer User = player(Helper, "scp539_thrower", 4.5, 12.5, -90.0f, 35.0f);
        double StartX = User.getX();
        try {
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp539Registry.Stone.get()));
            Scp539Registry.Stone.get().use(Helper.getLevel(), User, InteractionHand.MAIN_HAND);
            Helper.assertTrue(User.getMainHandItem().isEmpty(), "Throwing the stone left it in the hand");
            Helper.assertTrue(User.getInventory().countItem(Scp539Registry.Stone.get()) == 0, "The thrower still holds a stone");
            Helper.assertTrue(flyingStones(Helper).size() == 1, "Throwing did not launch exactly one stone: " + flyingStones(Helper).size());
            Helper.assertTrue(droppedStones(Helper).isEmpty(), "A stone item appeared on the throw itself");
            Helper.assertTrue(User.getCooldowns().isOnCooldown(Scp539Registry.Stone.get()), "The throw set no cooldown");
            Scp539Stone Thrown = flyingStones(Helper).get(0);
            Helper.assertTrue(Thrown.getDeltaMovement().x > 0.5, "The stone was not thrown forward: " + Thrown.getDeltaMovement());
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeedWhen(() -> {
            Helper.assertTrue(flyingStones(Helper).isEmpty(), "The stone is still in flight");
            List<ItemEntity> Dropped = droppedStones(Helper);
            Helper.assertTrue(Dropped.size() == 1, "The stone landed as " + Dropped.size() + " items");
            Helper.assertTrue(Dropped.get(0).getItem().getCount() == 1, "The landed stack holds " + Dropped.get(0).getItem().getCount());
            Helper.assertTrue(Dropped.get(0).getX() > StartX + 1.0, "The stone landed at " + Dropped.get(0).position() + " but was thrown from x " + StartX);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp539_skimsOnWater")
    public static void skimsOnWater(GameTestHelper Helper) {
        tidy(Helper);
        fill(Helper, Blocks.WATER, 2, 2, 4, 23, 2, 20);
        ServerPlayer User = player(Helper, "scp539_skimmer", 2.5, 12.5, -90.0f, 0.0f);
        Scp539Stone Stone;
        try {
            Stone = Scp539Stone.thrown(Helper.getLevel(), User, new ItemStack(Scp539Registry.Stone.get()), 4.0f, -90.0f, 0.9f, 0.0f);
            Stone.setPos(Stone.getX(), Helper.absoluteVec(new Vec3(0.0, 3.4, 0.0)).y, Stone.getZ());
        } finally {
            dismiss(Helper, User);
        }
        AtomicReference<Integer> Skipped = new AtomicReference<>(0);
        Helper.onEachTick(() -> {
            if (!Stone.isRemoved()) {
                Skipped.set(Math.max(Skipped.get(), Stone.skips()));
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Stone.isRemoved(), "The stone is still skimming");
            Helper.assertTrue(Skipped.get() >= 1, "The stone never skipped off the water");
            Helper.assertTrue(droppedStones(Helper).size() == 1, "The skimmed stone did not come back as one item");
            tidy(Helper);
            fill(Helper, Blocks.AIR, 2, 2, 4, 23, 2, 20);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_rangeDecidesWhoIsCompelled")
    public static void rangeDecidesWhoIsCompelled(GameTestHelper Helper) {
        ServerPlayer Thrower = player(Helper, "scp539_range_thrower", 2.5, 2.5, 0.0f, 0.0f);
        ServerPlayer Near = player(Helper, "scp539_near", 12.5, 2.5, 0.0f, 0.0f);
        ServerPlayer Far = player(Helper, "scp539_far", 22.5, 22.5, 0.0f, 0.0f);
        try {
            Scp539Stone Stone = looseStone(Helper, Thrower, 2.5, 3.0, 2.5);
            Helper.assertTrue(Near.distanceTo(Stone) < Scp539Compulsion.Radius, "The near player is not within reach");
            Helper.assertTrue(Far.distanceTo(Stone) > Scp539Compulsion.Radius, "The far player is not out of reach");
            Scp539Compulsion.flight(Helper.getLevel(), Stone);
            long Now = now(Helper);
            Helper.assertTrue(Scp539Compulsion.isAffected(Near.getUUID(), Now), "A player 10 blocks away was not compelled");
            Helper.assertTrue(Scp539Compulsion.isGazing(Near.getUUID(), Now), "The near player's gaze was not pulled");
            Helper.assertTrue(Scp539Compulsion.isAffected(Thrower.getUUID(), Now), "The thrower beside the stone was not compelled");
            Helper.assertFalse(Scp539Compulsion.isAffected(Far.getUUID(), Now), "A player 28 blocks away was compelled");
            Helper.assertFalse(Scp539Compulsion.isGazing(Far.getUUID(), Now), "The far player's gaze was pulled");
            Scp539Compulsion.Gaze Pull = Scp539Compulsion.gaze(Near.getUUID());
            Helper.assertTrue(Pull != null && Pull.TargetId() == Stone.getId(), "The gaze does not point at the stone");
            Stone.discard();
        } finally {
            dismiss(Helper, Thrower, Near, Far);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_chatReplacedWhileAffected")
    public static void chatReplacedWhileAffected(GameTestHelper Helper) {
        ServerPlayer User = player(Helper, "scp539_talker", 6.5, 6.5, 0.0f, 0.0f);
        try {
            Language Words = Language.getInstance();
            for (int Index = 0; Index < Scp539Chat.LineCount; Index++) {
                String Key = Scp539Chat.LinePrefix + Index;
                Helper.assertTrue(Words.has(Key), "Missing rock line " + Key);
                String Line = Words.getOrDefault(Key);
                Helper.assertFalse(Line.contains(";") || Line.contains(" - ") || Line.contains("—") || Line.contains("–"), "Rock line " + Key + " uses a dash or semicolon");
            }
            long Now = now(Helper);
            Scp539Compulsion.setUntil(User.getUUID(), Now + 100L);
            Helper.assertTrue(postChat(User, "has anyone seen my keys"), "Chat from a compelled player went through unchanged");
            Component Replaced = Scp539Chat.replacement(User);
            Helper.assertTrue(Replaced != null && key(Replaced).equals("chat.type.text"), "The replacement is not shown as chat: " + Replaced);
            TranslatableContents Contents = (TranslatableContents) Replaced.getContents();
            Object Said = Contents.getArgs()[1];
            Helper.assertTrue(Said instanceof Component Line && key(Line).startsWith(Scp539Chat.LinePrefix), "The replacement is not a rock line: " + Said);
            Scp539Compulsion.setUntil(User.getUUID(), Now - 1L);
            Helper.assertFalse(Scp539Compulsion.isAffected(User), "The player is still compelled after the effect ended");
            Helper.assertFalse(postChat(User, "has anyone seen my keys"), "Chat was still replaced after the effect ended");
            Helper.assertTrue(Scp539Chat.replacement(User) == null, "A replacement was offered after the effect ended");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_effectLastsThirtySeconds")
    public static void effectLastsThirtySeconds(GameTestHelper Helper) {
        Helper.assertTrue(Scp539Compulsion.Duration == 600, "The effect does not last thirty seconds: " + Scp539Compulsion.Duration);
        ServerPlayer User = player(Helper, "scp539_timed", 6.5, 6.5, 0.0f, 0.0f);
        try {
            ServerLevel World = Helper.getLevel();
            long Now = now(Helper);
            Scp539Compulsion.compel(User, World, -5, User.position(), Now, Now + Scp539Compulsion.Duration, Now + Scp539Compulsion.Hold);
            Helper.assertTrue(Scp539Compulsion.isAffected(User.getUUID(), Now + 599L), "The effect ended before thirty seconds");
            Helper.assertFalse(Scp539Compulsion.isAffected(User.getUUID(), Now + 600L), "The effect outlasted thirty seconds");
            Helper.assertFalse(Scp539Compulsion.isGazing(User.getUUID(), Now + Scp539Compulsion.Hold), "The gaze outlasted the flight");
            long Later = Now + 300L;
            Scp539Compulsion.compel(User, World, -6, User.position(), Later, Later + Scp539Compulsion.Duration, Later + Scp539Compulsion.Hold);
            Helper.assertTrue(Scp539Compulsion.isAffected(User.getUUID(), Now + 899L), "A second throw did not refresh the effect");
            Helper.assertFalse(Scp539Compulsion.isAffected(User.getUUID(), Now + 900L), "A second throw stacked past thirty seconds");
            Scp539Compulsion.compel(User, World, -7, User.position(), Now + 10L, Now + 10L + Scp539Compulsion.Duration, Now + 10L + Scp539Compulsion.Hold);
            Helper.assertTrue(Scp539Compulsion.affectedUntil(User.getUUID()) == Now + 900L, "An older throw shortened the effect");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp539_dclassStopsAndStares")
    public static void dclassStopsAndStares(GameTestHelper Helper) {
        ServerPlayer Thrower = player(Helper, "scp539_dclass_thrower", 2.5, 2.5, 0.0f, 0.0f);
        DClass Prisoner = Helper.spawn(DClassRegistry.DClassType.get(), new Vec3(12.5, Floor, 12.5));
        DClass Distant = Helper.spawn(DClassRegistry.DClassType.get(), new Vec3(1.5, Floor, 22.5));
        Scp539Stone Stone = looseStone(Helper, Thrower, 18.5, 3.0, 12.5);
        dismiss(Helper, Thrower);
        Helper.assertTrue(Distant.distanceTo(Stone) > Scp539Compulsion.Radius, "The distant D-Class is within reach");
        Scp539Compulsion.flight(Helper.getLevel(), Stone);
        Helper.assertTrue(Scp539Compulsion.isStaring(Prisoner.getUUID(), now(Helper)), "The nearby D-Class was not drawn to the stone");
        Helper.assertFalse(Scp539Compulsion.isStaring(Distant.getUUID(), now(Helper)), "A distant D-Class was drawn to the stone");
        Helper.runAfterDelay(3, () -> {
            AttributeInstance Speed = Prisoner.getAttribute(Attributes.MOVEMENT_SPEED);
            Helper.assertTrue(Speed != null && Speed.hasModifier(Scp539Compulsion.StareId), "The staring D-Class can still walk");
            Helper.assertTrue(Speed.getValue() <= 1.0e-6, "The staring D-Class keeps a walking speed of " + Speed.getValue());
            float Facing = net.minecraft.util.Mth.wrapDegrees(Prisoner.getYHeadRot() - (-90.0f));
            Helper.assertTrue(Math.abs(Facing) < 12.0f, "The D-Class is not looking at the stone, head yaw " + Prisoner.getYHeadRot());
        });
        Helper.runAfterDelay(Scp539Compulsion.Hold + 6, () -> {
            AttributeInstance Speed = Prisoner.getAttribute(Attributes.MOVEMENT_SPEED);
            Helper.assertFalse(Speed != null && Speed.hasModifier(Scp539Compulsion.StareId), "The D-Class stayed rooted after the stone was gone");
            Prisoner.discard();
            Distant.discard();
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_lockerKeepsIt")
    public static void lockerKeepsIt(GameTestHelper Helper) {
        Helper.setBlock(LockerSpot, StorageRegistry.SmallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Direction.NORTH));
        Helper.assertTrue(Helper.getBlockEntity(LockerSpot) instanceof StorageContainerBlockEntity, "No storage locker formed");
        StorageContainerBlockEntity Locker = Helper.getBlockEntity(LockerSpot);
        ItemStack Stone = new ItemStack(Scp539Registry.Stone.get());
        Helper.assertTrue(StorageTags.isStorable(Stone), "SCP-539 is not tagged as storable");
        Helper.assertTrue(Locker.refusalFor(Stone.copy()) == null, "The locker refuses the stone: " + Locker.refusalFor(Stone));
        Player Keeper = Helper.makeMockPlayer(GameType.SURVIVAL);
        Keeper.setItemInHand(InteractionHand.MAIN_HAND, Stone);
        Locker.store(Keeper, Keeper.getMainHandItem());
        Helper.assertTrue(Keeper.getMainHandItem().isEmpty(), "Storing the stone left it in the hand");
        Helper.assertTrue(Scp539StoneItem.is(Locker.peekContents()), "The locker did not take the stone: " + Locker.peekContents());
        Locker.take(Keeper, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Scp539StoneItem.is(Keeper.getMainHandItem()) && Keeper.getMainHandItem().getCount() == 1, "The stone came back out as " + Keeper.getMainHandItem());
        Helper.assertFalse(Locker.hasContents(), "The locker still holds the stone after it was taken");
        Helper.setBlock(LockerSpot, Blocks.AIR.defaultBlockState());
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_strangerBringsIt")
    public static void strangerBringsIt(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp539Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-539 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Euclid, "SCP-539 is not filed as Euclid");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-539 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Scp539StoneItem.is(Stack) && Stack.getCount() == 1, "The stranger's gift is not the stone: " + Stack);
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp539Registry.DocumentId).isPresent(), "No spawner is registered for SCP-539");
        ServerPlayer Reader = player(Helper, "scp539_reader", 6.5, 6.5, 0.0f, 0.0f);
        try {
            BlockPos Pos = Helper.absolutePos(Centre);
            boolean Spawned = AnomalySpawners.spawnerFor(Scp539Registry.DocumentId).get()
                    .spawn(new AnomalySpawners.Discovery(Helper.getLevel(), Pos, Reader, Scp539Registry.DocumentId, Document, false));
            Helper.assertTrue(Spawned, "The SCP-539 spawner refused the delivery");
            Helper.assertTrue(flyingStones(Helper).isEmpty() && droppedStones(Helper).isEmpty(), "The delivery dropped a stone into the world");
        } finally {
            dismiss(Helper, Reader);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_planTriggerWindow")
    public static void planTriggerWindow(GameTestHelper Helper) {
        Helper.assertTrue(Scp539Plan.matches("I got the perfect plan"), "The plain phrase did not match");
        Helper.assertTrue(Scp539Plan.matches("i GOT the Perfect plan!!!"), "Case or trailing punctuation broke the match");
        Helper.assertTrue(Scp539Plan.matches("  I got the perfect plan...  "), "Spaces and dots broke the match");
        Helper.assertTrue(Scp539Plan.matches("I got  the perfect plan?!"), "A doubled space broke the match");
        Helper.assertFalse(Scp539Plan.matches("I got a perfect plan"), "A different phrase matched");
        Helper.assertFalse(Scp539Plan.matches("I got the perfect plan for lunch"), "A longer sentence matched");
        Helper.assertFalse(Scp539Plan.matches("!I got the perfect plan"), "Leading punctuation was ignored");
        ServerPlayer User = player(Helper, "scp539_planner", 6.5, 6.5, -90.0f, 0.0f);
        ServerPlayer Dazed = player(Helper, "scp539_dazed", 8.5, 8.5, -90.0f, 0.0f);
        try {
            long Now = now(Helper);
            Helper.assertFalse(postChat(User, "I got the perfect plan."), "The trigger phrase was blocked from chat");
            Helper.assertTrue(Scp539Plan.isArmed(User.getUUID(), Now), "Saying the phrase did not arm the plan");
            Helper.assertTrue(Scp539Plan.claim(User, Now + Scp539Plan.Window), "A throw at the end of the window did not use the plan");
            Helper.assertFalse(Scp539Plan.claim(User, Now + Scp539Plan.Window), "The same phrase was used twice");
            Scp539Plan.clear(User.getUUID());
            Scp539Plan.arm(User, Now);
            Helper.assertFalse(Scp539Plan.claim(User, Now + Scp539Plan.Window + 1L), "A throw after the window still used the plan");
            Scp539Plan.clear(User.getUUID());
            Scp539Compulsion.setUntil(Dazed.getUUID(), Now + 200L);
            Helper.assertTrue(postChat(Dazed, "I got the perfect plan"), "A compelled player's phrase was not replaced");
            Helper.assertFalse(Scp539Plan.isArmed(Dazed.getUUID(), Now), "A compelled player armed the plan");
            Scp539Plan.arm(User, now(Helper));
            Scp539Stone Thrown = Scp539Stone.thrown(Helper.getLevel(), User, new ItemStack(Scp539Registry.Stone.get()), 0.0f, -90.0f, 0.4f, 0.0f);
            Helper.assertTrue(Thrown.isPlan(), "A throw right after the phrase was not a plan throw");
            Thrown.discard();
            Scp539Stone Plain = Scp539Stone.thrown(Helper.getLevel(), Dazed, new ItemStack(Scp539Registry.Stone.get()), 0.0f, -90.0f, 0.4f, 0.0f);
            Helper.assertFalse(Plain.isPlan(), "A throw without the phrase became a plan throw");
            Plain.discard();
        } finally {
            tidy(Helper);
            dismiss(Helper, User, Dazed);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_oneHelicopterPerThrower")
    public static void oneHelicopterPerThrower(GameTestHelper Helper) {
        tidy(Helper);
        ServerPlayer User = player(Helper, "scp539_schemer", 4.5, 12.5, -90.0f, 0.0f);
        ServerLevel World = Helper.getLevel();
        try {
            Scp539Plan.arm(User, now(Helper));
            Scp539Stone First = Scp539Stone.thrown(World, User, new ItemStack(Scp539Registry.Stone.get()), 0.0f, -90.0f, 0.4f, 0.0f);
            Helper.assertTrue(First.isPlan(), "The first plan throw did not count");
            First.land(World, Helper.absoluteVec(new Vec3(12.5, Floor, 12.5)), Direction.UP, Helper.absolutePos(Centre.below()));
            Helper.assertTrue(helicopters(Helper).size() == 1, "The plan throw summoned " + helicopters(Helper).size() + " helicopters");
            Helper.assertTrue(Scp539Plan.busy(World.getServer(), User.getUUID()), "The helicopter is not tied to its thrower");
            Scp539Plan.arm(User, now(Helper));
            Helper.assertFalse(Scp539Plan.claim(User, now(Helper)), "A second plan was allowed while the helicopter was flying");
            for (Scp539Helicopter Craft : helicopters(Helper)) {
                Craft.discard();
            }
            Helper.assertFalse(Scp539Plan.busy(World.getServer(), User.getUUID()), "The thrower stayed busy after the helicopter left");
            Scp539Plan.arm(User, now(Helper));
            Helper.assertFalse(Scp539Plan.claim(User, now(Helper)), "The cooldown did not hold back a new plan");
            Helper.assertTrue(Scp539Plan.readyAt(User.getUUID()) >= now(Helper) + Scp539Plan.Cooldown - 2L, "The cooldown is shorter than expected");
        } finally {
            tidy(Helper);
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_pickupEndsEffect")
    public static void pickupEndsEffect(GameTestHelper Helper) {
        tidy(Helper);
        ServerPlayer User = player(Helper, "scp539_finder", 10.5, 12.5, -90.0f, 0.0f);
        ServerPlayer Other = player(Helper, "scp539_watcher", 14.5, 12.5, 90.0f, 0.0f);
        ServerLevel World = Helper.getLevel();
        try {
            ItemStack Rock = new ItemStack(Scp539Registry.Stone.get());
            Vec3 Spot = Helper.absoluteVec(new Vec3(12.5, Floor, 12.5));
            ItemEntity Dropped = new ItemEntity(World, Spot.x, Spot.y, Spot.z, Rock.copy());
            World.addFreshEntity(Dropped);
            long Now = now(Helper);
            for (ServerPlayer Each : List.of(User, Other)) {
                Scp539Compulsion.compel(Each, World, Dropped.getId(), Spot, Now, Now + Scp539Compulsion.Duration, Now + Scp539Compulsion.Hold);
                Helper.assertTrue(Scp539Compulsion.isAffected(Each), "The rock did not affect " + Each.getGameProfile().getName());
            }
            NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(User, Dropped, Rock.copy()));
            for (ServerPlayer Each : List.of(User, Other)) {
                Helper.assertFalse(Scp539Compulsion.isAffected(Each), "Picking up the rock left " + Each.getGameProfile().getName() + " affected");
                Helper.assertFalse(Scp539Compulsion.isGazing(Each.getUUID(), now(Helper)), "Picking up the rock left " + Each.getGameProfile().getName() + " staring");
            }
            Helper.assertTrue(Scp539Chat.replacement(User) == null, "Chat was still replaced after the rock was picked up");
        } finally {
            tidy(Helper);
            dismiss(Helper, User, Other);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_helicopterFliesAtThrower")
    public static void helicopterFliesAtThrower(GameTestHelper Helper) {
        tidy(Helper);
        ServerPlayer User = player(Helper, "scp539_schemer", 4.5, 12.5, -90.0f, 0.0f);
        ServerLevel World = Helper.getLevel();
        try {
            Scp539Plan.arm(User, now(Helper));
            Scp539Stone Thrown = Scp539Stone.thrown(World, User, new ItemStack(Scp539Registry.Stone.get()), 0.0f, -90.0f, 0.4f, 0.0f);
            Thrown.land(World, Helper.absoluteVec(new Vec3(16.5, Floor, 12.5)), Direction.UP, Helper.absolutePos(Centre.below()));
            List<Scp539Helicopter> Crafts = helicopters(Helper);
            Helper.assertTrue(Crafts.size() == 1, "The plan throw summoned " + Crafts.size() + " helicopters");
            Vec3 End = Crafts.get(0).end();
            double Across = Math.hypot(End.x - User.getX(), End.z - User.getZ());
            Helper.assertTrue(Across < 2.0, "The helicopter is not diving at its thrower, it ends " + String.format("%.1f", Across) + " blocks away");
            Vec3 Start = Crafts.get(0).start();
            Helper.assertTrue(Start.x > User.getX(), "The helicopter does not come in from beyond the rock");
        } finally {
            tidy(Helper);
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_bannerWarnsThoseInDanger")
    public static void bannerWarnsThoseInDanger(GameTestHelper Helper) {
        tidy(Helper);
        Helper.assertTrue(BannerText.equals(Language.getInstance().getOrDefault(BannerKey)), "The banner text reads " + Language.getInstance().getOrDefault(BannerKey));
        ServerLevel World = Helper.getLevel();
        ServerPlayer Target = player(Helper, "scp539_target", 13.5, 12.5, 0.0f, 0.0f);
        ServerPlayer Bystander = player(Helper, "scp539_bystander", 2.5, 2.5, 0.0f, 0.0f);
        try {
            Vec3 Landing = Helper.absoluteVec(new Vec3(12.5, Floor, 12.5));
            Scp539Crash.Route Route = Scp539Crash.plan(World, Landing, Direction.UP, Helper.absolutePos(Centre.below()), new Vec3(1.0, 0.0, 0.0));
            Scp539Helicopter Craft = Scp539Helicopter.launch(World, Route, null, 40);
            Helper.assertTrue(Craft != null, "The helicopter did not launch");
            Craft.warn(World);
            Helper.assertTrue(Craft.warned().contains(Target.getUUID()), "The player at the landing spot was not warned");
            Helper.assertFalse(Craft.warned().contains(Bystander.getUUID()), "A bystander far from the landing spot was warned");
            Craft.discard();
        } finally {
            tidy(Helper);
            dismiss(Helper, Target, Bystander);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp539_helicopterHitsAndLeaves")
    public static void helicopterHitsAndLeaves(GameTestHelper Helper) {
        tidy(Helper);
        ServerLevel World = Helper.getLevel();
        Pig Victim = pig(Helper, 12.5, Floor, 12.5);
        float Full = Victim.getHealth();
        Vec3 Landing = Helper.absoluteVec(new Vec3(12.5, Floor, 12.5));
        Scp539Crash.Route Route = Scp539Crash.plan(World, Landing, Direction.UP, Helper.absolutePos(Centre.below()), new Vec3(1.0, 0.0, 0.0));
        Helper.assertFalse(Route.Wall(), "Open floor was read as a wall");
        Scp539Helicopter Craft = Scp539Helicopter.launch(World, Route, null, 20);
        Helper.assertTrue(Craft != null, "The helicopter did not launch");
        Helper.assertTrue(Craft.position().distanceTo(Route.Start()) < 0.01, "The helicopter did not start far away");
        Helper.assertTrue(Route.Start().distanceTo(Route.End()) > 12.0, "The helicopter starts too close");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Craft.isRemoved(), "The helicopter is still around");
            Helper.assertTrue(Craft.hasCrashed(), "The helicopter left without crashing");
            Helper.assertTrue(Craft.struck().contains(Victim.getId()), "The helicopter flew through the pig without hitting it");
            Helper.assertTrue(!Victim.isAlive() || Victim.getHealth() < Full, "The pig took no damage");
            Helper.assertTrue(helicopters(Helper).isEmpty(), "A helicopter lingered after the crash");
            Victim.discard();
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_crashIntoFloor")
    public static void crashIntoFloor(GameTestHelper Helper) {
        tidy(Helper);
        ServerLevel World = Helper.getLevel();
        GameRules.BooleanValue Rule = griefing(Helper);
        boolean Before = Rule.get();
        try {
            for (boolean Griefing : new boolean[]{true, false}) {
                Rule.set(Griefing, World.getServer());
                fill(Helper, Blocks.OAK_PLANKS, 9, 2, 9, 15, 2, 15);
                fill(Helper, Blocks.AIR, 9, 3, 9, 15, 6, 15);
                int Planks = count(Helper, Blocks.OAK_PLANKS, 9, 2, 9, 15, 2, 15);
                Pig Bystander = pig(Helper, 15.0, 3.0, 12.5);
                float Full = Bystander.getHealth();
                Vec3 Landing = Helper.absoluteVec(new Vec3(12.5, 3.0, 12.5));
                Scp539Crash.Route Route = Scp539Crash.plan(World, Landing, Direction.UP, Helper.absolutePos(new BlockPos(12, 2, 12)), new Vec3(1.0, 0.0, 0.0));
                Helper.assertFalse(Route.Wall(), "Open floor was read as a wall");
                Helper.assertTrue(Route.End().distanceTo(Landing) < 0.01, "The floor crash does not end at the landing spot");
                Helper.assertTrue(Route.Start().y > Route.End().y + 10.0, "The floor crash does not dive from above");
                Scp539Helicopter Craft = Scp539Helicopter.launch(World, Route, null, 30);
                Helper.assertTrue(Craft != null, "The helicopter did not launch");
                Craft.crash(World);
                Helper.assertTrue(Craft.isRemoved(), "The helicopter lingered after the crash");
                int Left = count(Helper, Blocks.OAK_PLANKS, 9, 2, 9, 15, 2, 15);
                if (Griefing) {
                    Helper.assertTrue(Left < Planks, "The floor crash broke no planks with mobGriefing on");
                } else {
                    Helper.assertTrue(Left == Planks, "The floor crash broke " + (Planks - Left) + " planks with mobGriefing off");
                }
                Helper.assertTrue(!Bystander.isAlive() || Bystander.getHealth() < Full, "The crash did not hurt a pig beside it with mobGriefing " + Griefing);
                Bystander.discard();
            }
        } finally {
            Rule.set(Before, World.getServer());
            tidy(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_crashIntoWall")
    public static void crashIntoWall(GameTestHelper Helper) {
        tidy(Helper);
        ServerLevel World = Helper.getLevel();
        GameRules.BooleanValue Rule = griefing(Helper);
        boolean Before = Rule.get();
        try {
            for (boolean Griefing : new boolean[]{true, false}) {
                Rule.set(Griefing, World.getServer());
                fill(Helper, Blocks.AIR, 4, 2, 6, 19, 7, 18);
                fill(Helper, Blocks.OAK_PLANKS, 16, 2, 8, 16, 6, 16);
                fill(Helper, Blocks.OBSIDIAN, 17, 2, 8, 17, 6, 16);
                int Planks = count(Helper, Blocks.OAK_PLANKS, 16, 2, 8, 16, 6, 16);
                int Obsidian = count(Helper, Blocks.OBSIDIAN, 17, 2, 8, 17, 6, 16);
                Vec3 Struck = Helper.absoluteVec(new Vec3(15.88, 3.5, 12.5));
                Scp539Crash.Route Route = Scp539Crash.plan(World, Struck, Direction.WEST, Helper.absolutePos(new BlockPos(16, 3, 12)), new Vec3(1.0, 0.0, 0.0));
                Helper.assertTrue(Route.Wall(), "A stone that struck a wall was not read as a wall crash");
                Helper.assertTrue(Route.Normal().x < -0.9, "The wall crash does not come in from the open side: " + Route.Normal());
                Helper.assertTrue(Math.abs(Route.Start().y - Route.End().y) < 1.0, "The wall crash does not fly in level");
                Helper.assertTrue(Route.Blast().x < Helper.absoluteVec(new Vec3(16.0, 0.0, 0.0)).x && Route.Blast().x > Helper.absoluteVec(new Vec3(15.0, 0.0, 0.0)).x,
                        "The wall crash does not hit the wall face: " + Route.Blast());
                Vec3 Resting = Helper.absoluteVec(new Vec3(15.5, Floor, 12.5));
                Scp539Crash.Route Beside = Scp539Crash.plan(World, Resting, Direction.UP, Helper.absolutePos(new BlockPos(15, 1, 12)), new Vec3(1.0, 0.0, 0.0));
                Helper.assertTrue(Beside.Wall() && Beside.Normal().x < -0.9, "A stone resting against the wall was not read as a wall crash");
                Scp539Helicopter Craft = Scp539Helicopter.launch(World, Route, null, 30);
                Helper.assertTrue(Craft != null, "The helicopter did not launch");
                Craft.crash(World);
                int Left = count(Helper, Blocks.OAK_PLANKS, 16, 2, 8, 16, 6, 16);
                if (Griefing) {
                    Helper.assertTrue(Left < Planks, "The wall crash broke no planks with mobGriefing on");
                } else {
                    Helper.assertTrue(Left == Planks, "The wall crash broke " + (Planks - Left) + " planks with mobGriefing off");
                }
                Helper.assertTrue(count(Helper, Blocks.OBSIDIAN, 17, 2, 8, 17, 6, 16) == Obsidian, "The crash broke blast proof blocks");
            }
        } finally {
            Rule.set(Before, World.getServer());
            fill(Helper, Blocks.AIR, 4, 2, 6, 19, 7, 18);
            tidy(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp539_deathMessageIsDammitCharles")
    public static void deathMessageIsDammitCharles(GameTestHelper Helper) {
        tidy(Helper);
        ServerLevel World = Helper.getLevel();
        Language Words = Language.getInstance();
        Helper.assertTrue(DeathText.equals(Words.getOrDefault(DeathKey)), "The death message reads " + Words.getOrDefault(DeathKey));
        Helper.assertTrue(DeathText.equals(Words.getOrDefault(DeathKey + ".player")), "The player death message reads " + Words.getOrDefault(DeathKey + ".player"));
        Vec3 Landing = Helper.absoluteVec(new Vec3(12.5, Floor, 12.5));
        Scp539Crash.Route Route = Scp539Crash.plan(World, Landing, Direction.UP, Helper.absolutePos(Centre.below()), new Vec3(1.0, 0.0, 0.0));
        Scp539Helicopter Craft = Scp539Helicopter.launch(World, Route, null, 30);
        Helper.assertTrue(Craft != null, "The helicopter did not launch");
        Pig Rammed = pig(Helper, 12.5, Floor, 12.5);
        Rammed.setHealth(1.0f);
        Craft.hit(World, Rammed, new Vec3(-1.0, 0.0, 0.0));
        Helper.assertFalse(Rammed.isAlive(), "The direct hit did not kill a pig on its last heart");
        DamageSource Killer = Rammed.getLastDamageSource();
        Helper.assertTrue(Killer != null && Killer.is(Scp539Registry.HelicopterDamage), "The direct hit used " + Killer);
        Component Message = Killer.getLocalizedDeathMessage(Rammed);
        Helper.assertTrue(DeathKey.equals(key(Message)) && DeathText.equals(Message.getString()), "The direct hit death message reads " + Message.getString());
        Pig Blasted = pig(Helper, 12.5, Floor, 15.6);
        Blasted.setHealth(1.0f);
        Craft.crash(World);
        Helper.assertFalse(Blasted.isAlive(), "The crash did not kill a pig on its last heart");
        Helper.assertFalse(Craft.struck().contains(Blasted.getId()), "The blasted pig was hit by the body instead of the blast");
        DamageSource Blast = Blasted.getLastDamageSource();
        Helper.assertTrue(Blast != null && Blast.is(Scp539Registry.HelicopterDamage), "The crash explosion used " + Blast);
        Helper.assertTrue(DeathText.equals(Blast.getLocalizedDeathMessage(Blasted).getString()), "The crash death message reads " + Blast.getLocalizedDeathMessage(Blasted).getString());
        ServerPlayer Victim = player(Helper, "scp539_victim", 4.5, 4.5, 0.0f, 0.0f);
        try {
            Component PlayerMessage = Scp539Helicopter.damage(World, Craft).getLocalizedDeathMessage(Victim);
            Helper.assertTrue(DeathText.equals(PlayerMessage.getString()), "A player's death message reads " + PlayerMessage.getString());
        } finally {
            dismiss(Helper, Victim);
            tidy(Helper);
        }
        Helper.succeed();
    }
}
