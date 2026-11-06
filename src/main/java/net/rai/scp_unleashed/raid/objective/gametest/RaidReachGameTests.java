package net.rai.scp_unleashed.raid.objective.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.chaos.ChaosFaction;
import net.rai.scp_unleashed.raid.chaos.ChaosInsurgent;
import net.rai.scp_unleashed.raid.hostile.HostileHauls;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.objective.RaidAccess;
import net.rai.scp_unleashed.raid.objective.RaidStash;
import net.rai.scp_unleashed.raid.underground.gametest.RaidUndergroundGameTests;
import net.rai.scp_unleashed.scp005.Scp005Registry;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class RaidReachGameTests {
    public static final String Arena = RaidUndergroundGameTests.Arena;

    private RaidReachGameTests() {
    }

    private static ChestBlockEntity chest(GameTestHelper Helper, BlockPos At) {
        Helper.setBlock(At, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity Chest = (ChestBlockEntity) Helper.getLevel().getBlockEntity(Helper.absolutePos(At));
        Helper.assertTrue(Chest != null, "the chest has no block entity");
        Chest.setItem(0, new ItemStack(Items.BREAD, 5));
        Chest.setItem(4, new ItemStack(Scp005Registry.Key.get()));
        return Chest;
    }

    private static void stone(GameTestHelper Helper, int X0, int Y0, int Z0, int X1, int Y1, int Z1) {
        RaidUndergroundGameTests.fill(Helper, X0, Y0, Z0, X1, Y1, Z1, Blocks.STONE.defaultBlockState());
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "raid_reach_access")
    public static void accessNeedsReachAndSight(GameTestHelper Helper) {
        int F = RaidUndergroundGameTests.floor(Helper);
        BlockPos Open = new BlockPos(14, F, 12);
        BlockPos Walled = new BlockPos(14, F, 20);
        BlockPos Buried = new BlockPos(24, F - 1, 12);
        chest(Helper, Open);
        chest(Helper, Walled);
        stone(Helper, 13, F, 19, 13, F + 1, 21);
        Helper.setBlock(Buried, Blocks.CHEST.defaultBlockState());
        Helper.setBlock(new BlockPos(24, F, 12), Blocks.STONE.defaultBlockState());
        RaidContext Context = RaidUndergroundGameTests.open(Helper, ChaosFaction.Instance, Open, new BlockPos(30, F, 30), 3, 0.0);
        ChaosInsurgent Probe = RaidUndergroundGameTests.raider(Helper, HostileRegistry.InsurgentType.get(), Context, 12.5, F, 12.5, 3, HostileRaider.RoleSeeker);
        Probe.setNoAi(true);
        try {
            Helper.assertTrue(RaidAccess.touch(Probe, Helper.absolutePos(Open)), "a chest right in front of the raider was out of reach");
            Probe.moveTo(Helper.absoluteVec(new Vec3(12.5, F, 20.5)));
            Helper.assertFalse(RaidAccess.touch(Probe, Helper.absolutePos(Walled)), "a chest behind a wall counted as reachable");
            Helper.assertTrue(RaidStash.takeBy(Probe, Helper.absolutePos(Walled), Stack -> true).isEmpty(), "a chest behind a wall was emptied");
            Probe.moveTo(Helper.absoluteVec(new Vec3(24.5, F + 1, 12.5)));
            Helper.assertFalse(RaidAccess.touch(Probe, Helper.absolutePos(Buried)), "a chest under the floor counted as reachable");
            Probe.moveTo(Helper.absoluteVec(new Vec3(4.5, F, 12.5)));
            Helper.assertFalse(RaidAccess.touch(Probe, Helper.absolutePos(Open)), "a chest ten blocks away counted as reachable");
            Probe.moveTo(Helper.absoluteVec(new Vec3(12.5, F, 12.5)));
            Probe.setOnGround(true);
            Helper.assertTrue(RaidAccess.reachable(Probe, Helper.absolutePos(Walled)), "the walk around the short wall was not found");
        } finally {
            RaidUndergroundGameTests.close(Helper, Context);
            RaidUndergroundGameTests.sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "raid_reach_wall")
    public static void chaosCannotLootThroughWalls(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int F = RaidUndergroundGameTests.floor(Helper);
        stone(Helper, 13, F, 10, 15, F + 2, 18);
        RaidUndergroundGameTests.fill(Helper, 14, F, 16, 14, F + 1, 16, Blocks.AIR.defaultBlockState());
        BlockPos ChestAt = new BlockPos(14, F, 12);
        ChestBlockEntity Chest = chest(Helper, ChestAt);
        Vec3 Loose = Helper.absoluteVec(new Vec3(14.5, F + 0.1, 16.5));
        ItemEntity Key = new ItemEntity(World, Loose.x, Loose.y, Loose.z, new ItemStack(Scp005Registry.Key.get()));
        Key.setDeltaMovement(Vec3.ZERO);
        World.addFreshEntity(Key);
        RaidContext Context = RaidUndergroundGameTests.open(Helper, ChaosFaction.Instance, ChestAt, new BlockPos(30, F, 30), 3, 0.0);
        ChaosInsurgent Thief = RaidUndergroundGameTests.raider(Helper, HostileRegistry.InsurgentType.get(), Context, 12.5, F, 12.5, 3, HostileRaider.RoleSeeker);
        Helper.startSequence()
                .thenExecuteAfter(300, () -> {
                    Helper.assertFalse(HostileHauls.has(Context.Current(), Thief.getUUID()), "the insurgent took something through a wall");
                    Helper.assertTrue(Chest.getItem(4).is(Scp005Registry.Key.get()), "the walled chest lost its anomaly");
                    Helper.assertTrue(Key.isAlive(), "the walled loose anomaly was taken");
                    Helper.assertTrue(Context.Current().stolen() == 0, "a theft was counted");
                })
                .thenExecute(() -> {
                    RaidUndergroundGameTests.close(Helper, Context);
                    RaidUndergroundGameTests.sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "raid_reach_open")
    public static void chaosLootsAnOpenChest(GameTestHelper Helper) {
        int F = RaidUndergroundGameTests.floor(Helper);
        BlockPos ChestAt = new BlockPos(14, F, 12);
        ChestBlockEntity Chest = chest(Helper, ChestAt);
        RaidContext Context = RaidUndergroundGameTests.open(Helper, ChaosFaction.Instance, ChestAt, new BlockPos(30, F, 30), 3, 0.0);
        ChaosInsurgent Thief = RaidUndergroundGameTests.raider(Helper, HostileRegistry.InsurgentType.get(), Context, 6.5, F, 12.5, 3, HostileRaider.RoleSeeker);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(HostileHauls.has(Context.Current(), Thief.getUUID()), "the insurgent never took the anomaly from the open chest"))
                .thenExecute(() -> {
                    Helper.assertTrue(Chest.getItem(4).isEmpty(), "the chest still holds the anomaly");
                    Helper.assertTrue(Chest.getItem(0).is(Items.BREAD), "the insurgent took the bread");
                    Helper.assertTrue(Thief.getEyePosition().distanceTo(Vec3.atCenterOf(Helper.absolutePos(ChestAt))) <= RaidAccess.Reach + 1.0, "the theft happened from too far away");
                    RaidUndergroundGameTests.close(Helper, Context);
                    RaidUndergroundGameTests.sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1400, batch = "raid_reach_door")
    public static void chaosLootsBehindADoorOnlyOnceItOpens(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int F = RaidUndergroundGameTests.floor(Helper);
        stone(Helper, 13, F, 9, 17, F + 3, 15);
        RaidUndergroundGameTests.fill(Helper, 14, F, 10, 16, F + 2, 14, Blocks.AIR.defaultBlockState());
        BlockPos Lower = Helper.absolutePos(new BlockPos(13, F, 12));
        BlockState Door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST);
        World.setBlock(Lower.above(), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        World.setBlock(Lower, Door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        BlockPos ChestAt = new BlockPos(16, F, 12);
        ChestBlockEntity Chest = chest(Helper, ChestAt);
        RaidContext Context = RaidUndergroundGameTests.open(Helper, ChaosFaction.Instance, ChestAt, new BlockPos(30, F, 30), 3, 0.0);
        ChaosInsurgent Thief = RaidUndergroundGameTests.raider(Helper, HostileRegistry.InsurgentType.get(), Context, 8.5, F, 12.5, 3, HostileRaider.RoleSeeker);
        Helper.startSequence()
                .thenExecuteAfter(220, () -> {
                    Helper.assertFalse(HostileHauls.has(Context.Current(), Thief.getUUID()), "the insurgent looted through a closed iron door");
                    Helper.assertTrue(Chest.getItem(4).is(Scp005Registry.Key.get()), "the chest behind the closed door lost its anomaly");
                    BlockState Shut = World.getBlockState(Lower);
                    Helper.assertTrue(Shut.is(Blocks.IRON_DOOR) && !Shut.getValue(DoorBlock.OPEN), "the iron door is not closed");
                    ((DoorBlock) Blocks.IRON_DOOR).setOpen(null, World, Shut, Lower, true);
                    Helper.assertTrue(World.getBlockState(Lower).getValue(DoorBlock.OPEN), "the iron door did not open");
                })
                .thenWaitUntil(() -> Helper.assertTrue(HostileHauls.has(Context.Current(), Thief.getUUID()), "the insurgent never looted the chest after the door opened, at "
                        + Helper.relativeVec(Thief.position())))
                .thenExecute(() -> {
                    Helper.assertTrue(Chest.getItem(4).isEmpty(), "the chest still holds the anomaly");
                    RaidUndergroundGameTests.close(Helper, Context);
                    RaidUndergroundGameTests.sweep(Helper);
                })
                .thenSucceed();
    }
}
