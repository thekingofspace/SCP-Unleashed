package net.rai.scp_unleashed.oil;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.materials.MaterialItem;

import java.util.List;

public final class OilRegistry {
    public static final ResourceLocation DistillingId = id("distilling");
    public static final ResourceLocation ChemicalProcessingId = id("chemical_processing");
    public static final ResourceLocation OilRefiningId = id("oil_refining");
    public static final ResourceLocation CombustionFuelId = id("combustion_fuel");

    public static final DeferredRegister<FluidType> FluidTypeRegister = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<Fluid> FluidRegister = DeferredRegister.create(Registries.FLUID, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeType<?>> RecipeTypeRegister = DeferredRegister.create(Registries.RECIPE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final OilLiquid Oil = liquid(OilGrade.Crude);
    public static final OilLiquid LightOil = liquid(OilGrade.LightOil);
    public static final OilLiquid HeavyOil = liquid(OilGrade.HeavyOil);
    public static final OilLiquid LightFuel = liquid(OilGrade.LightFuel);
    public static final OilLiquid HeavyFuel = liquid(OilGrade.HeavyFuel);
    public static final List<OilLiquid> Liquids = List.of(Oil, LightOil, HeavyOil, LightFuel, HeavyFuel);

    public static final DeferredItem<MaterialItem> Plastic = ItemRegister.register("plastic", () -> new MaterialItem(new Item.Properties()));

    public static final DeferredBlock<OilExtractorBlock> OilExtractor = BlockRegister.register("oil_extractor", () -> new OilExtractorBlock(machine(MapColor.COLOR_YELLOW)));
    public static final DeferredItem<BlockItem> OilExtractorItem = ItemRegister.register("oil_extractor", () -> new BlockItem(OilExtractor.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredBlock<DistillationTowerBlock> DistillationTower = BlockRegister.register("distillation_tower", () -> new DistillationTowerBlock(machine(MapColor.METAL)
            .lightLevel(DistillationTowerBlock::lightOf)));
    public static final DeferredItem<BlockItem> DistillationTowerItem = ItemRegister.registerSimpleBlockItem("distillation_tower", DistillationTower);
    public static final DeferredBlock<ChemicalProcessorBlock> ChemicalProcessor = BlockRegister.register("chemical_processor", () -> new ChemicalProcessorBlock(machine(MapColor.METAL)
            .lightLevel(ChemicalProcessorBlock::lightOf)));
    public static final DeferredItem<BlockItem> ChemicalProcessorItem = ItemRegister.registerSimpleBlockItem("chemical_processor", ChemicalProcessor);
    public static final DeferredBlock<CombustionGeneratorBlock> CombustionGenerator = BlockRegister.register("combustion_generator", () -> new CombustionGeneratorBlock(machine(MapColor.COLOR_RED)
            .lightLevel(CombustionGeneratorBlock::lightOf)));
    public static final DeferredItem<BlockItem> CombustionGeneratorItem = ItemRegister.register("combustion_generator", () -> new BlockItem(CombustionGenerator.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredBlock<OilRefineryBlock> OilRefinery = BlockRegister.register("oil_refinery", () -> new OilRefineryBlock(machine(MapColor.METAL)
            .lightLevel(OilRefineryBlock::lightOf)));
    public static final DeferredItem<BlockItem> OilRefineryItem = ItemRegister.registerSimpleBlockItem("oil_refinery", OilRefinery);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilExtractorBlockEntity>> OilExtractorEntity = BlockEntityRegister.register("oil_extractor",
            () -> BlockEntityType.Builder.of(OilExtractorBlockEntity::new, OilExtractor.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistillationTowerBlockEntity>> DistillationTowerEntity = BlockEntityRegister.register("distillation_tower",
            () -> BlockEntityType.Builder.of(DistillationTowerBlockEntity::new, DistillationTower.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalProcessorBlockEntity>> ChemicalProcessorEntity = BlockEntityRegister.register("chemical_processor",
            () -> BlockEntityType.Builder.of(ChemicalProcessorBlockEntity::new, ChemicalProcessor.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> CombustionGeneratorEntity = BlockEntityRegister.register("combustion_generator",
            () -> BlockEntityType.Builder.of(CombustionGeneratorBlockEntity::new, CombustionGenerator.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilRefineryBlockEntity>> OilRefineryEntity = BlockEntityRegister.register("oil_refinery",
            () -> BlockEntityType.Builder.of(OilRefineryBlockEntity::new, OilRefinery.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<OilMachineMenu>> ExtractorMenu = menu(OilMachineKind.Extractor);
    public static final DeferredHolder<MenuType<?>, MenuType<OilMachineMenu>> TowerMenu = menu(OilMachineKind.Tower);
    public static final DeferredHolder<MenuType<?>, MenuType<OilMachineMenu>> ProcessorMenu = menu(OilMachineKind.Processor);
    public static final DeferredHolder<MenuType<?>, MenuType<OilMachineMenu>> GeneratorMenu = menu(OilMachineKind.Generator);
    public static final DeferredHolder<MenuType<?>, MenuType<OilMachineMenu>> RefineryMenu = menu(OilMachineKind.Refinery);

    public static final DeferredHolder<RecipeType<?>, RecipeType<DistillingRecipe>> Distilling = RecipeTypeRegister.register("distilling",
            () -> RecipeType.simple(DistillingId));
    public static final DeferredHolder<RecipeSerializer<?>, DistillingRecipe.Serializer> DistillingSerializer = RecipeSerializerRegister.register("distilling",
            DistillingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ChemicalProcessingRecipe>> ChemicalProcessing = RecipeTypeRegister.register("chemical_processing",
            () -> RecipeType.simple(ChemicalProcessingId));
    public static final DeferredHolder<RecipeSerializer<?>, ChemicalProcessingRecipe.Serializer> ChemicalProcessingSerializer = RecipeSerializerRegister.register("chemical_processing",
            ChemicalProcessingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<OilRefiningRecipe>> OilRefining = RecipeTypeRegister.register("oil_refining",
            () -> RecipeType.simple(OilRefiningId));
    public static final DeferredHolder<RecipeSerializer<?>, OilRefiningRecipe.Serializer> OilRefiningSerializer = RecipeSerializerRegister.register("oil_refining",
            OilRefiningRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<CombustionFuel>> CombustionFuelType = RecipeTypeRegister.register("combustion_fuel",
            () -> RecipeType.simple(CombustionFuelId));
    public static final DeferredHolder<RecipeSerializer<?>, CombustionFuel.Serializer> CombustionFuelSerializer = RecipeSerializerRegister.register("combustion_fuel",
            CombustionFuel.Serializer::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> ExtractorThump = sound("oil_extractor_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExtractorClank = sound("oil_extractor_clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExtractorCreak = sound("oil_extractor_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExtractorGlug = sound("oil_extractor_glug");
    public static final DeferredHolder<SoundEvent, SoundEvent> TowerBoil = sound("oil_tower_boil");
    public static final DeferredHolder<SoundEvent, SoundEvent> TowerCrackle = sound("oil_tower_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ProcessorBubble = sound("oil_processor_bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> ProcessorHiss = sound("oil_processor_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> GeneratorEngine = sound("oil_generator_engine");
    public static final DeferredHolder<SoundEvent, SoundEvent> GeneratorKnock = sound("oil_generator_knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> RefineryHiss = sound("oil_refinery_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> RefineryDrop = sound("oil_refinery_drop");

    private OilRegistry() {
    }

    public static ResourceLocation id(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
    }

    private static BlockBehaviour.Properties machine(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
                .pushReaction(PushReaction.BLOCK);
    }

    private static DeferredHolder<MenuType<?>, MenuType<OilMachineMenu>> menu(OilMachineKind Kind) {
        return MenuRegister.register(Kind.getName(), () -> new MenuType<>((ContainerId, PlayerInventory) -> new OilMachineMenu(Kind, ContainerId, PlayerInventory),
                FeatureFlags.DEFAULT_FLAGS));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(id(Name)));
    }

    private static OilLiquid liquid(OilGrade Grade) {
        String Name = Grade.getName();
        OilLiquid Entry = new OilLiquid(Grade);
        DeferredHolder<FluidType, FluidType> Type = FluidTypeRegister.register(Name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid_type.scp_unleashed." + Name)
                .canConvertToSource(false)
                .canExtinguish(false)
                .canHydrate(false)
                .supportsBoating(false)
                .canSwim(true)
                .canDrown(true)
                .fallDistanceModifier(0.0f)
                .motionScale(Grade.getMotion())
                .density(Grade.getDensity())
                .viscosity(Grade.getViscosity())
                .temperature(300)
                .pathType(PathType.STICKY_HONEY)
                .adjacentPathType(null)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
        DeferredHolder<Fluid, BaseFlowingFluid.Source> Source = FluidRegister.register(Name, () -> new OilFluid.Still(Entry.properties()));
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing> Flowing = FluidRegister.register("flowing_" + Name, () -> new OilFluid.Moving(Entry.properties()));
        DeferredBlock<OilLiquidBlock> Pool = BlockRegister.register(Name, () -> new OilLiquidBlock(Entry.getSource(), Grade, BlockBehaviour.Properties.of()
                .mapColor(Grade.getMapColour()).replaceable().noCollission().strength(100.0f).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY)));
        DeferredItem<BucketItem> Bucket = ItemRegister.register(Grade.getBucketName(), () -> new BucketItem(Entry.getSource(),
                new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
        Entry.bind(Type, Source, Flowing, Pool, Bucket);
        return Entry;
    }

    public static void register(IEventBus ModEventBus) {
        FluidTypeRegister.register(ModEventBus);
        FluidRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        RecipeTypeRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
