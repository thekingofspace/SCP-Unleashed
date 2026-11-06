package net.rai.scp_unleashed.scp5001.props.hab.gametest;

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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.props.hab.Scp5001HabCatalog;
import net.rai.scp_unleashed.scp5001.props.hab.Scp5001HabProps;
import net.rai.scp_unleashed.scp5001.props.hab.Scp5001HabTallBlock;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp5001HabGameTests {
    public static final String Arena = "gear_arena";
    public static final String Batch = "scp5001props_hab_props";
    private static final Map<String, String> Roles = Map.ofEntries(Map.entry("bunk", "floor_tall"), Map.entry("locker", "floor_tall"), Map.entry("mess_table", "floor"),
            Map.entry("mess_seat", "floor"), Map.entry("galley_unit", "floor"), Map.entry("med_bed", "floor"), Map.entry("med_cabinet", "wall"), Map.entry("crate", "floor"),
            Map.entry("shelf", "floor_tall"), Map.entry("hab_clutter", "floor"), Map.entry("drone_dock", "wall"));

    private Scp5001HabGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = Batch)
    public static void scp5001propsHabPalette(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Optional<Resource> Found = World.getServer().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp5001_palette/hab.json"));
        Helper.assertTrue(Found.isPresent(), "the hab palette is missing");
        JsonObject Palette;
        try (Reader Source = Found.get().openAsReader()) {
            Palette = JsonParser.parseReader(Source).getAsJsonObject().getAsJsonObject("roles");
        } catch (Exception Failure) {
            throw new IllegalStateException("the hab palette cannot be read", Failure);
        }
        int Checked = 0;
        for (Map.Entry<String, String> Wanted : Roles.entrySet()) {
            Helper.assertTrue(Palette.has(Wanted.getKey()), "the palette lacks role " + Wanted.getKey());
            JsonObject Role = Palette.getAsJsonObject(Wanted.getKey());
            String Kind = Role.get("kind").getAsString();
            Helper.assertTrue(Wanted.getValue().equals(Kind), "role " + Wanted.getKey() + " has kind " + Kind);
            Helper.assertTrue(Role.getAsJsonArray("entries").size() > 0, "role " + Wanted.getKey() + " has no entries");
            for (JsonElement Element : Role.getAsJsonArray("entries")) {
                JsonObject Choice = Element.getAsJsonObject();
                String Id = Choice.get("block").getAsString();
                Helper.assertTrue(Choice.get("weight").getAsInt() > 0, Id + " has no weight");
                Optional<Block> Resolved = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(Id));
                Helper.assertTrue(Resolved.isPresent() && Resolved.get() != Blocks.AIR, Id + " is not a registered block");
                BlockState State = Resolved.get().defaultBlockState();
                Property<?> Facing = Resolved.get().getStateDefinition().getProperty("facing");
                Helper.assertTrue(Facing != null && Facing.getPossibleValues().containsAll(List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)),
                        Id + " lacks a horizontal facing property");
                Property<?> Half = Resolved.get().getStateDefinition().getProperty("half");
                if ("floor_tall".equals(Kind)) {
                    Helper.assertTrue(Half != null && State.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF), Id + " lacks the half property");
                } else {
                    Helper.assertTrue(Half == null, Id + " has a half property but is not floor_tall");
                }
                Helper.assertTrue(Resolved.get().asItem() != Items.AIR, Id + " has no item");
                Checked++;
            }
        }
        Helper.assertTrue(Checked == Scp5001HabCatalog.values().length, "the palette lists " + Checked + " props but the catalog has " + Scp5001HabCatalog.values().length);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = Batch)
    public static void scp5001propsHabTallPairs(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(new BlockPos(5, 1, 3));
        AABB Around = new AABB(Pos).inflate(4.0);
        for (Scp5001HabCatalog Entry : Scp5001HabCatalog.values()) {
            if (Entry.kind() != Scp5001HabCatalog.Kind.FloorTall) {
                continue;
            }
            Block Made = Scp5001HabProps.blocks().get(Entry).get();
            for (boolean BreakTop : new boolean[] {true, false}) {
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                World.setBlock(Pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                BlockState Lower = Made.defaultBlockState().setValue(Scp5001HabTallBlock.FACING, Direction.EAST);
                World.setBlock(Pos, Lower, Block.UPDATE_ALL);
                Made.setPlacedBy(World, Pos, Lower, null, new ItemStack(Made));
                BlockState Upper = World.getBlockState(Pos.above());
                Helper.assertTrue(Upper.is(Made) && Upper.getValue(Scp5001HabTallBlock.Half) == DoubleBlockHalf.UPPER, Entry.id() + " did not place its upper half");
                Helper.assertTrue(Upper.getValue(Scp5001HabTallBlock.FACING) == Direction.EAST, Entry.id() + " upper half lost its facing");
                World.destroyBlock(BreakTop ? Pos.above() : Pos, true);
                Helper.assertTrue(World.getBlockState(Pos).isAir() && World.getBlockState(Pos.above()).isAir(), Entry.id() + " left a half behind when the "
                        + (BreakTop ? "upper" : "lower") + " half broke");
                int Dropped = 0;
                for (ItemEntity Drop : World.getEntitiesOfClass(ItemEntity.class, Around)) {
                    if (Drop.getItem().is(Made.asItem())) {
                        Dropped += Drop.getItem().getCount();
                    }
                    Drop.discard();
                }
                Helper.assertTrue(Dropped == 1, Entry.id() + " dropped " + Dropped + " items when the " + (BreakTop ? "upper" : "lower") + " half broke");
            }
        }
        Helper.succeed();
    }
}
