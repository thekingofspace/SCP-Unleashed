package net.rai.scp_unleashed.scp3008.plywood;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class Scp3008PlywoodRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final List<DeferredItem<? extends Item>> Placeables = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> Parts = new ArrayList<>();

    public static final float Hardness = 2.0f;
    public static final float Resistance = 3.0f;
    public static final float DoorHardness = 3.0f;
    public static final float PanelHardness = 1.5f;
    public static final float ChestHardness = 2.5f;
    public static final float SwitchHardness = 0.5f;
    public static final int ButtonTicks = 30;

    public static final BlockSetType PlywoodSet = BlockSetType.register(new BlockSetType("scp_unleashed:scp3008_plywood"));
    public static final WoodType PlywoodWood = new WoodType("scp_unleashed:scp3008_plywood", PlywoodSet);

    public static final DeferredHolder<SoundEvent, SoundEvent> ChestOpen = sound("scp3008_plywood_chest_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChestClose = sound("scp3008_plywood_chest_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChestLatch = sound("scp3008_plywood_chest_latch");

    public static final DeferredBlock<RotatedPillarBlock> Plywood = BlockRegister.register("scp3008_plywood",
            () -> new RotatedPillarBlock(wood(MapColor.SAND).strength(Hardness, Resistance)));
    public static final DeferredBlock<SlabBlock> Slab = BlockRegister.register("scp3008_plywood_slab",
            () -> new SlabBlock(wood(MapColor.SAND).strength(Hardness, Resistance)));
    public static final DeferredBlock<StairBlock> Stairs = BlockRegister.register("scp3008_plywood_stairs",
            () -> new StairBlock(Plywood.get().defaultBlockState(), wood(MapColor.SAND).strength(Hardness, Resistance)));
    public static final DeferredBlock<IronBarsBlock> Panel = BlockRegister.register("scp3008_plywood_panel",
            () -> new IronBarsBlock(wood(MapColor.SAND).strength(PanelHardness, Resistance).noOcclusion()));
    public static final DeferredBlock<FenceBlock> Fence = BlockRegister.register("scp3008_plywood_fence",
            () -> new FenceBlock(wood(MapColor.SAND).forceSolidOn().strength(Hardness, Resistance)));
    public static final DeferredBlock<FenceGateBlock> FenceGate = BlockRegister.register("scp3008_plywood_fence_gate",
            () -> new FenceGateBlock(PlywoodWood, wood(MapColor.SAND).forceSolidOn().strength(Hardness, Resistance)));
    public static final DeferredBlock<DoorBlock> Door = BlockRegister.register("scp3008_plywood_door",
            () -> new DoorBlock(PlywoodSet, wood(MapColor.SAND).strength(DoorHardness).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<TrapDoorBlock> Trapdoor = BlockRegister.register("scp3008_plywood_trapdoor",
            () -> new TrapDoorBlock(PlywoodSet, wood(MapColor.SAND).strength(DoorHardness).noOcclusion().isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<PressurePlateBlock> PressurePlate = BlockRegister.register("scp3008_plywood_pressure_plate",
            () -> new PressurePlateBlock(PlywoodSet, wood(MapColor.SAND).forceSolidOn().noCollission().strength(SwitchHardness).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<ButtonBlock> Button = BlockRegister.register("scp3008_plywood_button",
            () -> new ButtonBlock(PlywoodSet, ButtonTicks, BlockBehaviour.Properties.of().noCollission().strength(SwitchHardness).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> WhiteLaminate = BlockRegister.register("scp3008_plywood_white_laminate",
            () -> new Block(wood(MapColor.SNOW).strength(Hardness, Resistance)));
    public static final DeferredBlock<SlabBlock> WhiteLaminateSlab = BlockRegister.register("scp3008_plywood_white_laminate_slab",
            () -> new SlabBlock(wood(MapColor.SNOW).strength(Hardness, Resistance)));
    public static final DeferredBlock<StairBlock> WhiteLaminateStairs = BlockRegister.register("scp3008_plywood_white_laminate_stairs",
            () -> new StairBlock(WhiteLaminate.get().defaultBlockState(), wood(MapColor.SNOW).strength(Hardness, Resistance)));
    public static final DeferredBlock<Block> DarkVeneer = BlockRegister.register("scp3008_plywood_dark_veneer",
            () -> new Block(wood(MapColor.COLOR_BROWN).strength(Hardness, Resistance)));
    public static final DeferredBlock<SlabBlock> DarkVeneerSlab = BlockRegister.register("scp3008_plywood_dark_veneer_slab",
            () -> new SlabBlock(wood(MapColor.COLOR_BROWN).strength(Hardness, Resistance)));
    public static final DeferredBlock<StairBlock> DarkVeneerStairs = BlockRegister.register("scp3008_plywood_dark_veneer_stairs",
            () -> new StairBlock(DarkVeneer.get().defaultBlockState(), wood(MapColor.COLOR_BROWN).strength(Hardness, Resistance)));
    public static final DeferredBlock<Scp3008PlywoodChestBlock> Chest = BlockRegister.register("scp3008_plywood_chest",
            () -> new Scp3008PlywoodChestBlock(wood(MapColor.SAND).strength(ChestHardness).noOcclusion()));

    public static final DeferredItem<BlockItem> PlywoodItem = placeable("scp3008_plywood", Plywood);
    public static final DeferredItem<BlockItem> SlabItem = placeable("scp3008_plywood_slab", Slab);
    public static final DeferredItem<BlockItem> StairsItem = placeable("scp3008_plywood_stairs", Stairs);
    public static final DeferredItem<BlockItem> PanelItem = placeable("scp3008_plywood_panel", Panel);
    public static final DeferredItem<BlockItem> FenceItem = placeable("scp3008_plywood_fence", Fence);
    public static final DeferredItem<BlockItem> FenceGateItem = placeable("scp3008_plywood_fence_gate", FenceGate);
    public static final DeferredItem<DoubleHighBlockItem> DoorItem = listed(Placeables, "scp3008_plywood_door",
            () -> new DoubleHighBlockItem(Door.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TrapdoorItem = placeable("scp3008_plywood_trapdoor", Trapdoor);
    public static final DeferredItem<BlockItem> PressurePlateItem = placeable("scp3008_plywood_pressure_plate", PressurePlate);
    public static final DeferredItem<BlockItem> ButtonItem = placeable("scp3008_plywood_button", Button);
    public static final DeferredItem<BlockItem> WhiteLaminateItem = placeable("scp3008_plywood_white_laminate", WhiteLaminate);
    public static final DeferredItem<BlockItem> WhiteLaminateSlabItem = placeable("scp3008_plywood_white_laminate_slab", WhiteLaminateSlab);
    public static final DeferredItem<BlockItem> WhiteLaminateStairsItem = placeable("scp3008_plywood_white_laminate_stairs", WhiteLaminateStairs);
    public static final DeferredItem<BlockItem> DarkVeneerItem = placeable("scp3008_plywood_dark_veneer", DarkVeneer);
    public static final DeferredItem<BlockItem> DarkVeneerSlabItem = placeable("scp3008_plywood_dark_veneer_slab", DarkVeneerSlab);
    public static final DeferredItem<BlockItem> DarkVeneerStairsItem = placeable("scp3008_plywood_dark_veneer_stairs", DarkVeneerStairs);
    public static final DeferredItem<BlockItem> ChestItem = placeable("scp3008_plywood_chest", Chest);
    public static final DeferredItem<Item> Strip = listed(Parts, "scp3008_plywood_strip", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp3008PlywoodChestEntity>> ChestEntity = BlockEntityRegister.register("scp3008_plywood_chest",
            () -> BlockEntityType.Builder.of(Scp3008PlywoodChestEntity::new, Chest.get()).build(null));

    private Scp3008PlywoodRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static BlockBehaviour.Properties wood(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).ignitedByLava();
    }

    private static <T extends Item> DeferredItem<T> listed(List<DeferredItem<? extends Item>> Group, String Name, Supplier<T> Factory) {
        DeferredItem<T> Made = ItemRegister.register(Name, Factory);
        Group.add(Made);
        return Made;
    }

    private static <T extends Block> DeferredItem<BlockItem> placeable(String Name, Supplier<T> Placed) {
        return listed(Placeables, Name, () -> new BlockItem(Placed.get(), new Item.Properties()));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
