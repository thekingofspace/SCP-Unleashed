package net.rai.scp_unleashed.scp323;

import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
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
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.storage.StorageBehaviours;

import java.util.List;
import java.util.UUID;

public final class Scp323Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_323");
    public static final ResourceKey<DamageType> Claw = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp323_claw"));
    public static final ResourceKey<DamageType> Severed = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp323_severed"));

    public static final DeferredItem<Scp323SkullItem> SkullItem = ItemRegister.register("scp323_skull",
            () -> new Scp323SkullItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp323Skull>> Skull = EntityTypeRegister.register("scp323_skull",
            () -> EntityType.Builder.<Scp323Skull>of(Scp323Skull::new, MobCategory.MISC)
                    .sized(Scp323Skull.Width, Scp323Skull.Height)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("scp323_skull"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp323Beast>> Beast = EntityTypeRegister.register("scp323",
            () -> EntityType.Builder.<Scp323Beast>of(Scp323Beast::new, MobCategory.MONSTER)
                    .sized(Scp323Beast.Width, Scp323Beast.Height)
                    .eyeHeight(Scp323Beast.EyeHeight)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("scp323"));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp323_spawn_egg",
            () -> new DeferredSpawnEggItem(Beast, 0x1C1918, 0xD8CBA4, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp323Bond>> Bond = AttachmentRegister.register("scp323_bond",
            () -> AttachmentType.builder(() -> Scp323Bond.None).serialize(Scp323Bond.BondCodec, Saved -> Saved.Stage() > 0).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UUID>> Watch = AttachmentRegister.register("scp323_watch",
            () -> AttachmentType.builder(() -> Util.NIL_UUID).serialize(UUIDUtil.CODEC, Saved -> !Util.NIL_UUID.equals(Saved)).copyOnDeath().build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp323_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperLow = sound("scp323_whisper_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heartbeat = sound("scp323_heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Compel = sound("scp323_compel");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lift = sound("scp323_lift");
    public static final DeferredHolder<SoundEvent, SoundEvent> SetDown = sound("scp323_set");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("scp323_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fuse = sound("scp323_fuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> FuseCreak = sound("scp323_fuse_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Morph = sound("scp323_morph");
    public static final DeferredHolder<SoundEvent, SoundEvent> MorphClick = sound("scp323_morph_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Retract = sound("scp323_retract");
    public static final DeferredHolder<SoundEvent, SoundEvent> RetractClick = sound("scp323_retract_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Idle = sound("scp323_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> IdleCreak = sound("scp323_idle_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cry = sound("scp323_cry");
    public static final DeferredHolder<SoundEvent, SoundEvent> CryLow = sound("scp323_cry_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Howl = sound("scp323_howl");
    public static final DeferredHolder<SoundEvent, SoundEvent> HowlLow = sound("scp323_howl_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swipe = sound("scp323_swipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwipeCreak = sound("scp323_swipe_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp323_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtCreak = sound("scp323_hurt_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp323_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathCrack = sound("scp323_death_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp323_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepHeavy = sound("scp323_step_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eject = sound("scp323_eject");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FuseLayers = List.of(Fuse, FuseCreak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MorphLayers = List.of(Morph, MorphClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RetractLayers = List.of(Retract, RetractClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CryLayers = List.of(Cry, CryLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HowlLayers = List.of(Howl, HowlLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwipeLayers = List.of(Swipe, SwipeCreak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathCrack);

    private Scp323Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StorageBehaviours.register(Scp323SkullItem::is, Scp323Containment.Instance);
        StartConditions.register(Scp323AppearanceCondition.TypeName, Scp323AppearanceCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp323AppearanceCondition::manifest);
        StasisPods.addCaptureVeto(Scp323Bond::isBound);
    }
}
