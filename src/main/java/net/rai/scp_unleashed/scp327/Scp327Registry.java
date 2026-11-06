package net.rai.scp_unleashed.scp327;

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
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.List;

public final class Scp327Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_327");
    public static final ResourceKey<DamageType> Drowned = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp327_drowned"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp327Siren>> Siren = EntityTypeRegister.register("scp327_siren",
            () -> EntityType.Builder.<Scp327Siren>of(Scp327Siren::new, MobCategory.MISC)
                    .sized(Scp327Siren.Width, Scp327Siren.Height)
                    .eyeHeight(Scp327Siren.EyeHeight)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("scp327_siren"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp327Siren>> Pup = EntityTypeRegister.register("scp327_pup",
            () -> EntityType.Builder.<Scp327Siren>of(Scp327Siren::new, MobCategory.MISC)
                    .sized(Scp327Siren.Width, Scp327Siren.Height)
                    .eyeHeight(Scp327Siren.EyeHeight)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("scp327_pup"));

    public static final DeferredItem<DeferredSpawnEggItem> SirenEgg = ItemRegister.register("scp327_siren_spawn_egg",
            () -> new DeferredSpawnEggItem(Siren, 0x8C9A88, 0x1E2420, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> PupEgg = ItemRegister.register("scp327_pup_spawn_egg",
            () -> new DeferredSpawnEggItem(Pup, 0xA8B4A2, 0x5E7068, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<SoundEvent, SoundEvent> SongHum = sound("scp327_song_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> SongBreath = sound("scp327_song_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> SongFlute = sound("scp327_song_flute");
    public static final DeferredHolder<SoundEvent, SoundEvent> SongChime = sound("scp327_song_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> SongBell = sound("scp327_song_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> SongHarp = sound("scp327_song_harp");
    public static final DeferredHolder<SoundEvent, SoundEvent> SongShimmer = sound("scp327_song_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> SongBed = sound("scp327_song_bed");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ambient = sound("scp327_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> AmbientBubbles = sound("scp327_ambient_bubbles");
    public static final DeferredHolder<SoundEvent, SoundEvent> AmbientChirp = sound("scp327_ambient_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Surface = sound("scp327_surface");
    public static final DeferredHolder<SoundEvent, SoundEvent> SurfaceDrip = sound("scp327_surface_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dive = sound("scp327_dive");
    public static final DeferredHolder<SoundEvent, SoundEvent> DiveBubbles = sound("scp327_dive_bubbles");
    public static final DeferredHolder<SoundEvent, SoundEvent> Haul = sound("scp327_haul");
    public static final DeferredHolder<SoundEvent, SoundEvent> HaulWet = sound("scp327_haul_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grab = sound("scp327_grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrabSplash = sound("scp327_grab_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> Struggle = sound("scp327_struggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> StruggleThrash = sound("scp327_struggle_thrash");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp327_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtVoice = sound("scp327_hurt_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp327_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathVoice = sound("scp327_death_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flop = sound("scp327_flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlopGasp = sound("scp327_flop_gasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> PupChirp = sound("scp327_pup_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> PupChirpHigh = sound("scp327_pup_chirp_high");
    public static final DeferredHolder<SoundEvent, SoundEvent> Release = sound("scp327_release");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AmbientLayers = List.of(Ambient, AmbientBubbles);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChirpLayers = List.of(AmbientChirp);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SurfaceLayers = List.of(Surface, SurfaceDrip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DiveLayers = List.of(Dive, DiveBubbles);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HaulLayers = List.of(Haul, HaulWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrabLayers = List.of(Grab, GrabSplash);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StruggleLayers = List.of(Struggle, StruggleThrash);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtVoice);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathVoice);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FlopLayers = List.of(Flop, FlopGasp);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PupLayers = List.of(PupChirp, PupChirpHigh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReleaseLayers = List.of(Release, DiveBubbles);

    private Scp327Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp327ShoalCondition.TypeName, Scp327ShoalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp327Shoal::manifest);
        StasisPods.addReleaseHook((World, Spawned) -> {
            if (Spawned instanceof Scp327Siren Creature) {
                Creature.leaveStasis(World);
            }
        });
    }
}
