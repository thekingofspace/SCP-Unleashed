package net.rai.scp_unleashed.scp098;

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
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp098Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_098");
    public static final ResourceKey<DamageType> Emergence = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp098_emergence"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp098_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepClick = sound("scp098_step_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ambient = sound("scp098_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp098_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp098_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathCrack = sound("scp098_death_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snip = sound("scp098_snip");
    public static final DeferredHolder<SoundEvent, SoundEvent> SnipClack = sound("scp098_snip_clack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Nibble = sound("scp098_nibble");
    public static final DeferredHolder<SoundEvent, SoundEvent> NibbleCrunch = sound("scp098_nibble_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burrow = sound("scp098_burrow");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurrowSift = sound("scp098_burrow_sift");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burst = sound("scp098_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstCrack = sound("scp098_burst_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstSand = sound("scp098_burst_sand");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spores = sound("scp098_spores");
    public static final DeferredHolder<SoundEvent, SoundEvent> SporesHiss = sound("scp098_spores_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> HiveGrow = sound("scp098_hive_grow");
    public static final DeferredHolder<SoundEvent, SoundEvent> VialPour = sound("scp098_vial_pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> VialSand = sound("scp098_vial_sand");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spread = sound("scp098_spread");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SnipLayers = List.of(Snip, SnipClack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> NibbleLayers = List.of(Nibble, NibbleCrunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurrowLayers = List.of(Burrow, BurrowSift);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurstLayers = List.of(Burst, BurstCrack, BurstSand);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SporeLayers = List.of(Spores, SporesHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> VialLayers = List.of(VialPour, VialSand);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SpreadLayers = List.of(Spread, BurrowSift);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Spore = ParticleRegister.register("scp098_spore", () -> new SimpleParticleType(false));

    public static final DeferredBlock<Scp098HiveBlock> Hive = BlockRegister.register("scp098_hive", () -> new Scp098HiveBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND).strength(Scp098HiveBlock.Hardness, Scp098HiveBlock.Resistance).sound(SoundType.SAND).pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> HiveItem = ItemRegister.register("scp098_hive", () -> new BlockItem(Hive.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Scp098VialItem> Vial = ItemRegister.register("scp098_sand_vial",
            () -> new Scp098VialItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp098HiveBlockEntity>> HiveEntity = BlockEntityTypeRegister.register("scp098_hive",
            () -> BlockEntityType.Builder.of(Scp098HiveBlockEntity::new, Hive.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp098Crab>> Crab = EntityTypeRegister.register("scp098_crab",
            () -> EntityType.Builder.<Scp098Crab>of(Scp098Crab::new, MobCategory.MONSTER)
                    .sized(Scp098Crab.Width, Scp098Crab.Height)
                    .eyeHeight(Scp098Crab.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp098_crab"));
    public static final DeferredItem<DeferredSpawnEggItem> CrabEgg = ItemRegister.register("scp098_crab_spawn_egg",
            () -> new DeferredSpawnEggItem(Crab, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp098Host.State>> Host = AttachmentRegister.register("scp098_host",
            () -> AttachmentType.builder(() -> Scp098Host.State.None).serialize(Scp098Host.State.StateCodec, Saved -> Saved.active()).build());

    private Scp098Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
