package net.rai.scp_unleashed.props;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class PropsRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuTypeRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeType<?>> RecipeTypeRegister = DeferredRegister.create(Registries.RECIPE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);

    private static final List<DeferredItem<? extends Item>> CreativeOrder = new ArrayList<>();

    public static final DeferredBlock<DecorationWorkbenchBlock> DecorationWorkbench = block("prop_workbench", () -> new DecorationWorkbenchBlock(wood(MapColor.WOOD, 2.5f)));
    public static final DeferredItem<DecorativeMaterialItem> DecorativeMaterial = item("decorative_material", () -> new DecorativeMaterialItem(new Item.Properties()));
    public static final DeferredItem<DecorativeMaterialItem> DecorativeConcrete = item("decorative_concrete", () -> new DecorativeMaterialItem(new Item.Properties()));

    public static final DeferredBlock<LockerBlock> Locker = block("locker", () -> new LockerBlock(metal(MapColor.COLOR_GRAY, 3.0f).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<FilingCabinetBlock> FilingCabinet = block("filing_cabinet", () -> new FilingCabinetBlock(metal(MapColor.COLOR_LIGHT_GRAY, 2.5f)));
    public static final DeferredBlock<SupplyCrateBlock> SupplyCrate = block("supply_crate", () -> new SupplyCrateBlock(wood(MapColor.WOOD, 2.5f)));
    public static final DeferredBlock<MedicalCabinetBlock> MedicalCabinet = block("medical_cabinet", () -> new MedicalCabinetBlock(metal(MapColor.SNOW, 2.0f).pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<DeskBlock> OfficeDesk = block("office_desk", () -> new DeskBlock(DeskKind.OfficeDesk, wood(MapColor.COLOR_BROWN, 2.0f)));
    public static final DeferredBlock<CornerDeskBlock> CornerDesk = block("corner_desk", () -> new CornerDeskBlock(wood(MapColor.COLOR_BROWN, 2.0f)));
    public static final DeferredBlock<DeskBlock> MetalDesk = block("metal_desk", () -> new DeskBlock(DeskKind.MetalDesk, metal(MapColor.COLOR_GRAY, 2.5f)));
    public static final DeferredBlock<ReceptionDeskBlock> ReceptionDesk = block("reception_desk", () -> new ReceptionDeskBlock(wood(MapColor.TERRACOTTA_WHITE, 2.0f)));
    public static final DeferredBlock<DeskBlock> LabBenchDesk = block("lab_bench_desk", () -> new DeskBlock(DeskKind.LabBenchDesk, metal(MapColor.SNOW, 2.0f)));
    public static final DeferredBlock<DeskBlock> LabTable = block("lab_table", () -> new DeskBlock(DeskKind.LabTable, metal(MapColor.SNOW, 2.0f)));
    public static final DeferredBlock<OfficeChairBlock> OfficeChair = block("office_chair", () -> new OfficeChairBlock(prop(MapColor.COLOR_BLACK, 1.0f, SoundType.WOOL)));

    public static final DeferredBlock<ScreenBlock> ComputerTerminal = block("computer_terminal", () -> new ScreenBlock(ScreenKind.ComputerTerminal, screen(MapColor.TERRACOTTA_WHITE)));
    public static final DeferredBlock<ScreenBlock> CrtMonitor = block("crt_monitor", () -> new ScreenBlock(ScreenKind.CrtMonitor, screen(MapColor.TERRACOTTA_WHITE)));
    public static final DeferredBlock<ScreenBlock> SecurityMonitor = block("security_monitor", () -> new ScreenBlock(ScreenKind.SecurityMonitor, screen(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<WallMonitorBlock> WallMonitor = block("wall_monitor", () -> new WallMonitorBlock(screen(MapColor.COLOR_BLACK).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<ServerRackBlock> ServerRack = block("server_rack", () -> new ServerRackBlock(metal(MapColor.COLOR_BLACK, 3.0f).pushReaction(PushReaction.BLOCK)));

    public static final DeferredBlock<PropBlock> LabShelf = block("lab_shelf", () -> new PropBlock(PropKind.LabShelf, metal(MapColor.COLOR_LIGHT_GRAY, 1.5f)));
    public static final DeferredBlock<WaterCoolerBlock> WaterCooler = block("water_cooler", () -> new WaterCoolerBlock(metal(MapColor.SNOW, 1.5f).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<PropBlock> ChemicalBarrel = block("chemical_barrel", () -> new PropBlock(PropKind.ChemicalBarrel, metal(MapColor.COLOR_BLUE, 2.0f)));
    public static final DeferredBlock<PropBlock> BiohazardBin = block("biohazard_bin", () -> new PropBlock(PropKind.BiohazardBin, prop(MapColor.COLOR_RED, 1.0f, SoundType.METAL)));
    public static final DeferredBlock<PropBlock> WetFloorSign = block("wet_floor_sign", () -> new PropBlock(PropKind.WetFloorSign, prop(MapColor.COLOR_YELLOW, 0.5f, SoundType.SCAFFOLDING).pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<FireExtinguisherBlock> FireExtinguisher = block("fire_extinguisher", () -> new FireExtinguisherBlock(metal(MapColor.COLOR_RED, 1.0f).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<SecurityCameraBlock> SecurityCamera = block("security_camera", () -> new SecurityCameraBlock(metal(MapColor.COLOR_LIGHT_GRAY, 1.5f).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<FluorescentLightBlock> FluorescentLight = block("fluorescent_light", () -> new FluorescentLightBlock(prop(MapColor.SNOW, 1.0f, SoundType.GLASS)
            .pushReaction(PushReaction.DESTROY).lightLevel(State -> State.getValue(FluorescentLightBlock.Lit) ? FluorescentLightBlock.Brightness : 0)));
    public static final DeferredBlock<EmergencyLightBlock> EmergencyLight = block("emergency_light", () -> new EmergencyLightBlock(prop(MapColor.COLOR_RED, 1.0f, SoundType.GLASS)
            .pushReaction(PushReaction.DESTROY).lightLevel(State -> State.getValue(EmergencyLightBlock.Powered) ? EmergencyLightBlock.Brightness : 0)));
    public static final DeferredBlock<WallClockBlock> WallClock = block("wall_clock", () -> new WallClockBlock(prop(MapColor.COLOR_BLACK, 0.8f, SoundType.METAL).pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PropStorageBlockEntity>> Storage = BlockEntityTypeRegister.register("props_storage",
            () -> BlockEntityType.Builder.of(PropStorageBlockEntity::new, Locker.get(), FilingCabinet.get(), SupplyCrate.get(), MedicalCabinet.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SecurityCameraBlockEntity>> SecurityCameraEntity = BlockEntityTypeRegister.register("props_security_camera",
            () -> BlockEntityType.Builder.of(SecurityCameraBlockEntity::new, SecurityCamera.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WallClockBlockEntity>> WallClockEntity = BlockEntityTypeRegister.register("props_wall_clock",
            () -> BlockEntityType.Builder.of(WallClockBlockEntity::new, WallClock.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<PropSeatEntity>> Seat = EntityTypeRegister.register("props_seat",
            () -> EntityType.Builder.<PropSeatEntity>of(PropSeatEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).noSave().noSummon().fireImmune()
                    .clientTrackingRange(10).updateInterval(20).build("props_seat"));

    public static final DeferredHolder<MenuType<?>, MenuType<DecorationWorkbenchMenu>> DecorationWorkbenchMenuType = MenuTypeRegister.register("decoration_workbench",
            () -> new MenuType<>(DecorationWorkbenchMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>, RecipeType<DecorationRecipe>> DecorationType = RecipeTypeRegister.register("decoration",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "decoration")));
    public static final DeferredHolder<RecipeSerializer<?>, DecorationRecipe.Serializer> DecorationSerializer = RecipeSerializerRegister.register("decoration",
            DecorationRecipe.Serializer::new);

    private PropsRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        MenuTypeRegister.register(ModEventBus);
        RecipeTypeRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        PropsSounds.SoundEventRegister.register(ModEventBus);
    }

    public static List<DeferredItem<? extends Item>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static <B extends Block> DeferredBlock<B> block(String Name, Supplier<B> Factory) {
        DeferredBlock<B> Registered = BlockRegister.register(Name, Factory);
        CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name, Registered));
        return Registered;
    }

    private static <I extends Item> DeferredItem<I> item(String Name, Supplier<I> Factory) {
        DeferredItem<I> Registered = ItemRegister.register(Name, Factory);
        CreativeOrder.add(Registered);
        return Registered;
    }

    private static BlockBehaviour.Properties prop(MapColor Colour, float Hardness, SoundType Sound) {
        return BlockBehaviour.Properties.of().mapColor(Colour).strength(Hardness, Hardness * 2.0f).sound(Sound).noOcclusion();
    }

    private static BlockBehaviour.Properties metal(MapColor Colour, float Hardness) {
        return prop(Colour, Hardness, SoundType.METAL);
    }

    private static BlockBehaviour.Properties wood(MapColor Colour, float Hardness) {
        return prop(Colour, Hardness, SoundType.WOOD).ignitedByLava();
    }

    private static BlockBehaviour.Properties screen(MapColor Colour) {
        return prop(Colour, 1.5f, SoundType.METAL).lightLevel(State -> State.getValue(ScreenBlock.Lit) ? ScreenBlock.Glow : 0);
    }
}
