package net.rai.scp_unleashed.scp553;

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

import java.util.List;

public final class Scp553Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_553");
    public static final ResourceKey<DamageType> WingDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp553_wings"));
    public static final TagKey<Block> BreedingRock = TagKey.create(Registries.BLOCK, location("scp553_breeding_rock"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Flutter = sound("scp553_flutter");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlutterChime = sound("scp553_flutter_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Call = sound("scp553_call");
    public static final DeferredHolder<SoundEvent, SoundEvent> CallTinkle = sound("scp553_call_tinkle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spit = sound("scp553_spit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpitChime = sound("scp553_spit_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpitWet = sound("scp553_spit_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpitLaunch = sound("scp553_spit_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpitFleck = sound("scp553_spit_fleck");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpitSlap = sound("scp553_spit_slap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grow = sound("scp553_grow");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrowDrip = sound("scp553_grow_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hatch = sound("scp553_hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> HatchShatter = sound("scp553_hatch_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swarm = sound("scp553_swarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwarmRing = sound("scp553_swarm_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dive = sound("scp553_dive");
    public static final DeferredHolder<SoundEvent, SoundEvent> DiveWhistle = sound("scp553_dive_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slice = sound("scp553_slice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SliceChime = sound("scp553_slice_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp553_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp553_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathShatter = sound("scp553_death_shatter");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FlutterLayers = List.of(Flutter, FlutterChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CallLayers = List.of(Call, CallTinkle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HockLayers = List.of(Spit, SpitWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SpitLayers = List.of(SpitLaunch, SpitFleck, SpitSlap, SpitChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrowLayers = List.of(Grow, GrowDrip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HatchLayers = List.of(Hatch, HatchShatter);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwarmLayers = List.of(Swarm, SwarmRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DiveLayers = List.of(Dive, DiveWhistle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SliceLayers = List.of(Slice, SliceChime);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Glint = ParticleRegister.register("scp553_glint", () -> new SimpleParticleType(false));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp553Butterfly>> Butterfly = EntityTypeRegister.register("scp553_butterfly",
            () -> EntityType.Builder.<Scp553Butterfly>of(Scp553Butterfly::new, MobCategory.CREATURE)
                    .sized(Scp553Butterfly.Width, Scp553Butterfly.Height)
                    .eyeHeight(Scp553Butterfly.EyeHeight)
                    .clientTrackingRange(8)
                    .build("scp553_butterfly"));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp553_spawn_egg",
            () -> new DeferredSpawnEggItem(Butterfly, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredBlock<Scp553StalactiteBlock> Stalactite = BlockRegister.register("scp553_stalactite", () -> new Scp553StalactiteBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_BLUE).strength(Scp553StalactiteBlock.Hardness, Scp553StalactiteBlock.Resistance).sound(SoundType.AMETHYST_CLUSTER).noOcclusion()
            .lightLevel(State -> State.getValue(Scp553StalactiteBlock.Stage)).pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<BlockItem> StalactiteItem = ItemRegister.register("scp553_stalactite", () -> new BlockItem(Stalactite.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp553StalactiteBlockEntity>> StalactiteEntity = BlockEntityTypeRegister.register("scp553_stalactite",
            () -> BlockEntityType.Builder.of(Scp553StalactiteBlockEntity::new, Stalactite.get()).build(null));

    private Scp553Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp553CaveCondition.TypeName, Scp553CaveCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp553Cave::manifest);
    }
}
