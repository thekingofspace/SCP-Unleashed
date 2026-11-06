package net.rai.scp_unleashed.upgrade.gametest;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.refinery.OrePurifierBlock;
import net.rai.scp_unleashed.refinery.PurifierLayout;
import net.rai.scp_unleashed.refinery.RefineryRegistry;
import net.rai.scp_unleashed.registry.ModItems;
import net.rai.scp_unleashed.upgrade.MufflerData;
import net.rai.scp_unleashed.upgrade.MufflerPayload;
import net.rai.scp_unleashed.upgrade.Mufflers;
import net.rai.scp_unleashed.upgrade.SoundMuffling;
import net.rai.scp_unleashed.upgrade.UpgradeType;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class MufflerGameTests {
    public static final String Arena = "dclassroles_arena";
    public static final BlockPos Machine = new BlockPos(6, 2, 6);
    public static final BlockPos Neighbour = new BlockPos(8, 2, 6);
    public static final BlockPos Purifier = new BlockPos(14, 2, 14);
    public static final float Tolerance = 1.0e-4f;

    private MufflerGameTests() {
    }

    private static Block modBlock(String Path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    private static void floor(GameTestHelper Helper) {
        for (int X = 2; X <= 20; X++) {
            for (int Z = 2; Z <= 20; Z++) {
                Helper.setBlock(new BlockPos(X, 1, Z), Blocks.STONE);
                for (int Y = 2; Y <= 6; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR);
                }
            }
        }
    }

    private static InteractionResult fit(GameTestHelper Helper, ItemStack Stack, BlockPos Absolute) {
        ServerLevel World = Helper.getLevel();
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Absolute), Direction.UP, Absolute, false);
        return Stack.getItem().onItemUseFirst(Stack, new UseOnContext(World, null, InteractionHand.MAIN_HAND, Stack, Hit));
    }

    private static float heard(ServerLevel World, Vec3 At, SoundSource Source) {
        PlayLevelSoundEvent.AtPosition Event = EventHooks.onPlaySoundAtPosition(World, At.x, At.y, At.z, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ANVIL_LAND), Source, 1.0f, 1.0f);
        return Event.getNewVolume();
    }

    private static void near(GameTestHelper Helper, float Value, float Wanted, String Message) {
        Helper.assertTrue(Math.abs(Value - Wanted) < Tolerance, Message + ": " + Value + " instead of " + Wanted);
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "muffler_decision")
    public static void decision(GameTestHelper Helper) {
        LongSet Cells = new LongOpenHashSet();
        Cells.add(BlockPos.asLong(10, 64, 10));
        Cells.add(BlockPos.asLong(10, 65, 10));
        float Quiet = SoundMuffling.Factor;
        Helper.assertTrue(Quiet >= 0.15f && Quiet <= 0.25f, "the muffler factor left the 15 to 25 percent band: " + Quiet);
        near(Helper, SoundMuffling.factor(Cells, 10.5, 64.5, 10.5, SoundSource.BLOCKS), Quiet, "a block sound inside the machine");
        near(Helper, SoundMuffling.factor(Cells, 10.5, 66.0, 10.5, SoundSource.BLOCKS), Quiet, "a block sound on the top face");
        near(Helper, SoundMuffling.factor(Cells, 11.06, 64.5, 10.5, SoundSource.BLOCKS), Quiet, "a block sound just past a side face");
        near(Helper, SoundMuffling.factor(Cells, 11.5, 64.5, 10.5, SoundSource.BLOCKS), 1.0f, "a block sound in the next block");
        near(Helper, SoundMuffling.factor(Cells, 10.5, 66.5, 10.5, SoundSource.AMBIENT), 1.0f, "an ambient sound above the machine");
        near(Helper, SoundMuffling.factor(Cells, 10.5, 64.5, 10.5, SoundSource.HOSTILE), 1.0f, "a hostile mob sound");
        near(Helper, SoundMuffling.factor(Cells, 10.5, 64.5, 10.5, SoundSource.PLAYERS), 1.0f, "a player sound standing on a low machine");
        near(Helper, SoundMuffling.factor(Cells, 10.5, 66.0, 10.5, SoundSource.PLAYERS), 1.0f, "footsteps on top of the machine");
        near(Helper, SoundMuffling.factor(Cells, Double.NaN, 64.5, 10.5, SoundSource.BLOCKS), 1.0f, "a sound at no position");
        near(Helper, SoundMuffling.factor(new LongOpenHashSet(), 10.5, 64.5, 10.5, SoundSource.BLOCKS), 1.0f, "a sound with no mufflers");
        MufflerPayload Sent = new MufflerPayload(true, List.of(new MufflerPayload.Machine(7L, new long[]{7L, 8L})), new long[]{3L});
        FriendlyByteBuf Buffer = new FriendlyByteBuf(Unpooled.buffer());
        MufflerPayload.PayloadCodec.encode(Buffer, Sent);
        MufflerPayload Read = MufflerPayload.PayloadCodec.decode(Buffer);
        Helper.assertTrue(Read.Replace() && Read.Added().size() == 1 && Read.Added().get(0).Cells().length == 2 && Read.Removed()[0] == 3L, "the muffler payload did not survive the wire");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "muffler_fitted")
    public static void fitted(GameTestHelper Helper) {
        floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Centrifuge = Helper.absolutePos(Machine);
        BlockPos Other = Helper.absolutePos(Neighbour);
        Helper.setBlock(Machine, modBlock("centrifuge"));
        Helper.setBlock(Neighbour, modBlock("item_press"));
        Helper.assertTrue(World.getBlockState(Centrifuge).is(SoundMuffling.Muffleable), "the centrifuge is not muffleable");
        Helper.assertFalse(World.getBlockState(Centrifuge.below()).is(SoundMuffling.Muffleable), "plain stone counts as a machine");
        near(Helper, heard(World, Vec3.atCenterOf(Centrifuge), SoundSource.BLOCKS), 1.0f, "an unmuffled centrifuge");
        ItemStack Stack = new ItemStack(ModItems.SoundMufflerUpgrade.get(), 2);
        Helper.assertTrue(fit(Helper, Stack, Centrifuge) == InteractionResult.SUCCESS, "the muffler did not fit the centrifuge");
        Helper.assertTrue(Stack.getCount() == 1, "fitting did not use up the muffler");
        Helper.assertTrue(Mufflers.isMuffled(World, Centrifuge), "the centrifuge is not muffled");
        near(Helper, heard(World, Vec3.atCenterOf(Centrifuge), SoundSource.BLOCKS), SoundMuffling.Factor, "the muffled centrifuge through the server hook");
        near(Helper, heard(World, Vec3.atCenterOf(Other), SoundSource.BLOCKS), 1.0f, "the item press next door");
        Helper.assertTrue(fit(Helper, Stack, Centrifuge) == InteractionResult.FAIL, "a second muffler fitted the same machine");
        Helper.assertTrue(Stack.getCount() == 1, "the refused muffler was used up");
        Helper.assertTrue(fit(Helper, Stack, Centrifuge.below()) == InteractionResult.PASS, "a muffler fitted plain stone");
        World.destroyBlock(Centrifuge, false);
        Helper.assertFalse(Mufflers.isMuffled(World, Centrifuge), "the broken centrifuge stayed muffled");
        near(Helper, heard(World, Vec3.atCenterOf(Centrifuge), SoundSource.BLOCKS), 1.0f, "the spot of the broken centrifuge");
        List<ItemEntity> Drops = World.getEntitiesOfClass(ItemEntity.class, new AABB(Centrifuge).inflate(1.5), Found -> Found.getItem().is(UpgradeType.Muffler.getItem()));
        Helper.assertTrue(Drops.size() == 1, "breaking the machine did not drop its muffler");
        Drops.forEach(ItemEntity::discard);
        World.destroyBlock(Other, false);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "muffler_multiblock")
    public static void multiblock(GameTestHelper Helper) {
        floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Master = Helper.absolutePos(Purifier);
        BlockState State = RefineryRegistry.OrePurifier.get().defaultBlockState().setValue(OrePurifierBlock.Facing, Direction.NORTH).setValue(OrePurifierBlock.Part, PurifierLayout.MasterPart);
        World.setBlock(Master, State, Block.UPDATE_ALL);
        State.getBlock().setPlacedBy(World, Master, State, null, new ItemStack(RefineryRegistry.OrePurifierItem.get()));
        List<BlockPos> Cells = PurifierLayout.cells(Master, Direction.NORTH);
        BlockPos Upper = Cells.get(PurifierLayout.part(1, 1, 1));
        ItemStack Stack = new ItemStack(ModItems.SoundMufflerUpgrade.get());
        Helper.assertTrue(fit(Helper, Stack, Upper) == InteractionResult.SUCCESS, "the muffler did not fit the purifier from its top corner");
        for (BlockPos Cell : Cells) {
            Helper.assertTrue(Mufflers.isMuffled(World, Cell), "a purifier part is not muffled at " + Helper.relativePos(Cell));
        }
        near(Helper, heard(World, PurifierLayout.centre(Master, Direction.NORTH), SoundSource.BLOCKS), SoundMuffling.Factor, "the purifier drain sound at its centre");
        MufflerData Data = MufflerData.peek(World);
        Helper.assertTrue(Data != null && Data.entry(Master.asLong()) != null && Data.entry(Master.asLong()).Cells().length == PurifierLayout.CellCount,
                "the purifier footprint is not the eight purifier cells");
        World.destroyBlock(Cells.get(PurifierLayout.part(1, 0, 1)), false);
        Helper.assertFalse(Mufflers.isMuffled(World, Master), "the dismantled purifier stayed muffled");
        near(Helper, heard(World, PurifierLayout.centre(Master, Direction.NORTH), SoundSource.BLOCKS), 1.0f, "the empty purifier spot");
        World.getEntitiesOfClass(ItemEntity.class, new AABB(Master).inflate(3.0)).forEach(ItemEntity::discard);
        Helper.succeed();
    }
}
