package net.rai.scp_unleashed.scp131;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
import net.rai.scp_unleashed.storage.StorageBehaviours;

import java.util.List;

public final class Scp131Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_131");

    public static final DeferredHolder<EntityType<?>, EntityType<Scp131Pod>> Pod = EntityTypeRegister.register("scp131_pod",
            () -> EntityType.Builder.<Scp131Pod>of(Scp131Pod::new, MobCategory.CREATURE)
                    .sized(Scp131Pod.Width, Scp131Pod.Height)
                    .eyeHeight(Scp131Pod.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp131_pod"));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp131PodData>> PodData = ComponentRegister.registerComponentType("scp131_pod",
            Builder -> Builder.persistent(Scp131PodData.PodCodec).networkSynchronized(Scp131PodData.PodStreamCodec));

    public static final DeferredItem<Scp131PodItem> PodItem = ItemRegister.register("scp131_eye_pod",
            () -> new Scp131PodItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> PodEgg = ItemRegister.register("scp131_pod_spawn_egg",
            () -> new DeferredSpawnEggItem(Pod, 0xF0645A, 0x5EA2F0, new Item.Properties()));

    public static final DeferredHolder<SoundEvent, SoundEvent> Chirp = sound("scp131_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChirpRing = sound("scp131_chirp_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Trill = sound("scp131_trill");
    public static final DeferredHolder<SoundEvent, SoundEvent> TrillRise = sound("scp131_trill_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> Zoom = sound("scp131_zoom");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZoomPip = sound("scp131_zoom_pip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Greet = sound("scp131_greet");
    public static final DeferredHolder<SoundEvent, SoundEvent> GreetReply = sound("scp131_greet_reply");
    public static final DeferredHolder<SoundEvent, SoundEvent> Purr = sound("scp131_purr");
    public static final DeferredHolder<SoundEvent, SoundEvent> PurrChirp = sound("scp131_purr_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pickup = sound("scp131_pickup");
    public static final DeferredHolder<SoundEvent, SoundEvent> PickupPop = sound("scp131_pickup_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Release = sound("scp131_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReleasePop = sound("scp131_release_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp131_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtSqueal = sound("scp131_hurt_squeal");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp131_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathToll = sound("scp131_death_toll");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sad = sound("scp131_sad");
    public static final DeferredHolder<SoundEvent, SoundEvent> SadLow = sound("scp131_sad_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whirr = sound("scp131_whirr");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bump = sound("scp131_bump");
    public static final DeferredHolder<SoundEvent, SoundEvent> BumpTap = sound("scp131_bump_tap");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChirpLayers = List.of(Chirp, ChirpRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TrillLayers = List.of(Trill, TrillRise);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ZoomLayers = List.of(Zoom, ZoomPip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GreetLayers = List.of(Greet, ChirpRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReplyLayers = List.of(GreetReply, ChirpRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PurrLayers = List.of(Purr, PurrChirp);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PickupLayers = List.of(Pickup, PickupPop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReleaseLayers = List.of(Release, ReleasePop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtSqueal);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathToll);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SadLayers = List.of(Sad, SadLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BumpLayers = List.of(Bump, BumpTap);

    private Scp131Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        StartConditions.register(Scp131ArrivalCondition.TypeName, Scp131ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp131ArrivalCondition::manifest);
        StorageBehaviours.register(Scp131PodItem::is, Scp131Locker.Instance);
    }
}
