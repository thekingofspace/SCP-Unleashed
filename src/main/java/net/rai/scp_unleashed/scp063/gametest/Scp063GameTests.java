package net.rai.scp_unleashed.scp063.gametest;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp063.Scp063Brushing;
import net.rai.scp_unleashed.scp063.Scp063Erosion;
import net.rai.scp_unleashed.scp063.Scp063Registry;
import net.rai.scp_unleashed.scp063.Scp063RestingBlock;
import net.rai.scp_unleashed.scp063.Scp063RestingBlockEntity;
import net.rai.scp_unleashed.scp063.Scp063Scrubbing;
import net.rai.scp_unleashed.scp063.Scp063ToothbrushItem;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp063GameTests {
    public static final String Arena = "scp063_arena";
    public static final int LayerTicks = Scp063Erosion.ErodeTicks * 9;
    public static final String BrushName = "Exhibit 63";

    private Scp063GameTests() {
    }

    private static void fill(GameTestHelper Helper, int Y, int Radius, BlockState State) {
        for (int X = 8 - Radius; X <= 8 + Radius; X++) {
            for (int Z = 8 - Radius; Z <= 8 + Radius; Z++) {
                Helper.setBlock(new BlockPos(X, Y, Z), State);
            }
        }
    }

    private static ItemStack named() {
        ItemStack Stack = new ItemStack(Scp063Registry.Toothbrush.get());
        Stack.set(DataComponents.CUSTOM_NAME, Component.literal(BrushName));
        return Stack;
    }

    private static void rest(GameTestHelper Helper, BlockPos Pos, ItemStack Stack) {
        Helper.setBlock(Pos, Scp063Registry.Resting.get().defaultBlockState().setValue(Scp063RestingBlock.Facing, Direction.SOUTH));
        Scp063RestingBlockEntity Rest = Helper.getBlockEntity(Pos);
        Rest.rest(Stack, null);
    }

    private static List<BlockPos> brushes(GameTestHelper Helper) {
        List<BlockPos> Found = new ArrayList<>();
        for (int X = 0; X < 16; X++) {
            for (int Y = 0; Y < 10; Y++) {
                for (int Z = 0; Z < 16; Z++) {
                    BlockPos Pos = new BlockPos(X, Y, Z);
                    if (Helper.getBlockState(Pos).is(Scp063Registry.Resting.get())) {
                        Found.add(Pos);
                    }
                }
            }
        }
        return Found;
    }

    private static List<ItemEntity> drops(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Helper.getBounds().inflate(2.0));
    }

    private static List<BlockState> snapshot(GameTestHelper Helper) {
        List<BlockState> States = new ArrayList<>();
        for (int X = 5; X <= 11; X++) {
            for (int Y = 0; Y < 6; Y++) {
                for (int Z = 5; Z <= 11; Z++) {
                    States.add(Helper.getBlockState(new BlockPos(X, Y, Z)));
                }
            }
        }
        return States;
    }

    private static List<Block> blocks(GameTestHelper Helper) {
        return snapshot(Helper).stream().map(BlockState::getBlock).toList();
    }

    private static void layerGone(GameTestHelper Helper, int Y, String Label) {
        for (int X = 7; X <= 9; X++) {
            for (int Z = 7; Z <= 9; Z++) {
                BlockPos Pos = new BlockPos(X, Y, Z);
                BlockState State = Helper.getBlockState(Pos);
                Helper.assertTrue(State.isAir() || State.is(Scp063Registry.Resting.get()), Label + ": " + State + " survived at " + Pos);
            }
        }
    }

    private static void ringIntact(GameTestHelper Helper, int Y, Block Expected) {
        for (int X = 6; X <= 10; X++) {
            for (int Z = 6; Z <= 10; Z++) {
                if (Math.abs(X - 8) == 2 || Math.abs(Z - 8) == 2) {
                    Helper.assertTrue(Helper.getBlockState(new BlockPos(X, Y, Z)).is(Expected), "The outer ring was eroded at " + X + "," + Y + "," + Z);
                }
            }
        }
    }

    private static void watchForCopies(GameTestHelper Helper, int[] Most) {
        Helper.onEachTick(() -> Most[0] = Math.max(Most[0], brushes(Helper).size()));
    }

    private static Scp063TestPlayer player(GameTestHelper Helper, Vec3 Relative, GameType Mode, String Name) {
        ServerLevel World = Helper.getLevel();
        Scp063TestPlayer User = new Scp063TestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(Relative);
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(Mode);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Scp063Scrubbing.stop(User);
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static Scp063TestPlayer scrubber(GameTestHelper Helper, GameType Mode, String Name) {
        fill(Helper, 1, 2, Blocks.SMOOTH_STONE.defaultBlockState());
        Scp063TestPlayer User = player(Helper, new Vec3(8.5, 2.0, 6.5), Mode, Name);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp063Registry.Toothbrush.get()));
        User.lookAt(EntityAnchorArgument.Anchor.EYES, Helper.absoluteVec(new Vec3(8.5, 2.0, 8.5)));
        return User;
    }

    private static void clickTarget(GameTestHelper Helper, ServerPlayer User) {
        BlockPos Target = Helper.absolutePos(new BlockPos(8, 1, 8));
        BlockHitResult Hit = new BlockHitResult(Helper.absoluteVec(new Vec3(8.5, 2.0, 8.5)), Direction.UP, Target, false);
        User.gameMode.useItemOn(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND, Hit);
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp063_grassStaysPut")
    public static void grassStaysPut(GameTestHelper Helper) {
        fill(Helper, 0, 2, Blocks.STONE.defaultBlockState());
        fill(Helper, 1, 2, Blocks.STONE.defaultBlockState());
        fill(Helper, 1, 1, Blocks.GRASS_BLOCK.defaultBlockState());
        rest(Helper, new BlockPos(8, 2, 8), named());
        List<BlockState> Before = snapshot(Helper);
        Helper.runAfterDelay(LayerTicks, () -> {
            Helper.assertTrue(snapshot(Helper).equals(Before), "Something changed under SCP-063 resting on grass");
            Scp063RestingBlockEntity Rest = Helper.getBlockEntity(new BlockPos(8, 2, 8));
            Helper.assertFalse(Rest.isActive(), "SCP-063 is working on grass");
            Helper.assertTrue(drops(Helper).isEmpty(), "Something dropped");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp063_logsAndLeavesStayPut")
    public static void logsAndLeavesStayPut(GameTestHelper Helper) {
        fill(Helper, 1, 2, Blocks.OAK_LOG.defaultBlockState());
        fill(Helper, 2, 2, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        rest(Helper, new BlockPos(8, 3, 8), named());
        List<Block> Before = blocks(Helper);
        Helper.runAfterDelay(LayerTicks, () -> {
            Helper.assertTrue(blocks(Helper).equals(Before), "Something changed under SCP-063 resting on leaves over logs");
            Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 3, 8)).is(Scp063Registry.Resting.get()), "SCP-063 moved off the leaves");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp063_stoneErodesAndSinks")
    public static void stoneErodesAndSinks(GameTestHelper Helper) {
        fill(Helper, 1, 2, Blocks.DIRT.defaultBlockState());
        fill(Helper, 2, 2, Blocks.STONE.defaultBlockState());
        fill(Helper, 3, 2, Blocks.STONE.defaultBlockState());
        rest(Helper, new BlockPos(8, 4, 8), named());
        int[] Most = {0};
        watchForCopies(Helper, Most);
        Helper.startSequence()
                .thenExecuteAfter(Scp063Erosion.ErodeTicks * 3 + 2, () -> {
                    int Gone = 0;
                    for (int X = 7; X <= 9; X++) {
                        for (int Z = 7; Z <= 9; Z++) {
                            Gone += Helper.getBlockState(new BlockPos(X, 3, Z)).isAir() ? 1 : 0;
                        }
                    }
                    Helper.assertTrue(Gone == 3, "Expected three blocks worn away after three intervals, found " + Gone);
                    Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 3, 8)).is(Blocks.STONE), "The centre went before the ring");
                    Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 4, 8)).is(Scp063Registry.Resting.get()), "SCP-063 moved too early");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 3, 8)).is(Scp063Registry.Resting.get()), "SCP-063 has not settled to the next layer"))
                .thenExecute(() -> {
                    layerGone(Helper, 3, "First layer");
                    ringIntact(Helper, 3, Blocks.STONE);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 2, 8)).is(Scp063Registry.Resting.get()), "SCP-063 has not reached the dirt"))
                .thenExecuteAfter(LayerTicks, () -> {
                    layerGone(Helper, 2, "Second layer");
                    ringIntact(Helper, 2, Blocks.STONE);
                    for (int X = 6; X <= 10; X++) {
                        for (int Z = 6; Z <= 10; Z++) {
                            Helper.assertTrue(Helper.getBlockState(new BlockPos(X, 1, Z)).is(Blocks.DIRT), "The dirt was eroded at " + X + "," + Z);
                        }
                    }
                    Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 2, 8)).is(Scp063Registry.Resting.get()), "SCP-063 did not stay on the dirt");
                    Scp063RestingBlockEntity Rest = Helper.getBlockEntity(new BlockPos(8, 2, 8));
                    Helper.assertFalse(Rest.isActive(), "SCP-063 is still working on dirt");
                    Helper.assertTrue(drops(Helper).isEmpty(), "Erosion dropped items: " + drops(Helper));
                    Helper.assertTrue(Most[0] == 1, "Saw " + Most[0] + " copies of SCP-063 at once");
                    Helper.assertTrue(brushes(Helper).size() == 1, "SCP-063 count is " + brushes(Helper).size());
                    Helper.assertTrue(Rest.kept().getHoverName().getString().equals(BrushName), "The resting toothbrush lost its name");
                    Helper.getLevel().destroyBlock(Helper.absolutePos(new BlockPos(8, 2, 8)), true);
                    List<ItemEntity> Dropped = drops(Helper);
                    Helper.assertTrue(Dropped.size() == 1 && Dropped.get(0).getItem().getCount() == 1 && Scp063ToothbrushItem.is(Dropped.get(0).getItem()),
                            "Breaking SCP-063 should drop exactly one toothbrush: " + Dropped);
                    Helper.assertTrue(Dropped.get(0).getItem().getHoverName().getString().equals(BrushName), "The dropped toothbrush lost its name");
                    Dropped.forEach(Entity::discard);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp063_fallsAndStopsOnBedrock")
    public static void fallsAndStopsOnBedrock(GameTestHelper Helper) {
        fill(Helper, 1, 2, Blocks.BEDROCK.defaultBlockState());
        fill(Helper, 2, 2, Blocks.STONE.defaultBlockState());
        rest(Helper, new BlockPos(8, 6, 8), new ItemStack(Scp063Registry.Toothbrush.get()));
        int[] Most = {0};
        watchForCopies(Helper, Most);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 3, 8)).is(Scp063Registry.Resting.get()), "SCP-063 did not fall onto the stone"))
                .thenWaitUntil(() -> Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 2, 8)).is(Scp063Registry.Resting.get()), "SCP-063 did not sink onto the bedrock"))
                .thenExecuteAfter(LayerTicks, () -> {
                    layerGone(Helper, 2, "Stone layer");
                    for (int X = 6; X <= 10; X++) {
                        for (int Z = 6; Z <= 10; Z++) {
                            Helper.assertTrue(Helper.getBlockState(new BlockPos(X, 1, Z)).is(Blocks.BEDROCK), "Bedrock was eroded at " + X + "," + Z);
                        }
                    }
                    Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 2, 8)).is(Scp063Registry.Resting.get()), "SCP-063 left the bedrock");
                    Helper.assertTrue(Most[0] == 1, "Saw " + Most[0] + " copies of SCP-063 at once");
                    Helper.assertTrue(drops(Helper).isEmpty(), "Erosion dropped items");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp063_unbreakableStaysPut")
    public static void unbreakableStaysPut(GameTestHelper Helper) {
        fill(Helper, 1, 2, Blocks.STONE.defaultBlockState());
        Helper.setBlock(new BlockPos(8, 1, 8), Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
        Helper.setBlock(new BlockPos(2, 0, 2), Blocks.BARRIER.defaultBlockState());
        rest(Helper, new BlockPos(8, 2, 8), new ItemStack(Scp063Registry.Toothbrush.get()));
        rest(Helper, new BlockPos(2, 1, 2), new ItemStack(Scp063Registry.Toothbrush.get()));
        List<BlockState> Before = snapshot(Helper);
        Helper.runAfterDelay(LayerTicks, () -> {
            Helper.assertTrue(snapshot(Helper).equals(Before), "SCP-063 resting on reinforced deepslate wore something away");
            Helper.assertTrue(Helper.getBlockState(new BlockPos(2, 0, 2)).is(Blocks.BARRIER), "The barrier was eroded");
            Helper.assertTrue(Helper.getBlockState(new BlockPos(2, 1, 2)).is(Scp063Registry.Resting.get()), "SCP-063 left the barrier");
            Helper.assertTrue(Scp063Erosion.isUnbreakable(Helper.getLevel(), Helper.absolutePos(new BlockPos(2, 0, 2)), Blocks.BARRIER.defaultBlockState()),
                    "Barriers do not count as unbreakable");
            Helper.assertTrue(Scp063Erosion.isUnbreakable(Helper.getLevel(), Helper.absolutePos(new BlockPos(8, 1, 8)), Blocks.BEDROCK.defaultBlockState()),
                    "Bedrock does not count as unbreakable");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp063_ruleOffStopsErosion")
    public static void ruleOffStopsErosion(GameTestHelper Helper) {
        GameRules.BooleanValue Rule = Helper.getLevel().getGameRules().getRule(Scp063Registry.ErosionRule);
        Rule.set(false, Helper.getLevel().getServer());
        fill(Helper, 1, 2, Blocks.STONE.defaultBlockState());
        rest(Helper, new BlockPos(8, 2, 8), new ItemStack(Scp063Registry.Toothbrush.get()));
        List<BlockState> Before = snapshot(Helper);
        Helper.runAfterDelay(LayerTicks, () -> {
            boolean Unchanged = snapshot(Helper).equals(Before);
            Scp063RestingBlockEntity Rest = Helper.getBlockEntity(new BlockPos(8, 2, 8));
            boolean Idle = !Rest.isActive();
            Rule.set(true, Helper.getLevel().getServer());
            Helper.assertTrue(Unchanged, "Stone wore away with scp063Erosion off");
            Helper.assertTrue(Idle, "SCP-063 is working with scp063Erosion off");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp063_containersAreSpared")
    public static void containersAreSpared(GameTestHelper Helper) {
        fill(Helper, 1, 2, Blocks.DIRT.defaultBlockState());
        fill(Helper, 2, 1, Blocks.STONE.defaultBlockState());
        Helper.setBlock(new BlockPos(9, 2, 8), Blocks.CHEST.defaultBlockState());
        BlockEntity ChestEntity = Helper.getBlockEntity(new BlockPos(9, 2, 8));
        ((Container) ChestEntity).setItem(0, new ItemStack(Items.DIAMOND, 3));
        Helper.setBlock(new BlockPos(3, 1, 3), Blocks.BARREL.defaultBlockState());
        rest(Helper, new BlockPos(8, 3, 8), new ItemStack(Scp063Registry.Toothbrush.get()));
        rest(Helper, new BlockPos(3, 2, 3), new ItemStack(Scp063Registry.Toothbrush.get()));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 2, 8)).is(Scp063Registry.Resting.get()), "SCP-063 has not sunk past the chest"))
                .thenExecuteAfter(Scp063Erosion.ErodeTicks * 2, () -> {
                    Helper.assertTrue(Helper.getBlockState(new BlockPos(9, 2, 8)).is(Blocks.CHEST), "The chest was eroded");
                    BlockEntity Kept = Helper.getBlockEntity(new BlockPos(9, 2, 8));
                    ItemStack Inside = ((Container) Kept).getItem(0);
                    Helper.assertTrue(Inside.is(Items.DIAMOND) && Inside.getCount() == 3, "The chest lost its contents");
                    for (int X = 7; X <= 9; X++) {
                        for (int Z = 7; Z <= 9; Z++) {
                            if (X != 9 || Z != 8) {
                                Helper.assertTrue(Helper.getBlockState(new BlockPos(X, 2, Z)).isAir() || (X == 8 && Z == 8), "Stone survived at " + X + ",2," + Z);
                            }
                        }
                    }
                    Helper.assertTrue(Helper.getBlockState(new BlockPos(3, 1, 3)).is(Blocks.BARREL), "The barrel under SCP-063 was eroded");
                    Helper.assertTrue(Helper.getBlockState(new BlockPos(3, 2, 3)).is(Scp063Registry.Resting.get()), "SCP-063 left the barrel");
                    Helper.assertTrue(drops(Helper).isEmpty(), "Erosion dropped items");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp063_handScrubRemovesStone")
    public static void handScrubRemovesStone(GameTestHelper Helper) {
        Scp063TestPlayer User = scrubber(Helper, GameType.SURVIVAL, "scp063_scrub_stone");
        BlockPos Target = new BlockPos(8, 1, 8);
        Helper.setBlock(Target, Blocks.STONE.defaultBlockState());
        int Needed = Scp063Scrubbing.ticksFor(Helper.getLevel(), Helper.absolutePos(Target), Blocks.STONE.defaultBlockState());
        clickTarget(Helper, User);
        Helper.assertTrue(Scp063Scrubbing.scrubbingNow(User), "Using SCP-063 on stone did not start scrubbing");
        long Start = Helper.getTick();
        long[] Gone = {-1L};
        Helper.onEachTick(() -> {
            if (!User.isRemoved()) {
                User.doTick();
            }
            if (Gone[0] < 0 && Helper.getBlockState(Target).isAir()) {
                Gone[0] = Helper.getTick() - Start;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Gone[0] >= 0, "The stone is still there");
            Helper.assertTrue(Gone[0] >= Needed - 2 && Gone[0] <= Needed + 3, "Stone took " + Gone[0] + " ticks, expected about " + Needed);
            Helper.assertTrue(drops(Helper).isEmpty(), "Scrubbing dropped items: " + drops(Helper));
            Helper.assertTrue(Scp063ToothbrushItem.is(User.getMainHandItem()), "The toothbrush left the hand");
            Helper.assertTrue(Scp063Brushing.brushTime(User) == 0, "Scrubbing a block counted as brushing teeth");
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp063_handScrubSparesLog")
    public static void handScrubSparesLog(GameTestHelper Helper) {
        Scp063TestPlayer User = scrubber(Helper, GameType.SURVIVAL, "scp063_scrub_log");
        BlockPos Target = new BlockPos(8, 1, 8);
        Helper.setBlock(Target, Blocks.OAK_LOG.defaultBlockState());
        clickTarget(Helper, User);
        Helper.assertTrue(Scp063Scrubbing.scrubbingNow(User), "Using SCP-063 on a log did not start scrubbing");
        Helper.onEachTick(() -> {
            if (!User.isRemoved()) {
                User.doTick();
            }
        });
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(Helper.getBlockState(Target).is(Blocks.OAK_LOG), "The log was scrubbed away");
            Helper.assertTrue(Scp063Scrubbing.scrubbingNow(User), "Scrubbing the log stopped on its own");
            Helper.assertFalse(Scp063Scrubbing.working(User), "Scrubbing a log made progress");
            Helper.assertTrue(drops(Helper).isEmpty(), "Scrubbing dropped items");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp063_adventureCannotScrub")
    public static void adventureCannotScrub(GameTestHelper Helper) {
        Scp063TestPlayer User = scrubber(Helper, GameType.ADVENTURE, "scp063_scrub_adventure");
        BlockPos Target = new BlockPos(8, 1, 8);
        Helper.setBlock(Target, Blocks.STONE.defaultBlockState());
        clickTarget(Helper, User);
        Helper.assertFalse(Scp063Scrubbing.scrubbingNow(User), "Adventure mode started scrubbing");
        Helper.assertFalse(Scp063Scrubbing.permitted(User, Helper.absolutePos(Target), Blocks.STONE.defaultBlockState()), "Adventure mode may erase stone");
        Scp063Scrubbing.begin(User, InteractionHand.MAIN_HAND);
        Helper.onEachTick(() -> {
            if (!User.isRemoved()) {
                User.doTick();
            }
        });
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(Helper.getBlockState(Target).is(Blocks.STONE), "Adventure mode scrubbed the stone away");
            Helper.assertFalse(Scp063Scrubbing.working(User), "Adventure mode made scrubbing progress");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp063_teethBrushingStillWorks")
    public static void teethBrushingStillWorks(GameTestHelper Helper) {
        fill(Helper, 1, 2, Blocks.SMOOTH_STONE.defaultBlockState());
        Scp063TestPlayer User = player(Helper, new Vec3(8.5, 2.0, 8.5), GameType.SURVIVAL, "scp063_teeth");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp063Registry.Toothbrush.get()));
        User.setXRot(-90.0f);
        User.gameMode.useItem(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND);
        Helper.assertTrue(Scp063ToothbrushItem.isBrushingTeeth(User), "Using SCP-063 in the air did not brush teeth");
        Helper.assertFalse(Scp063Scrubbing.isScrubbing(User), "Brushing teeth was marked as scrubbing");
        Helper.onEachTick(() -> {
            if (!User.isRemoved()) {
                User.doTick();
            }
        });
        Helper.runAfterDelay(40, () -> {
            int Time = Scp063Brushing.brushTime(User);
            Helper.assertTrue(Time >= 38 && Time <= 42, "Brush time is " + Time + " after 40 ticks of brushing");
            Helper.assertTrue(Helper.getBlockState(new BlockPos(8, 1, 8)).is(Blocks.SMOOTH_STONE), "Brushing teeth wore the floor away");
            User.releaseUsingItem();
            Scp063Brushing.setBrushTime(User, 0);
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp063_sneakPlacesAndBreakReturns")
    public static void sneakPlacesAndBreakReturns(GameTestHelper Helper) {
        fill(Helper, 1, 2, Blocks.GRASS_BLOCK.defaultBlockState());
        Scp063TestPlayer User = player(Helper, new Vec3(8.5, 2.0, 6.5), GameType.SURVIVAL, "scp063_place");
        User.setItemInHand(InteractionHand.MAIN_HAND, named());
        User.setShiftKeyDown(true);
        BlockPos Target = Helper.absolutePos(new BlockPos(8, 1, 8));
        BlockHitResult Hit = new BlockHitResult(Helper.absoluteVec(new Vec3(8.5, 2.0, 8.5)), Direction.UP, Target, false);
        User.gameMode.useItemOn(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND, Hit);
        BlockPos Placed = new BlockPos(8, 2, 8);
        Helper.assertTrue(Helper.getBlockState(Placed).is(Scp063Registry.Resting.get()), "Sneak use on grass did not set SCP-063 down");
        Helper.assertTrue(User.getMainHandItem().isEmpty(), "Setting SCP-063 down did not take it from the hand");
        Helper.assertFalse(User.isUsingItem(), "Setting SCP-063 down also started brushing");
        Scp063RestingBlockEntity Rest = Helper.getBlockEntity(Placed);
        Helper.assertTrue(User.getUUID().equals(Rest.owner()), "The resting toothbrush did not remember who set it down");
        Helper.getLevel().destroyBlock(Helper.absolutePos(Placed), true);
        List<ItemEntity> Dropped = drops(Helper);
        Helper.assertTrue(Dropped.size() == 1 && Scp063ToothbrushItem.is(Dropped.get(0).getItem()), "Breaking SCP-063 did not drop it once: " + Dropped);
        Helper.assertTrue(Dropped.get(0).getItem().getHoverName().getString().equals(BrushName), "The dropped toothbrush lost its name");
        Dropped.forEach(Entity::discard);
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp063_stateSurvivesReload")
    public static void stateSurvivesReload(GameTestHelper Helper) {
        fill(Helper, 1, 2, Blocks.STONE.defaultBlockState());
        BlockPos Pos = new BlockPos(8, 2, 8);
        rest(Helper, Pos, named());
        Helper.runAfterDelay(6, () -> {
            Scp063RestingBlockEntity Rest = Helper.getBlockEntity(Pos);
            int Work = Rest.work();
            Helper.assertTrue(Work > 0, "SCP-063 has not started on the stone");
            CompoundTag Saved = Rest.saveWithFullMetadata(Helper.getLevel().registryAccess());
            BlockEntity Loaded = BlockEntity.loadStatic(Helper.absolutePos(Pos), Helper.getBlockState(Pos), Saved, Helper.getLevel().registryAccess());
            Helper.assertTrue(Loaded instanceof Scp063RestingBlockEntity, "The saved block entity did not load back");
            Scp063RestingBlockEntity Back = (Scp063RestingBlockEntity) Loaded;
            Helper.assertTrue(Back.work() == Work, "Erosion progress was lost: " + Back.work() + " instead of " + Work);
            Helper.assertTrue(Back.kept().getHoverName().getString().equals(BrushName), "The kept toothbrush was lost");
            Helper.getLevel().setBlock(Helper.absolutePos(Pos), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            Helper.succeed();
        });
    }
}
