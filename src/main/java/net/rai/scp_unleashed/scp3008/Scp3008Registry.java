package net.rai.scp_unleashed.scp3008;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.scp3008.block.Scp3008ArrowBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008AxisBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008BeamBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008RackPostBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008RafterBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008CableBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008CeilingBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008CounterBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008CounterBlockEntity;
import net.rai.scp_unleashed.scp3008.block.Scp3008CrateBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008CrateBlockEntity;
import net.rai.scp_unleashed.scp3008.block.Scp3008EntranceBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008ExitDoorBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FacadeBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FacadeSignBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FacingSealedBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FixtureBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FloorBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008PillarBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008SealedBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008ShapedBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008SignBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008StoolBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008TrayBlock;
import net.rai.scp_unleashed.scp3008.item.Scp3008FoodItem;
import net.rai.scp_unleashed.scp3008.world.Scp3008BiomeSource;
import net.rai.scp_unleashed.scp3008.world.Scp3008ChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class Scp3008Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GeneratorRegister = DeferredRegister.create(Registries.CHUNK_GENERATOR, Scp_unleashed.ModId);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BiomeSourceRegister = DeferredRegister.create(Registries.BIOME_SOURCE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> LayoutVersion = AttachmentRegister.register("scp3008_layout",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());
    public static final List<DeferredItem<? extends Item>> Placeables = new ArrayList<>();

    public static final ResourceLocation DocumentId = location("scp_3008");
    public static final ResourceKey<Level> Dimension = ResourceKey.create(Registries.DIMENSION, location("scp3008_store"));
    public static final TagKey<Block> Unbreakable = blockTag("scp3008_unbreakable");
    public static final TagKey<Block> Liftable = blockTag("scp3008_liftable");
    public static final TagKey<Block> Multiblock = blockTag("scp3008_multiblock");
    public static final TagKey<Block> FloorStanding = blockTag("scp3008_floor_standing");
    public static final TagKey<Block> WallMounted = blockTag("scp3008_wall_mounted");
    public static final TagKey<Block> CeilingHung = blockTag("scp3008_ceiling_hung");
    public static final TagKey<Item> CrowbarLoot = TagKey.create(Registries.ITEM, location("scp3008_loot/crowbar"));

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<Scp3008ChunkGenerator>> Generator = GeneratorRegister.register("scp3008_store",
            () -> Scp3008ChunkGenerator.GeneratorCodec);
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<Scp3008BiomeSource>> Departments = BiomeSourceRegister.register("scp3008_departments",
            () -> Scp3008BiomeSource.SourceCodec);

    public static final DeferredBlock<Scp3008FloorBlock> Floor = placeable("scp3008_floor",
            () -> new Scp3008FloorBlock(Scp3008SealedBlock.sealed(MapColor.STONE).sound(SoundType.STONE)));
    public static final DeferredBlock<Scp3008AxisBlock> FloorLine = placeable("scp3008_floor_line",
            () -> new Scp3008AxisBlock(Scp3008SealedBlock.sealed(MapColor.COLOR_YELLOW).sound(SoundType.STONE), null));
    public static final DeferredBlock<Scp3008ArrowBlock> FloorArrow = placeable("scp3008_floor_arrow",
            () -> new Scp3008ArrowBlock(Scp3008SealedBlock.sealed(MapColor.STONE).sound(SoundType.STONE)));
    public static final DeferredBlock<Scp3008SealedBlock> FloorHatch = placeable("scp3008_floor_hatch",
            () -> new Scp3008SealedBlock(Scp3008SealedBlock.sealed(MapColor.COLOR_YELLOW).sound(SoundType.STONE)));
    public static final DeferredBlock<Scp3008PillarBlock> Pillar = placeable("scp3008_pillar",
            () -> new Scp3008PillarBlock(Scp3008SealedBlock.sealed(MapColor.QUARTZ).sound(SoundType.STONE)));
    public static final DeferredBlock<Scp3008CeilingBlock> Ceiling = placeable("scp3008_ceiling",
            () -> new Scp3008CeilingBlock(Scp3008SealedBlock.sealed(MapColor.COLOR_GRAY).sound(SoundType.STONE).noOcclusion().isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<Scp3008CableBlock> Cable = placeable("scp3008_cable",
            () -> new Scp3008CableBlock(Scp3008SealedBlock.sealed(MapColor.METAL).sound(SoundType.CHAIN).noOcclusion().isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<Scp3008AxisBlock> StoreLight = placeable("scp3008_store_light",
            () -> new Scp3008AxisBlock(Scp3008SealedBlock.sealed(MapColor.QUARTZ).sound(SoundType.METAL).noOcclusion().isValidSpawn((State, World, Pos, Kind) -> false),
                    new double[]{0, 11, 4, 16, 16, 12}));
    public static final DeferredBlock<Scp3008SignBlock> DepartmentSign = placeable("scp3008_department_sign",
            () -> new Scp3008SignBlock(Scp3008SealedBlock.sealed(MapColor.COLOR_BLUE).sound(SoundType.METAL).noOcclusion().isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<Scp3008FacingSealedBlock> EmergencyLight = placeable("scp3008_emergency_light",
            () -> new Scp3008FacingSealedBlock(Scp3008SealedBlock.sealed(MapColor.COLOR_GREEN).sound(SoundType.METAL).noOcclusion().lightLevel(State -> 8)
                    .isValidSpawn((State, World, Pos, Kind) -> false), new double[][]{{2, 3, 13, 14, 12, 16}}));
    public static final DeferredBlock<Scp3008FacadeBlock> Facade = placeable("scp3008_facade",
            () -> new Scp3008FacadeBlock(Scp3008SealedBlock.sealed(MapColor.COLOR_BLUE).sound(SoundType.STONE)));
    public static final DeferredBlock<Scp3008FacadeSignBlock> FacadeSign = placeable("scp3008_facade_sign",
            () -> new Scp3008FacadeSignBlock(Scp3008SealedBlock.sealed(MapColor.COLOR_BLUE).sound(SoundType.METAL)));
    public static final DeferredBlock<Scp3008EntranceBlock> Entrance = placeable("scp3008_entrance",
            () -> new Scp3008EntranceBlock(Scp3008SealedBlock.sealed(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.GLASS).noOcclusion().noCollission()
                    .isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<Scp3008ExitDoorBlock> ExitDoor = placeable("scp3008_exit_door",
            () -> new Scp3008ExitDoorBlock(Scp3008SealedBlock.sealed(MapColor.METAL).sound(SoundType.METAL).noOcclusion().isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<Scp3008FacingSealedBlock> ExitSign = placeable("scp3008_exit_sign",
            () -> new Scp3008FacingSealedBlock(Scp3008SealedBlock.sealed(MapColor.COLOR_GREEN).sound(SoundType.METAL).noOcclusion().lightLevel(State -> 12)
                    .isValidSpawn((State, World, Pos, Kind) -> false), new double[][]{{0, 3, 13, 16, 13, 16}}));

    public static final DeferredBlock<Block> DisplayWall = placeable("scp3008_display_wall",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5f, 4.0f).sound(SoundType.WOOD)));
    public static final DeferredBlock<Scp3008RafterBlock> Rafter = placeable("scp3008_rafter",
            () -> new Scp3008RafterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.2f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<Scp3008FixtureBlock> Shelf = placeable("scp3008_aisle_shelf",
            () -> new Scp3008FixtureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 4.0f).sound(SoundType.METAL).noOcclusion(),
                    new double[][]{{0, 0, 2, 16, 16, 16}}, true));
    public static final DeferredBlock<Scp3008RackPostBlock> RackPost = placeable("scp3008_rack_post",
            () -> new Scp3008RackPostBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<Scp3008BeamBlock> RackBeam = placeable("scp3008_rack_beam",
            () -> new Scp3008BeamBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion(),
                    new double[]{0, 13, 0, 16, 16, 16}));
    public static final DeferredBlock<Scp3008CrateBlock> Crate = placeable("scp3008_crate",
            () -> new Scp3008CrateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(1.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<Scp3008CounterBlock> Counter = placeable("scp3008_food_counter",
            () -> new Scp3008CounterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<Scp3008FixtureBlock> MenuBoard = placeable("scp3008_menu_board",
            () -> new Scp3008FixtureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.WOOD).noOcclusion().noCollission(),
                    new double[][]{{0, 0, 14, 16, 16, 16}}, false));
    public static final DeferredBlock<Scp3008ShapedBlock> FoodTable = placeable("scp3008_food_table",
            () -> new Scp3008ShapedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5f).sound(SoundType.WOOD).noOcclusion(),
                    new double[][]{{0, 13, 0, 16, 16, 16}, {6, 0, 6, 10, 13, 10}, {3, 0, 3, 13, 1, 13}}));
    public static final DeferredBlock<Scp3008TrayBlock> Tray = placeable("scp3008_food_tray",
            () -> new Scp3008TrayBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.3f).sound(SoundType.WOOD).noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Scp3008StoolBlock> Stool = placeable("scp3008_food_stool",
            () -> new Scp3008StoolBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<Scp3008ShapedBlock> Planter = placeable("scp3008_planter",
            () -> new Scp3008ShapedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.8f).sound(SoundType.GRASS).noOcclusion(),
                    new double[][]{{2, 0, 2, 14, 14, 14}}));

    public static final DeferredBlock<Scp3008FixtureBlock> PriceStand = placeable("scp3008_price_stand",
            () -> new Scp3008FixtureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.4f).sound(SoundType.WOOD).noOcclusion().noCollission(),
                    new double[][]{{7, 0, 7, 9, 9, 9}, {3, 8, 7, 13, 15, 9}}, false));
    public static final DeferredBlock<Scp3008FixtureBlock> WallFrame = placeable("scp3008_wall_frame",
            () -> new Scp3008FixtureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.4f).sound(SoundType.WOOD).noOcclusion().noCollission(),
                    new double[][]{{2, 3, 15, 14, 13, 16}}, false));

    public static final DeferredItem<Item> Meatballs = ItemRegister.register("scp3008_meatballs",
            () -> new Scp3008FoodItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.7f).build()), false,
                    "item.scp_unleashed.scp3008_meatballs.tooltip"));
    public static final DeferredItem<Item> Fries = ItemRegister.register("scp3008_fries",
            () -> new Scp3008FoodItem(new Item.Properties().stacksTo(32).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.35f).fast().build()), false,
                    "item.scp_unleashed.scp3008_fries.tooltip"));
    public static final DeferredItem<Item> SoftDrink = ItemRegister.register("scp3008_soft_drink",
            () -> new Scp3008FoodItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0), 1.0f).build()), true, "item.scp_unleashed.scp3008_soft_drink.tooltip"));
    public static final DeferredItem<Item> Jam = ItemRegister.register("scp3008_lingonberry_jam",
            () -> new Scp3008FoodItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).build()), false,
                    "item.scp_unleashed.scp3008_lingonberry_jam.tooltip"));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp3008CrateBlockEntity>> CrateEntity = BlockEntityTypeRegister.register("scp3008_crate",
            () -> BlockEntityType.Builder.of(Scp3008CrateBlockEntity::new, Crate.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp3008CounterBlockEntity>> CounterEntity = BlockEntityTypeRegister.register("scp3008_food_counter",
            () -> BlockEntityType.Builder.of(Scp3008CounterBlockEntity::new, Counter.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> FloodRelay = sound("scp3008_core_floodlight_relay");
    public static final DeferredHolder<SoundEvent, SoundEvent> FloodClunk = sound("scp3008_core_floodlight_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> FloodThud = sound("scp3008_core_floodlight_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> FloodRing = sound("scp3008_core_floodlight_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> FloodTick = sound("scp3008_core_floodlight_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> FloodHum = sound("scp3008_core_floodlight_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> PowerDown = sound("scp3008_core_power_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> PowerUp = sound("scp3008_core_power_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> MuzakPling = sound("scp3008_core_muzak_pling");
    public static final DeferredHolder<SoundEvent, SoundEvent> MuzakHarp = sound("scp3008_core_muzak_harp");
    public static final DeferredHolder<SoundEvent, SoundEvent> MuzakBass = sound("scp3008_core_muzak_bass");
    public static final DeferredHolder<SoundEvent, SoundEvent> MuzakBell = sound("scp3008_core_muzak_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> MuzakFlute = sound("scp3008_core_muzak_flute");
    public static final DeferredHolder<SoundEvent, SoundEvent> MuzakChime = sound("scp3008_core_muzak_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorSlide = sound("scp3008_core_door_slide");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorHiss = sound("scp3008_core_door_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExitOpen = sound("scp3008_core_exit_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExitBar = sound("scp3008_core_exit_bar");
    public static final DeferredHolder<SoundEvent, SoundEvent> CounterServe = sound("scp3008_core_counter_serve");
    public static final DeferredHolder<SoundEvent, SoundEvent> TrayTake = sound("scp3008_core_tray_take");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrateOpen = sound("scp3008_core_crate_open");

    private Scp3008Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static TagKey<Block> blockTag(String Path) {
        return TagKey.create(Registries.BLOCK, location(Path));
    }

    private static <T extends Block> DeferredBlock<T> placeable(String Name, Supplier<T> Factory) {
        DeferredBlock<T> Made = BlockRegister.register(Name, Factory);
        DeferredItem<BlockItem> Entry = ItemRegister.register(Name, () -> new BlockItem(Made.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
        Placeables.add(Entry);
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
        GeneratorRegister.register(ModEventBus);
        BiomeSourceRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(Scp3008StoreCondition.TypeName, Scp3008StoreCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp3008Storefront::spawn);
    }
}
