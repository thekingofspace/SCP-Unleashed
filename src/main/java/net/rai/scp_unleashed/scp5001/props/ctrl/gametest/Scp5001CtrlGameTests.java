package net.rai.scp_unleashed.scp5001.props.ctrl.gametest;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.props.ctrl.Scp5001CtrlProp;
import net.rai.scp_unleashed.scp5001.props.ctrl.Scp5001CtrlProps;

import java.io.Reader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp5001CtrlGameTests {
    public static final String Arena = "gear_arena";
    private static final Map<String, String> Roles = new LinkedHashMap<>();

    static {
        Roles.put("workstation", "floor");
        Roles.put("wall_screen", "wall");
        Roles.put("server_rack", "floor_tall");
        Roles.put("holo_table", "floor");
        Roles.put("camera", "ceiling");
        Roles.put("security_gate", "floor_tall");
        Roles.put("weapon_locker", "floor_tall");
        Roles.put("archive_cabinet", "floor_tall");
        Roles.put("ctrl_clutter", "floor");
        Roles.put("signage", "wall");
    }

    private Scp5001CtrlGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp5001props_ctrl_palette")
    public static void scp5001props_ctrl_paletteResolves(GameTestHelper Helper) {
        Optional<Resource> Found = Helper.getLevel().getServer().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp5001_palette/ctrl.json"));
        if (Found.isEmpty()) {
            Helper.fail("ctrl palette missing");
            return;
        }
        JsonObject Palette;
        try (Reader Text = Found.get().openAsReader()) {
            Palette = JsonParser.parseReader(Text).getAsJsonObject().getAsJsonObject("roles");
        } catch (Exception Error) {
            Helper.fail("ctrl palette unreadable: " + Error.getMessage());
            return;
        }
        Set<String> Placed = new HashSet<>();
        for (Map.Entry<String, String> Role : Roles.entrySet()) {
            JsonObject Entry = Palette.getAsJsonObject(Role.getKey());
            Helper.assertTrue(Entry != null, "role " + Role.getKey() + " missing");
            Helper.assertTrue(Role.getValue().equals(Entry.get("kind").getAsString()), "role " + Role.getKey() + " has the wrong kind");
            Helper.assertTrue(Entry.getAsJsonArray("entries").size() > 0, "role " + Role.getKey() + " is empty");
            for (JsonElement Choice : Entry.getAsJsonArray("entries")) {
                ResourceLocation Id = ResourceLocation.parse(Choice.getAsJsonObject().get("block").getAsString());
                Helper.assertTrue(Choice.getAsJsonObject().get("weight").getAsInt() > 0, Id + " has no weight");
                Helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Id), Id + " is not registered");
                Helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Id), Id + " has no item");
                BlockState State = BuiltInRegistries.BLOCK.get(Id).defaultBlockState();
                Helper.assertTrue(State.hasProperty(BlockStateProperties.HORIZONTAL_FACING), Id + " has no facing");
                boolean Tall = State.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
                Helper.assertTrue(Tall == Role.getValue().equals("floor_tall"), Id + " half property does not match its kind");
                Placed.add(Id.getPath());
            }
        }
        for (Scp5001CtrlProp Entry : Scp5001CtrlProp.values()) {
            Helper.assertTrue(Placed.contains(Entry.id()), Entry.id() + " is not in the palette");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp5001props_ctrl_tall")
    public static void scp5001props_ctrl_tallPairsHold(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Lower = Helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos Upper = Lower.above();
        int Checked = 0;
        for (Scp5001CtrlProp Entry : Scp5001CtrlProp.values()) {
            if (Entry.kind() != Scp5001CtrlProp.Kind.FloorTall) {
                continue;
            }
            Block Made = Scp5001CtrlProps.Blocks.get(Entry).get();
            for (boolean BreakUpper : List.of(false, true)) {
                World.setBlock(Lower, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                World.setBlock(Upper, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                BlockState State = Made.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
                World.setBlock(Lower, State, Block.UPDATE_ALL);
                Made.setPlacedBy(World, Lower, State, null, ItemStack.EMPTY);
                BlockState Top = World.getBlockState(Upper);
                Helper.assertTrue(Top.is(Made) && Top.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER, Entry.id() + " did not place its upper half");
                Helper.assertTrue(Top.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST, Entry.id() + " upper half lost its facing");
                Helper.assertTrue(Block.getDrops(State, World, Lower, null).stream().anyMatch(Stack -> Stack.is(Made.asItem())), Entry.id() + " lower half drops nothing");
                Helper.assertTrue(Block.getDrops(Top, World, Upper, null).isEmpty(), Entry.id() + " upper half drops an item");
                World.destroyBlock(BreakUpper ? Upper : Lower, false);
                Helper.assertTrue(World.getBlockState(Lower).isAir() && World.getBlockState(Upper).isAir(), Entry.id() + " half survived breaking its partner");
            }
            Checked++;
        }
        Helper.assertTrue(Checked >= 8, "too few tall props");
        World.setBlock(Lower, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(Upper, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp5001props_ctrl_single")
    public static void scp5001props_ctrl_singlesDropThemselves(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(new BlockPos(2, 1, 2));
        for (Scp5001CtrlProp Entry : Scp5001CtrlProp.values()) {
            Block Made = Scp5001CtrlProps.Blocks.get(Entry).get();
            BlockState State = Made.defaultBlockState();
            Helper.assertTrue(State.getDestroySpeed(World, Pos) == 1.5f, Entry.id() + " is not stone hard");
            if (Entry.kind() == Scp5001CtrlProp.Kind.FloorTall) {
                continue;
            }
            Helper.assertTrue(Block.getDrops(State, World, Pos, null).stream().anyMatch(Stack -> Stack.is(Made.asItem())), Entry.id() + " drops nothing");
            for (Direction Facing : Direction.Plane.HORIZONTAL) {
                Helper.assertTrue(!State.setValue(BlockStateProperties.HORIZONTAL_FACING, Facing).getShape(World, Pos).isEmpty(), Entry.id() + " has an empty shape");
            }
        }
        Helper.succeed();
    }
}
