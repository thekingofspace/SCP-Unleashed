package net.rai.scp_unleashed.block.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.registry.ModBlocks;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class OutletPlacementGameTests {
    public static final String Arena = "gear_arena";

    private OutletPlacementGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "outlet_posts")
    public static void outletsSitOnPosts(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockState Up = ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.UP);
        BlockPos Fence = Helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos Wall = Helper.absolutePos(new BlockPos(5, 3, 2));
        BlockPos Bare = Helper.absolutePos(new BlockPos(8, 5, 2));
        World.setBlockAndUpdate(Fence, Blocks.OAK_FENCE.defaultBlockState());
        World.setBlockAndUpdate(Wall, Blocks.COBBLESTONE_WALL.defaultBlockState());
        Helper.assertTrue(Up.canSurvive(World, Fence.above()), "an outlet cannot sit on a fence post");
        Helper.assertTrue(Up.canSurvive(World, Wall.above()), "an outlet cannot sit on a wall");
        Helper.assertFalse(Up.canSurvive(World, Bare), "an outlet survives with nothing under it");
        World.setBlockAndUpdate(Fence.above(), Up);
        World.setBlockAndUpdate(Wall.above(), Up);
        Helper.runAfterDelay(2, () -> {
            Helper.assertTrue(World.getBlockState(Fence.above()).is(ModBlocks.Outlet.get()), "the outlet on the fence post broke");
            Helper.assertTrue(World.getBlockState(Wall.above()).is(ModBlocks.Outlet.get()), "the outlet on the wall broke");
            Helper.succeed();
        });
    }
}
