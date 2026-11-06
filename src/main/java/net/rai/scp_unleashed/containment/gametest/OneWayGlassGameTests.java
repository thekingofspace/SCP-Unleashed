package net.rai.scp_unleashed.containment.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.containment.ContainmentRegistry;
import net.rai.scp_unleashed.containment.OneWayGlassBlock;
import net.rai.scp_unleashed.containment.SmartGlassBlock;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class OneWayGlassGameTests {
    public static final String Arena = "dclassroles_arena";
    public static final int Timeout = 200;
    public static final BlockPos Pocket = new BlockPos(12, 3, 12);
    public static final BlockPos Pane = Pocket.east();
    public static final BlockPos Lamp = Pane.east();
    public static final BlockPos Switch = Pane.above();
    public static final int LitAtLeast = 10;
    public static final int Ceiling = 7;

    private OneWayGlassGameTests() {
    }

    private static void clear(GameTestHelper Helper) {
        for (int X = Pocket.getX() - 3; X <= Pocket.getX() + 4; X++) {
            for (int Z = Pocket.getZ() - 3; Z <= Pocket.getZ() + 3; Z++) {
                Helper.setBlock(new BlockPos(X, Pocket.getY() - 2, Z), Blocks.STONE);
                for (int Y = Pocket.getY() - 1; Y <= Ceiling; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR);
                }
            }
        }
    }

    private static void seal(GameTestHelper Helper, BlockState Glass, Direction Side) {
        clear(Helper);
        for (Direction Around : Direction.values()) {
            Helper.setBlock(Pocket.relative(Around), Around == Side ? Glass : Blocks.OBSIDIAN.defaultBlockState());
        }
    }

    private static BlockState oneWay() {
        return ContainmentRegistry.OneWayGlass.get().defaultBlockState().setValue(OneWayGlassBlock.Facing, Direction.WEST);
    }

    private static BlockState smart(boolean Active) {
        return ContainmentRegistry.SmartGlass.get().defaultBlockState().setValue(OneWayGlassBlock.Facing, Direction.WEST).setValue(SmartGlassBlock.Active, Active);
    }

    private static int light(GameTestHelper Helper, LightLayer Layer) {
        return Helper.getLevel().getBrightness(Layer, Helper.absolutePos(Pocket));
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "onewayglass_blocksLamp")
    public static void blocksLamp(GameTestHelper Helper) {
        seal(Helper, Blocks.GLASS.defaultBlockState(), Direction.EAST);
        Helper.setBlock(Lamp, Blocks.GLOWSTONE);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(light(Helper, LightLayer.BLOCK) >= LitAtLeast, "the lamp does not reach the pocket through plain glass: " + light(Helper, LightLayer.BLOCK)))
                .thenExecute(() -> Helper.setBlock(Pane, oneWay()))
                .thenWaitUntil(() -> Helper.assertTrue(light(Helper, LightLayer.BLOCK) == 0, "block light passes one way glass: " + light(Helper, LightLayer.BLOCK)))
                .thenIdle(10)
                .thenExecute(() -> Helper.assertTrue(light(Helper, LightLayer.BLOCK) == 0, "block light crept back through one way glass: " + light(Helper, LightLayer.BLOCK)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "onewayglass_blocksSky", skyAccess = true)
    public static void blocksSky(GameTestHelper Helper) {
        seal(Helper, oneWay(), Direction.UP);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(light(Helper, LightLayer.SKY) == 0, "sky light passes one way glass: " + light(Helper, LightLayer.SKY)))
                .thenExecute(() -> Helper.setBlock(Pocket.above(), smart(false)))
                .thenWaitUntil(() -> Helper.assertTrue(light(Helper, LightLayer.SKY) >= LitAtLeast, "sky light does not pass inactive smart glass: " + light(Helper, LightLayer.SKY)))
                .thenExecute(() -> Helper.setBlock(Pocket.above(), smart(true)))
                .thenWaitUntil(() -> Helper.assertTrue(light(Helper, LightLayer.SKY) == 0, "sky light passes active smart glass: " + light(Helper, LightLayer.SKY)))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "onewayglass_smartToggles")
    public static void smartToggles(GameTestHelper Helper) {
        seal(Helper, smart(false), Direction.EAST);
        Helper.setBlock(Lamp, Blocks.GLOWSTONE);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(light(Helper, LightLayer.BLOCK) >= LitAtLeast, "inactive smart glass blocks the lamp: " + light(Helper, LightLayer.BLOCK)))
                .thenExecute(() -> Helper.getLevel().setBlock(Helper.absolutePos(Switch), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL))
                .thenWaitUntil(() -> {
                    Helper.assertTrue(Helper.getBlockState(Pane).getValue(SmartGlassBlock.Active), "the powered smart glass never turned active");
                    Helper.assertTrue(light(Helper, LightLayer.BLOCK) == 0, "block light passes active smart glass: " + light(Helper, LightLayer.BLOCK));
                })
                .thenExecute(() -> Helper.getLevel().setBlock(Helper.absolutePos(Switch), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL))
                .thenWaitUntil(() -> {
                    Helper.assertFalse(Helper.getBlockState(Pane).getValue(SmartGlassBlock.Active), "the unpowered smart glass stayed active");
                    Helper.assertTrue(light(Helper, LightLayer.BLOCK) >= LitAtLeast, "light never came back through inactive smart glass: " + light(Helper, LightLayer.BLOCK));
                })
                .thenSucceed();
    }
}
