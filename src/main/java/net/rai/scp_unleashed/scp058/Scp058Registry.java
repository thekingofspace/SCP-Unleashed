package net.rai.scp_unleashed.scp058;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
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

import java.util.List;

public final class Scp058Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_058");
    public static final TagKey<EntityType<?>> PersonLike = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "person_like"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp058Heart>> Heart = EntityTypeRegister.register("scp058_heart",
            () -> EntityType.Builder.<Scp058Heart>of(Scp058Heart::new, MobCategory.MONSTER)
                    .sized(Scp058Heart.Width, Scp058Heart.Height)
                    .eyeHeight(Scp058Heart.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp058_heart"));

    public static final DeferredItem<DeferredSpawnEggItem> HeartEgg = ItemRegister.register("scp058_heart_spawn_egg",
            () -> new DeferredSpawnEggItem(Heart, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Heartbeat = sound("scp058_heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> HeartbeatLow = sound("scp058_heartbeat_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp058_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperVoices = sound("scp058_whisper_voices");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp058_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepClick = sound("scp058_step_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lash = sound("scp058_lash");
    public static final DeferredHolder<SoundEvent, SoundEvent> LashCrack = sound("scp058_lash_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sting = sound("scp058_sting");
    public static final DeferredHolder<SoundEvent, SoundEvent> StingPierce = sound("scp058_sting_pierce");
    public static final DeferredHolder<SoundEvent, SoundEvent> Leap = sound("scp058_leap");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeapHiss = sound("scp058_leap_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp058_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp058_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathThump = sound("scp058_death_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> Emerge = sound("scp058_emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmergeTear = sound("scp058_emerge_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmergeShriek = sound("scp058_emerge_shriek");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EmergeLayers = List.of(Emerge, EmergeTear, HeartbeatLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HeartbeatLayers = List.of(Heartbeat, HeartbeatLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhisperLayers = List.of(Whisper, WhisperVoices);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LashLayers = List.of(Lash, LashCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StingLayers = List.of(Sting, StingPierce);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LeapLayers = List.of(Leap, LeapHiss);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Mote = ParticleRegister.register("scp058_mote", () -> new SimpleParticleType(false));

    private Scp058Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp058CowCondition.TypeName, Scp058CowCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp058Emergence::manifest);
    }
}
