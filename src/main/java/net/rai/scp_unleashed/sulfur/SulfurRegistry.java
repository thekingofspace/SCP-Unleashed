package net.rai.scp_unleashed.sulfur;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.liquidsound.LiquidSoundSet;
import net.rai.scp_unleashed.materials.MaterialBlock;
import net.rai.scp_unleashed.materials.MaterialItem;
import net.rai.scp_unleashed.sulfur.acid.SulfuricAcidBlock;
import net.rai.scp_unleashed.sulfur.acid.SulfuricAcidBucketItem;
import net.rai.scp_unleashed.sulfur.acid.SulfuricAcidFluid;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlock;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlockEntity;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankMenu;

import java.util.List;

public final class SulfurRegistry {
    public static final String TankId = "industrial_tank";
    public static final String AcidId = "sulfuric_acid";
    public static final ResourceKey<DamageType> AcidDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location(AcidId));
    public static final float OreHardness = 3.0f;
    public static final float DeepslateOreHardness = 4.5f;
    public static final float OreResistance = 3.0f;
    public static final float SulfurBlockHardness = 2.0f;
    public static final float SulfurBlockResistance = 3.0f;
    public static final float TankHardness = 3.5f;
    public static final float TankResistance = 6.0f;
    public static final int TankLight = 4;
    public static final int AcidDensity = 1830;
    public static final int AcidViscosity = 1600;
    public static final double AcidMotion = 0.012;

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuTypeRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<FluidType> FluidTypeRegister = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<Fluid> FluidRegister = DeferredRegister.create(Registries.FLUID, Scp_unleashed.ModId);

    public static final DeferredHolder<FluidType, FluidType> AcidType = FluidTypeRegister.register(AcidId, () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.scp_unleashed." + AcidId)
            .canConvertToSource(false)
            .canExtinguish(true)
            .canHydrate(false)
            .supportsBoating(false)
            .canSwim(true)
            .canDrown(true)
            .fallDistanceModifier(0.0f)
            .motionScale(AcidMotion)
            .density(AcidDensity)
            .viscosity(AcidViscosity)
            .temperature(300)
            .pathType(PathType.DAMAGE_OTHER)
            .adjacentPathType(PathType.DANGER_OTHER)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> Acid = FluidRegister.register(AcidId, () -> new SulfuricAcidFluid.Still(acidProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FlowingAcid = FluidRegister.register("flowing_" + AcidId,
            () -> new SulfuricAcidFluid.Moving(acidProperties()));
    public static final DeferredBlock<SulfuricAcidBlock> AcidBlock = BlockRegister.register(AcidId, () -> new SulfuricAcidBlock(Acid.get(), BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).replaceable().noCollission().strength(100.0f).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY)));
    public static final DeferredItem<SulfuricAcidBucketItem> AcidBucket = ItemRegister.register(AcidId + "_bucket",
            () -> new SulfuricAcidBucketItem(Acid.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredBlock<SulfurOreBlock> Ore = BlockRegister.register("sulfur_ore", () -> new SulfurOreBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(OreHardness, OreResistance)));
    public static final DeferredBlock<SulfurOreBlock> DeepslateOre = BlockRegister.register("deepslate_sulfur_ore", () -> new SulfurOreBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(DeepslateOreHardness, OreResistance)
            .sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<MaterialBlock> SulfurBlock = BlockRegister.register("sulfur_block", () -> new MaterialBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(SulfurBlockHardness, SulfurBlockResistance)
            .sound(SoundType.CALCITE)));
    public static final DeferredItem<BlockItem> OreItem = ItemRegister.registerSimpleBlockItem("sulfur_ore", Ore);
    public static final DeferredItem<BlockItem> DeepslateOreItem = ItemRegister.registerSimpleBlockItem("deepslate_sulfur_ore", DeepslateOre);
    public static final DeferredItem<BlockItem> SulfurBlockItem = ItemRegister.registerSimpleBlockItem("sulfur_block", SulfurBlock);
    public static final DeferredItem<MaterialItem> Sulfur = ItemRegister.register("sulfur", () -> new MaterialItem(new Item.Properties()));

    public static final DeferredBlock<IndustrialTankBlock> Tank = BlockRegister.register(TankId, () -> new IndustrialTankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(TankHardness, TankResistance).sound(SoundType.METAL).noOcclusion()
            .pushReaction(PushReaction.BLOCK).lightLevel(State -> State.getValue(IndustrialTankBlock.Active) ? TankLight : 0)));
    public static final DeferredItem<BlockItem> TankItem = ItemRegister.registerSimpleBlockItem(TankId, Tank);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialTankBlockEntity>> TankEntity = BlockEntityTypeRegister.register(TankId,
            () -> BlockEntityType.Builder.of(IndustrialTankBlockEntity::new, Tank.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<IndustrialTankMenu>> TankMenu = MenuTypeRegister.register(TankId,
            () -> IMenuTypeExtension.create(IndustrialTankMenu::new));

    public static final DeferredHolder<SoundEvent, SoundEvent> AcidSwim = sound("acid_swim");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidSwimFizz = sound("acid_swim_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidSplash = sound("acid_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidSplashFizz = sound("acid_splash_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidSplashHeavy = sound("acid_splash_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidFlow = sound("acid_flow");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidBubble = sound("acid_bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidBubbleFizz = sound("acid_bubble_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidSizzle = sound("acid_sizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidSizzlePop = sound("acid_sizzle_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidMelt = sound("acid_melt");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcidMeltGurgle = sound("acid_melt_gurgle");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankChurn = sound("industrial_tank_churn");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankChurnBrew = sound("industrial_tank_churn_brew");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankStart = sound("industrial_tank_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankStartClank = sound("industrial_tank_start_clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankStop = sound("industrial_tank_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankDone = sound("industrial_tank_done");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankDoneFizz = sound("industrial_tank_done_fizz");

    public static final LiquidSoundSet AcidSounds = new LiquidSoundSet(List.of(AcidSwim, AcidSwimFizz), List.of(AcidSplash, AcidSplashFizz),
            List.of(AcidSplashHeavy, AcidSplashFizz), List.of(AcidFlow), List.of(AcidBubble, AcidBubbleFizz));
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SizzleLayers = List.of(AcidSizzle, AcidSizzlePop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MeltLayers = List.of(AcidMelt, AcidMeltGurgle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChurnLayers = List.of(TankChurn, TankChurnBrew);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StartLayers = List.of(TankStart, TankStartClank);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DoneLayers = List.of(TankDone, TankDoneFizz);

    private SulfurRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static BaseFlowingFluid.Properties acidProperties() {
        return new BaseFlowingFluid.Properties(AcidType, Acid, FlowingAcid)
                .bucket(AcidBucket).block(AcidBlock).slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(5).explosionResistance(100.0f);
    }

    public static void register(IEventBus ModEventBus) {
        FluidTypeRegister.register(ModEventBus);
        FluidRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        MenuTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
