package net.rai.scp_unleashed.scp4845;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.scp4845.block.Scp4845ApplianceBlock;
import net.rai.scp_unleashed.scp4845.block.Scp4845CounterBlock;
import net.rai.scp_unleashed.scp4845.block.Scp4845EntranceBlock;
import net.rai.scp_unleashed.scp4845.block.Scp4845FacadeBlock;
import net.rai.scp_unleashed.scp4845.block.Scp4845FridgeDoorBlock;
import net.rai.scp_unleashed.scp4845.block.Scp4845FurnitureBlock;
import net.rai.scp_unleashed.scp4845.block.Scp4845MarkingBlock;
import net.rai.scp_unleashed.scp4845.block.Scp4845MenuBoardBlock;
import net.rai.scp_unleashed.scp4845.block.Scp4845SealedBlock;
import net.rai.scp_unleashed.scp4845.block.Scp4845Shapes;
import net.rai.scp_unleashed.scp4845.block.Scp4845SignBlock;
import net.rai.scp_unleashed.scp4845.item.Scp4845FoodItem;
import net.rai.scp_unleashed.scp4845.item.Scp4845MenuItem;
import net.rai.scp_unleashed.scp4845.order.Scp4845OrderMenu;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Employee;
import net.rai.scp_unleashed.space.SpaceRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class Scp4845Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final List<DeferredItem<? extends Item>> Placeables = new ArrayList<>();
    private static final Map<Scp4845MenuItem, DeferredItem<Scp4845FoodItem>> Foods = new EnumMap<>(Scp4845MenuItem.class);

    public static final ResourceLocation DocumentId = location("scp_4845");
    public static final String EmployeeId = "scp4845_employee";
    public static final float FurnitureHardness = 2.0f;
    public static final float FurnitureResistance = 6.0f;
    public static final int SignLight = 9;
    public static final int BoardLight = 7;
    public static final int ScreenLight = 4;
    public static final int FountainLight = 6;
    public static final double SeatTop = 8.0;
    public static final int GrillLight = 3;
    public static final int TrackingRange = 10;

    public static final DeferredBlock<Scp4845FacadeBlock> Facade = placeable("scp4845_facade",
            () -> new Scp4845FacadeBlock(Scp4845SealedBlock.sealed(MapColor.COLOR_PURPLE).sound(SoundType.STONE).lightLevel(Scp4845FacadeBlock::light)));
    public static final DeferredBlock<Scp4845MarkingBlock> LotMarking = placeable("scp4845_lot_marking",
            () -> new Scp4845MarkingBlock(Scp4845SealedBlock.sealed(MapColor.STONE).sound(SoundType.GRAVEL)));
    public static final DeferredBlock<Scp4845SignBlock> Sign = placeable("scp4845_sign",
            () -> new Scp4845SignBlock(Scp4845SealedBlock.sealed(MapColor.COLOR_PURPLE).sound(SoundType.METAL).noOcclusion().lightLevel(State -> SignLight)));
    public static final DeferredBlock<Scp4845MenuBoardBlock> MenuBoard = placeable("scp4845_menu_board",
            () -> new Scp4845MenuBoardBlock(Scp4845SealedBlock.sealed(MapColor.COLOR_BLACK).sound(SoundType.METAL).noOcclusion().lightLevel(State -> BoardLight)));
    public static final DeferredBlock<Scp4845EntranceBlock> Entrance = placeable("scp4845_entrance",
            () -> new Scp4845EntranceBlock(Scp4845SealedBlock.sealed(MapColor.COLOR_PURPLE).sound(SoundType.GLASS).noOcclusion()));

    public static final DeferredBlock<Scp4845CounterBlock> Counter = placeable("scp4845_counter",
            () -> new Scp4845CounterBlock(furniture(MapColor.COLOR_PURPLE, SoundType.WOOD), Scp4845Shapes.Counter));
    public static final DeferredBlock<Scp4845CounterBlock> Register = placeable("scp4845_register",
            () -> new Scp4845CounterBlock(furniture(MapColor.COLOR_PURPLE, SoundType.WOOD).lightLevel(State -> ScreenLight), Scp4845Shapes.Register));
    public static final DeferredBlock<Scp4845FurnitureBlock> SodaFountain = placeable("scp4845_soda_fountain",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.METAL, SoundType.METAL).lightLevel(State -> FountainLight), Scp4845Shapes.SodaFountain));
    public static final DeferredBlock<Scp4845FurnitureBlock> Condiments = placeable("scp4845_condiment_station",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.COLOR_PURPLE, SoundType.WOOD), Scp4845Shapes.Condiments));
    public static final DeferredBlock<Scp4845FurnitureBlock> Booth = placeable("scp4845_booth",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.COLOR_PURPLE, SoundType.WOOL), Scp4845Shapes.Booth, SeatTop));
    public static final DeferredBlock<Scp4845FurnitureBlock> Table = placeable("scp4845_table",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.SNOW, SoundType.WOOD), Scp4845Shapes.Table));
    public static final DeferredBlock<Scp4845FurnitureBlock> Chair = placeable("scp4845_chair",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.COLOR_MAGENTA, SoundType.METAL), Scp4845Shapes.Chair, SeatTop));
    public static final DeferredBlock<Scp4845ApplianceBlock> Grill = placeable("scp4845_grill",
            () -> new Scp4845ApplianceBlock(furniture(MapColor.METAL, SoundType.METAL).lightLevel(State -> GrillLight), Scp4845Shapes.Grill, Scp4845ApplianceBlock.Kind.Grill));
    public static final DeferredBlock<Scp4845ApplianceBlock> Fryer = placeable("scp4845_fryer",
            () -> new Scp4845ApplianceBlock(furniture(MapColor.METAL, SoundType.METAL), Scp4845Shapes.Fryer, Scp4845ApplianceBlock.Kind.Fryer));
    public static final DeferredBlock<Scp4845FurnitureBlock> PrepTable = placeable("scp4845_prep_table",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.METAL, SoundType.METAL), Scp4845Shapes.PrepTable));
    public static final DeferredBlock<Scp4845FurnitureBlock> Sink = placeable("scp4845_sink",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.METAL, SoundType.METAL), Scp4845Shapes.Sink));
    public static final DeferredBlock<Scp4845FurnitureBlock> MopBucket = placeable("scp4845_mop_bucket",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.COLOR_YELLOW, SoundType.WOOD), Scp4845Shapes.MopBucket));
    public static final DeferredBlock<Scp4845FridgeDoorBlock> FridgeDoor = placeable("scp4845_fridge_door",
            () -> new Scp4845FridgeDoorBlock(furniture(MapColor.METAL, SoundType.METAL)));
    public static final DeferredBlock<Scp4845FurnitureBlock> Shelf = placeable("scp4845_shelf",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.METAL, SoundType.METAL), Scp4845Shapes.Shelf));
    public static final DeferredBlock<Scp4845FurnitureBlock> OfficeDesk = placeable("scp4845_office_desk",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.WOOD, SoundType.WOOD).lightLevel(State -> ScreenLight), Scp4845Shapes.Desk));
    public static final DeferredBlock<Scp4845FurnitureBlock> OfficeChair = placeable("scp4845_office_chair",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.COLOR_BLACK, SoundType.WOOL), Scp4845Shapes.OfficeChair, SeatTop));
    public static final DeferredBlock<Scp4845FurnitureBlock> TrashCan = placeable("scp4845_trash_can",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.COLOR_PURPLE, SoundType.WOOD), Scp4845Shapes.TrashCan));
    public static final DeferredBlock<Scp4845FurnitureBlock> DriveWindow = placeable("scp4845_drive_thru_window",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.COLOR_PURPLE, SoundType.GLASS), Scp4845Shapes.DriveWindow));
    public static final DeferredBlock<Scp4845FurnitureBlock> Speaker = placeable("scp4845_drive_thru_speaker",
            () -> new Scp4845FurnitureBlock(furniture(MapColor.COLOR_PURPLE, SoundType.METAL).lightLevel(State -> FountainLight), Scp4845Shapes.Speaker));

    static {
        for (Scp4845MenuItem Entry : Scp4845MenuItem.values()) {
            Foods.put(Entry, ItemRegister.register(Entry.Id, () -> new Scp4845FoodItem(Entry, new Item.Properties().stacksTo(Scp4845MenuItem.StackSize).food(Entry.food()))));
        }
    }

    public static final DeferredHolder<EntityType<?>, EntityType<Scp4845Employee>> Employee = EntityTypeRegister.register(EmployeeId,
            () -> EntityType.Builder.<Scp4845Employee>of(Scp4845Employee::new, MobCategory.MISC)
                    .sized(Scp4845Employee.Width, Scp4845Employee.Height)
                    .eyeHeight(Scp4845Employee.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(TrackingRange)
                    .build(EmployeeId));
    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp4845_employee_spawn_egg",
            () -> new DeferredSpawnEggItem(Employee, 0x6C4A8A, 0xB0568A, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<MenuType<?>, MenuType<Scp4845OrderMenu>> OrderMenuType = MenuRegister.register("scp4845_order",
            () -> IMenuTypeExtension.create(Scp4845OrderMenu::new));

    public static final DeferredHolder<SoundEvent, SoundEvent> Sizzle = sound("scp4845_grill_sizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SizzleCrackle = sound("scp4845_grill_sizzle_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bubble = sound("scp4845_fryer_bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> BubblePop = sound("scp4845_fryer_bubble_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Beep = sound("scp4845_register_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> BeepClick = sound("scp4845_register_beep_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drawer = sound("scp4845_register_drawer");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerCoins = sound("scp4845_register_drawer_coins");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pour = sound("scp4845_soda_pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> PourFizz = sound("scp4845_soda_pour_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> Squeak = sound("scp4845_mop_squeak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slosh = sound("scp4845_mop_slosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Keyboard = sound("scp4845_keyboard");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeyboardClack = sound("scp4845_keyboard_clack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp4845_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumLow = sound("scp4845_hum_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bag = sound("scp4845_bag");
    public static final DeferredHolder<SoundEvent, SoundEvent> BagRustle = sound("scp4845_bag_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chime = sound("scp4845_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChimeBell = sound("scp4845_chime_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crunch = sound("scp4845_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrunchBite = sound("scp4845_crunch_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fizz = sound("scp4845_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> FizzBubble = sound("scp4845_fizz_bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wash = sound("scp4845_wash");
    public static final DeferredHolder<SoundEvent, SoundEvent> WashClink = sound("scp4845_wash_clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> Restock = sound("scp4845_restock");
    public static final DeferredHolder<SoundEvent, SoundEvent> RestockBox = sound("scp4845_restock_box");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sigh = sound("scp4845_sigh");
    public static final DeferredHolder<SoundEvent, SoundEvent> SighCreak = sound("scp4845_sigh_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicPling = sound("scp4845_music_pling");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicHarp = sound("scp4845_music_harp");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicBass = sound("scp4845_music_bass");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicBell = sound("scp4845_music_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicFlute = sound("scp4845_music_flute");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicHat = sound("scp4845_music_hat");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicSnare = sound("scp4845_music_snare");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicKick = sound("scp4845_music_kick");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SizzleLayers = List.of(Sizzle, SizzleCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BubbleLayers = List.of(Bubble, BubblePop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BeepLayers = List.of(Beep, BeepClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DrawerLayers = List.of(Drawer, DrawerCoins);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PourLayers = List.of(Pour, PourFizz);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MopLayers = List.of(Squeak, Slosh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> KeyboardLayers = List.of(Keyboard, KeyboardClack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BagLayers = List.of(Bag, BagRustle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChimeLayers = List.of(Chime, ChimeBell);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrunchLayers = List.of(Crunch, CrunchBite);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FizzLayers = List.of(Fizz, FizzBubble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WashLayers = List.of(Wash, WashClink);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RestockLayers = List.of(Restock, RestockBox);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SighLayers = List.of(Sigh, SighCreak);

    private Scp4845Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static BlockBehaviour.Properties furniture(MapColor Colour, SoundType Sound) {
        return BlockBehaviour.Properties.of().mapColor(Colour).strength(FurnitureHardness, FurnitureResistance).sound(Sound).noOcclusion().pushReaction(PushReaction.BLOCK)
                .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false)
                .isViewBlocking((State, World, Pos) -> false);
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

    public static Scp4845FoodItem food(Scp4845MenuItem Entry) {
        return Foods.get(Entry).get();
    }

    public static void playLayers(Level World, @Nullable Player Except, double X, double Y, double Z, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source,
                                  float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(Except, X, Y, Z, Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        StartConditions.register(Scp4845SiteCondition.TypeName, Scp4845SiteCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp4845Restaurant::spawn);
        AnomalyRealm.exempt(DocumentId, SpaceRegistry.MoonLevel);
        Scp4845Air.register();
    }
}
