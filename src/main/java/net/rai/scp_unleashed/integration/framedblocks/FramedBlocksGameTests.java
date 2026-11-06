package net.rai.scp_unleashed.integration.framedblocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.DecoratableBlock;
import net.rai.scp_unleashed.decor.FacilityMeshBlock;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class FramedBlocksGameTests {
    public static final String Arena = "framedblocks_compat_arena";
    public static final String FramedModId = "framedblocks";
    public static final TagKey<Block> EntityWhitelist = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(FramedModId, "blockentity_whitelisted"));
    public static final TagKey<Block> Frameable = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(FramedModId, "frameable"));
    public static final ResourceLocation FramedCube = ResourceLocation.fromNamespaceAndPath(FramedModId, "framed_cube");
    private static final BlockPos FramePos = new BlockPos(2, 1, 2);

    private FramedBlocksGameTests() {
    }

    private static List<Block> decoratableBlocks() {
        return BuiltInRegistries.BLOCK.stream().filter(Candidate -> Candidate instanceof DecoratableBlock).toList();
    }

    private static String id(Block Candidate) {
        return BuiltInRegistries.BLOCK.getKey(Candidate).toString();
    }

    @GameTest(template = Arena, timeoutTicks = 40)
    public static void decoratableBlocksAreFramedCamoTagged(GameTestHelper Helper) {
        List<String> Unlisted = new ArrayList<>();
        List<String> Unframeable = new ArrayList<>();
        for (Block Candidate : decoratableBlocks()) {
            BlockState State = Candidate.defaultBlockState();
            if (!State.is(EntityWhitelist)) {
                Unlisted.add(id(Candidate));
            }
            if (Candidate instanceof FacilityMeshBlock && !State.is(Frameable)) {
                Unframeable.add(id(Candidate));
            }
        }
        if (!Unlisted.isEmpty()) {
            Helper.fail("Decoratable blocks missing from data/framedblocks/tags/block/blockentity_whitelisted.json: " + String.join(", ", Unlisted));
            return;
        }
        if (!Unframeable.isEmpty()) {
            Helper.fail("Mesh blocks missing from data/framedblocks/tags/block/frameable.json: " + String.join(", ", Unframeable));
            return;
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40)
    public static void framedCubeTakesDecoratableCamo(GameTestHelper Helper) {
        if (!ModList.get().isLoaded(FramedModId)) {
            Helper.succeed();
            return;
        }
        Block Cube = BuiltInRegistries.BLOCK.get(FramedCube);
        if (Cube == Blocks.AIR) {
            Helper.fail("FramedBlocks is loaded but " + FramedCube + " is not registered");
            return;
        }
        BlockPos AbsolutePos = Helper.absolutePos(FramePos);
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        List<String> Refused = new ArrayList<>();
        List<String> Unexpected = new ArrayList<>();
        int Applied = 0;
        for (Block Candidate : decoratableBlocks()) {
            if (Candidate.asItem() == Items.AIR) {
                continue;
            }
            Helper.setBlock(FramePos, Blocks.AIR);
            Helper.setBlock(FramePos, Cube);
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Candidate));
            Helper.useBlock(FramePos, User);
            BlockState State = Candidate.defaultBlockState();
            boolean Expected = State.isSolidRender(Helper.getLevel(), AbsolutePos) || State.is(Frameable);
            boolean Took = id(Candidate).equals(camoName(Helper, AbsolutePos));
            if (Took) {
                Applied++;
            }
            if (Expected && !Took) {
                Refused.add(id(Candidate));
            } else if (!Expected && Took) {
                Unexpected.add(id(Candidate));
            }
        }
        Helper.setBlock(FramePos, Blocks.AIR);
        if (!Refused.isEmpty()) {
            Helper.fail("Framed cube refused decoratable camo: " + String.join(", ", Refused));
            return;
        }
        if (!Unexpected.isEmpty()) {
            Helper.fail("Framed cube took camo it should refuse: " + String.join(", ", Unexpected));
            return;
        }
        if (Applied == 0) {
            Helper.fail("No decoratable block was applied as camo");
            return;
        }
        Helper.succeed();
    }

    private static String camoName(GameTestHelper Helper, BlockPos AbsolutePos) {
        BlockEntity Frame = Helper.getLevel().getBlockEntity(AbsolutePos);
        if (Frame == null) {
            return "";
        }
        CompoundTag Saved = Frame.saveWithoutMetadata(Helper.getLevel().registryAccess());
        return Saved.getCompound("camo").getCompound("state").getString("Name");
    }
}
