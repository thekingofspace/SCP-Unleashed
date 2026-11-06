package net.rai.scp_unleashed.block.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.FluidProof;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class FluidProofGameTests {
    public static final String Arena = "gun_perf_arena";
    public static final int Samples = 4;

    private FluidProofGameTests() {
    }

    private static List<BlockState> partialBlocks(GameTestHelper Helper, List<BlockPos> Spots) {
        ServerLevel World = Helper.getLevel();
        List<BlockState> Found = new ArrayList<>();
        for (Block Each : BuiltInRegistries.BLOCK) {
            if (Found.size() >= Spots.size()) {
                break;
            }
            ResourceLocation Id = BuiltInRegistries.BLOCK.getKey(Each);
            BlockState State = Each.defaultBlockState();
            if (!Scp_unleashed.ModId.equals(Id.getNamespace()) || Each instanceof LiquidBlockContainer || Each instanceof EntityBlock || State.blocksMotion()
                    || State.canBeReplaced() || State.isAir() || !State.getFluidState().isEmpty()
                    || !State.canSurvive(World, Helper.absolutePos(Spots.get(Found.size()))) || !standsAlone(World, State, Helper.absolutePos(Spots.get(Found.size())))) {
                continue;
            }
            Found.add(State);
        }
        return Found;
    }

    private static boolean standsAlone(ServerLevel World, BlockState State, BlockPos Spot) {
        for (Direction Side : Direction.values()) {
            if (!State.updateShape(Side, Blocks.AIR.defaultBlockState(), World, Spot, Spot.relative(Side)).is(State.getBlock())) {
                return false;
            }
        }
        return true;
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "fluid_proof")
    public static void waterLeavesModBlocksAlone(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int Floor = 1;
        for (int X = 1; X <= 20; X++) {
            for (int Z = 1; Z <= 8; Z++) {
                Helper.setBlock(new BlockPos(X, Floor, Z), Blocks.STONE);
                Helper.setBlock(new BlockPos(X, Floor + 1, Z), Blocks.AIR);
                Helper.setBlock(new BlockPos(X, Floor + 2, Z), Blocks.AIR);
            }
        }
        List<BlockPos> Spots = new ArrayList<>();
        for (int Index = 0; Index < Samples; Index++) {
            Spots.add(new BlockPos(2 + Index * 3, Floor + 1, 3));
        }
        List<BlockState> Picked = partialBlocks(Helper, Spots);
        Helper.assertTrue(!Picked.isEmpty(), "no partial mod blocks were found to test");
        for (int Index = 0; Index < Picked.size(); Index++) {
            BlockPos Spot = Spots.get(Index);
            World.setBlock(Helper.absolutePos(Spot), Picked.get(Index), Block.UPDATE_ALL);
            Helper.assertTrue(FluidProof.holdsBack(Picked.get(Index)), BuiltInRegistries.BLOCK.getKey(Picked.get(Index).getBlock()) + " is not protected from water");
            Helper.setBlock(Spot.north(), Blocks.WATER);
        }
        BlockPos Torch = new BlockPos(17, Floor + 1, 3);
        Helper.setBlock(Torch, Blocks.TORCH);
        Helper.setBlock(Torch.north(), Blocks.WATER);
        Helper.runAfterDelay(60, () -> {
            for (int Index = 0; Index < Picked.size(); Index++) {
                BlockState Now = World.getBlockState(Helper.absolutePos(Spots.get(Index)));
                Helper.assertTrue(Now.is(Picked.get(Index).getBlock()), "water broke " + BuiltInRegistries.BLOCK.getKey(Picked.get(Index).getBlock()) + ", now " + Now);
            }
            Helper.assertFalse(World.getBlockState(Helper.absolutePos(Torch)).is(Blocks.TORCH), "water no longer washes away vanilla torches");
            Helper.assertTrue(World.getFluidState(Helper.absolutePos(Torch)).is(Fluids.FLOWING_WATER) || World.getFluidState(Helper.absolutePos(Torch)).is(Fluids.WATER),
                    "water did not flow into the torch cell");
            Helper.succeed();
        });
    }
}
