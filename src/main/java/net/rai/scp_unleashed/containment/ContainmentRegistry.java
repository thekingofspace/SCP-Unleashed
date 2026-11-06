package net.rai.scp_unleashed.containment;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class ContainmentRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);

    public static final DeferredBlock<ContainmentBlock> ReinforcedWall = BlockRegister.register("reinforced_containment_wall", () -> new ContainmentBlock(wallProperties()));
    public static final DeferredBlock<ContainmentBlock> HazardWall = BlockRegister.register("containment_wall_hazard", () -> new ContainmentBlock(wallProperties()));
    public static final DeferredBlock<ContainmentBlock> FloorDrain = BlockRegister.register("containment_floor_drain", () -> new ContainmentBlock(wallProperties()
            .sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<ReinforcedGlassBlock> ReinforcedGlass = BlockRegister.register("reinforced_glass", () -> new ReinforcedGlassBlock(glassProperties()));
    public static final DeferredBlock<ReinforcedPaneBlock> ReinforcedPane = BlockRegister.register("reinforced_glass_pane", () -> new ReinforcedPaneBlock(glassProperties()));
    public static final DeferredBlock<OneWayGlassBlock> OneWayGlass = BlockRegister.register("one_way_glass", () -> new OneWayGlassBlock(glassProperties()));
    public static final DeferredBlock<SmartGlassBlock> SmartGlass = BlockRegister.register("smart_one_way_glass", () -> new SmartGlassBlock(glassProperties()));

    public static final DeferredItem<BlockItem> ReinforcedWallItem = ItemRegister.registerSimpleBlockItem("reinforced_containment_wall", ReinforcedWall);
    public static final DeferredItem<BlockItem> HazardWallItem = ItemRegister.registerSimpleBlockItem("containment_wall_hazard", HazardWall);
    public static final DeferredItem<BlockItem> FloorDrainItem = ItemRegister.registerSimpleBlockItem("containment_floor_drain", FloorDrain);
    public static final DeferredItem<BlockItem> ReinforcedGlassItem = ItemRegister.registerSimpleBlockItem("reinforced_glass", ReinforcedGlass);
    public static final DeferredItem<BlockItem> ReinforcedPaneItem = ItemRegister.registerSimpleBlockItem("reinforced_glass_pane", ReinforcedPane);
    public static final DeferredItem<BlockItem> OneWayGlassItem = ItemRegister.registerSimpleBlockItem("one_way_glass", OneWayGlass);
    public static final DeferredItem<BlockItem> SmartGlassItem = ItemRegister.registerSimpleBlockItem("smart_one_way_glass", SmartGlass);

    private ContainmentRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    private static BlockBehaviour.Properties wallProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(15.0f, 1200.0f).sound(SoundType.POLISHED_DEEPSLATE);
    }

    private static BlockBehaviour.Properties glassProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.HAT).strength(1.5f, 1200.0f).sound(SoundType.GLASS).noOcclusion()
                .isValidSpawn((State, World, Pos, Type) -> false).isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
    }
}
