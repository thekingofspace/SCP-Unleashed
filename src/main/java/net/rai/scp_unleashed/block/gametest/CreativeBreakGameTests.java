package net.rai.scp_unleashed.block.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.forcefield.ForcefieldRegistry;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp087.Scp087Registry;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class CreativeBreakGameTests {
    public static final String Arena = "gun_perf_arena";
    public static final int Floor = 1;

    private CreativeBreakGameTests() {
    }

    private static FakePlayer breaker(GameTestHelper Helper, GameType Mode, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(10.5, Floor + 1, 6.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        User.setGameMode(Mode);
        return User;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "creative_break")
    public static void creativeBreaksGuardedBlocks(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<Block> Guarded = List.of(Scp024Registry.Concrete.get(), Scp087Registry.Concrete.get(), Scp087Registry.Stairs.get(), ForcefieldRegistry.Barrier.get());
        FakePlayer Survivor = breaker(Helper, GameType.SURVIVAL, "guarded_survivor");
        FakePlayer Builder = breaker(Helper, GameType.CREATIVE, "guarded_builder");
        for (int Index = 0; Index < Guarded.size(); Index++) {
            Block Kind = Guarded.get(Index);
            String Name = BuiltInRegistries.BLOCK.getKey(Kind).toString();
            BlockPos Spot = new BlockPos(2 + Index * 3, Floor + 1, 3);
            Helper.setBlock(Spot.below(), Blocks.STONE);
            Helper.setBlock(Spot, Kind);
            BlockPos At = Helper.absolutePos(Spot);
            Helper.assertFalse(Survivor.gameMode.destroyBlock(At), "a survival player broke " + Name);
            Helper.assertTrue(World.getBlockState(At).is(Kind), "a survival player removed " + Name);
            Helper.assertTrue(Builder.gameMode.destroyBlock(At), "a creative player could not break " + Name);
            Helper.assertTrue(World.getBlockState(At).isAir(), Name + " is still there after a creative player broke it");
        }
        Helper.succeed();
    }
}
