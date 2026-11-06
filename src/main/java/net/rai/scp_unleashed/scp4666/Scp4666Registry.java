package net.rai.scp_unleashed.scp4666;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.scp4666.block.Scp4666BoxBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666BoxBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666CalendarBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666ChoreListBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666DiaryBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666ExitDoorBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666FireplaceBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666FootprintsBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666FrozenGatewayBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666CellarDoorBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666CoatHooksBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666CoatHooksBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666FurnitureBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666GramophoneBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666GramophoneBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666HeaterBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666LampBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666OwnedBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666PresentBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666PuzzleBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666StockingBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666StoveBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666SwitchBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666TreeBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666WindowBlock;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666DoorNote;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666NoteItem;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666Sighting;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666Visit;
import net.rai.scp_unleashed.scp4666.item.Scp4666CaneItem;
import net.rai.scp_unleashed.scp4666.item.Scp4666ChoreItem;
import net.rai.scp_unleashed.scp4666.item.Scp4666CocoaItem;
import net.rai.scp_unleashed.scp4666.item.Scp4666DollItem;
import net.rai.scp_unleashed.scp4666.item.Scp4666NecklaceItem;
import net.rai.scp_unleashed.scp4666.item.Scp4666WinterGearItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public final class Scp4666Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp4666Visit>> Visit = AttachmentRegister.register("scp4666_visit",
            () -> AttachmentType.builder(() -> new Scp4666Visit()).serialize(Scp4666Visit.CODEC).copyOnDeath().build());
    public static final int HatWear = 150;
    public static final int CoatWear = 240;
    public static final int BootsWear = 200;
    public static final float HatShare = 0.15f;
    public static final float CoatShare = 0.35f;
    public static final float BootsShare = 0.20f;
    public static final List<DeferredItem<? extends Item>> Furniture = new ArrayList<>();

    public static final ResourceLocation DocumentId = location("scp_4666");
    public static final ResourceKey<DamageType> Taken = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp4666_taken"));
    public static final ResourceKey<DamageType> Beaten = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp4666_beaten"));
    public static final ResourceKey<DamageType> Cold = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp4666_cold"));
    public static final ResourceKey<DamageType> Chill = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp4666_chill"));
    public static final String DayMinutesName = "scp4666DayMinutes";
    public static final String AudioEventsName = "scp4666AudioEvents";
    public static final int DefaultDayMinutes = 11;
    public static final int MinDayMinutes = 2;
    public static final int MaxDayMinutes = 120;
    public static final int DefaultAudioEvents = 50;
    public static final int MaxAudioEvents = 200;
    public static GameRules.Key<GameRules.IntegerValue> DayMinutes;
    public static GameRules.Key<GameRules.IntegerValue> AudioEvents;
    public static final ResourceKey<Level> Dimension = ResourceKey.create(Registries.DIMENSION, location("scp4666_winter"));

    public static final DeferredBlock<Scp4666SwitchBlock> LightSwitch = furniture("scp4666_light_switch",
            () -> new Scp4666SwitchBlock(wood().strength(0.8f).noCollission()));
    public static final DeferredBlock<Scp4666LampBlock> Lamp = furniture("scp4666_cabin_lamp",
            () -> new Scp4666LampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8f).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(State -> State.getValue(Scp4666LampBlock.Lit) ? 14 : 0)));
    public static final DeferredBlock<Scp4666TreeBlock> Tree = furniture("scp4666_christmas_tree",
            () -> new Scp4666TreeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.6f).sound(SoundType.GRASS).noOcclusion()
                    .lightLevel(Scp4666TreeBlock::lightOf).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Scp4666FireplaceBlock> Fireplace = furniture("scp4666_fireplace",
            () -> new Scp4666FireplaceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f, 6.0f).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(Scp4666FireplaceBlock::lightOf).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<Scp4666StoveBlock> Stove = furniture("scp4666_stove",
            () -> new Scp4666StoveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(State -> 6)));
    public static final DeferredBlock<Scp4666FurnitureBlock> Counter = furniture("scp4666_counter",
            () -> new Scp4666FurnitureBlock(wood().noOcclusion(), Scp4666FurnitureBlock.Kind.Counter));
    public static final DeferredBlock<Scp4666FurnitureBlock> Cupboard = furniture("scp4666_cupboard",
            () -> new Scp4666FurnitureBlock(wood().noOcclusion(), Scp4666FurnitureBlock.Kind.Cupboard));
    public static final DeferredBlock<Scp4666FurnitureBlock> Table = furniture("scp4666_table",
            () -> new Scp4666FurnitureBlock(wood().noOcclusion(), Scp4666FurnitureBlock.Kind.Table));
    public static final DeferredBlock<Scp4666FurnitureBlock> Chair = furniture("scp4666_chair",
            () -> new Scp4666FurnitureBlock(wood().noOcclusion(), Scp4666FurnitureBlock.Kind.Chair));
    public static final DeferredBlock<Scp4666FurnitureBlock> Armchair = furniture("scp4666_armchair",
            () -> new Scp4666FurnitureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.8f).sound(SoundType.WOOL).noOcclusion().ignitedByLava(),
                    Scp4666FurnitureBlock.Kind.Armchair));
    public static final DeferredBlock<Scp4666FurnitureBlock> RockingChair = furniture("scp4666_rocking_chair",
            () -> new Scp4666FurnitureBlock(wood().noOcclusion(), Scp4666FurnitureBlock.Kind.Rocking));
    public static final DeferredBlock<Scp4666FurnitureBlock> Nightstand = furniture("scp4666_nightstand",
            () -> new Scp4666FurnitureBlock(wood().noOcclusion(), Scp4666FurnitureBlock.Kind.Nightstand));
    public static final DeferredBlock<Scp4666GramophoneBlock> Gramophone = furniture("scp4666_gramophone",
            () -> new Scp4666GramophoneBlock(wood().noOcclusion()));
    public static final DeferredBlock<Scp4666PuzzleBlock> Puzzle = furniture("scp4666_puzzle_table",
            () -> new Scp4666PuzzleBlock(wood().noOcclusion()));
    public static final DeferredBlock<Scp4666CalendarBlock> Calendar = furniture("scp4666_advent_calendar",
            () -> new Scp4666CalendarBlock(wood().strength(0.6f).noCollission()));
    public static final DeferredBlock<Scp4666ChoreListBlock> ChoreList = furniture("scp4666_chore_list",
            () -> new Scp4666ChoreListBlock(wood().strength(0.6f).noCollission().noOcclusion()));
    public static final DeferredBlock<Scp4666DiaryBlock> Diary = furniture("scp4666_diary",
            () -> new Scp4666DiaryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.4f).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<Scp4666FurnitureBlock> Trunk = decor("scp4666_trunk", wood().noOcclusion(), Scp4666FurnitureBlock.Kind.Trunk);
    public static final DeferredBlock<Scp4666FurnitureBlock> Washstand = decor("scp4666_washstand", wood().noOcclusion(), Scp4666FurnitureBlock.Kind.Washstand);
    public static final DeferredBlock<Scp4666FurnitureBlock> PantryShelf = decor("scp4666_pantry_shelf", wood().noOcclusion(), Scp4666FurnitureBlock.Kind.PantryShelf);
    public static final DeferredBlock<Scp4666FurnitureBlock> HangingPans = decor("scp4666_hanging_pans",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f).sound(SoundType.LANTERN).noOcclusion().noCollission(), Scp4666FurnitureBlock.Kind.HangingPans);
    public static final DeferredBlock<Scp4666CoatHooksBlock> CoatHooks = furniture("scp4666_coat_hooks", () -> new Scp4666CoatHooksBlock(wood().noOcclusion().noCollission()));
    public static final DeferredBlock<Scp4666FurnitureBlock> Boots = decor("scp4666_boots",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.4f).sound(SoundType.WOOL).noOcclusion(), Scp4666FurnitureBlock.Kind.Boots);
    public static final DeferredBlock<Scp4666FurnitureBlock> Picture = decor("scp4666_picture", wood().strength(0.6f).noOcclusion().noCollission(), Scp4666FurnitureBlock.Kind.Picture);
    public static final DeferredBlock<Scp4666FurnitureBlock> Wreath = decor("scp4666_wreath",
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.3f).sound(SoundType.AZALEA_LEAVES).noOcclusion().noCollission(), Scp4666FurnitureBlock.Kind.Wreath);
    public static final DeferredBlock<Scp4666FurnitureBlock> RockingHorse = decor("scp4666_rocking_horse", wood().noOcclusion(), Scp4666FurnitureBlock.Kind.RockingHorse);
    public static final DeferredBlock<Scp4666FurnitureBlock> Sheeted = decor("scp4666_sheeted",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8f).sound(SoundType.WOOL).noOcclusion(), Scp4666FurnitureBlock.Kind.Sheeted);
    public static final DeferredBlock<Scp4666FurnitureBlock> Icicles = decor("scp4666_icicles",
            BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.3f).sound(SoundType.GLASS).noOcclusion().noCollission(), Scp4666FurnitureBlock.Kind.Icicles);
    public static final DeferredBlock<Scp4666FurnitureBlock> ChoppingBlock = decor("scp4666_chopping_block", wood().noOcclusion(), Scp4666FurnitureBlock.Kind.ChoppingBlock);
    public static final DeferredBlock<Scp4666FurnitureBlock> CoalBin = decor("scp4666_coal_bin", wood().noOcclusion(), Scp4666FurnitureBlock.Kind.CoalBin);
    public static final DeferredBlock<Scp4666FurnitureBlock> Workbench = decor("scp4666_workbench", wood().noOcclusion(), Scp4666FurnitureBlock.Kind.Workbench);
    public static final DeferredBlock<Scp4666FurnitureBlock> MoonBoard = decor("scp4666_moon_board", wood().noOcclusion(), Scp4666FurnitureBlock.Kind.MoonBoard);
    public static final DeferredBlock<Scp4666HeaterBlock> Boiler = furniture("scp4666_boiler",
            () -> new Scp4666HeaterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(Scp4666HeaterBlock::lightOf).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<Scp4666FurnitureBlock> Parcel = decor("scp4666_parcel",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.4f).sound(SoundType.WOOL).noOcclusion(), Scp4666FurnitureBlock.Kind.Parcel);
    public static final DeferredBlock<Scp4666WindowBlock> Window = BlockRegister.register("scp4666_window",
            () -> new Scp4666WindowBlock(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.3f).sound(SoundType.GLASS).noOcclusion().noLootTable()
                    .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false)));
    public static final DeferredBlock<Scp4666CellarDoorBlock> CellarDoor = BlockRegister.register("scp4666_cellar_door",
            () -> new Scp4666CellarDoorBlock(wood().strength(3.0f).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<DoubleHighBlockItem> CellarDoorItem = listed(ItemRegister.register("scp4666_cellar_door",
            () -> new DoubleHighBlockItem(CellarDoor.get(), new Item.Properties())));

    public static final DeferredBlock<Scp4666FrozenGatewayBlock> FrozenGateway = BlockRegister.register("scp4666_frozen_gateway",
            () -> new Scp4666FrozenGatewayBlock(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).requiresCorrectToolForDrops().strength(6.0f, 1200.0f).sound(SoundType.GLASS)
                    .noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false).isSuffocating((State, World, Pos) -> false)
                    .isViewBlocking((State, World, Pos) -> false).lightLevel(State -> 2)));
    public static final DeferredItem<BlockItem> FrozenGatewayItem = ItemRegister.registerSimpleBlockItem("scp4666_frozen_gateway", FrozenGateway, new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredBlock<Scp4666BoxBlock> Box = BlockRegister.register("scp4666_morning_box",
            () -> new Scp4666BoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000.0f).sound(SoundType.WOOD).noOcclusion().noLootTable()));
    public static final DeferredBlock<Scp4666PresentBlock> Present = BlockRegister.register("scp4666_present",
            () -> new Scp4666PresentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(-1.0f, 3600000.0f).sound(SoundType.WOOL).noOcclusion().noLootTable()));
    public static final DeferredBlock<Scp4666StockingBlock> Stocking = BlockRegister.register("scp4666_stocking",
            () -> new Scp4666StockingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(-1.0f, 3600000.0f).sound(SoundType.WOOL).noCollission().noLootTable()));
    public static final DeferredBlock<Scp4666ExitDoorBlock> ExitDoor = BlockRegister.register("scp4666_exit_door",
            () -> new Scp4666ExitDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000.0f).sound(SoundType.WOOD).noOcclusion().noLootTable()
                    .lightLevel(State -> 7)));
    public static final DeferredBlock<Scp4666FootprintsBlock> Footprints = BlockRegister.register("scp4666_footprints",
            () -> new Scp4666FootprintsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.1f).sound(SoundType.SNOW).noOcclusion().noLootTable().replaceable()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp4666BoxBlockEntity>> BoxEntity = BlockEntityTypeRegister.register("scp4666_morning_box",
            () -> BlockEntityType.Builder.of(Scp4666BoxBlockEntity::new, Box.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp4666OwnedBlockEntity>> OwnedEntity = BlockEntityTypeRegister.register("scp4666_owned",
            () -> BlockEntityType.Builder.of(Scp4666OwnedBlockEntity::new, Present.get(), Stocking.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp4666CoatHooksBlockEntity>> CoatHooksEntity = BlockEntityTypeRegister.register("scp4666_coat_hooks",
            () -> BlockEntityType.Builder.of(Scp4666CoatHooksBlockEntity::new, CoatHooks.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp4666GramophoneBlockEntity>> GramophoneEntity = BlockEntityTypeRegister.register("scp4666_gramophone",
            () -> BlockEntityType.Builder.of(Scp4666GramophoneBlockEntity::new, Gramophone.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp4666YuleMan>> YuleMan = EntityTypeRegister.register("scp4666_yule_man",
            () -> EntityType.Builder.<Scp4666YuleMan>of(Scp4666YuleMan::new, MobCategory.MONSTER)
                    .sized(Scp4666YuleMan.Width, Scp4666YuleMan.Height)
                    .eyeHeight(Scp4666YuleMan.EyeHeight)
                    .clientTrackingRange(12)
                    .fireImmune()
                    .build("scp4666_yule_man"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp4666DoorNote>> DoorNote = EntityTypeRegister.register("scp4666_door_note",
            () -> EntityType.Builder.<Scp4666DoorNote>of(Scp4666DoorNote::new, MobCategory.MISC).sized(0.4f, 0.5f).clientTrackingRange(8).updateInterval(10)
                    .fireImmune().build("scp4666_door_note"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp4666Sighting>> Sighting = EntityTypeRegister.register("scp4666_sighting",
            () -> EntityType.Builder.<Scp4666Sighting>of(Scp4666Sighting::new, MobCategory.MISC).sized(0.8f, 2.8f).clientTrackingRange(10).updateInterval(2)
                    .noSave().noSummon().fireImmune().build("scp4666_sighting"));
    public static final DeferredItem<Scp4666NoteItem> SaintNote = ItemRegister.register("scp4666_saint_note",
            () -> new Scp4666NoteItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Scp4666CocoaItem> Cocoa = ItemRegister.register("scp4666_hot_cocoa",
            () -> new Scp4666CocoaItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().build())));
    public static final DeferredItem<Scp4666DollItem> Doll = ItemRegister.register("scp4666_flesh_doll",
            () -> new Scp4666DollItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Scp4666NecklaceItem> Necklace = ItemRegister.register("scp4666_tooth_necklace",
            () -> new Scp4666NecklaceItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Scp4666CaneItem> Cane = ItemRegister.register("scp4666_bone_cane",
            () -> new Scp4666CaneItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Scp4666ChoreItem> Firewood = ItemRegister.register("scp4666_firewood",
            () -> new Scp4666ChoreItem(new Item.Properties().stacksTo(16), "scp4666_firewood", false));
    public static final DeferredItem<Scp4666ChoreItem> Scuttle = ItemRegister.register("scp4666_coal_scuttle",
            () -> new Scp4666ChoreItem(new Item.Properties().stacksTo(1), "scp4666_coal_scuttle", true));
    public static final DeferredItem<Scp4666ChoreItem> Boards = ItemRegister.register("scp4666_boards",
            () -> new Scp4666ChoreItem(new Item.Properties().stacksTo(16), "scp4666_boards", false));
    public static final DeferredItem<Scp4666ChoreItem> Gift = ItemRegister.register("scp4666_wrapped_gift",
            () -> new Scp4666ChoreItem(new Item.Properties().stacksTo(1), "scp4666_wrapped_gift", false));
    public static final DeferredItem<Scp4666ChoreItem> Oats = ItemRegister.register("scp4666_oats",
            () -> new Scp4666ChoreItem(new Item.Properties().stacksTo(16), "scp4666_oats", false));
    public static final DeferredItem<Scp4666ChoreItem> OatBread = ItemRegister.register("scp4666_oat_bread",
            () -> new Scp4666ChoreItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build()), "scp4666_oat_bread", false));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WinterMaterial = MaterialRegister.register("scp4666_winter", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type Kind : ArmorItem.Type.values()) {
            Defense.put(Kind, 0);
        }
        return new ArmorMaterial(Defense, 0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(location("scp4666_winter"))), 0.0f, 0.0f);
    });
    public static final DeferredItem<Scp4666WinterGearItem> WinterHat = ItemRegister.register("scp4666_winter_hat",
            () -> new Scp4666WinterGearItem(WinterMaterial, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).durability(HatWear), HatShare, "scp4666_winter_hat"));
    public static final DeferredItem<Scp4666WinterGearItem> WinterCoat = ItemRegister.register("scp4666_winter_coat",
            () -> new Scp4666WinterGearItem(WinterMaterial, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1).durability(CoatWear), CoatShare, "scp4666_winter_coat"));
    public static final DeferredItem<Scp4666WinterGearItem> WinterBoots = ItemRegister.register("scp4666_winter_boots",
            () -> new Scp4666WinterGearItem(WinterMaterial, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1).durability(BootsWear), BootsShare, "scp4666_winter_boots"));
    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp4666_spawn_egg",
            () -> new DeferredSpawnEggItem(YuleMan, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<SoundEvent, SoundEvent> WindLoop = sound("scp4666_wind_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> WindGust = sound("scp4666_wind_gust");
    public static final DeferredHolder<SoundEvent, SoundEvent> WindHowl = sound("scp4666_wind_howl");
    public static final DeferredHolder<SoundEvent, SoundEvent> Creak = sound("scp4666_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> CreakLow = sound("scp4666_creak_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoofStep = sound("scp4666_roof_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoofSnow = sound("scp4666_roof_snow");
    public static final DeferredHolder<SoundEvent, SoundEvent> Knock = sound("scp4666_knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> KnockHeavy = sound("scp4666_knock_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp4666_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreathLow = sound("scp4666_breath_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chime = sound("scp4666_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChimeBell = sound("scp4666_chime_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whistle = sound("scp4666_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CarolNote = sound("scp4666_carol_note");
    public static final DeferredHolder<SoundEvent, SoundEvent> CarolBell = sound("scp4666_carol_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crackle = sound("scp4666_gramophone_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp4666_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> YuleStep = sound("scp4666_yule_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> YuleRustle = sound("scp4666_yule_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> YuleFlee = sound("scp4666_yule_flee");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thud = sound("scp4666_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThudHeavy = sound("scp4666_thud_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sack = sound("scp4666_sack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drag = sound("scp4666_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorCreak = sound("scp4666_door_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> CellarOpen = sound("scp4666_cellar_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CellarClose = sound("scp4666_cellar_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> GlassBreath = sound("scp4666_glass_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> KettleBoil = sound("scp4666_kettle_boil");
    public static final DeferredHolder<SoundEvent, SoundEvent> KettleWhistle = sound("scp4666_kettle_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drink = sound("scp4666_cocoa_drink");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxSet = sound("scp4666_box_set");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwitchClick = sound("scp4666_switch_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> LightsDie = sound("scp4666_lights_die");
    public static final DeferredHolder<SoundEvent, SoundEvent> Twinkle = sound("scp4666_twinkle");
    public static final DeferredHolder<SoundEvent, SoundEvent> FireCrackle = sound("scp4666_fire_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PresentOpen = sound("scp4666_present_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> PuzzlePiece = sound("scp4666_puzzle_piece");
    public static final DeferredHolder<SoundEvent, SoundEvent> Page = sound("scp4666_page");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExitOpen = sound("scp4666_exit_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wake = sound("scp4666_wake");
    public static final DeferredHolder<SoundEvent, SoundEvent> Scratch = sound("scp4666_scratch");
    public static final DeferredHolder<SoundEvent, SoundEvent> YuleStepWood = sound("scp4666_yule_step_wood");
    public static final DeferredHolder<SoundEvent, SoundEvent> Laugh = sound("scp4666_laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Giggle = sound("scp4666_giggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp4666_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sniff = sound("scp4666_sniff");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhistleTannenbaum = sound("scp4666_whistle_tannenbaum");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhistleDeck = sound("scp4666_whistle_deck");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhistleJingle = sound("scp4666_whistle_jingle");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhistleKettleReply = sound("scp4666_whistle_kettle_reply");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumTannenbaum = sound("scp4666_hum_tannenbaum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumDeck = sound("scp4666_hum_deck");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumJingle = sound("scp4666_hum_jingle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LaughDeep = sound("scp4666_laugh_deep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chop = sound("scp4666_chop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChopSplit = sound("scp4666_chop_split");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoalShovel = sound("scp4666_coal_shovel");
    public static final DeferredHolder<SoundEvent, SoundEvent> Saw = sound("scp4666_saw");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hammer = sound("scp4666_hammer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rummage = sound("scp4666_rummage");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wrapping = sound("scp4666_wrapping");
    public static final DeferredHolder<SoundEvent, SoundEvent> FireFeed = sound("scp4666_fire_feed");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoilerDoor = sound("scp4666_boiler_door");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoilerRumble = sound("scp4666_boiler_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoilerTick = sound("scp4666_boiler_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> GlassCrack = sound("scp4666_glass_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> OvenDoor = sound("scp4666_oven_door");
    public static final DeferredHolder<SoundEvent, SoundEvent> GearTear = sound("scp4666_gear_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> SleighBells = sound("scp4666_sleigh_bells");
    public static final DeferredHolder<SoundEvent, SoundEvent> SleighRattle = sound("scp4666_sleigh_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SightingDive = sound("scp4666_sighting_dive");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhistlePhrases = List.of(WhistleTannenbaum, WhistleDeck, WhistleJingle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumPhrases = List.of(HumTannenbaum, HumDeck, HumJingle);

    private Scp4666Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f, 3.0f).sound(SoundType.WOOD).ignitedByLava();
    }

    private static <T extends Block> DeferredBlock<T> furniture(String Name, Supplier<T> Maker) {
        DeferredBlock<T> Made = BlockRegister.register(Name, Maker);
        Furniture.add(ItemRegister.registerSimpleBlockItem(Name, Made, new Item.Properties()));
        return Made;
    }

    private static <T extends Item> DeferredItem<T> listed(DeferredItem<T> Made) {
        Furniture.add(Made);
        return Made;
    }

    private static DeferredBlock<Scp4666FurnitureBlock> decor(String Name, BlockBehaviour.Properties Properties, Scp4666FurnitureBlock.Kind Kind) {
        return furniture(Name, () -> new Scp4666FurnitureBlock(Properties, Kind));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MaterialRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        GatewayDestinations.register(Scp4666Destination.make());
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp4666Registry::registerRules));
    }

    private static void registerRules() {
        if (DayMinutes == null) {
            DayMinutes = GameRules.register(DayMinutesName, GameRules.Category.UPDATES, GameRules.IntegerValue.create(DefaultDayMinutes));
        }
        if (AudioEvents == null) {
            AudioEvents = GameRules.register(AudioEventsName, GameRules.Category.UPDATES, GameRules.IntegerValue.create(DefaultAudioEvents));
        }
    }

    public static int dayMinutes(@Nullable MinecraftServer Server) {
        return Math.max(MinDayMinutes, Math.min(MaxDayMinutes, rule(Server, DayMinutes, DefaultDayMinutes)));
    }

    public static float audioShare(@Nullable MinecraftServer Server) {
        return Math.max(0, Math.min(MaxAudioEvents, rule(Server, AudioEvents, DefaultAudioEvents))) / 100.0f;
    }

    private static int rule(@Nullable MinecraftServer Server, @Nullable GameRules.Key<GameRules.IntegerValue> Key, int Fallback) {
        if (Server == null || Key == null) {
            return Fallback;
        }
        GameRules.IntegerValue Value = Server.getGameRules().getRule(Key);
        return Value == null ? Fallback : Value.get();
    }
}
