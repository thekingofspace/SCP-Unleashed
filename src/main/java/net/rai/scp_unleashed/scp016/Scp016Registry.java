package net.rai.scp_unleashed.scp016;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp016Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_016");
    public static final ResourceKey<DamageType> SplitDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp016_split"));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> Affliction = AttachmentRegister.register("scp016_affliction",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT, Saved -> Saved >= 0).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> SkitterAmbient = sound("scp016_skitter_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkitterHurt = sound("scp016_skitter_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkitterDeath = sound("scp016_skitter_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkitterStep = sound("scp016_skitter_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkitterTick = sound("scp016_skitter_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkitterAttack = sound("scp016_skitter_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteAmbient = sound("scp016_brute_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteHurt = sound("scp016_brute_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteDeath = sound("scp016_brute_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteStep = sound("scp016_brute_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteThud = sound("scp016_brute_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteGrind = sound("scp016_brute_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteSlam = sound("scp016_brute_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteSlamCrack = sound("scp016_brute_slam_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlingerAmbient = sound("scp016_slinger_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlingerHurt = sound("scp016_slinger_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlingerDeath = sound("scp016_slinger_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlingerStep = sound("scp016_slinger_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlingerThrow = sound("scp016_slinger_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlingerThrowWhip = sound("scp016_slinger_throw_whip");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShardHit = sound("scp016_shard_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShardHitChime = sound("scp016_shard_hit_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Split = sound("scp016_split");
    public static final DeferredHolder<SoundEvent, SoundEvent> SplitCrack = sound("scp016_split_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SplitWet = sound("scp016_split_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Collapse = sound("scp016_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> CollapseShatter = sound("scp016_collapse_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stage = sound("scp016_stage");
    public static final DeferredHolder<SoundEvent, SoundEvent> StageLow = sound("scp016_stage_low");

    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistAmbient = sound("scp016_duelist_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistHurt = sound("scp016_duelist_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistDeath = sound("scp016_duelist_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistStep = sound("scp016_duelist_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistDrag = sound("scp016_duelist_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistSlash = sound("scp016_duelist_slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistSlashRing = sound("scp016_duelist_slash_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistLunge = sound("scp016_duelist_lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistLungeRush = sound("scp016_duelist_lunge_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistGuard = sound("scp016_duelist_guard");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistParry = sound("scp016_duelist_parry");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistParryRing = sound("scp016_duelist_parry_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> DuelistBreak = sound("scp016_duelist_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> GluttonAmbient = sound("scp016_glutton_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GluttonHurt = sound("scp016_glutton_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GluttonDeath = sound("scp016_glutton_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GluttonStep = sound("scp016_glutton_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> GluttonThud = sound("scp016_glutton_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> GluttonGrab = sound("scp016_glutton_grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> GluttonGulp = sound("scp016_glutton_gulp");
    public static final DeferredHolder<SoundEvent, SoundEvent> GluttonGulpWet = sound("scp016_glutton_gulp_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> GluttonSwipe = sound("scp016_glutton_swipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> GluttonSwipeHeavy = sound("scp016_glutton_swipe_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimmerAmbient = sound("scp016_swimmer_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimmerHurt = sound("scp016_swimmer_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimmerDeath = sound("scp016_swimmer_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimmerFlop = sound("scp016_swimmer_flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimmerSwim = sound("scp016_swimmer_swim");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimmerBite = sound("scp016_swimmer_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimmerDrag = sound("scp016_swimmer_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimmerDragSplash = sound("scp016_swimmer_drag_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimmerBreach = sound("scp016_swimmer_breach");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimmerBreachSplash = sound("scp016_swimmer_breach_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlyerAmbient = sound("scp016_flyer_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlyerHurt = sound("scp016_flyer_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlyerDeath = sound("scp016_flyer_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlyerFlap = sound("scp016_flyer_flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlyerStep = sound("scp016_flyer_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlyerSwoop = sound("scp016_flyer_swoop");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlyerStrike = sound("scp016_flyer_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlyerStrikeHit = sound("scp016_flyer_strike_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlyerTakeoff = sound("scp016_flyer_takeoff");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SplitLayers = List.of(Split, SplitCrack, SplitWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SlashLayers = List.of(DuelistSlash, DuelistSlashRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LungeLayers = List.of(DuelistLunge, DuelistLungeRush);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ParryLayers = List.of(DuelistParry, DuelistParryRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GulpLayers = List.of(GluttonGulp, GluttonGulpWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwipeLayers = List.of(GluttonSwipe, GluttonSwipeHeavy);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DragLayers = List.of(SwimmerDrag, SwimmerDragSplash);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreachLayers = List.of(SwimmerBreach, SwimmerBreachSplash);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StrikeLayers = List.of(FlyerStrike, FlyerStrikeHit);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CollapseLayers = List.of(Collapse, CollapseShatter);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StageLayers = List.of(Stage, StageLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SlamLayers = List.of(BruteSlam, BruteSlamCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ThrowLayers = List.of(SlingerThrow, SlingerThrowWhip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShardLayers = List.of(ShardHit, ShardHitChime);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Fleck = ParticleRegister.register("scp016_fleck", () -> new SimpleParticleType(false));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp016Skitter>> SkitterType = EntityTypeRegister.register("scp016_skitter",
            () -> EntityType.Builder.<Scp016Skitter>of(Scp016Skitter::new, MobCategory.MONSTER).sized(1.0f, 0.6f).eyeHeight(0.42f).clientTrackingRange(10)
                    .build("scp016_skitter"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp016Brute>> BruteType = EntityTypeRegister.register("scp016_brute",
            () -> EntityType.Builder.<Scp016Brute>of(Scp016Brute::new, MobCategory.MONSTER).sized(1.3f, 2.3f).eyeHeight(1.95f).clientTrackingRange(10)
                    .build("scp016_brute"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp016Slinger>> SlingerType = EntityTypeRegister.register("scp016_slinger",
            () -> EntityType.Builder.<Scp016Slinger>of(Scp016Slinger::new, MobCategory.MONSTER).sized(0.7f, 2.05f).eyeHeight(1.8f).clientTrackingRange(10)
                    .build("scp016_slinger"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp016Duelist>> DuelistType = EntityTypeRegister.register("scp016_duelist",
            () -> EntityType.Builder.<Scp016Duelist>of(Scp016Duelist::new, MobCategory.MONSTER).sized(0.75f, 2.1f).eyeHeight(1.85f).clientTrackingRange(10)
                    .build("scp016_duelist"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp016Glutton>> GluttonType = EntityTypeRegister.register("scp016_glutton",
            () -> EntityType.Builder.<Scp016Glutton>of(Scp016Glutton::new, MobCategory.MONSTER).sized(1.6f, 2.45f).eyeHeight(2.05f).clientTrackingRange(10)
                    .build("scp016_glutton"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp016Swimmer>> SwimmerType = EntityTypeRegister.register("scp016_swimmer",
            () -> EntityType.Builder.<Scp016Swimmer>of(Scp016Swimmer::new, MobCategory.MONSTER).sized(0.9f, 0.55f).eyeHeight(0.32f).clientTrackingRange(10)
                    .build("scp016_swimmer"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp016Flyer>> FlyerType = EntityTypeRegister.register("scp016_flyer",
            () -> EntityType.Builder.<Scp016Flyer>of(Scp016Flyer::new, MobCategory.MONSTER).sized(1.1f, 1.0f).eyeHeight(0.78f).clientTrackingRange(10)
                    .build("scp016_flyer"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp016Shard>> ShardType = EntityTypeRegister.register("scp016_shard",
            () -> EntityType.Builder.<Scp016Shard>of(Scp016Shard::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(4).updateInterval(10)
                    .build("scp016_shard"));

    public static final DeferredItem<DeferredSpawnEggItem> SkitterEgg = ItemRegister.register("scp016_skitter_spawn_egg",
            () -> new DeferredSpawnEggItem(SkitterType, 0x5A0A16, 0xE0404E, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> BruteEgg = ItemRegister.register("scp016_brute_spawn_egg",
            () -> new DeferredSpawnEggItem(BruteType, 0x5A0A16, 0xB01E30, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> SlingerEgg = ItemRegister.register("scp016_slinger_spawn_egg",
            () -> new DeferredSpawnEggItem(SlingerType, 0x5A0A16, 0xFF7A70, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<DeferredSpawnEggItem> DuelistEgg = ItemRegister.register("scp016_duelist_spawn_egg",
            () -> new DeferredSpawnEggItem(DuelistType, 0xB07A5C, 0xC21530, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> GluttonEgg = ItemRegister.register("scp016_glutton_spawn_egg",
            () -> new DeferredSpawnEggItem(GluttonType, 0x8C5836, 0xE8823A, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> SwimmerEgg = ItemRegister.register("scp016_swimmer_spawn_egg",
            () -> new DeferredSpawnEggItem(SwimmerType, 0x6B4226, 0x3A42A0, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> FlyerEgg = ItemRegister.register("scp016_flyer_spawn_egg",
            () -> new DeferredSpawnEggItem(FlyerType, 0xD99F80, 0x18868E, new Item.Properties().rarity(Rarity.RARE)));

    private Scp016Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        LabOrganisms.register(new Scp016Organism());
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
