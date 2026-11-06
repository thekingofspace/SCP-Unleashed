package net.rai.scp_unleashed.scp5001.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.scp5001.engine.Scp5001EngineBlock;
import net.rai.scp_unleashed.scp5001.engine.Scp5001EngineBlockEntity;
import net.rai.scp_unleashed.scp5001.terminal.Scp5001TerminalBlock;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001ArchDoorBlock;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001EntryDoorBlock;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001FacingBlock;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001GlyphBlock;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001PillarBlock;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001RailingBlock;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001SeamFloorBlock;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001WallBlock;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001ShellBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class Scp5001WorldRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final ResourceLocation DocumentId = location("scp_5001");
    public static final List<DeferredItem<BlockItem>> TabItems = new ArrayList<>();

    public static final DeferredBlock<Scp5001ShellBlock> Shell = BlockRegister.register("scp5001_shell", () -> new Scp5001ShellBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM).strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.NETHERITE_BLOCK)
            .pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)
            .lightLevel(State -> State.getValue(Scp5001ShellBlock.Seam) ? 3 : 0)));
    public static final DeferredBlock<Scp5001EntryDoorBlock> EntryDoor = BlockRegister.register("scp5001_entry_door", () -> new Scp5001EntryDoorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).strength(-1.0f, 3600000.0f).noLootTable().noOcclusion().sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK)
            .isValidSpawn((State, Getter, Pos, Type) -> false).lightLevel(State -> 4)));
    public static final DeferredBlock<Scp5001EngineBlock> Engine = BlockRegister.register("scp5001_engine", () -> new Scp5001EngineBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE).strength(-1.0f, 3600000.0f).noLootTable().noOcclusion().sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK)
            .lightLevel(State -> 15)));
    public static final DeferredBlock<Scp5001TerminalBlock> Terminal = BlockRegister.register("scp5001_data_terminal", () -> new Scp5001TerminalBlock(arch(MapColor.SNOW, 9)
            .noOcclusion()));
    public static final DeferredBlock<Block> Panel = plain("scp5001_arch_panel", MapColor.SNOW, 0, SoundType.STONE);
    public static final DeferredBlock<Block> PanelSeam = plain("scp5001_arch_panel_seam", MapColor.SNOW, 10, SoundType.STONE);
    public static final DeferredBlock<Scp5001GlyphBlock> PanelGlyph = BlockRegister.register("scp5001_arch_panel_glyph", () -> new Scp5001GlyphBlock(arch(MapColor.SNOW, 6)));
    public static final DeferredBlock<Block> Graphite = plain("scp5001_arch_graphite", MapColor.COLOR_BLACK, 0, SoundType.METAL);
    public static final DeferredBlock<Block> GraphitePanel = plain("scp5001_arch_graphite_panel", MapColor.COLOR_BLACK, 0, SoundType.METAL);
    public static final DeferredBlock<Block> GraphiteSeam = plain("scp5001_arch_graphite_seam", MapColor.COLOR_BLACK, 10, SoundType.METAL);
    public static final DeferredBlock<Block> Rib = plain("scp5001_arch_rib", MapColor.COLOR_BLACK, 7, SoundType.METAL);
    public static final DeferredBlock<Scp5001FacingBlock> Hatch = BlockRegister.register("scp5001_arch_hatch", () -> new Scp5001FacingBlock(arch(MapColor.COLOR_BLACK, 3)));
    public static final DeferredBlock<Scp5001FacingBlock> Vent = BlockRegister.register("scp5001_arch_vent", () -> new Scp5001FacingBlock(arch(MapColor.COLOR_BLACK, 0)));
    public static final DeferredBlock<Block> Floor = plain("scp5001_arch_floor", MapColor.SNOW, 0, SoundType.STONE);
    public static final DeferredBlock<Block> FloorHex = plain("scp5001_arch_floor_hex", MapColor.COLOR_BLACK, 0, SoundType.METAL);
    public static final DeferredBlock<Block> FloorGrate = plain("scp5001_arch_floor_grate", MapColor.METAL, 5, SoundType.METAL);
    public static final DeferredBlock<Scp5001SeamFloorBlock> FloorSeam = BlockRegister.register("scp5001_arch_floor_seam", () -> new Scp5001SeamFloorBlock(arch(MapColor.COLOR_BLACK, 9)));
    public static final DeferredBlock<Block> FloorGlyph = plain("scp5001_arch_floor_glyph", MapColor.COLOR_PURPLE, 7, SoundType.METAL);
    public static final DeferredBlock<Block> ChamberFloor = plain("scp5001_arch_chamber_floor", MapColor.COLOR_BLACK, 0, SoundType.METAL);
    public static final DeferredBlock<Block> Ceiling = plain("scp5001_arch_ceiling", MapColor.SNOW, 0, SoundType.STONE);
    public static final DeferredBlock<Block> CeilingLight = plain("scp5001_arch_ceiling_light", MapColor.SNOW, 15, SoundType.GLASS);
    public static final DeferredBlock<TransparentBlock> Glass = BlockRegister.register("scp5001_arch_glass", () -> new TransparentBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE).strength(2.0f, 9.0f).sound(SoundType.GLASS).noOcclusion().isValidSpawn((State, Getter, Pos, Type) -> false)
            .isRedstoneConductor((State, Getter, Pos) -> false).isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false)));
    public static final DeferredBlock<Scp5001PillarBlock> Pillar = BlockRegister.register("scp5001_arch_pillar", () -> new Scp5001PillarBlock(arch(MapColor.METAL, 0).noOcclusion()));
    public static final DeferredBlock<Block> Trim = plain("scp5001_arch_trim", MapColor.METAL, 9, SoundType.METAL);
    public static final DeferredBlock<Block> TrimAmber = plain("scp5001_arch_trim_amber", MapColor.METAL, 9, SoundType.METAL);
    public static final DeferredBlock<Block> ChamberWall = plain("scp5001_arch_chamber_wall", MapColor.COLOR_BLACK, 7, SoundType.METAL);
    public static final DeferredBlock<StairBlock> Stairs = BlockRegister.register("scp5001_arch_stairs", () -> new StairBlock(Graphite.get().defaultBlockState(),
            arch(MapColor.COLOR_BLACK, 0)));
    public static final DeferredBlock<SlabBlock> Slab = BlockRegister.register("scp5001_arch_slab", () -> new SlabBlock(arch(MapColor.COLOR_BLACK, 0)));
    public static final DeferredBlock<Scp5001RailingBlock> Railing = BlockRegister.register("scp5001_arch_railing", () -> new Scp5001RailingBlock(arch(MapColor.METAL, 6)
            .noOcclusion()));
    public static final DeferredBlock<Scp5001ArchDoorBlock> Door = BlockRegister.register("scp5001_arch_door", () -> new Scp5001ArchDoorBlock(arch(MapColor.COLOR_BLACK, 0)
            .noOcclusion().pushReaction(PushReaction.BLOCK)));

    public static final DeferredItem<BlockItem> ShellItem = item("scp5001_shell", Shell, Rarity.EPIC);
    public static final DeferredItem<BlockItem> EntryDoorItem = item("scp5001_entry_door", EntryDoor, Rarity.EPIC);
    public static final DeferredItem<BlockItem> EngineItem = item("scp5001_engine", Engine, Rarity.EPIC);
    public static final DeferredItem<BlockItem> TerminalItem = item("scp5001_data_terminal", Terminal, Rarity.UNCOMMON);

    static {
        for (DeferredBlock<? extends Block> Each : List.of(Panel, PanelSeam, PanelGlyph, Graphite, GraphitePanel, GraphiteSeam, Rib, Hatch, Vent, Floor, FloorHex, FloorGrate,
                FloorSeam, FloorGlyph, ChamberFloor, Ceiling, CeilingLight, Glass, Pillar, Trim, TrimAmber, ChamberWall, Stairs, Slab, Railing, Door)) {
            item(Each.getId().getPath(), Each, Rarity.COMMON);
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp5001EngineBlockEntity>> EngineEntity = BlockEntityTypeRegister.register("scp5001_engine",
            () -> BlockEntityType.Builder.of(Scp5001EngineBlockEntity::new, Engine.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> EngineHum = sound("scp5001.world.engine_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> EngineLow = sound("scp5001.world.engine_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> EngineShimmer = sound("scp5001.world.engine_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorOpen = sound("scp5001.world.door_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorClose = sound("scp5001.world.door_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorChime = sound("scp5001.world.door_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> AirlockOpen = sound("scp5001.world.airlock_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> AirlockClose = sound("scp5001.world.airlock_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> AirlockHiss = sound("scp5001.world.airlock_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> AirlockSeal = sound("scp5001.world.airlock_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> TerminalOpen = sound("scp5001.world.terminal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> TerminalPage = sound("scp5001.world.terminal_page");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EngineLayers = List.of(EngineHum, EngineLow, EngineShimmer);

    private Scp5001WorldRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static BlockBehaviour.Properties arch(MapColor Colour, int Light) {
        BlockBehaviour.Properties Made = BlockBehaviour.Properties.of().mapColor(Colour).instrument(NoteBlockInstrument.BASEDRUM).strength(3.5f, 9.0f)
                .requiresCorrectToolForDrops().sound(SoundType.METAL).isValidSpawn((State, Getter, Pos, Type) -> false);
        if (Light > 0) {
            Made = Made.lightLevel(State -> Light);
        }
        return Made;
    }

    private static DeferredBlock<Block> plain(String Name, MapColor Colour, int Light, SoundType Sound) {
        return BlockRegister.register(Name, () -> new Scp5001WallBlock(arch(Colour, Light).sound(Sound)));
    }

    private static DeferredItem<BlockItem> item(String Name, Supplier<? extends Block> Owner, Rarity Grade) {
        DeferredItem<BlockItem> Made = ItemRegister.register(Name, () -> new BlockItem(Owner.get(), new Item.Properties().rarity(Grade)));
        TabItems.add(Made);
        return Made;
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp5001FacilityCondition.TypeName, Scp5001FacilityCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp5001Sites::confirm);
    }
}
