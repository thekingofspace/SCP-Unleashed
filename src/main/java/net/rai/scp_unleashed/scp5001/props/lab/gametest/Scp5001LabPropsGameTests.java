package net.rai.scp_unleashed.scp5001.props.lab.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.props.lab.Scp5001LabProp;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp5001LabPropsGameTests {
    public static final String Arena = "scp5001_lab_arena";
    private static final String Batch = "scp5001props_lab_props";
    private static final int FloorY = 1;
    private static final List<String> Roles = List.of("lab_bench", "lab_wall_unit", "specimen_tank", "sample_rack", "analysis_machine", "scope_station", "fume_hood",
            "lab_cart", "lab_clutter", "lab_ceiling");
    private static final Set<String> Kinds = Set.of("floor", "floor_tall", "wall", "ceiling");

    private Scp5001LabPropsGameTests() {
    }

    private static JsonObject palette(GameTestHelper Helper) {
        Optional<Resource> Found = Helper.getLevel().getServer().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp5001_palette/lab.json"));
        if (Found.isEmpty()) {
            throw new GameTestAssertException("the lab palette data/scp_unleashed/scp5001_palette/lab.json is missing");
        }
        try (Reader Input = Found.get().openAsReader()) {
            return JsonParser.parseReader(Input).getAsJsonObject();
        } catch (IOException Failure) {
            throw new GameTestAssertException("the lab palette could not be read: " + Failure.getMessage());
        }
    }

    private static Block block(GameTestHelper Helper, String Id) {
        ResourceLocation Location = ResourceLocation.parse(Id);
        Helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Location), "palette block " + Id + " is not registered");
        return BuiltInRegistries.BLOCK.get(Location);
    }

    private static List<Block> entries(GameTestHelper Helper, String Wanted) {
        List<Block> Found = new ArrayList<>();
        for (Map.Entry<String, JsonElement> Role : palette(Helper).getAsJsonObject("roles").entrySet()) {
            JsonObject Body = Role.getValue().getAsJsonObject();
            if (Wanted == null || Wanted.equals(Body.get("kind").getAsString())) {
                for (JsonElement Choice : Body.getAsJsonArray("entries")) {
                    Found.add(block(Helper, Choice.getAsJsonObject().get("block").getAsString()));
                }
            }
        }
        return Found;
    }

    private static int dropped(GameTestHelper Helper, Item Wanted) {
        int Total = 0;
        for (ItemEntity Drop : Helper.getEntities(EntityType.ITEM)) {
            if (Drop.getItem().is(Wanted)) {
                Total += Drop.getItem().getCount();
            }
            Drop.discard();
        }
        return Total;
    }

    private static void place(GameTestHelper Helper, Player User, Block Prop, BlockPos Cell, float Yaw) {
        BlockPos Floor = Helper.absolutePos(Cell.below());
        ItemStack Stack = new ItemStack(Prop.asItem(), 2);
        User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
        User.setYRot(Yaw);
        Stack.useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false)));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void scp5001propsLabPaletteResolves(GameTestHelper Helper) {
        JsonObject Roles0 = palette(Helper).getAsJsonObject("roles");
        Set<String> Listed = new HashSet<>();
        for (String Role : Roles) {
            Helper.assertTrue(Roles0.has(Role), "the lab palette has no role " + Role);
        }
        for (Map.Entry<String, JsonElement> Role : Roles0.entrySet()) {
            JsonObject Body = Role.getValue().getAsJsonObject();
            String Kind = Body.get("kind").getAsString();
            Helper.assertTrue(Kinds.contains(Kind), "role " + Role.getKey() + " has unknown kind " + Kind);
            Helper.assertTrue(Body.getAsJsonArray("entries").size() > 0, "role " + Role.getKey() + " has no entries");
            for (JsonElement Choice : Body.getAsJsonArray("entries")) {
                String Id = Choice.getAsJsonObject().get("block").getAsString();
                Helper.assertTrue(Choice.getAsJsonObject().get("weight").getAsInt() > 0, Id + " has no weight");
                Block Prop = block(Helper, Id);
                BlockState State = Prop.defaultBlockState();
                Helper.assertTrue(State.hasProperty(BlockStateProperties.HORIZONTAL_FACING), Id + " has no horizontal facing");
                Helper.assertTrue(State.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) == "floor_tall".equals(Kind), Id + " half property does not match kind " + Kind);
                Helper.assertTrue(Prop.asItem() != Items.AIR, Id + " has no item");
                Listed.add(Id);
            }
        }
        for (Scp5001LabProp Entry : Scp5001LabProp.values()) {
            Helper.assertTrue(Listed.contains(Scp_unleashed.ModId + ":" + Entry.id()), Entry.id() + " is registered but not in the lab palette");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void scp5001propsLabTallPairs(GameTestHelper Helper) {
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        List<Block> Tall = entries(Helper, "floor_tall");
        Helper.assertTrue(!Tall.isEmpty(), "the lab palette has no tall props");
        for (int Index = 0; Index < Tall.size(); Index++) {
            Block Prop = Tall.get(Index);
            String Name = BuiltInRegistries.BLOCK.getKey(Prop).toString();
            BlockPos Lower = new BlockPos(1 + (Index % 4) * 2, FloorY + 1, 1 + (Index / 4) * 2);
            BlockPos Upper = Lower.above();
            for (int Round = 0; Round < 2; Round++) {
                place(Helper, User, Prop, Lower, 0.0f);
                BlockState Bottom = Helper.getBlockState(Lower);
                BlockState Top = Helper.getBlockState(Upper);
                Helper.assertTrue(Bottom.is(Prop) && Bottom.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER, Name + " lower half missing: " + Bottom);
                Helper.assertTrue(Top.is(Prop) && Top.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER, Name + " upper half missing: " + Top);
                Helper.assertTrue(Bottom.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.NORTH && Top.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.NORTH,
                        Name + " halves do not face north");
                Helper.getLevel().destroyBlock(Helper.absolutePos(Round == 0 ? Upper : Lower), true);
                Helper.assertTrue(Helper.getBlockState(Lower).isAir() && Helper.getBlockState(Upper).isAir(), Name + " pair did not break together (round " + Round + ")");
                int Drops = dropped(Helper, Prop.asItem());
                Helper.assertTrue(Drops == 1, Name + " dropped " + Drops + " items instead of 1 (round " + Round + ")");
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void scp5001propsLabSinglesPlace(GameTestHelper Helper) {
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos Cell = new BlockPos(4, FloorY + 1, 6);
        for (Block Prop : entries(Helper, null)) {
            if (Prop.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                continue;
            }
            String Name = BuiltInRegistries.BLOCK.getKey(Prop).toString();
            place(Helper, User, Prop, Cell, 90.0f);
            BlockState Placed = Helper.getBlockState(Cell);
            Helper.assertTrue(Placed.is(Prop), Name + " did not place: " + Placed);
            Helper.assertTrue(Placed.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST, Name + " faces " + Placed.getValue(BlockStateProperties.HORIZONTAL_FACING));
            Helper.assertTrue(!Placed.getShape(Helper.getLevel(), Helper.absolutePos(Cell)).isEmpty(), Name + " has an empty shape");
            Helper.getLevel().destroyBlock(Helper.absolutePos(Cell), true);
            Helper.assertTrue(Helper.getBlockState(Cell).is(Blocks.AIR), Name + " did not break");
            int Drops = dropped(Helper, Prop.asItem());
            Helper.assertTrue(Drops == 1, Name + " dropped " + Drops + " items instead of 1");
        }
        Helper.succeed();
    }
}
