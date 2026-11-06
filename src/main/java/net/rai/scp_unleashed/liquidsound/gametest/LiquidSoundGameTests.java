package net.rai.scp_unleashed.liquidsound.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.liquidsound.LiquidSoundRegistry;
import net.rai.scp_unleashed.liquidsound.LiquidSoundSet;
import net.rai.scp_unleashed.liquidsound.LiquidSounds;
import net.rai.scp_unleashed.oil.OilLiquid;
import net.rai.scp_unleashed.oil.OilRegistry;
import net.rai.scp_unleashed.scp006.Scp006Registry;
import net.rai.scp_unleashed.scp354.Scp354Registry;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class LiquidSoundGameTests {
    public static final String Arena = "gun_perf_arena";
    public static final int Floor = 1;
    public static final int PitZ = 4;
    public static final int Spacing = 4;

    private LiquidSoundGameTests() {
    }

    private static BlockPos pit(GameTestHelper Helper, int X, BlockState Liquid) {
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                Helper.setBlock(new BlockPos(X + Dx, Floor, PitZ + Dz), Blocks.STONE);
                Helper.setBlock(new BlockPos(X + Dx, Floor + 1, PitZ + Dz), Blocks.STONE);
                Helper.setBlock(new BlockPos(X + Dx, Floor + 2, PitZ + Dz), Blocks.AIR);
                Helper.setBlock(new BlockPos(X + Dx, Floor + 3, PitZ + Dz), Blocks.AIR);
            }
        }
        BlockPos Inside = new BlockPos(X, Floor + 1, PitZ);
        Helper.setBlock(Inside, Liquid);
        return Inside;
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "liquid_sounds")
    public static void liquidsKnowTheirSounds(GameTestHelper Helper) {
        List<BlockState> Liquids = List.of(Scp006Registry.Liquid.get().defaultBlockState(), OilRegistry.Oil.getBlock().get().defaultBlockState(),
                FissionRegistry.WasteWaterLiquid.get().defaultBlockState(), Scp354Registry.RedLiquid.get().defaultBlockState());
        List<LiquidSoundSet> Expected = List.of(LiquidSoundRegistry.Spring, LiquidSoundRegistry.Oil, LiquidSoundRegistry.Waste, LiquidSoundRegistry.Red);
        for (OilLiquid Grade : OilRegistry.Liquids) {
            Helper.assertTrue(LiquidSounds.of(Grade.getType().get()) == LiquidSoundRegistry.Oil, Grade.getGrade().getName() + " has no oil sounds");
        }
        List<Pig> Swimmers = new ArrayList<>();
        for (int Index = 0; Index < Liquids.size(); Index++) {
            BlockPos Inside = pit(Helper, 3 + Index * Spacing, Liquids.get(Index));
            Swimmers.add(Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(Inside.getX() + 0.5, Inside.getY() + 0.1, Inside.getZ() + 0.5)));
        }
        Pig Dry = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(19.5, Floor + 1, 7.5));
        Helper.runAfterDelay(10, () -> {
            for (int Index = 0; Index < Swimmers.size(); Index++) {
                Pig Swimmer = Swimmers.get(Index);
                LiquidSoundSet Found = LiquidSounds.setFor(Swimmer);
                Helper.assertTrue(Found == Expected.get(Index), "the pig in " + Liquids.get(Index).getBlock().getName().getString() + " got the wrong liquid sounds, in fluid "
                        + Swimmer.isInFluidType());
                Helper.assertFalse(Found.Swim().isEmpty() || Found.Splash().isEmpty() || Found.HeavySplash().isEmpty(), "a liquid is missing swim or splash sounds");
            }
            Helper.assertTrue(LiquidSounds.setFor(Dry) == null, "a pig on dry ground got liquid sounds");
            Helper.succeed();
        });
    }
}
