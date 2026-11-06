package net.rai.scp_unleashed.scp106;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

public final class Scp106Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_106");
    public static final ResourceKey<DamageType> Corrosion = damage("scp106_corrosion");
    public static final ResourceKey<DamageType> FemurBreak = damage("scp106_femur_breaker");
    public static final ResourceKey<DamageType> Taken = damage("scp106_taken");

    public static final DeferredBlock<Scp106FemurBreakerBlock> FemurBreaker = BlockRegister.register("scp106_femur_breaker",
            () -> new Scp106FemurBreakerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(6.0f, 12.0f)
                    .sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<Scp106PocketBlock> PocketStone = BlockRegister.register("scp106_pocket_stone",
            () -> new Scp106PocketBlock(Scp106PocketBlock.sealed(MapColor.COLOR_BLACK).sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Scp106PocketBlock> CorrodedStone = BlockRegister.register("scp106_corroded_stone",
            () -> new Scp106PocketBlock(Scp106PocketBlock.sealed(MapColor.TERRACOTTA_BLACK).sound(SoundType.MUD_BRICKS)));
    public static final DeferredBlock<Scp106PocketBlock> PocketTiles = BlockRegister.register("scp106_pocket_tiles",
            () -> new Scp106PocketBlock(Scp106PocketBlock.sealed(MapColor.COLOR_BLACK).sound(SoundType.DEEPSLATE_TILES)));
    public static final DeferredBlock<Scp106PocketBlock> PocketSeep = BlockRegister.register("scp106_pocket_seep",
            () -> new Scp106PocketBlock(Scp106PocketBlock.sealed(MapColor.COLOR_GREEN).sound(SoundType.MUD).lightLevel(State -> Scp106PocketBlock.SeepLight)));
    public static final DeferredBlock<Scp106DoorwayBlock> Doorway = BlockRegister.register("scp106_doorway",
            () -> new Scp106DoorwayBlock(Scp106PocketBlock.sealed(MapColor.COLOR_BLACK).sound(SoundType.SCULK).noCollission().noOcclusion()));

    public static final DeferredItem<BlockItem> FemurBreakerItem = ItemRegister.register("scp106_femur_breaker",
            () -> new BlockItem(FemurBreaker.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> PocketStoneItem = ItemRegister.register("scp106_pocket_stone",
            () -> new BlockItem(PocketStone.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> CorrodedStoneItem = ItemRegister.register("scp106_corroded_stone",
            () -> new BlockItem(CorrodedStone.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> PocketTilesItem = ItemRegister.register("scp106_pocket_tiles",
            () -> new BlockItem(PocketTiles.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> PocketSeepItem = ItemRegister.register("scp106_pocket_seep",
            () -> new BlockItem(PocketSeep.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> DoorwayItem = ItemRegister.register("scp106_doorway",
            () -> new BlockItem(Doorway.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp106FemurBreakerBlockEntity>> FemurBreakerEntity = BlockEntityRegister.register("scp106_femur_breaker",
            () -> BlockEntityType.Builder.of(Scp106FemurBreakerBlockEntity::new, FemurBreaker.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp106OldMan>> OldMan = EntityTypeRegister.register("scp106_old_man",
            () -> EntityType.Builder.<Scp106OldMan>of(Scp106OldMan::new, MobCategory.MONSTER).sized(Scp106OldMan.Width, Scp106OldMan.Height)
                    .eyeHeight(Scp106OldMan.EyeHeight).fireImmune().clientTrackingRange(10).updateInterval(2).build("scp106_old_man"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp106Restraint>> Restraint = EntityTypeRegister.register("scp106_restraint",
            () -> EntityType.Builder.<Scp106Restraint>of(Scp106Restraint::new, MobCategory.MISC).sized(0.001f, 0.001f).noSummon().fireImmune()
                    .clientTrackingRange(10).build("scp106_restraint"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp106Portal>> Portal = EntityTypeRegister.register("scp106_portal",
            () -> EntityType.Builder.<Scp106Portal>of(Scp106Portal::new, MobCategory.MISC).sized(0.1f, 0.1f).noSummon().fireImmune()
                    .clientTrackingRange(8).updateInterval(40).build("scp106_portal"));

    public static final DeferredItem<DeferredSpawnEggItem> OldManEgg = ItemRegister.register("scp106_spawn_egg",
            () -> new DeferredSpawnEggItem(OldMan, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Goo = ParticleRegister.register("scp106_goo", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Puddle = ParticleRegister.register("scp106_puddle", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Pool = ParticleRegister.register("scp106_pool", () -> new SimpleParticleType(true));

    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp106_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreathWet = sound("scp106_breath_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreathLow = sound("scp106_breath_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Laugh = sound("scp106_laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> LaughLow = sound("scp106_laugh_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp106_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepWet = sound("scp106_step_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sink = sound("scp106_sink");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rise = sound("scp106_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> Touch = sound("scp106_touch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Phase = sound("scp106_phase");
    public static final DeferredHolder<SoundEvent, SoundEvent> Corrode = sound("scp106_corrode");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breach = sound("scp106_breach");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp106_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp106_pocket_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> PocketLaugh = sound("scp106_pocket_laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drone = sound("scp106_pocket_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorwayPass = sound("scp106_doorway");
    public static final DeferredHolder<SoundEvent, SoundEvent> Escape = sound("scp106_escape");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lever = sound("scp106_femur_lever");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crunch = sound("scp106_femur_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slam = sound("scp106_femur_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> Scream = sound("scp106_femur_scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScreamLayer = sound("scp106_femur_scream_layer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Buckle = sound("scp106_restraint");

    private Scp106Registry() {
    }

    private static ResourceKey<DamageType> damage(String Name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp106NightfallCondition.TypeName, Scp106NightfallCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp106NightfallCondition::manifest);
    }
}
