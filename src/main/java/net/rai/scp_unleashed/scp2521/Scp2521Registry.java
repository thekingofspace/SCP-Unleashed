package net.rai.scp_unleashed.scp2521;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.List;

public final class Scp2521Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_2521");
    public static final ResourceKey<Level> Void = ResourceKey.create(Registries.DIMENSION, location("scp2521_void"));
    public static final ResourceLocation VoidEffects = location("scp2521_void");

    public static final DeferredHolder<EntityType<?>, EntityType<Scp2521>> CreatureType = EntityTypeRegister.register("scp2521",
            () -> EntityType.Builder.<Scp2521>of(Scp2521::new, MobCategory.MISC)
                    .sized(Scp2521.Width, Scp2521.Height)
                    .eyeHeight(Scp2521.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("scp2521"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp2521Tendrils>> TendrilType = EntityTypeRegister.register("scp2521_tendrils",
            () -> EntityType.Builder.<Scp2521Tendrils>of(Scp2521Tendrils::new, MobCategory.MISC)
                    .sized(Scp2521Tendrils.Width, Scp2521Tendrils.Height)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("scp2521_tendrils"));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp2521Trip>> TripAttachment = AttachmentRegister.register("scp2521_trip",
            () -> AttachmentType.builder(() -> Scp2521Trip.None).serialize(Scp2521Trip.TripCodec, Saved -> Saved.active()).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = sound("scp2521_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RustleWet = sound("scp2521_rustle_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rise = sound("scp2521_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> RiseEarth = sound("scp2521_rise_earth");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bind = sound("scp2521_bind");
    public static final DeferredHolder<SoundEvent, SoundEvent> BindTight = sound("scp2521_bind_tight");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grab = sound("scp2521_grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrabRush = sound("scp2521_grab_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drone = sound("scp2521_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> DroneLow = sound("scp2521_drone_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stare = sound("scp2521_stare");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fall = sound("scp2521_fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> FallRush = sound("scp2521_fall_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> Return = sound("scp2521_return");
    public static final DeferredHolder<SoundEvent, SoundEvent> Absorb = sound("scp2521_absorb");
    public static final DeferredHolder<SoundEvent, SoundEvent> AbsorbPull = sound("scp2521_absorb_pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> Break = sound("scp2521_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreakTear = sound("scp2521_break_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> Anchor = sound("scp2521_anchor");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RustleLayers = List.of(Rustle, RustleWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RiseLayers = List.of(Rise, RiseEarth);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BindLayers = List.of(Bind, BindTight);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrabLayers = List.of(Grab, GrabRush);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DroneLayers = List.of(Drone, DroneLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FallLayers = List.of(Fall, FallRush);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AbsorbLayers = List.of(Absorb, AbsorbPull);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreakLayers = List.of(Break, BreakTear);

    private Scp2521Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(Scp2521KnownCondition.TypeName, Scp2521KnownCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp2521KnownCondition::manifest);
        StasisPods.addCaptureVeto(Target -> Target instanceof Scp2521 || Target instanceof Scp2521Tendrils);
    }
}
