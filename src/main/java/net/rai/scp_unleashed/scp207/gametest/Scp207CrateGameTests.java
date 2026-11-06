package net.rai.scp_unleashed.scp207.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp207.Scp207CrateBlockEntity;
import net.rai.scp_unleashed.scp207.Scp207Ration;
import net.rai.scp_unleashed.scp207.Scp207Registry;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp207CrateGameTests {
    public static final String Arena = "gear_arena";
    public static final int TakenBefore = 5;

    private Scp207CrateGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp207_crateKeepsRation")
    public static void minedCrateKeepsItsRation(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos First = Helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos Second = Helper.absolutePos(new BlockPos(5, 2, 2));
        BlockState Crate = Scp207Registry.Crate.get().defaultBlockState();
        World.setBlockAndUpdate(First, Crate);
        if (!(World.getBlockEntity(First) instanceof Scp207CrateBlockEntity Placed)) {
            Helper.fail("the crate has no block entity");
            return;
        }
        Placed.setTaken(TakenBefore);
        int LeftBefore = Placed.left();
        Helper.assertTrue(LeftBefore == Scp207Ration.Daily - TakenBefore, "the crate did not record the taken bottles: " + LeftBefore);
        List<ItemStack> Drops = Block.getDrops(Crate, World, First, Placed);
        ItemStack Dropped = Drops.stream().filter(Stack -> Stack.is(Scp207Registry.CrateItem.get())).findFirst().orElse(ItemStack.EMPTY);
        Helper.assertFalse(Dropped.isEmpty(), "breaking the crate did not drop it");
        Helper.assertTrue(Dropped.has(Scp207Registry.RationData.get()), "the dropped crate forgot its ration");
        World.removeBlock(First, false);
        World.setBlockAndUpdate(Second, Crate);
        if (!(World.getBlockEntity(Second) instanceof Scp207CrateBlockEntity Replaced)) {
            Helper.fail("the replaced crate has no block entity");
            return;
        }
        Replaced.applyComponentsFromItemStack(Dropped);
        Helper.assertTrue(Replaced.left() == LeftBefore, "the replaced crate reset its ration: " + Replaced.left() + " left instead of " + LeftBefore);
        BlockPos Third = Helper.absolutePos(new BlockPos(8, 2, 2));
        World.setBlockAndUpdate(Third, Crate);
        if (World.getBlockEntity(Third) instanceof Scp207CrateBlockEntity Fresh) {
            List<ItemStack> FreshDrops = Block.getDrops(Crate, World, Third, Fresh);
            Helper.assertTrue(FreshDrops.stream().noneMatch(Stack -> Stack.has(Scp207Registry.RationData.get())), "an untouched crate carries ration data");
        }
        Helper.succeed();
    }
}
