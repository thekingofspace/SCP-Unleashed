package net.rai.scp_unleashed.scp427;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp427Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_427");
    public static final ResourceKey<DamageType> Overcome = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp427_overcome"));
    public static final ResourceKey<DamageType> Maul = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp427_maul"));

    public static final DeferredItem<Scp427LocketItem> Locket = ItemRegister.register("scp427_locket",
            () -> new Scp427LocketItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp427Flesh>> Flesh = EntityTypeRegister.register("scp427_1",
            () -> EntityType.Builder.<Scp427Flesh>of(Scp427Flesh::new, MobCategory.MONSTER)
                    .sized(Scp427Flesh.Width, Scp427Flesh.Height)
                    .eyeHeight(Scp427Flesh.EyeHeight)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("scp427_1"));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp427_1_spawn_egg",
            () -> new DeferredSpawnEggItem(Flesh, 0xD49A90, 0x16141A, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp427State>> Exposure = AttachmentRegister.register("scp427_exposure",
            () -> AttachmentType.builder(() -> new Scp427State()).serialize(Scp427State.StateCodec, Scp427State::worthSaving).build());

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MoteParticle = ParticleRegister.register("scp427_mote", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FleshParticle = ParticleRegister.register("scp427_flesh", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> Open = sound("scp427_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> OpenCreak = sound("scp427_open_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Close = sound("scp427_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> CloseClick = sound("scp427_close_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp427_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chime = sound("scp427_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Itch = sound("scp427_itch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heartbeat = sound("scp427_heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Transform = sound("scp427_transform");
    public static final DeferredHolder<SoundEvent, SoundEvent> TransformCreak = sound("scp427_transform_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> TransformRoar = sound("scp427_transform_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> Idle = sound("scp427_1_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> IdleCreak = sound("scp427_1_idle_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = sound("scp427_1_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarLow = sound("scp427_1_roar_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp427_1_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtCreak = sound("scp427_1_hurt_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp427_1_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathCreak = sound("scp427_1_death_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lunge = sound("scp427_1_lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LungeBreath = sound("scp427_1_lunge_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slam = sound("scp427_1_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> Impact = sound("scp427_1_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> ImpactCreak = sound("scp427_1_impact_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp427_1_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> Knuckle = sound("scp427_1_knuckle");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(Open, OpenCreak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CloseLayers = List.of(Close, CloseClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TransformLayers = List.of(Transform, TransformCreak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RoarLayers = List.of(Roar, RoarLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LungeLayers = List.of(Lunge, LungeBreath);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ImpactLayers = List.of(Impact, ImpactCreak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathCreak);

    private Scp427Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void play(Level World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Volume, float Pitch) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        float Varied = Pitch * (0.95f + Server.getRandom().nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Server.playSound(null, At.x, At.y, At.z, Layer.get(), Source, Volume, Varied);
        }
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
