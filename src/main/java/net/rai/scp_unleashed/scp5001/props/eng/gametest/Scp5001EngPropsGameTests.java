package net.rai.scp_unleashed.scp5001.props.eng.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.props.eng.Scp5001EngCatalog;
import net.rai.scp_unleashed.scp5001.props.eng.Scp5001EngProp;
import net.rai.scp_unleashed.scp5001.props.eng.Scp5001EngProps;
import net.rai.scp_unleashed.scp5001.props.eng.Scp5001EngTallPropBlock;

import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp5001EngPropsGameTests {
    public static final String Arena = "gateway_arena";
    public static final Map<String, String> Roles = Map.of("pipe_wall", "wall", "pipe_ceiling", "ceiling", "conduit_floor", "floor", "coolant_tank", "floor_tall",
            "capacitor_bank", "floor_tall", "transformer", "floor", "service_panel", "wall", "tool_rack", "wall", "eng_clutter", "floor", "cable_ceiling", "ceiling");

    private Scp5001EngPropsGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp5001props_eng_palette")
    public static void scp5001propsEngPaletteResolves(GameTestHelper Helper) {
        ResourceLocation File = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp5001_palette/eng.json");
        Optional<Resource> Source = Helper.getLevel().getServer().getResourceManager().getResource(File);
        Helper.assertTrue(Source.isPresent(), "the eng palette is missing");
        JsonObject Root;
        try (Reader Input = Source.get().openAsReader()) {
            Root = JsonParser.parseReader(Input).getAsJsonObject();
        } catch (Exception Problem) {
            throw new IllegalStateException("the eng palette could not be read", Problem);
        }
        JsonObject RoleTable = Root.getAsJsonObject("roles");
        Set<String> Listed = new HashSet<>();
        for (Map.Entry<String, String> Wanted : Roles.entrySet()) {
            Helper.assertTrue(RoleTable.has(Wanted.getKey()), "role " + Wanted.getKey() + " is missing");
            JsonObject Role = RoleTable.getAsJsonObject(Wanted.getKey());
            String Kind = Role.get("kind").getAsString();
            Helper.assertTrue(Kind.equals(Wanted.getValue()), "role " + Wanted.getKey() + " has kind " + Kind);
            Helper.assertTrue(Role.getAsJsonArray("entries").size() > 0, "role " + Wanted.getKey() + " has no entries");
            for (JsonElement Element : Role.getAsJsonArray("entries")) {
                JsonObject Entry = Element.getAsJsonObject();
                ResourceLocation Id = ResourceLocation.parse(Entry.get("block").getAsString());
                Helper.assertTrue(Entry.get("weight").getAsInt() > 0, Id + " has no weight");
                Helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Id), Id + " is not registered");
                Block Found = BuiltInRegistries.BLOCK.get(Id);
                BlockState State = Found.defaultBlockState();
                Helper.assertTrue(State.hasProperty(BlockStateProperties.HORIZONTAL_FACING), Id + " has no facing");
                boolean Tall = State.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
                Helper.assertTrue(Tall == Kind.equals("floor_tall"), Id + " half property does not match kind " + Kind);
                Helper.assertTrue(Found.asItem() != Items.AIR, Id + " has no item");
                Listed.add(Id.getPath());
            }
        }
        for (Scp5001EngProp Entry : Scp5001EngCatalog.Entries) {
            Helper.assertTrue(Listed.contains(Entry.id()), Entry.id() + " is not in the palette");
            Helper.assertTrue(Scp5001EngProps.blocks().containsKey(Entry.id()), Entry.id() + " is not registered by the eng registry");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp5001props_eng_tall")
    public static void scp5001propsEngTallPairs(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Local = new BlockPos(6, 2, 6);
        BlockPos Pos = Helper.absolutePos(Local);
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        User.setYRot(0.0f);
        AABB Around = new AABB(Pos).inflate(3.0);
        for (Scp5001EngProp Entry : Scp5001EngCatalog.Entries) {
            if (!Entry.tall()) {
                continue;
            }
            Block Tall = Scp5001EngProps.blocks().get(Entry.id()).get();
            Helper.assertTrue(Tall instanceof Scp5001EngTallPropBlock, Entry.id() + " is not a tall prop block");
            for (boolean BreakTop : new boolean[]{true, false}) {
                clear(World, Pos, Around);
                InteractionResult Placed = place(User, Tall, Pos);
                Helper.assertTrue(Placed.consumesAction(), Entry.id() + " could not be placed from its item");
                BlockState Lower = World.getBlockState(Pos);
                BlockState Upper = World.getBlockState(Pos.above());
                Helper.assertTrue(Lower.is(Tall) && Lower.getValue(Scp5001EngTallPropBlock.Half) == DoubleBlockHalf.LOWER, Entry.id() + " lower half is " + Lower);
                Helper.assertTrue(Upper.is(Tall) && Upper.getValue(Scp5001EngTallPropBlock.Half) == DoubleBlockHalf.UPPER, Entry.id() + " upper half is " + Upper);
                Helper.assertTrue(Upper.getValue(Scp5001EngTallPropBlock.FACING) == Lower.getValue(Scp5001EngTallPropBlock.FACING), Entry.id() + " halves face different ways");
                World.destroyBlock(BreakTop ? Pos.above() : Pos, true);
                Helper.assertTrue(World.getBlockState(Pos).isAir() && World.getBlockState(Pos.above()).isAir(), Entry.id() + " left a half behind when broken");
                long Drops = World.getEntitiesOfClass(ItemEntity.class, Around).stream().filter(Drop -> Drop.getItem().is(Tall.asItem())).mapToInt(Drop -> Drop.getItem().getCount()).sum();
                Helper.assertTrue(Drops == 1, Entry.id() + " dropped " + Drops + " items when broken");
            }
            clear(World, Pos, Around);
            World.setBlock(Pos.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            InteractionResult Blocked = place(User, Tall, Pos);
            Helper.assertFalse(Blocked.consumesAction(), Entry.id() + " was placed under a block");
            World.setBlock(Pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        clear(World, Pos, Around);
        Helper.succeed();
    }

    private static void clear(ServerLevel World, BlockPos Pos, AABB Around) {
        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(Pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        World.getEntitiesOfClass(ItemEntity.class, Around).forEach(ItemEntity::discard);
    }

    private static InteractionResult place(Player User, Block Tall, BlockPos Pos) {
        User.level().setBlock(Pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack Stack = new ItemStack(Tall.asItem());
        User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
        BlockPos Floor = Pos.below();
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false);
        return ((BlockItem) Stack.getItem()).place(new BlockPlaceContext(User, InteractionHand.MAIN_HAND, Stack, Hit));
    }
}
