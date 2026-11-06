package net.rai.scp_unleashed.scp035;

import com.mojang.serialization.Codec;
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
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.storage.StorageBehaviours;

public final class Scp035Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_035");
    public static final TagKey<EntityType<?>> PersonLike = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "person_like"));

    public static final DeferredItem<Scp035MaskItem> MaskItem = ItemRegister.register("scp035_mask",
            () -> new Scp035MaskItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> Worn = AttachmentRegister.register("scp035_worn",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    public static final DeferredHolder<EntityType<?>, EntityType<Scp035Mask>> Mask = EntityTypeRegister.register("scp035_mask",
            () -> EntityType.Builder.<Scp035Mask>of(Scp035Mask::new, MobCategory.MISC)
                    .sized(Scp035Mask.Width, Scp035Mask.Height)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("scp035_mask"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp035Host>> Host = EntityTypeRegister.register("scp035_host",
            () -> EntityType.Builder.<Scp035Host>of(Scp035Host::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("scp035_host"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp035_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperLow = sound("scp035_whisper_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heartbeat = sound("scp035_heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Latch = sound("scp035_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> LatchVoice = sound("scp035_latch_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> HelmetCrack = sound("scp035_helmet_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lift = sound("scp035_lift");
    public static final DeferredHolder<SoundEvent, SoundEvent> SetDown = sound("scp035_set");
    public static final DeferredHolder<SoundEvent, SoundEvent> Compel = sound("scp035_compel");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rise = sound("scp035_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostIdle = sound("scp035_host_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostHurt = sound("scp035_host_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostDeath = sound("scp035_host_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("scp035_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Strike = sound("scp035_strike");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Weep = ParticleRegister.register("scp035_weep", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Glint = ParticleRegister.register("scp035_glint", () -> new SimpleParticleType(false));

    private Scp035Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StorageBehaviours.register(Scp035MaskItem::is, Scp035Containment.Instance);
        StartConditions.register(Scp035AppearanceCondition.TypeName, Scp035AppearanceCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp035AppearanceCondition::manifest);
    }
}
