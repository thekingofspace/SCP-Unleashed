package net.rai.scp_unleashed.scp330.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp330.Scp330Barrier;
import net.rai.scp_unleashed.scp330.Scp330Bloom;
import net.rai.scp_unleashed.scp330.Scp330BowlBlockEntity;
import net.rai.scp_unleashed.scp330.Scp330Candies;
import net.rai.scp_unleashed.scp330.Scp330CandyItem;
import net.rai.scp_unleashed.scp330.Scp330Colour;
import net.rai.scp_unleashed.scp330.Scp330Greed;
import net.rai.scp_unleashed.scp330.Scp330Memory;
import net.rai.scp_unleashed.scp330.Scp330Pop;
import net.rai.scp_unleashed.scp330.Scp330Registry;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp330GameTests {
    public static final String Arena = "scp330_arena";
    public static final int FastestDeath = 600;
    public static final int SlowestDeath = 1200;

    private Scp330GameTests() {
    }

    private static Scp330TestPlayer player(GameTestHelper Helper, double X, double Z, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Scp330TestPlayer User = new Scp330TestPlayer(World, "scp330_test");
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(Mode);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static Scp330BowlBlockEntity bowl(GameTestHelper Helper, BlockPos Relative) {
        Helper.setBlock(Relative, Scp330Registry.Bowl.get().defaultBlockState());
        if (Helper.getLevel().getBlockEntity(Helper.absolutePos(Relative)) instanceof Scp330BowlBlockEntity Bowl) {
            return Bowl;
        }
        throw new IllegalStateException("The bowl at " + Relative + " has no block entity");
    }

    private static InteractionResult click(ServerPlayer User, BlockPos Absolute) {
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Absolute), Direction.UP, Absolute, false);
        return User.gameMode.useItemOn(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND, Hit);
    }

    private static int candiesHeld(Player User) {
        int Count = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = User.getInventory().getItem(Slot);
            if (Stack.getItem() instanceof Scp330CandyItem) {
                Count += Stack.getCount();
            }
        }
        return Count;
    }

    private static int candiesLoose(GameTestHelper Helper, Entity Near) {
        int Count = 0;
        for (ItemEntity Found : Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Near.getBoundingBox().inflate(6.0))) {
            if (Found.getItem().getItem() instanceof Scp330CandyItem) {
                Count += Found.getItem().getCount();
            }
        }
        return Count;
    }

    private static boolean looseItem(GameTestHelper Helper, Entity Near, Item Kind) {
        for (ItemEntity Found : Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Near.getBoundingBox().inflate(6.0))) {
            if (Found.getItem().is(Kind)) {
                return true;
            }
        }
        return false;
    }

    private static String bowlsNear(ServerLevel World, BlockPos Centre) {
        StringBuilder Found = new StringBuilder();
        for (BlockPos Each : BlockPos.betweenClosed(Centre.offset(-3, -2, -3), Centre.offset(3, 2, 3))) {
            if (World.getBlockState(Each).is(Scp330Registry.Bowl.get())) {
                Found.append(Each.subtract(Centre).toShortString()).append(' ');
            }
        }
        return Found.toString();
    }

    private static String deathKey(DamageSource Source, LivingEntity Victim) {
        Component Message = Source.getLocalizedDeathMessage(Victim);
        return Message.getContents() instanceof TranslatableContents Translated ? Translated.getKey() : "";
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp330_twoThenGreed")
    public static void twoThenGreed(GameTestHelper Helper) {
        BlockPos Spot = new BlockPos(8, 2, 8);
        Scp330BowlBlockEntity Bowl = bowl(Helper, Spot);
        Scp330TestPlayer User = player(Helper, 8.5, 6.5, GameType.SURVIVAL);
        BlockPos At = Helper.absolutePos(Spot);
        try {
            Helper.assertTrue(click(User, At).consumesAction(), "The first take did not consume the click");
            Helper.assertTrue(click(User, At).consumesAction(), "The second take did not consume the click");
            Helper.assertTrue(candiesHeld(User) == 2, "Two takes gave " + candiesHeld(User) + " candies instead of 2");
            Helper.assertFalse(Scp330Greed.isGreedy(User), "Taking two made the player greedy");
            click(User, At);
            Helper.assertTrue(Scp330Greed.isGreedy(User), "The third take did not make the player greedy");
            Helper.assertTrue(User.hasEffect(Scp330Registry.Greed), "Greed has no effect icon");
            Helper.assertTrue(candiesHeld(User) + candiesLoose(Helper, User) == 2, "The third take gave a candy: held " + candiesHeld(User) + ", loose " + candiesLoose(Helper, User));
            Helper.assertTrue(User.getMainHandItem().isEmpty() && User.getOffhandItem().isEmpty(), "Greed left something in the hands");
            Helper.assertTrue(Bowl.takes(User.getUUID()) == 3, "The bowl remembers " + Bowl.takes(User.getUUID()) + " takes instead of 3");
            Scp330Greed.cure(User);
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp330_creativeFree")
    public static void creativeFree(GameTestHelper Helper) {
        BlockPos Spot = new BlockPos(8, 2, 8);
        Scp330BowlBlockEntity Bowl = bowl(Helper, Spot);
        Scp330TestPlayer User = player(Helper, 8.5, 6.5, GameType.CREATIVE);
        BlockPos At = Helper.absolutePos(Spot);
        try {
            for (int Take = 0; Take < 5; Take++) {
                click(User, At);
            }
            Helper.assertTrue(candiesHeld(User) == 5, "Five creative takes gave " + candiesHeld(User) + " candies");
            Helper.assertFalse(Scp330Greed.isGreedy(User), "A creative player became greedy");
            Helper.assertTrue(Bowl.takers() == 0, "The bowl remembered a creative player");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp330_memoryTravels")
    public static void memoryTravels(GameTestHelper Helper) {
        BlockPos Spot = new BlockPos(6, 2, 8);
        bowl(Helper, Spot);
        Scp330TestPlayer User = player(Helper, 8.5, 6.5, GameType.SURVIVAL);
        ServerLevel World = Helper.getLevel();
        BlockPos At = Helper.absolutePos(Spot);
        try {
            click(User, At);
            click(User, At);
            User.getInventory().clearContent();
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            Helper.assertTrue(User.gameMode.destroyBlock(At), "The pickaxe could not break the bowl");
            Helper.assertTrue(World.getBlockState(At).isAir(), "The bowl is still standing");
            ItemEntity Dropped = null;
            for (ItemEntity Found : World.getEntitiesOfClass(ItemEntity.class, new AABB(At).inflate(3.0))) {
                if (Found.getItem().is(Scp330Registry.BowlItem.get())) {
                    Dropped = Found;
                }
            }
            Helper.assertTrue(Dropped != null, "Breaking the bowl with a pickaxe dropped nothing");
            ItemStack Carried = Dropped.getItem().copy();
            Dropped.discard();
            Scp330Memory Remembered = Carried.get(Scp330Registry.Memory.get());
            Helper.assertTrue(Remembered != null && Remembered.Takes().getOrDefault(User.getUUID(), 0) == 2, "The dropped bowl forgot the two takes: " + Remembered);
            BlockPos Floor = Helper.absolutePos(new BlockPos(11, 1, 8));
            User.setItemInHand(InteractionHand.MAIN_HAND, Carried);
            InteractionResult Placed = User.gameMode.useItemOn(User, World, User.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false));
            BlockPos Moved = Floor.above();
            Helper.assertTrue(Placed.consumesAction() && World.getBlockState(Moved).is(Scp330Registry.Bowl.get()), "The bowl could not be placed again: " + Placed
                    + ", at the spot " + World.getBlockState(Moved) + ", below " + World.getBlockState(Floor) + ", hand " + User.getMainHandItem() + ", near " + bowlsNear(World, Moved));
            Helper.assertTrue(World.getBlockEntity(Moved) instanceof Scp330BowlBlockEntity Again && Again.takes(User.getUUID()) == 2, "The placed bowl forgot the takes");
            User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            click(User, Moved);
            Helper.assertTrue(Scp330Greed.isGreedy(User), "The first take from the moved bowl was not greedy");
            Scp330Greed.cure(User);
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp330_handsGone")
    public static void handsGone(GameTestHelper Helper) {
        Scp330TestPlayer User = player(Helper, 8.5, 8.5, GameType.SURVIVAL);
        User.getInventory().selected = 0;
        User.getInventory().setItem(0, new ItemStack(Items.STONE, 10));
        User.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIRT, 5));
        User.getInventory().setItem(3, new ItemStack(Items.BREAD, 2));
        Scp330Greed.afflict(User);
        Helper.assertTrue(User.getMainHandItem().isEmpty(), "The main hand kept its stone");
        Helper.assertTrue(User.getOffhandItem().isEmpty(), "The off-hand kept its dirt");
        Helper.assertTrue(looseItem(Helper, User, Items.STONE) && looseItem(Helper, User, Items.DIRT), "The emptied hands did not drop their items");
        User.getInventory().selected = 3;
        Helper.startSequence().thenIdle(2).thenExecute(() -> {
            Helper.assertTrue(User.getInventory().getItem(3).isEmpty(), "Bread selected into the main hand was not dropped");
            Helper.assertTrue(looseItem(Helper, User, Items.BREAD), "The bread did not land on the ground");
            ItemEntity Apple = new ItemEntity(Helper.getLevel(), User.getX(), User.getY(), User.getZ(), new ItemStack(Items.APPLE, 3));
            Apple.setNoPickUpDelay();
            Helper.getLevel().addFreshEntity(Apple);
            Apple.playerTouch(User);
            Helper.assertTrue(User.getMainHandItem().isEmpty(), "A picked up apple went into the main hand");
            Helper.assertTrue(User.getInventory().countItem(Items.APPLE) == 3, "The apples were not picked into another slot");
            Helper.assertTrue(Apple.isRemoved(), "The apple entity is still lying there");
            Scp330Greed.cure(User);
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1300, batch = "scp330_greedKills")
    public static void greedKills(GameTestHelper Helper) {
        Scp330TestPlayer User = player(Helper, 8.5, 8.5, GameType.SURVIVAL);
        User.setHealth(User.getMaxHealth());
        new ItemStack(Scp330Registry.candy(Scp330Colour.Red)).finishUsingItem(Helper.getLevel(), User);
        Scp330Greed.afflict(User);
        long Start = Helper.getTick();
        float[] Lowest = {User.getHealth()};
        boolean[] Rose = {false};
        boolean[] RegenMidway = {false};
        long[] Died = {-1L};
        Helper.onEachTick(() -> {
            if (User.isRemoved()) {
                return;
            }
            if (!User.isDeadOrDying()) {
                User.doTick();
            }
            float Health = User.getHealth();
            if (Health > Lowest[0] + 0.001f && !User.isDeadOrDying()) {
                Rose[0] = true;
            }
            Lowest[0] = Math.min(Lowest[0], Health);
            if (Helper.getTick() - Start == 300) {
                RegenMidway[0] = User.hasEffect(MobEffects.REGENERATION);
            }
            if (User.isDeadOrDying() && Died[0] < 0) {
                Died[0] = Helper.getTick() - Start;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Died[0] >= 0, "Greed has not killed yet");
            Helper.assertTrue(RegenMidway[0], "The Red candy's regeneration was not active while greedy");
            Helper.assertFalse(Rose[0], "Health rose while greedy");
            Helper.assertTrue(Died[0] >= FastestDeath && Died[0] <= SlowestDeath, "Greed killed after " + Died[0] + " ticks, outside " + FastestDeath + ".." + SlowestDeath);
            DamageSource Last = User.getLastDamageSource();
            Helper.assertTrue(Last != null && Last.is(Scp330Registry.GreedDamage), "The killing blow was not greed: " + Last);
            Helper.assertTrue(deathKey(Last, User).equals("death.attack.scp_unleashed.scp330_greed"), "Wrong death message " + deathKey(Last, User));
            Helper.assertFalse(Scp330Greed.isGreedy(User), "Greed outlived the player");
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp330_cureClears")
    public static void cureClears(GameTestHelper Helper) {
        Scp330TestPlayer User = player(Helper, 8.5, 8.5, GameType.SURVIVAL);
        Scp330Greed.afflict(User);
        Helper.assertTrue(AnomalyCure.cure(User, null), "SCP-006's cure event did not report a cure");
        Helper.assertFalse(Scp330Greed.isGreedy(User), "The cure left the player greedy");
        Helper.assertFalse(User.hasEffect(Scp330Registry.Greed), "The cure left the greed icon");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        Helper.startSequence().thenIdle(3).thenExecute(() -> {
            Helper.assertTrue(User.getMainHandItem().is(Items.STICK), "Cured hands still drop what they hold");
            Helper.assertFalse(AnomalyCure.cure(User, null), "Curing a cured player reported a cure again");
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp330_blueShell")
    public static void blueShell(GameTestHelper Helper) {
        Scp330TestPlayer User = player(Helper, 8.5, 8.5, GameType.SURVIVAL);
        try {
            User.setHealth(20.0f);
            Scp330Candies.eat(User, Scp330Colour.Blue);
            Helper.assertTrue(Math.abs(Scp330Barrier.pool(User) - 10.0f) < 0.001f, "The barrier pool is " + Scp330Barrier.pool(User));
            User.hurt(User.damageSources().generic(), 4.0f);
            User.hurt(User.damageSources().generic(), 4.0f);
            Helper.assertTrue(User.getHealth() == 20.0f, "The barrier let damage through early, health " + User.getHealth());
            Helper.assertTrue(Math.abs(Scp330Barrier.pool(User) - 2.0f) < 0.001f, "The pool after 8 damage is " + Scp330Barrier.pool(User));
            User.hurt(User.damageSources().generic(), 4.0f);
            Helper.assertTrue(Math.abs(User.getHealth() - 18.0f) < 0.001f, "Exactly 10 damage should have been absorbed, health " + User.getHealth());
            Helper.assertFalse(User.hasEffect(Scp330Registry.Barrier), "The barrier did not break once spent");
            Scp330Candies.eat(User, Scp330Colour.Blue);
            User.hurt(Scp330Greed.source(User), 1.0f);
            Helper.assertTrue(Math.abs(User.getHealth() - 17.0f) < 0.001f, "The barrier stopped greed damage, health " + User.getHealth());
            Helper.assertTrue(Math.abs(Scp330Barrier.pool(User) - 10.0f) < 0.001f, "Greed damage drained the barrier");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp330_redRegen")
    public static void redRegen(GameTestHelper Helper) {
        Scp330TestPlayer User = player(Helper, 8.5, 8.5, GameType.SURVIVAL);
        try {
            Scp330Candies.eat(User, Scp330Colour.Red);
            MobEffectInstance Regen = User.getEffect(MobEffects.REGENERATION);
            Helper.assertTrue(Regen != null && Regen.getAmplifier() == 4 && Regen.getDuration() == 600, "Red gave " + Regen + " instead of Regeneration V for 600 ticks");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp330_greenGrows")
    public static void greenGrows(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Wheat = new BlockPos(4, 3, 8);
        BlockPos Sapling = new BlockPos(11, 3, 8);
        Helper.setBlock(Wheat.below(), Blocks.FARMLAND.defaultBlockState());
        Helper.setBlock(Wheat, Blocks.WHEAT.defaultBlockState());
        Helper.setBlock(Sapling.below(), Blocks.DIRT.defaultBlockState());
        Helper.setBlock(Sapling, Blocks.OAK_SAPLING.defaultBlockState());
        Scp330TestPlayer User = player(Helper, 8.5, 6.5, GameType.SURVIVAL);
        Scp330Candies.eat(User, Scp330Colour.Green);
        BlockPos WheatAt = Helper.absolutePos(Wheat);
        BlockPos SaplingAt = Helper.absolutePos(Sapling);
        Helper.assertTrue(Scp330Bloom.use(User, World, WheatAt) == Scp330Bloom.Result.Grew, "Green did not grow the wheat");
        int AgeOnce = World.getBlockState(WheatAt).getValue(CropBlock.AGE);
        Helper.assertTrue(AgeOnce > 0, "The wheat did not age");
        Helper.assertTrue(Scp330Bloom.use(User, World, WheatAt) == Scp330Bloom.Result.Resting, "A second click in the same tick was not held back");
        Helper.assertTrue(World.getBlockState(WheatAt).getValue(CropBlock.AGE) == AgeOnce, "The cooldown let the wheat grow twice");
        int[] Clicks = {0};
        Helper.startSequence().thenIdle(Scp330Bloom.Cooldown + 1).thenExecute(() -> {
            BlockState Before = World.getBlockState(WheatAt);
            InteractionResult Used = User.gameMode.useItemOn(User, World, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(WheatAt), Direction.UP, WheatAt, false));
            Helper.assertTrue(Used.consumesAction(), "Right-clicking the wheat with Green did nothing: " + Used);
            Helper.assertTrue(!Before.is(Blocks.WHEAT) || World.getBlockState(WheatAt).getValue(CropBlock.AGE) > Before.getValue(CropBlock.AGE)
                    || Before.getValue(CropBlock.AGE) == CropBlock.MAX_AGE, "The right-click did not grow the wheat");
        }).thenExecuteFor(400, () -> {
            if (World.getBlockState(SaplingAt).is(Blocks.OAK_SAPLING) && Helper.getTick() % (Scp330Bloom.Cooldown + 1) == 0) {
                Scp330Bloom.use(User, World, SaplingAt);
                Clicks[0]++;
            }
        }).thenExecute(() -> {
            Helper.assertTrue(World.getBlockState(SaplingAt).is(Blocks.OAK_LOG), "The sapling never grew after " + Clicks[0] + " clicks: " + World.getBlockState(SaplingAt));
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp330_blackHits")
    public static void blackHits(GameTestHelper Helper) {
        Scp330TestPlayer User = player(Helper, 8.5, 6.5, GameType.SURVIVAL);
        Pig Plain = Helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 10));
        Pig Struck = Helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(8, 2, 10));
        Pig Shot = Helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(12, 2, 10));
        try {
            Plain.hurt(User.damageSources().playerAttack(User), 1.0f);
            Helper.assertTrue(Math.abs(Plain.getHealth() - 9.0f) < 0.001f, "A plain punch did " + (10.0f - Plain.getHealth()));
            Scp330Candies.eat(User, Scp330Colour.Black);
            Struck.hurt(User.damageSources().playerAttack(User), 1.0f);
            Helper.assertTrue(Math.abs(Struck.getHealth() - 5.0f) < 0.001f, "Black multiplied a punch to " + (10.0f - Struck.getHealth()) + " instead of 5");
            Arrow Bolt = new Arrow(EntityType.ARROW, Helper.getLevel());
            Bolt.setOwner(User);
            Shot.hurt(User.damageSources().arrow(Bolt, User), 1.0f);
            Helper.assertTrue(Math.abs(Shot.getHealth() - 5.0f) < 0.001f, "Black multiplied an arrow to " + (10.0f - Shot.getHealth()) + " instead of 5");
            Bolt.discard();
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp330_pinkPops")
    public static void pinkPops(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (int X = 5; X <= 11; X++) {
            for (int Z = 5; Z <= 11; Z++) {
                Helper.setBlock(new BlockPos(X, 2, Z), Blocks.DIRT.defaultBlockState());
            }
        }
        Helper.setBlock(new BlockPos(8, 2, 10), Blocks.OBSIDIAN.defaultBlockState());
        Helper.setBlock(new BlockPos(1, 2, 1), Blocks.DIRT.defaultBlockState());
        Scp330TestPlayer User = player(Helper, 8.5, 8.5, GameType.SURVIVAL);
        User.moveTo(User.getX(), User.getY() + 1.0, User.getZ());
        boolean Pvp = World.getServer().isPvpAllowed();
        World.getServer().setPvpAllowed(true);
        Explosion Blast = Scp330Pop.pop(User);
        World.getServer().setPvpAllowed(Pvp);
        Helper.assertTrue(Blast != null, "Pink did not explode");
        Helper.assertTrue(Blast.radius() == 4.0f, "The pop has power " + Blast.radius() + " instead of TNT's 4");
        Explosion.BlockInteraction Expected = World.getGameRules().getBoolean(GameRules.RULE_TNT_EXPLOSION_DROP_DECAY)
                ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY;
        Helper.assertTrue(Blast.getBlockInteraction() == Expected, "The pop breaks blocks as " + Blast.getBlockInteraction() + ", TNT would " + Expected);
        Helper.assertBlockNotPresent(Blocks.DIRT, new BlockPos(8, 2, 8));
        Helper.assertBlockNotPresent(Blocks.DIRT, new BlockPos(7, 2, 8));
        Helper.assertBlockNotPresent(Blocks.DIRT, new BlockPos(9, 2, 9));
        Helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(8, 2, 10));
        Helper.assertBlockPresent(Blocks.DIRT, new BlockPos(1, 2, 1));
        Helper.assertTrue(User.isDeadOrDying(), "An unarmoured eater survived the pop with " + User.getHealth() + " health");
        DamageSource Last = User.getLastDamageSource();
        Helper.assertTrue(Last != null && deathKey(Last, User).equals("death.attack.scp_unleashed.scp330_pop"), "Wrong pop death message: " + (Last == null ? "none" : deathKey(Last, User)));
        dismiss(Helper, User);
        Helper.succeed();
    }

    public static final class DeathWatch {
        private final UUID Controller;
        private final UUID Body;
        private int ControllerDeaths;
        private int ControllerCancelled;
        private int BodyDeaths;

        public DeathWatch(UUID Controller, UUID Body) {
            this.Controller = Controller;
            this.Body = Body;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public void died(LivingDeathEvent Event) {
            UUID Dead = Event.getEntity().getUUID();
            if (Dead.equals(Controller) && Event.getEntity() instanceof ServerPlayer) {
                ControllerDeaths++;
                if (Event.isCanceled()) {
                    ControllerCancelled++;
                }
            } else if (Dead.equals(Body) && !Event.isCanceled()) {
                BodyDeaths++;
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp330_dclassDeath")
    public static void dclassDeath(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp330TestPlayer User = player(Helper, 8.5, 6.5, GameType.SURVIVAL);
        User.enlist(true);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(8, 2, 8));
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "The test player could not take control of the D-Class");
        Helper.assertTrue(ControlSessions.isControlling(User), "No control session after possessing");
        DeathWatch Watch = new DeathWatch(User.getUUID(), BodyId);
        NeoForge.EVENT_BUS.register(Watch);
        User.setHealth(0.2f);
        Scp330Greed.afflict(User);
        Helper.succeedWhen(() -> {
            Helper.assertFalse(ControlSessions.isControlling(User), "The controlled body has not died yet");
            Helper.assertFalse(ControlManager.hasPendingDeath(User), "A deferred death is still pending");
            Helper.assertTrue(Watch.ControllerDeaths == 1 && Watch.ControllerCancelled == 1, "The controller saw " + Watch.ControllerDeaths + " deaths, " + Watch.ControllerCancelled + " deferred");
            Helper.assertTrue(Watch.BodyDeaths == 1, "The D-Class body died " + Watch.BodyDeaths + " times");
            Helper.assertTrue(User.isAlive() && User.getHealth() > 1.0f, "The controller did not return home alive");
            Helper.assertFalse(Scp330Greed.isGreedy(User), "Greed followed the controller home");
            Entity Left = World.getEntity(BodyId);
            Helper.assertTrue(Left == null || !Left.isAlive(), "The greedy D-Class body is still alive");
            NeoForge.EVENT_BUS.unregister(Watch);
            User.enlist(false);
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp330_dclassRelease")
    public static void dclassRelease(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp330TestPlayer User = player(Helper, 8.5, 6.5, GameType.SURVIVAL);
        User.enlist(true);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(8, 2, 8));
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "The test player could not take control of the D-Class");
        Scp330Greed.afflict(User);
        ControlManager.release(User);
        Helper.assertFalse(ControlSessions.isControlling(User), "Release did not end the session");
        Helper.assertFalse(Scp330Greed.isGreedy(User), "Greed stayed with the controller after release");
        Helper.assertFalse(User.hasEffect(Scp330Registry.Greed), "The greed icon stayed with the controller");
        Entity Found = World.getEntity(BodyId);
        Helper.assertTrue(Found instanceof LivingEntity, "The released body is missing");
        LivingEntity Released = (LivingEntity) Found;
        Helper.assertTrue(Scp330Greed.isGreedy(Released), "Greed did not stay with the D-Class body");
        float Start = Released.getHealth();
        Released.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        Helper.startSequence().thenIdle(Scp330Greed.Delay + Scp330Greed.Interval * 2 + 2).thenExecute(() -> {
            Helper.assertTrue(Released.getMainHandItem().isEmpty(), "The greedy D-Class kept its bread");
            Helper.assertTrue(Released.getHealth() < Start, "Greed did not hurt the released D-Class: " + Released.getHealth() + " of " + Start);
            Scp330Greed.cure(Released);
            Released.discard();
            User.enlist(false);
            dismiss(Helper, User);
        }).thenSucceed();
    }
}
