package net.rai.scp_unleashed.door;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class DoorRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<SlidingDoorBlock> SlidingDoor = BlockRegister.register("sliding_door",
            () -> new SlidingDoorBlock(DoorKind.Sliding, doorProperties(MapColor.METAL, 5.0f, 6.0f, SoundType.METAL)));
    public static final DeferredBlock<RoomDoorBlock> RoomDoor = BlockRegister.register("room_door",
            () -> new RoomDoorBlock(DoorKind.Room, doorProperties(MapColor.METAL, 5.0f, 6.0f, SoundType.METAL)));
    public static final DeferredBlock<SlidingDoorBlock> LczDoor = BlockRegister.register("lcz_door",
            () -> new SlidingDoorBlock(DoorKind.Lcz, doorProperties(MapColor.QUARTZ, 5.0f, 6.0f, SoundType.METAL)));
    public static final DeferredBlock<RoomDoorBlock> LczRoomDoor = BlockRegister.register("lcz_room_door",
            () -> new RoomDoorBlock(DoorKind.LczRoom, doorProperties(MapColor.QUARTZ, 5.0f, 6.0f, SoundType.METAL)));
    public static final DeferredBlock<PanelDoorBlock> LczSingleDoor = door(DoorKind.LczSingle, MapColor.QUARTZ, 5.0f, 6.0f, SoundType.METAL);
    public static final DeferredBlock<SlidingDoorBlock> HczDoor = BlockRegister.register("hcz_door",
            () -> new SlidingDoorBlock(DoorKind.Hcz, doorProperties(MapColor.COLOR_GRAY, 8.0f, 12.0f, SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<RoomDoorBlock> HczRoomDoor = BlockRegister.register("hcz_room_door",
            () -> new RoomDoorBlock(DoorKind.HczRoom, doorProperties(MapColor.COLOR_GRAY, 8.0f, 12.0f, SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<PanelDoorBlock> HczSingleDoor = door(DoorKind.HczSingle, MapColor.COLOR_GRAY, 8.0f, 12.0f, SoundType.NETHERITE_BLOCK);
    public static final DeferredBlock<SlidingDoorBlock> EzDoor = BlockRegister.register("ez_door",
            () -> new SlidingDoorBlock(DoorKind.Ez, doorProperties(MapColor.SAND, 4.0f, 6.0f, SoundType.METAL)));
    public static final DeferredBlock<RoomDoorBlock> EzRoomDoor = BlockRegister.register("ez_room_door",
            () -> new RoomDoorBlock(DoorKind.EzRoom, doorProperties(MapColor.SAND, 4.0f, 6.0f, SoundType.METAL)));
    public static final DeferredBlock<PanelDoorBlock> EzSingleDoor = door(DoorKind.EzSingle, MapColor.SAND, 4.0f, 6.0f, SoundType.METAL);
    public static final DeferredBlock<SlidingDoorBlock> GlassSlidingDoor = BlockRegister.register("glass_sliding_door",
            () -> new SlidingDoorBlock(DoorKind.Glass, doorProperties(MapColor.METAL, 3.0f, 6.0f, SoundType.GLASS)));
    public static final DeferredBlock<PanelDoorBlock> GlassRoomDoor = door(DoorKind.GlassRoom, MapColor.METAL, 3.0f, 6.0f, SoundType.GLASS);
    public static final DeferredBlock<PanelDoorBlock> GlassSingleDoor = door(DoorKind.GlassSingle, MapColor.METAL, 3.0f, 6.0f, SoundType.GLASS);
    public static final DeferredBlock<SingleDoorBlock> SingleSlidingDoor = BlockRegister.register("single_sliding_door",
            () -> new SingleDoorBlock(DoorKind.Single, doorProperties(MapColor.METAL, 5.0f, 6.0f, SoundType.METAL)));
    public static final DeferredBlock<ContainmentDoorBlock> ContainmentDoor = BlockRegister.register("containment_door",
            () -> new ContainmentDoorBlock(doorProperties(MapColor.COLOR_GRAY, 15.0f, 600.0f, SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<PanelDoorBlock> ContainmentDoorXl = door(DoorKind.ContainmentXl, MapColor.COLOR_GRAY, 15.0f, 600.0f, SoundType.NETHERITE_BLOCK);
    public static final DeferredBlock<PanelDoorBlock> ContainmentTallDoor = door(DoorKind.ContainmentTall, MapColor.COLOR_GRAY, 15.0f, 600.0f, SoundType.NETHERITE_BLOCK);
    public static final DeferredBlock<PanelDoorBlock> ContainmentRoomDoor = door(DoorKind.ContainmentRoom, MapColor.COLOR_GRAY, 15.0f, 600.0f, SoundType.NETHERITE_BLOCK);
    public static final DeferredBlock<PanelDoorBlock> ContainmentSingleDoor = door(DoorKind.ContainmentSingle, MapColor.COLOR_GRAY, 15.0f, 600.0f, SoundType.NETHERITE_BLOCK);
    public static final DeferredBlock<BlastGateBlock> BlastGate = BlockRegister.register("blast_gate",
            () -> new BlastGateBlock(doorProperties(MapColor.COLOR_GRAY, 25.0f, 1200.0f, SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<PanelDoorBlock> TallBlastGate = door(DoorKind.BlastGateTall, MapColor.COLOR_GRAY, 25.0f, 1200.0f, SoundType.NETHERITE_BLOCK);
    public static final DeferredBlock<PanelDoorBlock> RoomBlastGate = door(DoorKind.BlastGateRoom, MapColor.COLOR_GRAY, 25.0f, 1200.0f, SoundType.NETHERITE_BLOCK);
    public static final DeferredBlock<PanelDoorBlock> SingleBlastGate = door(DoorKind.BlastGateSingle, MapColor.COLOR_GRAY, 25.0f, 1200.0f, SoundType.NETHERITE_BLOCK);
    public static final DeferredBlock<DoorButtonBlock> DoorButton = BlockRegister.register("door_button", () -> new DoorButtonBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.0f, 6.0f).sound(SoundType.METAL).noCollission().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<DoorLeverBlock> DoorLever = BlockRegister.register("door_lever", () -> new DoorLeverBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.0f, 6.0f).sound(SoundType.METAL).noCollission().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<KeycardScannerBlock> KeycardScanner = BlockRegister.register("keycard_scanner", () -> new KeycardScannerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.5f, 6.0f).sound(SoundType.METAL).noCollission().pushReaction(PushReaction.DESTROY)));
    public static final List<DeferredBlock<? extends PanelDoorBlock>> Doors = List.of(SlidingDoor, RoomDoor, LczDoor, LczRoomDoor, LczSingleDoor,
            HczDoor, HczRoomDoor, HczSingleDoor, EzDoor, EzRoomDoor, EzSingleDoor, GlassSlidingDoor, GlassRoomDoor, GlassSingleDoor, SingleSlidingDoor,
            ContainmentDoorXl, ContainmentDoor, ContainmentTallDoor, ContainmentRoomDoor, ContainmentSingleDoor, BlastGate, TallBlastGate, RoomBlastGate, SingleBlastGate);

    public static final List<DeferredItem<BlockItem>> DoorItems = Doors.stream().map(Door -> ItemRegister.registerSimpleBlockItem(Door)).toList();
    public static final DeferredItem<BlockItem> DoorButtonItem = ItemRegister.registerSimpleBlockItem("door_button", DoorButton);
    public static final DeferredItem<BlockItem> DoorLeverItem = ItemRegister.registerSimpleBlockItem("door_lever", DoorLever);
    public static final DeferredItem<BlockItem> KeycardScannerItem = ItemRegister.registerSimpleBlockItem("keycard_scanner", KeycardScanner);
    public static final DeferredItem<KeycardItem> KeycardLevel1 = keycard("keycard_level_1", 1, Rarity.COMMON);
    public static final DeferredItem<KeycardItem> KeycardLevel2 = keycard("keycard_level_2", 2, Rarity.COMMON);
    public static final DeferredItem<KeycardItem> KeycardLevel3 = keycard("keycard_level_3", 3, Rarity.COMMON);
    public static final DeferredItem<KeycardItem> KeycardLevel4 = keycard("keycard_level_4", 4, Rarity.UNCOMMON);
    public static final DeferredItem<KeycardItem> KeycardLevel5 = keycard("keycard_level_5", 5, Rarity.RARE);
    public static final DeferredItem<KeycardItem> KeycardOmni = keycard("keycard_omni", KeycardItem.OmniClearance, Rarity.EPIC);
    public static final List<DeferredItem<KeycardItem>> Keycards = List.of(KeycardLevel1, KeycardLevel2, KeycardLevel3, KeycardLevel4, KeycardLevel5, KeycardOmni);
    public static final DeferredItem<ScrewdriverItem> Screwdriver = ItemRegister.register("screwdriver",
            () -> new ScrewdriverItem(new Item.Properties().durability(ScrewdriverItem.Durability)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PanelDoorBlockEntity>> PanelDoor = BlockEntityTypeRegister.register("door_panels",
            () -> BlockEntityType.Builder.of(PanelDoorBlockEntity::new, Doors.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DoorLeverBlockEntity>> DoorLeverEntity = BlockEntityTypeRegister.register("door_lever",
            () -> BlockEntityType.Builder.of(DoorLeverBlockEntity::new, DoorLever.get()).build(null));

    private DoorRegistry() {
    }

    private static DeferredBlock<PanelDoorBlock> door(DoorKind Kind, MapColor Colour, float Strength, float Resistance, SoundType Sound) {
        return BlockRegister.register(Kind.getSerializedName(), () -> sized(Kind, doorProperties(Colour, Strength, Resistance, Sound)));
    }

    private static PanelDoorBlock sized(DoorKind Kind, BlockBehaviour.Properties Properties) {
        return switch (Kind.getSize()) {
            case Single -> new SingleDoorBlock(Kind, Properties);
            case Room -> new RoomDoorBlock(Kind, Properties);
            case Tall -> new SlidingDoorBlock(Kind, Properties);
            case Chamber -> new ContainmentDoorBlock(Properties);
            case Gate -> new BlastGateBlock(Properties);
            case Bulkhead -> new BulkheadDoorBlock(Kind, Properties);
        };
    }

    private static BlockBehaviour.Properties doorProperties(MapColor Colour, float Strength, float Resistance, SoundType Sound) {
        return BlockBehaviour.Properties.of().mapColor(Colour).requiresCorrectToolForDrops().strength(Strength, Resistance).sound(Sound).noOcclusion().dynamicShape()
                .pushReaction(PushReaction.BLOCK).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)
                .isRedstoneConductor((State, World, Pos) -> false);
    }

    private static DeferredItem<KeycardItem> keycard(String Name, int Clearance, Rarity Tier) {
        return ItemRegister.register(Name, () -> new KeycardItem(Clearance, new Item.Properties().stacksTo(1).rarity(Tier)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        DoorSounds.SoundEventRegister.register(ModEventBus);
    }
}
