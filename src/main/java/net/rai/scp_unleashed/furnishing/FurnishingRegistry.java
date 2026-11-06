package net.rai.scp_unleashed.furnishing;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
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

public final class FurnishingRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);

    private static final List<DeferredItem<? extends Item>> CreativeOrder = new ArrayList<>();

    public static final DeferredBlock<CouchBlock> CouchGrey = block("couch_grey", () -> new CouchBlock(fabric(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<CouchBlock> CouchBlue = block("couch_blue", () -> new CouchBlock(fabric(MapColor.COLOR_BLUE)));
    public static final DeferredBlock<CouchBlock> CouchLeather = block("couch_leather", () -> new CouchBlock(fabric(MapColor.TERRACOTTA_RED)));
    public static final DeferredBlock<TableBlock> ConferenceTable = block("conference_table", () -> new TableBlock(TableKind.Conference, wood(MapColor.COLOR_BROWN, 2.0f)));
    public static final DeferredBlock<TableBlock> CafeteriaTable = block("cafeteria_table", () -> new TableBlock(TableKind.Cafeteria, metal(MapColor.TERRACOTTA_WHITE, 2.0f)));
    public static final DeferredBlock<SeatBlock> CafeteriaChair = block("cafeteria_chair", () -> new SeatBlock(FurnishingKind.CafeteriaChair, metal(MapColor.COLOR_LIGHT_BLUE, 1.0f)));
    public static final DeferredBlock<SeatBlock> LabStool = block("lab_stool", () -> new SeatBlock(FurnishingKind.LabStool, metal(MapColor.COLOR_BLACK, 1.0f)));

    public static final DeferredBlock<PaperStackBlock> PaperStack = block("paper_stack", () -> new PaperStackBlock(prop(MapColor.SNOW, 0.3f, SoundType.WOOL)
            .pushReaction(PushReaction.DESTROY).ignitedByLava()));
    public static final DeferredBlock<FurnishingBlock> ArchiveBoxes = block("archive_boxes", () -> new FurnishingBlock(FurnishingKind.ArchiveBoxes, wood(MapColor.COLOR_BROWN, 0.8f)));
    public static final DeferredBlock<SwitchBlock> DeskLamp = block("desk_lamp", () -> new SwitchBlock(FurnishingKind.DeskLamp, metal(MapColor.COLOR_GREEN, 0.8f)
            .pushReaction(PushReaction.DESTROY).lightLevel(State -> State.getValue(SwitchBlock.Lit) ? 10 : 0)));
    public static final DeferredBlock<WhiteboardBlock> Whiteboard = block("whiteboard", () -> new WhiteboardBlock(metal(MapColor.SNOW, 1.5f).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<TallFurnishingBlock> CoatRack = block("coat_rack", () -> new TallFurnishingBlock(TallKind.CoatRack, wood(MapColor.COLOR_BROWN, 1.0f)
            .pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<FurnishingBlock> PottedPlant = block("potted_plant", () -> new FurnishingBlock(FurnishingKind.PottedPlant, prop(MapColor.PLANT, 0.5f,
            SoundType.DECORATED_POT).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<FurnishingBlock> TrashCan = block("trash_can", () -> new FurnishingBlock(FurnishingKind.TrashCan, metal(MapColor.COLOR_GRAY, 0.8f)
            .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<FurnishingBlock> MopBucket = block("mop_bucket", () -> new FurnishingBlock(FurnishingKind.MopBucket, metal(MapColor.COLOR_YELLOW, 1.0f)));

    public static final DeferredBlock<CoffeeMachineBlock> CoffeeMachine = block("coffee_machine", () -> new CoffeeMachineBlock(metal(MapColor.COLOR_BLACK, 1.0f)));
    public static final DeferredBlock<MicrowaveBlock> Microwave = block("microwave", () -> new MicrowaveBlock(metal(MapColor.SNOW, 1.5f)
            .lightLevel(State -> State.getValue(MicrowaveBlock.Lit) ? 6 : 0)));
    public static final DeferredBlock<FridgeBlock> MiniFridge = block("mini_fridge", () -> new FridgeBlock(metal(MapColor.SNOW, 2.0f)));

    public static final DeferredBlock<FurnishingBlock> ChemicalShelf = block("chemical_shelf", () -> new FurnishingBlock(FurnishingKind.ChemicalShelf, metal(MapColor.COLOR_LIGHT_GRAY, 1.5f)));
    public static final DeferredBlock<FurnishingBlock> SpecimenShelf = block("specimen_shelf", () -> new FurnishingBlock(FurnishingKind.SpecimenShelf, metal(MapColor.COLOR_LIGHT_GRAY, 1.5f)));
    public static final DeferredBlock<FurnishingBlock> BinderShelf = block("binder_shelf", () -> new FurnishingBlock(FurnishingKind.BinderShelf, metal(MapColor.COLOR_LIGHT_GRAY, 1.5f)));
    public static final DeferredBlock<FurnishingBlock> SteelShelf = block("steel_shelf", () -> new FurnishingBlock(FurnishingKind.SteelShelf, metal(MapColor.COLOR_LIGHT_GRAY, 1.5f)));

    public static final DeferredBlock<HospitalBedBlock> HospitalBed = BlockRegister.register("hospital_bed", () -> new HospitalBedBlock(metal(MapColor.SNOW, 1.0f)
            .pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<BedItem> HospitalBedItem = item("hospital_bed", () -> new BedItem(HospitalBed.get(), new Item.Properties()));
    public static final DeferredBlock<ExamTableBlock> ExaminationTable = block("examination_table", () -> new ExamTableBlock(metal(MapColor.COLOR_CYAN, 1.5f)
            .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<SeatBlock> Wheelchair = block("wheelchair", () -> new SeatBlock(FurnishingKind.Wheelchair, metal(MapColor.COLOR_BLACK, 1.0f)));
    public static final DeferredBlock<TallFurnishingBlock> IvStand = block("iv_stand", () -> new TallFurnishingBlock(TallKind.IvStand, metal(MapColor.COLOR_LIGHT_GRAY, 1.0f)
            .pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<SwitchBlock> VitalsMonitor = block("vitals_monitor", () -> new SwitchBlock(FurnishingKind.VitalsMonitor, metal(MapColor.COLOR_BLACK, 1.0f)
            .lightLevel(State -> State.getValue(SwitchBlock.Lit) ? 4 : 0)));
    public static final DeferredBlock<FurnishingBlock> MedicalCart = block("medical_cart", () -> new FurnishingBlock(FurnishingKind.MedicalCart, metal(MapColor.COLOR_RED, 1.5f)));
    public static final DeferredBlock<TallFurnishingBlock> PrivacyScreen = block("privacy_screen", () -> new TallFurnishingBlock(TallKind.PrivacyScreen, prop(MapColor.COLOR_LIGHT_GREEN, 1.0f,
            SoundType.WOOL).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<FurnishingBlock> MedicalSupplyShelf = block("medical_supply_shelf", () -> new FurnishingBlock(FurnishingKind.MedicalSupplyShelf,
            metal(MapColor.SNOW, 1.5f)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FridgeBlockEntity>> FridgeEntity = BlockEntityTypeRegister.register("furnishing_mini_fridge",
            () -> BlockEntityType.Builder.of(FridgeBlockEntity::new, MiniFridge.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<FurnishingSeat>> Seat = EntityTypeRegister.register("furnishing_seat",
            () -> EntityType.Builder.<FurnishingSeat>of(FurnishingSeat::new, MobCategory.MISC).sized(0.001f, 0.001f).noSummon().fireImmune().clientTrackingRange(10)
                    .build("furnishing_seat"));

    private FurnishingRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        FurnishingSounds.SoundEventRegister.register(ModEventBus);
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

    private static BlockBehaviour.Properties fabric(MapColor Colour) {
        return prop(Colour, 1.0f, SoundType.WOOL).ignitedByLava();
    }
}
