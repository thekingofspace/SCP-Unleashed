package net.rai.scp_unleashed.inkwire.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.entity.BatteryBlockEntity;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.guards.GuardsRegistry;
import net.rai.scp_unleashed.guards.PatrolNodeBlock;
import net.rai.scp_unleashed.guards.PatrolNodeBlockEntity;
import net.rai.scp_unleashed.inkwire.InkMarks;
import net.rai.scp_unleashed.inkwire.InkwireRegistry;
import net.rai.scp_unleashed.inkwire.InvisibleInkItem;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.registry.ModItems;
import net.rai.scp_unleashed.wire.WireCutting;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import net.rai.scp_unleashed.wire.WireTier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class InkwireGameTests {
    public static final String Arena = "inkwire_arena";
    private static final BlockPos LeftHost = new BlockPos(2, 1, 2);
    private static final BlockPos RightHost = new BlockPos(7, 1, 2);

    private InkwireGameTests() {
    }

    private static BlockPos outlet(GameTestHelper Helper, BlockPos Host, Block Base) {
        Helper.setBlock(Host, Base);
        BlockPos Rel = Host.above();
        Helper.setBlock(Rel, ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.UP));
        return Rel;
    }

    private static BlockPos node(GameTestHelper Helper, BlockPos Floor, Direction Facing) {
        Helper.setBlock(Floor, Blocks.SMOOTH_STONE);
        BlockPos Rel = Floor.above();
        Helper.setBlock(Rel, GuardsRegistry.PatrolNode.get().defaultBlockState().setValue(PatrolNodeBlock.Facing, Facing));
        return Rel;
    }

    private static void link(GameTestHelper Helper, BlockPos First, BlockPos Second, boolean Hidden) {
        OutletBlockEntity Start = Helper.getBlockEntity(First);
        OutletBlockEntity End = Helper.getBlockEntity(Second);
        BlockPos StartPos = Helper.absolutePos(First);
        BlockPos EndPos = Helper.absolutePos(Second);
        Start.addLink(new WireLink(EndPos, Direction.UP, 0, WireTier.Coated, false, Direction.UP).withHidden(Hidden));
        End.addLink(new WireLink(StartPos, Direction.UP, 0, WireTier.Coated, false, Direction.UP).withHidden(Hidden));
    }

    private static FakePlayer inker(GameTestHelper Helper, Vec3 Feet, Vec3 LookAt, ItemStack Held) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "inkwire_tester"));
        aim(User, Feet, LookAt);
        User.setItemInHand(InteractionHand.MAIN_HAND, Held);
        return User;
    }

    private static void aim(FakePlayer User, Vec3 Feet, Vec3 LookAt) {
        Vec3 Eye = Feet.add(0.0, User.getEyeHeight(), 0.0);
        Vec3 Toward = LookAt.subtract(Eye);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        float Pitch = (float) -(Mth.atan2(Toward.y, Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z)) * Mth.RAD_TO_DEG);
        User.moveTo(Feet.x, Feet.y, Feet.z, Yaw, Pitch);
        User.setYHeadRot(Yaw);
        User.yHeadRotO = Yaw;
    }

    private static Vec3 midSpan(GameTestHelper Helper, BlockPos First, BlockPos Second) {
        OutletBlockEntity Start = Helper.getBlockEntity(First);
        WireLink Link = Start.linkTo(Helper.absolutePos(Second));
        return Start.attachFor(Link).add(Link.targetAttach()).scale(0.5);
    }

    private static InteractionResult dab(GameTestHelper Helper, FakePlayer User, BlockPos Rel) {
        BlockPos Pos = Helper.absolutePos(Rel);
        return User.getMainHandItem().useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false)));
    }

    private static boolean linkHidden(GameTestHelper Helper, BlockPos Rel, BlockPos Other) {
        OutletBlockEntity Outlet = Helper.getBlockEntity(Rel);
        WireLink Link = Outlet.linkTo(Helper.absolutePos(Other));
        return Link != null && Link.Hidden();
    }

    private static ItemStack ink() {
        return new ItemStack(InkwireRegistry.InvisibleInk.get());
    }

    private static final Codec<PalettedContainer<BlockState>> SectionStates = PalettedContainer.codecRW(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC,
            PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState());

    private static BlockState savedState(CompoundTag Saved, BlockPos Pos) {
        ListTag Sections = Saved.getList("sections", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Sections.size(); Index++) {
            CompoundTag Section = Sections.getCompound(Index);
            if (Section.getByte("Y") == SectionPos.blockToSectionCoord(Pos.getY()) && Section.contains("block_states")) {
                PalettedContainer<BlockState> States = SectionStates.parse(NbtOps.INSTANCE, Section.getCompound("block_states")).getOrThrow();
                return States.get(Pos.getX() & 15, Pos.getY() & 15, Pos.getZ() & 15);
            }
        }
        return Blocks.VOID_AIR.defaultBlockState();
    }

    private static CompoundTag savedEntity(CompoundTag Saved, BlockPos Pos) {
        ListTag Entities = Saved.getList("block_entities", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entities.size(); Index++) {
            CompoundTag Entry = Entities.getCompound(Index);
            if (Entry.getInt("x") == Pos.getX() && Entry.getInt("y") == Pos.getY() && Entry.getInt("z") == Pos.getZ()) {
                return Entry;
            }
        }
        return new CompoundTag();
    }

    private static BlockState syncedState(ServerLevel World, LevelChunk Chunk, BlockPos Pos) {
        FriendlyByteBuf Buffer = new ClientboundLevelChunkPacketData(Chunk).getReadBuffer();
        Registry<Biome> Biomes = World.registryAccess().registryOrThrow(Registries.BIOME);
        BlockState Found = Blocks.VOID_AIR.defaultBlockState();
        for (int Index = 0; Index < World.getSectionsCount(); Index++) {
            LevelChunkSection Section = new LevelChunkSection(Biomes);
            Section.read(Buffer);
            if (Index == World.getSectionIndex(Pos.getY())) {
                Found = Section.getBlockState(Pos.getX() & 15, Pos.getY() & 15, Pos.getZ() & 15);
            }
        }
        return Found;
    }

    private static Map<BlockPos, CompoundTag> syncedEntities(LevelChunk Chunk) {
        Map<BlockPos, CompoundTag> Found = new HashMap<>();
        ChunkPos At = Chunk.getPos();
        new ClientboundLevelChunkPacketData(Chunk).getBlockEntitiesTagsConsumer(At.x, At.z).accept((Pos, Kind, Data) -> {
            if (Data != null) {
                Found.put(Pos.immutable(), Data);
            }
        });
        return Found;
    }

    private static Map<BlockPos, BlockEntityType<?>> syncedTypes(LevelChunk Chunk) {
        Map<BlockPos, BlockEntityType<?>> Found = new HashMap<>();
        ChunkPos At = Chunk.getPos();
        new ClientboundLevelChunkPacketData(Chunk).getBlockEntitiesTagsConsumer(At.x, At.z).accept((Pos, Kind, Data) -> Found.put(Pos.immutable(), Kind));
        return Found;
    }

    private static void reloadedNode(GameTestHelper Helper, BlockPos Rel, Direction Facing, boolean Expected) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Rel);
        LevelChunk Chunk = World.getChunkAt(Pos);
        CompoundTag Saved = ChunkSerializer.write(World, Chunk);
        BlockState SavedState = savedState(Saved, Pos);
        Helper.assertTrue(SavedState.is(GuardsRegistry.PatrolNode.get()) && SavedState.getValue(PatrolNodeBlock.Facing) == Facing,
                "the saved chunk lost the patrol node at " + Rel + ", found " + SavedState);
        Helper.assertTrue(InkMarks.isHidden(SavedState) == Expected, "the saved chunk changed whether the patrol node at " + Rel + " is hidden");
        CompoundTag SavedEntity = savedEntity(Saved, Pos);
        Helper.assertTrue(SavedEntity.isEmpty() != Expected, Expected ? "the saved chunk dropped the block entity of the hidden patrol node at " + Rel
                : "the saved chunk stores a block entity for the visible patrol node at " + Rel);
        if (Expected) {
            BlockEntity Loaded = BlockEntity.loadStatic(Pos, SavedState, SavedEntity, World.registryAccess());
            Helper.assertTrue(Loaded instanceof PatrolNodeBlockEntity, "the block entity of the hidden patrol node at " + Rel + " does not load back");
        }
        BlockState SyncedState = syncedState(World, Chunk, Pos);
        Helper.assertTrue(SyncedState.is(GuardsRegistry.PatrolNode.get()) && InkMarks.isHidden(SyncedState) == Expected,
                "the chunk packet changed whether the patrol node at " + Rel + " is hidden, found " + SyncedState);
        BlockEntityType<?> Synced = syncedTypes(Chunk).get(Pos);
        Helper.assertTrue(Expected ? Synced == GuardsRegistry.PatrolNodeEntity.get() : Synced == null, Expected
                ? "the chunk packet does not give the client a ghost block entity for the hidden patrol node at " + Rel
                : "the chunk packet sends a block entity for the visible patrol node at " + Rel);
    }

    private static void reloadedHidden(GameTestHelper Helper, BlockPos Rel, BlockPos OtherRel, boolean Expected) {
        ServerLevel World = Helper.getLevel();
        HolderLookup.Provider Lookup = World.registryAccess();
        BlockPos Pos = Helper.absolutePos(Rel);
        BlockPos Other = Helper.absolutePos(OtherRel);
        LevelChunk Chunk = World.getChunkAt(Pos);
        CompoundTag Saved = ChunkSerializer.write(World, Chunk);
        BlockState SavedState = savedState(Saved, Pos);
        Helper.assertTrue(SavedState.getBlock() instanceof OutletBlock, "the saved chunk lost the outlet at " + Rel + ", found " + SavedState);
        Helper.assertTrue(InkMarks.isHidden(SavedState) == Expected, "the saved chunk changed whether the outlet at " + Rel + " is hidden");
        BlockEntity Loaded = BlockEntity.loadStatic(Pos, SavedState, savedEntity(Saved, Pos), Lookup);
        Helper.assertTrue(Loaded instanceof OutletBlockEntity Copy && Copy.linkTo(Other) != null && Copy.linkTo(Other).Hidden() == Expected,
                "the saved chunk changed whether the wire from " + Rel + " is hidden");
        BlockState SyncedState = syncedState(World, Chunk, Pos);
        Helper.assertTrue(SyncedState.getBlock() instanceof OutletBlock && InkMarks.isHidden(SyncedState) == Expected,
                "the chunk packet changed whether the outlet at " + Rel + " is hidden, found " + SyncedState);
        CompoundTag Synced = syncedEntities(Chunk).get(Pos);
        Helper.assertTrue(Synced != null, "the chunk packet carries no block entity data for the outlet at " + Rel);
        OutletBlockEntity Client = new OutletBlockEntity(Pos, SyncedState);
        Client.handleUpdateTag(Synced, Lookup);
        Helper.assertTrue(Client.linkTo(Other) != null && Client.linkTo(Other).Hidden() == Expected, "the chunk packet changed whether the wire from " + Rel + " is hidden");
    }

    @GameTest(template = Arena, timeoutTicks = 80)
    public static void inkSurvivesChunkSaveAndClientSync(GameTestHelper Helper) {
        BlockPos Left = outlet(Helper, LeftHost, Blocks.SMOOTH_STONE);
        BlockPos Right = outlet(Helper, RightHost, Blocks.SMOOTH_STONE);
        BlockPos Near = outlet(Helper, new BlockPos(2, 1, 8), Blocks.SMOOTH_STONE);
        BlockPos Far = outlet(Helper, new BlockPos(7, 1, 8), Blocks.SMOOTH_STONE);
        BlockPos Lone = outlet(Helper, new BlockPos(9, 1, 5), Blocks.SMOOTH_STONE);
        link(Helper, Left, Right, true);
        link(Helper, Near, Far, false);
        ServerLevel World = Helper.getLevel();
        InkMarks.toggleBlock(World, Helper.absolutePos(Left));
        InkMarks.toggleBlock(World, Helper.absolutePos(Right));
        Helper.runAfterDelay(2, () -> {
            reloadedHidden(Helper, Left, Right, true);
            reloadedHidden(Helper, Right, Left, true);
            reloadedHidden(Helper, Near, Far, false);
            reloadedHidden(Helper, Far, Near, false);
            OutletBlockEntity Empty = Helper.getBlockEntity(Lone);
            Helper.assertTrue(Empty.getUpdateTag(World.registryAccess()).contains("Links"), "an outlet with no wires sends an empty update tag, so the client would skip it");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60)
    public static void inkHidesAndRevealsOutlet(GameTestHelper Helper) {
        BlockPos Outlet = outlet(Helper, LeftHost, Blocks.SMOOTH_STONE);
        FakePlayer User = inker(Helper, Helper.absoluteVec(new Vec3(2.5, 1.0, 5.5)), Helper.absoluteVec(new Vec3(2.5, 2.2, 2.5)), ink());
        ItemStack Stack = User.getMainHandItem();
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Outlet);
        Helper.assertTrue(dab(Helper, User, Outlet).consumesAction(), "the ink did nothing to the outlet");
        BlockState Hidden = Helper.getBlockState(Outlet);
        Helper.assertTrue(InkMarks.isHidden(Hidden), "the outlet is not hidden after one dab");
        Helper.assertTrue(Hidden.getRenderShape() == RenderShape.INVISIBLE, "a hidden outlet still renders its block model");
        Helper.assertFalse(Hidden.getShape(World, Pos).isEmpty(), "a hidden outlet lost its hitbox");
        Helper.assertTrue(Stack.getDamageValue() == 1, "hiding an outlet did not use one dab, damage " + Stack.getDamageValue());
        Helper.assertTrue(dab(Helper, User, Outlet).consumesAction(), "the second dab did nothing");
        BlockState Shown = Helper.getBlockState(Outlet);
        Helper.assertFalse(InkMarks.isHidden(Shown), "the outlet is still hidden after the second dab");
        Helper.assertTrue(Shown.getRenderShape() == RenderShape.MODEL, "a revealed outlet does not render its model");
        Helper.assertTrue(Stack.getDamageValue() == 1, "revealing an outlet cost a dab, damage " + Stack.getDamageValue());
        Helper.assertTrue(dab(Helper, User, LeftHost) == InteractionResult.PASS, "the ink acted on a plain block");
        Helper.assertTrue(Stack.getDamageValue() == 1, "dabbing a plain block cost a dab");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60)
    public static void inkHidesAndRevealsPatrolNode(GameTestHelper Helper) {
        BlockPos Node = node(Helper, new BlockPos(4, 1, 4), Direction.EAST);
        FakePlayer User = inker(Helper, Helper.absoluteVec(new Vec3(4.5, 2.0, 7.5)), Helper.absoluteVec(new Vec3(4.5, 2.1, 4.5)), ink());
        ItemStack Stack = User.getMainHandItem();
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Node);
        Helper.assertFalse(InkMarks.isHidden(Helper.getBlockState(Node)), "a freshly placed patrol node is hidden");
        Helper.assertTrue(World.getBlockEntity(Pos) == null, "a visible patrol node carries a block entity");
        Helper.assertTrue(dab(Helper, User, Node).consumesAction(), "the ink did nothing to the patrol node");
        BlockState Hidden = Helper.getBlockState(Node);
        Helper.assertTrue(InkMarks.isHidden(Hidden), "the patrol node is not hidden after one dab");
        Helper.assertTrue(Hidden.getValue(PatrolNodeBlock.Facing) == Direction.EAST, "hiding the patrol node turned it");
        Helper.assertTrue(Hidden.getRenderShape() == RenderShape.INVISIBLE, "a hidden patrol node still renders its block model");
        Helper.assertFalse(Hidden.getShape(World, Pos).isEmpty(), "a hidden patrol node lost its hitbox");
        Helper.assertTrue(World.getBlockEntity(Pos) instanceof PatrolNodeBlockEntity, "a hidden patrol node has no block entity to draw its ghost");
        Helper.assertTrue(Stack.getDamageValue() == 1, "hiding a patrol node did not use one dab, damage " + Stack.getDamageValue());
        Helper.assertTrue(dab(Helper, User, Node).consumesAction(), "the second dab did nothing to the patrol node");
        BlockState Shown = Helper.getBlockState(Node);
        Helper.assertFalse(InkMarks.isHidden(Shown), "the patrol node is still hidden after the second dab");
        Helper.assertTrue(Shown.getValue(PatrolNodeBlock.Facing) == Direction.EAST, "revealing the patrol node turned it");
        Helper.assertTrue(Shown.getRenderShape() == RenderShape.MODEL, "a revealed patrol node does not render its model");
        Helper.assertTrue(World.getBlockEntity(Pos) == null, "a revealed patrol node kept its ghost block entity");
        Helper.assertTrue(Stack.getDamageValue() == 1, "revealing a patrol node cost a dab, damage " + Stack.getDamageValue());
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80)
    public static void inkedPatrolNodeSurvivesChunkSaveAndClientSync(GameTestHelper Helper) {
        BlockPos Inked = node(Helper, new BlockPos(3, 1, 3), Direction.SOUTH);
        BlockPos Plain = node(Helper, new BlockPos(8, 1, 3), Direction.WEST);
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(InkMarks.toggleBlock(World, Helper.absolutePos(Inked)) == InkMarks.Toggle.Hidden, "the patrol node did not take the ink");
        Helper.runAfterDelay(2, () -> {
            reloadedNode(Helper, Inked, Direction.SOUTH, true);
            reloadedNode(Helper, Plain, Direction.WEST, false);
            CompoundTag Values = new CompoundTag();
            Values.putString("facing", "east");
            CompoundTag Older = new CompoundTag();
            Older.putString("Name", BuiltInRegistries.BLOCK.getKey(GuardsRegistry.PatrolNode.get()).toString());
            Older.put("Properties", Values);
            BlockState Read = NbtUtils.readBlockState(World.holderLookup(Registries.BLOCK), Older);
            Helper.assertTrue(Read.is(GuardsRegistry.PatrolNode.get()) && !InkMarks.isHidden(Read) && Read.getValue(PatrolNodeBlock.Facing) == Direction.EAST,
                    "a patrol node saved before invisible ink does not load as a visible node facing the same way, got " + Read);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60)
    public static void inkWashesOffReplacedPatrolNode(GameTestHelper Helper) {
        BlockPos Node = node(Helper, new BlockPos(5, 1, 5), Direction.NORTH);
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Node);
        InkMarks.toggleBlock(World, Pos);
        Helper.assertTrue(World.getBlockEntity(Pos) instanceof PatrolNodeBlockEntity, "the hidden patrol node has no ghost block entity");
        Helper.setBlock(Node, Blocks.AIR);
        Helper.assertTrue(World.getBlockEntity(Pos) == null, "breaking a hidden patrol node left its block entity behind");
        Helper.setBlock(Node, GuardsRegistry.PatrolNode.get().defaultBlockState());
        Helper.assertFalse(InkMarks.isHidden(Helper.getBlockState(Node)), "a replaced patrol node came back hidden");
        Helper.assertTrue(World.getBlockEntity(Pos) == null, "a replaced patrol node came back with a block entity");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60)
    public static void inkHidesAndRevealsWire(GameTestHelper Helper) {
        BlockPos Left = outlet(Helper, LeftHost, Blocks.SMOOTH_STONE);
        BlockPos Right = outlet(Helper, RightHost, Blocks.SMOOTH_STONE);
        link(Helper, Left, Right, false);
        BlockPos LeftPos = Helper.absolutePos(Left);
        BlockPos RightPos = Helper.absolutePos(Right);
        Vec3 Middle = midSpan(Helper, Left, Right);
        FakePlayer User = inker(Helper, Helper.absoluteVec(new Vec3(4.5, 1.5, 4.5)), Middle, ink());
        ItemStack Stack = User.getMainHandItem();
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(InvisibleInkItem.inkWire(User, InteractionHand.MAIN_HAND, LeftPos, RightPos) == InkMarks.Toggle.Hidden, "dabbing the wire did not hide it");
        Helper.assertTrue(linkHidden(Helper, Left, Right) && linkHidden(Helper, Right, Left), "both ends of the wire should be marked hidden");
        Helper.assertTrue(Stack.getDamageValue() == 1, "hiding a wire did not use one dab, damage " + Stack.getDamageValue());
        WireCutting.WireHit Picked = WireCutting.pick(World, User.getEyePosition(), User.getViewVector(1.0f), User.blockInteractionRange());
        Helper.assertTrue(Picked != null && Picked.Distance() < 4.0, "a hidden wire can no longer be picked by tools");
        Helper.assertTrue(InvisibleInkItem.inkWire(User, InteractionHand.MAIN_HAND, RightPos, LeftPos) == InkMarks.Toggle.Revealed, "dabbing the hidden wire did not reveal it");
        Helper.assertFalse(linkHidden(Helper, Left, Right) || linkHidden(Helper, Right, Left), "the wire is still hidden at one end");
        Helper.assertTrue(Stack.getDamageValue() == 1, "revealing a wire cost a dab, damage " + Stack.getDamageValue());
        aim(User, Helper.absoluteVec(new Vec3(4.5, 1.5, 4.5)), Helper.absoluteVec(new Vec3(4.5, 1.5, 9.5)));
        Helper.assertTrue(InvisibleInkItem.inkWire(User, InteractionHand.MAIN_HAND, LeftPos, RightPos) == InkMarks.Toggle.Missed, "the ink reached a wire the player is not looking at");
        Helper.assertFalse(linkHidden(Helper, Left, Right), "a missed dab changed the wire");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80)
    public static void inkFlagSurvivesSaveAndReload(GameTestHelper Helper) {
        BlockPos Left = outlet(Helper, LeftHost, Blocks.SMOOTH_STONE);
        BlockPos Right = outlet(Helper, RightHost, Blocks.SMOOTH_STONE);
        link(Helper, Left, Right, true);
        ServerLevel World = Helper.getLevel();
        BlockPos LeftPos = Helper.absolutePos(Left);
        BlockPos RightPos = Helper.absolutePos(Right);
        InkMarks.toggleBlock(World, LeftPos);
        Helper.runAfterDelay(3, () -> {
            HolderLookup.Provider Lookup = World.registryAccess();
            OutletBlockEntity Outlet = Helper.getBlockEntity(Left);
            BlockState State = World.getBlockState(LeftPos);
            CompoundTag Saved = Outlet.saveWithFullMetadata(Lookup);
            BlockEntity Loaded = BlockEntity.loadStatic(LeftPos, State, Saved, Lookup);
            Helper.assertTrue(Loaded instanceof OutletBlockEntity Copy && Copy.linkTo(RightPos) != null && Copy.linkTo(RightPos).Hidden(), "the hidden flag was lost in the saved block entity");
            OutletBlockEntity Client = new OutletBlockEntity(LeftPos, State);
            Client.loadWithComponents(Outlet.getUpdateTag(Lookup), Lookup);
            Helper.assertTrue(Client.linkTo(RightPos) != null && Client.linkTo(RightPos).Hidden(), "the hidden flag is missing from the client sync data");
            WireLink Visible = new WireLink(RightPos, Direction.UP, 0, WireTier.Coated, false, Direction.UP);
            Tag Plain = WireLink.LinkCodec.encodeStart(NbtOps.INSTANCE, Visible).getOrThrow();
            Helper.assertFalse(Plain instanceof CompoundTag Compound && Compound.contains("hidden"), "visible wires should save exactly as before");
            Tag Inked = WireLink.LinkCodec.encodeStart(NbtOps.INSTANCE, Visible.withHidden(true)).getOrThrow();
            Helper.assertTrue(WireLink.LinkCodec.parse(NbtOps.INSTANCE, Inked).getOrThrow().equals(Visible.withHidden(true)), "the wire codec does not round trip the hidden flag");
            CompoundTag StateTag = NbtUtils.writeBlockState(State);
            BlockState Read = NbtUtils.readBlockState(World.holderLookup(Registries.BLOCK), StateTag);
            Helper.assertTrue(InkMarks.isHidden(Read), "the hidden outlet state does not survive the chunk format");
            OutletBlockEntity Other = Helper.getBlockEntity(Right);
            WireNetworkManager Manager = WireNetworkManager.get(World);
            WireNetworkManager.outletUnloaded(World, Outlet);
            WireNetworkManager.outletUnloaded(World, Other);
            Manager.outletLoaded(Outlet);
            Manager.outletLoaded(Other);
            WireNetwork Network = Manager.networkAt(World, LeftPos);
            Helper.assertTrue(Network != null && Network == Manager.networkAt(World, RightPos), "the hidden wire no longer joins its outlets after an unload and reload");
            Helper.assertTrue(linkHidden(Helper, Left, Right) && linkHidden(Helper, Right, Left), "the hidden flag changed during the unload and reload");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120)
    public static void inkedWiringStillCarriesPower(GameTestHelper Helper) {
        BlockPos Source = new BlockPos(2, 1, 6);
        BlockPos Cell = new BlockPos(7, 1, 6);
        BlockPos Left = outlet(Helper, Source, ModBlocks.CreativeEnergySource.get());
        BlockPos Right = outlet(Helper, Cell, ModBlocks.Battery.get());
        link(Helper, Left, Right, true);
        ServerLevel World = Helper.getLevel();
        InkMarks.toggleBlock(World, Helper.absolutePos(Left));
        InkMarks.toggleBlock(World, Helper.absolutePos(Right));
        Helper.succeedWhen(() -> {
            BatteryBlockEntity Battery = Helper.getBlockEntity(Cell);
            Helper.assertTrue(Battery.getStored() > 0, "the battery behind hidden wiring is not charging");
            Helper.assertTrue(InkMarks.isHidden(Helper.getBlockState(Left)) && InkMarks.isHidden(Helper.getBlockState(Right)), "an outlet lost its ink");
            Helper.assertTrue(linkHidden(Helper, Left, Right), "the wire lost its ink");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80)
    public static void inkedOutletStillShowsInJade(GameTestHelper Helper) {
        BlockPos Source = new BlockPos(2, 1, 6);
        BlockPos Cell = new BlockPos(7, 1, 6);
        BlockPos Left = outlet(Helper, Source, ModBlocks.CreativeEnergySource.get());
        BlockPos Right = outlet(Helper, Cell, ModBlocks.Battery.get());
        link(Helper, Left, Right, true);
        ServerLevel World = Helper.getLevel();
        BlockPos LeftPos = Helper.absolutePos(Left);
        InkMarks.toggleBlock(World, LeftPos);
        FakePlayer User = inker(Helper, Helper.absoluteVec(new Vec3(2.5, 2.0, 4.5)), Vec3.atCenterOf(LeftPos), ItemStack.EMPTY);
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(InkMarks.isHidden(World.getBlockState(LeftPos)), "the outlet is not hidden");
            Vec3 Eye = User.getEyePosition();
            BlockHitResult Clip = World.clip(new ClipContext(Eye, Eye.add(User.getViewVector(1.0f).scale(5.0)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));
            Helper.assertTrue(Clip.getType() == HitResult.Type.BLOCK && Clip.getBlockPos().equals(LeftPos), "the outline ray Jade uses no longer finds the hidden outlet, it hit " + Clip.getBlockPos());
            if (ModList.get().isLoaded("jade")) {
                CompoundTag Data = InkJadeProbe.serverData(World, User, LeftPos);
                Helper.assertTrue(Data.contains("ScpPower"), "Jade gets no power data for the hidden outlet");
                CompoundTag Power = Data.getCompound("ScpPower");
                Helper.assertTrue(Power.getInt("Links") == 1, "Jade reports the wrong wire count for the hidden outlet: " + Power.getInt("Links"));
                Helper.assertTrue(Power.getInt("Devices") >= 2, "Jade does not see the network behind the hidden outlet: " + Power);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60)
    public static void inkedWireStillCuts(GameTestHelper Helper) {
        BlockPos Left = outlet(Helper, LeftHost, Blocks.SMOOTH_STONE);
        BlockPos Right = outlet(Helper, RightHost, Blocks.SMOOTH_STONE);
        link(Helper, Left, Right, true);
        Vec3 Middle = midSpan(Helper, Left, Right);
        FakePlayer User = inker(Helper, Helper.absoluteVec(new Vec3(4.5, 1.5, 4.5)), Middle, new ItemStack(ModItems.WireCutter.get()));
        Helper.assertTrue(WireCutting.cut(Helper.getLevel(), User, Helper.absolutePos(Left), Helper.absolutePos(Right)), "wire cutters could not cut a hidden wire");
        OutletBlockEntity Start = Helper.getBlockEntity(Left);
        OutletBlockEntity End = Helper.getBlockEntity(Right);
        Helper.assertTrue(Start.getLinks().isEmpty() && End.getLinks().isEmpty(), "the cut hidden wire is still linked");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60)
    public static void inkWashesOffWhenReplaced(GameTestHelper Helper) {
        BlockPos Left = outlet(Helper, LeftHost, Blocks.SMOOTH_STONE);
        BlockPos Right = outlet(Helper, RightHost, Blocks.SMOOTH_STONE);
        link(Helper, Left, Right, true);
        ServerLevel World = Helper.getLevel();
        InkMarks.toggleBlock(World, Helper.absolutePos(Left));
        Helper.setBlock(Left, Blocks.AIR);
        Helper.setBlock(Left, ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.UP));
        Helper.assertFalse(InkMarks.isHidden(Helper.getBlockState(Left)), "a replaced outlet came back hidden");
        OutletBlockEntity Fresh = Helper.getBlockEntity(Left);
        OutletBlockEntity Other = Helper.getBlockEntity(Right);
        Helper.assertTrue(Fresh.getLinks().isEmpty() && Other.getLinks().isEmpty(), "breaking the outlet left its hidden wire behind");
        link(Helper, Left, Right, false);
        Helper.assertFalse(linkHidden(Helper, Left, Right) || linkHidden(Helper, Right, Left), "a new wire came back hidden");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60)
    public static void inkRunsDryIntoABottle(GameTestHelper Helper) {
        BlockPos First = outlet(Helper, LeftHost, Blocks.SMOOTH_STONE);
        BlockPos Second = outlet(Helper, RightHost, Blocks.SMOOTH_STONE);
        ServerLevel World = Helper.getLevel();
        InkMarks.toggleBlock(World, Helper.absolutePos(Second));
        ItemStack Last = ink();
        Last.setDamageValue(InvisibleInkItem.Uses - 1);
        FakePlayer User = inker(Helper, Helper.absoluteVec(new Vec3(4.5, 1.0, 5.5)), Helper.absoluteVec(new Vec3(4.5, 2.2, 2.5)), Last);
        Helper.assertTrue(Last.getMaxDamage() == InvisibleInkItem.Uses, "the ink does not hold " + InvisibleInkItem.Uses + " dabs");
        dab(Helper, User, Second);
        Helper.assertFalse(InkMarks.isHidden(Helper.getBlockState(Second)), "revealing with the last dab failed");
        Helper.assertTrue(User.getMainHandItem().is(InkwireRegistry.InvisibleInk.get()) && User.getMainHandItem().getDamageValue() == InvisibleInkItem.Uses - 1, "revealing used up the last dab");
        dab(Helper, User, First);
        Helper.assertTrue(InkMarks.isHidden(Helper.getBlockState(First)), "the last dab did not hide the outlet");
        Helper.assertTrue(User.getMainHandItem().is(Items.GLASS_BOTTLE), "the empty ink did not leave a glass bottle, holding " + User.getMainHandItem());
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40)
    public static void inkRecipeUsesWaterInkAndSpiderEye(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ItemStack Water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        List<ItemStack> Grid = new ArrayList<>(List.of(Water, new ItemStack(Items.INK_SAC), new ItemStack(Items.FERMENTED_SPIDER_EYE), ItemStack.EMPTY));
        Optional<RecipeHolder<CraftingRecipe>> Found = World.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 2, Grid), World);
        Helper.assertTrue(Found.isPresent(), "a water bottle, an ink sac and a fermented spider eye match no recipe");
        ItemStack Made = Found.get().value().assemble(CraftingInput.of(2, 2, Grid), World.registryAccess());
        Helper.assertTrue(Made.is(InkwireRegistry.InvisibleInk.get()), "the recipe made " + Made);
        ItemStack Awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        List<ItemStack> Wrong = new ArrayList<>(List.of(Awkward, new ItemStack(Items.INK_SAC), new ItemStack(Items.FERMENTED_SPIDER_EYE), ItemStack.EMPTY));
        Optional<RecipeHolder<CraftingRecipe>> Other = World.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 2, Wrong), World);
        Helper.assertTrue(Other.isEmpty() || !Other.get().value().getResultItem(World.registryAccess()).is(InkwireRegistry.InvisibleInk.get()), "any potion makes invisible ink, not just water");
        Helper.succeed();
    }
}
