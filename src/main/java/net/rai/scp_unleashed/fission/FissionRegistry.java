package net.rai.scp_unleashed.fission;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.plumbing.PlumbingRegistry;

public final class FissionRegistry {
    public static final ResourceLocation IrradiatingId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "irradiating");
    public static final ResourceKey<DamageType> MeltdownDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fission_meltdown"));

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeType<?>> RecipeTypeRegister = DeferredRegister.create(Registries.RECIPE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<FluidType> FluidTypeRegister = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<Fluid> FluidRegister = DeferredRegister.create(Registries.FLUID, Scp_unleashed.ModId);

    public static final DeferredHolder<FluidType, FluidType> WasteWaterType = FluidTypeRegister.register("waste_water", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.scp_unleashed.waste_water")
            .canConvertToSource(false)
            .canExtinguish(true)
            .canHydrate(false)
            .supportsBoating(true)
            .fallDistanceModifier(0.0f)
            .motionScale(0.011)
            .density(1100)
            .viscosity(1400)
            .temperature(312)
            .pathType(PathType.WATER)
            .adjacentPathType(PathType.WATER_BORDER)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> WasteWater = FluidRegister.register("waste_water", () -> new WasteWaterFluid.Still(wasteProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FlowingWasteWater = FluidRegister.register("flowing_waste_water", () -> new WasteWaterFluid.Moving(wasteProperties()));
    public static final DeferredBlock<WasteWaterBlock> WasteWaterLiquid = BlockRegister.register("waste_water", () -> new WasteWaterBlock(WasteWater.get(), BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN).replaceable().noCollission().strength(100.0f).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY)));
    public static final DeferredItem<BucketItem> WasteWaterBucket = ItemRegister.register("waste_water_bucket", () -> new BucketItem(WasteWater.get(),
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredBlock<FissionReactorBlock> Reactor = BlockRegister.register("fission_reactor", () -> new FissionReactorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(6.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> ReactorItem = ItemRegister.register("fission_reactor", () -> new BlockItem(Reactor.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredBlock<HeatExchangerBlock> HeatExchanger = BlockRegister.register("fission_heat_exchanger", () -> new HeatExchangerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(4.0f, 8.0f).sound(SoundType.COPPER).pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> HeatExchangerItem = ItemRegister.registerSimpleBlockItem("fission_heat_exchanger", HeatExchanger);
    public static final DeferredBlock<SonicVibratorBlock> SonicVibrator = BlockRegister.register("sonic_vibrator", () -> new SonicVibratorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
            .lightLevel(State -> State.getValue(SonicVibratorBlock.Active) ? SonicVibratorBlock.ActiveLight : 0)));
    public static final DeferredItem<BlockItem> SonicVibratorItem = ItemRegister.registerSimpleBlockItem("sonic_vibrator", SonicVibrator);
    public static final DeferredBlock<GasDissolverBlock> GasDissolver = BlockRegister.register("gas_dissolver", () -> new GasDissolverBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> GasDissolverItem = ItemRegister.registerSimpleBlockItem("gas_dissolver", GasDissolver);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FissionReactorBlockEntity>> ReactorEntity = BlockEntityRegister.register("fission_reactor",
            () -> BlockEntityType.Builder.of(FissionReactorBlockEntity::new, Reactor.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatExchangerBlockEntity>> HeatExchangerEntity = BlockEntityRegister.register("fission_heat_exchanger",
            () -> BlockEntityType.Builder.of(HeatExchangerBlockEntity::new, HeatExchanger.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SonicVibratorBlockEntity>> SonicVibratorEntity = BlockEntityRegister.register("sonic_vibrator",
            () -> BlockEntityType.Builder.of(SonicVibratorBlockEntity::new, SonicVibrator.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasDissolverBlockEntity>> GasDissolverEntity = BlockEntityRegister.register("gas_dissolver",
            () -> BlockEntityType.Builder.of(GasDissolverBlockEntity::new, GasDissolver.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<FissionReactorMenu>> ReactorMenu = MenuRegister.register("fission_reactor",
            () -> new MenuType<>(FissionReactorMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<SonicVibratorMenu>> VibratorMenu = MenuRegister.register("sonic_vibrator",
            () -> new MenuType<>(SonicVibratorMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<GasDissolverMenu>> DissolverMenu = MenuRegister.register("gas_dissolver",
            () -> new MenuType<>(GasDissolverMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<RecipeType<?>, RecipeType<IrradiatingRecipe>> Irradiating = RecipeTypeRegister.register("irradiating",
            () -> RecipeType.simple(IrradiatingId));
    public static final DeferredHolder<RecipeSerializer<?>, IrradiatingRecipe.Serializer> IrradiatingSerializer = RecipeSerializerRegister.register("irradiating",
            IrradiatingRecipe.Serializer::new);

    public static final DeferredHolder<EntityType<?>, EntityType<ToxicGasCloud>> ToxicGas = EntityRegister.register("fission_toxic_gas",
            () -> EntityType.Builder.<ToxicGasCloud>of(ToxicGasCloud::new, MobCategory.MISC).sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(10).fireImmune().noSummon()
                    .build("fission_toxic_gas"));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ToxicGasParticle = ParticleRegister.register("fission_toxic_gas", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SonicRing = ParticleRegister.register("fission_sonic_ring", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> ReactorHum = sound("fission_reactor_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReactorDrone = sound("fission_reactor_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReactorCore = sound("fission_reactor_core");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReactorAlarm = fixedSound("fission_reactor_alarm", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> ReactorKlaxon = fixedSound("fission_reactor_klaxon", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> ReactorBell = fixedSound("fission_reactor_bell", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> ReactorStart = sound("fission_reactor_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReactorScram = sound("fission_reactor_scram");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReactorClunk = sound("fission_reactor_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> Meltdown = fixedSound("fission_meltdown", 160.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> MeltdownRumble = fixedSound("fission_meltdown_rumble", 160.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> GasHiss = sound("fission_gas_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> VibratorHum = sound("fission_vibrator_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> VibratorTone = sound("fission_vibrator_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> VibratorSlosh = sound("fission_vibrator_slosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> VibratorClick = sound("fission_vibrator_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExchangerHiss = sound("fission_exchanger_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> DissolverBubble = sound("fission_dissolver_bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> DissolverPop = sound("fission_dissolver_pop");

    private FissionRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixedSound(String Name, float Range) {
        return SoundRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), Range));
    }

    private static BaseFlowingFluid.Properties wasteProperties() {
        return new BaseFlowingFluid.Properties(WasteWaterType, WasteWater, FlowingWasteWater)
                .bucket(WasteWaterBucket).block(WasteWaterLiquid).slopeFindDistance(3).levelDecreasePerBlock(2).tickRate(10).explosionResistance(100.0f);
    }

    public static void register(IEventBus ModEventBus) {
        GasRegistry.register(ModEventBus);
        PlumbingRegistry.register(ModEventBus);
        FluidTypeRegister.register(ModEventBus);
        FluidRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        RecipeTypeRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        EntityRegister.register(ModEventBus);
    }
}
