package net.rai.scp_unleashed.scp354;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class Scp354Registry {
    public static final DeferredRegister<FluidType> FluidTypeRegister = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<Fluid> FluidRegister = DeferredRegister.create(Registries.FLUID, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final List<DeferredItem<? extends Item>> BlockItems = new ArrayList<>();

    public static final ResourceLocation DocumentId = location("scp_354");
    public static final TagKey<EntityType<?>> Spawns = TagKey.create(Registries.ENTITY_TYPE, location("scp354_spawns"));
    public static final ResourceKey<DamageType> TakenDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp354_taken"));
    public static final float Resistance = 3600000.0f;

    public static final DeferredHolder<FluidType, FluidType> RedType = FluidTypeRegister.register("scp354_red", () -> new Scp354FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.scp_unleashed.scp354_red")
            .canExtinguish(true)
            .canConvertToSource(false)
            .supportsBoating(false)
            .canHydrate(false)
            .canPushEntity(false)
            .density(1600)
            .viscosity(4000)
            .temperature(310)
            .fallDistanceModifier(0.0f)
            .rarity(Rarity.EPIC)));
    public static final DeferredHolder<Fluid, Scp354Fluid.Source> Red = FluidRegister.register("scp354_red", () -> new Scp354Fluid.Source(fluidProperties()));
    public static final DeferredHolder<Fluid, Scp354Fluid.Flowing> RedFlowing = FluidRegister.register("scp354_red_flowing", () -> new Scp354Fluid.Flowing(fluidProperties()));
    public static final DeferredBlock<Scp354LiquidBlock> RedLiquid = BlockRegister.register("scp354_red", () -> new Scp354LiquidBlock(Red.get(), BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).noCollission().strength(-1.0f, Resistance).pushReaction(PushReaction.BLOCK).noLootTable().liquid().sound(SoundType.EMPTY)));

    public static final DeferredBlock<Block> StainedEarth = block("scp354_stained_earth", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_RED).strength(-1.0f, Resistance).pushReaction(PushReaction.BLOCK).sound(SoundType.MUD)));
    public static final DeferredBlock<Block> StainedStone = block("scp354_stained_stone", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.CRIMSON_NYLIUM).instrument(NoteBlockInstrument.BASEDRUM).strength(-1.0f, Resistance).pushReaction(PushReaction.BLOCK).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DeadGrass = block("scp354_dead_grass", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_BROWN).strength(-1.0f, Resistance).pushReaction(PushReaction.BLOCK).sound(SoundType.ROOTED_DIRT)));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp354Grasp>> Grasp = EntityTypeRegister.register("scp354_grasp",
            () -> EntityType.Builder.<Scp354Grasp>of(Scp354Grasp::new, MobCategory.MISC)
                    .sized(Scp354Grasp.Width, Scp354Grasp.Height)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("scp354_grasp"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp354Tentacle>> Tentacle = EntityTypeRegister.register("scp354_tentacle",
            () -> EntityType.Builder.<Scp354Tentacle>of(Scp354Tentacle::new, MobCategory.MONSTER)
                    .sized(Scp354Tentacle.Width, Scp354Tentacle.Height)
                    .eyeHeight(Scp354Tentacle.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("scp354_tentacle"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp354Lion>> Lion = EntityTypeRegister.register("scp354_lion",
            () -> EntityType.Builder.<Scp354Lion>of(Scp354Lion::new, MobCategory.MONSTER)
                    .sized(Scp354Lion.Width, Scp354Lion.Height)
                    .eyeHeight(Scp354Lion.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp354_lion"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp354Lizard>> Lizard = EntityTypeRegister.register("scp354_lizard",
            () -> EntityType.Builder.<Scp354Lizard>of(Scp354Lizard::new, MobCategory.MONSTER)
                    .sized(Scp354Lizard.Width, Scp354Lizard.Height)
                    .eyeHeight(Scp354Lizard.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp354_lizard"));

    public static final DeferredItem<DeferredSpawnEggItem> TentacleEgg = ItemRegister.register("scp354_tentacle_spawn_egg",
            () -> new DeferredSpawnEggItem(Tentacle, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> LionEgg = ItemRegister.register("scp354_lion_spawn_egg",
            () -> new DeferredSpawnEggItem(Lion, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> LizardEgg = ItemRegister.register("scp354_lizard_spawn_egg",
            () -> new DeferredSpawnEggItem(Lizard, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Bubble = ParticleRegister.register("scp354_bubble", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ripple = ParticleRegister.register("scp354_ripple", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Drip = ParticleRegister.register("scp354_drip", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Splash = ParticleRegister.register("scp354_splash", () -> new SimpleParticleType(false));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> PoolBorn = AttachmentRegister.register("scp354_pool_born",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> PoolBubble = sound("scp354_pool_bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> PoolBubbleDeep = sound("scp354_pool_bubble_deep");
    public static final DeferredHolder<SoundEvent, SoundEvent> PoolChurn = sound("scp354_pool_churn");
    public static final DeferredHolder<SoundEvent, SoundEvent> PoolChurnLow = sound("scp354_pool_churn_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> PoolGurgle = sound("scp354_pool_gurgle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PoolGurgleLow = sound("scp354_pool_gurgle_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrabSplash = sound("scp354_grab_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrabSplashWet = sound("scp354_grab_splash_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrabCoil = sound("scp354_grab_coil");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrabCoilCreak = sound("scp354_grab_coil_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrabPull = sound("scp354_grab_pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrabPullDeep = sound("scp354_grab_pull_deep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Emerge = sound("scp354_emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmergeSurge = sound("scp354_emerge_surge");
    public static final DeferredHolder<SoundEvent, SoundEvent> Collapse = sound("scp354_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> CollapseDrain = sound("scp354_collapse_drain");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleIdle = sound("scp354_tentacle_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleIdleLow = sound("scp354_tentacle_idle_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleHurt = sound("scp354_tentacle_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleHurtLow = sound("scp354_tentacle_hurt_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleDeath = sound("scp354_tentacle_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleDeathLow = sound("scp354_tentacle_death_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleSweep = sound("scp354_tentacle_sweep");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleSweepAir = sound("scp354_tentacle_sweep_air");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleSlam = sound("scp354_tentacle_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleSlamImpact = sound("scp354_tentacle_slam_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleBurrow = sound("scp354_tentacle_burrow");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleBurrowEarth = sound("scp354_tentacle_burrow_earth");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionIdle = sound("scp354_lion_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionIdleGrowl = sound("scp354_lion_idle_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionHurt = sound("scp354_lion_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionHurtSnarl = sound("scp354_lion_hurt_snarl");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionDeath = sound("scp354_lion_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionDeathLow = sound("scp354_lion_death_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionStep = sound("scp354_lion_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionStepPad = sound("scp354_lion_step_pad");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionMaul = sound("scp354_lion_maul");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionMaulSnap = sound("scp354_lion_maul_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionPounce = sound("scp354_lion_pounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionPounceSnarl = sound("scp354_lion_pounce_snarl");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionRoar = sound("scp354_lion_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> LionRoarLow = sound("scp354_lion_roar_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardIdle = sound("scp354_lizard_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardIdleCroak = sound("scp354_lizard_idle_croak");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardHurt = sound("scp354_lizard_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardHurtLow = sound("scp354_lizard_hurt_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardDeath = sound("scp354_lizard_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardDeathLow = sound("scp354_lizard_death_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardStep = sound("scp354_lizard_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardStepScrape = sound("scp354_lizard_step_scrape");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardBite = sound("scp354_lizard_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardBiteSnap = sound("scp354_lizard_bite_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardWhip = sound("scp354_lizard_whip");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardWhipAir = sound("scp354_lizard_whip_air");
    public static final DeferredHolder<SoundEvent, SoundEvent> LizardHiss = sound("scp354_lizard_hiss");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BubbleLayers = List.of(PoolBubble, PoolBubbleDeep);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChurnLayers = List.of(PoolChurn, PoolChurnLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GurgleLayers = List.of(PoolGurgle, PoolGurgleLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SplashLayers = List.of(GrabSplash, GrabSplashWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CoilLayers = List.of(GrabCoil, GrabCoilCreak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PullLayers = List.of(GrabPull, GrabPullDeep);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EmergeLayers = List.of(Emerge, EmergeSurge);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CollapseLayers = List.of(Collapse, CollapseDrain);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TentacleIdleLayers = List.of(TentacleIdle, TentacleIdleLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TentacleHurtLayers = List.of(TentacleHurt, TentacleHurtLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TentacleDeathLayers = List.of(TentacleDeath, TentacleDeathLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SweepLayers = List.of(TentacleSweep, TentacleSweepAir);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SlamLayers = List.of(TentacleSlam, TentacleSlamImpact);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurrowLayers = List.of(TentacleBurrow, TentacleBurrowEarth);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LionIdleLayers = List.of(LionIdle, LionIdleGrowl);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LionHurtLayers = List.of(LionHurt, LionHurtSnarl);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LionDeathLayers = List.of(LionDeath, LionDeathLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MaulLayers = List.of(LionMaul, LionMaulSnap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PounceLayers = List.of(LionPounce, LionPounceSnarl);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RoarLayers = List.of(LionRoar, LionRoarLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LizardIdleLayers = List.of(LizardIdle, LizardIdleCroak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LizardHurtLayers = List.of(LizardHurt, LizardHurtLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LizardDeathLayers = List.of(LizardDeath, LizardDeathLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BiteLayers = List.of(LizardBite, LizardBiteSnap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhipLayers = List.of(LizardWhip, LizardWhipAir);

    private Scp354Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static <T extends Block> DeferredBlock<T> block(String Name, Supplier<T> Factory) {
        DeferredBlock<T> Registered = BlockRegister.register(Name, Factory);
        BlockItems.add(ItemRegister.register(Name, () -> new BlockItem(Registered.get(), new Item.Properties())));
        return Registered;
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static BaseFlowingFluid.Properties fluidProperties() {
        return new BaseFlowingFluid.Properties(RedType, Red, RedFlowing).block(RedLiquid).slopeFindDistance(1).levelDecreasePerBlock(8).tickRate(1000)
                .explosionResistance(Resistance);
    }

    public static void register(IEventBus ModEventBus) {
        FluidTypeRegister.register(ModEventBus);
        FluidRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(Scp354PoolCondition.TypeName, Scp354PoolCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp354Pools::spawn);
    }
}
