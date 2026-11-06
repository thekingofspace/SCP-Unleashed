package net.rai.scp_unleashed.scp513;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.storage.StorageBehaviours;
import net.rai.scp_unleashed.storage.gel.GelStorageRegistry;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp513Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_513");
    public static final ResourceKey<DamageType> Strangled = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp513_strangled"));
    public static final float StalkerWidth = 0.9f;
    public static final float StalkerHeight = 2.7f;

    public static final DeferredHolder<SoundEvent, SoundEvent> Ring = sound("scp513_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> RingShiver = sound("scp513_ring_shiver");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thunk = sound("scp513_thunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThunkRattle = sound("scp513_thunk_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> TapeTear = sound("scp513_tape_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> TapeSnap = sound("scp513_tape_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heartbeat = sound("scp513_heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> HeartbeatThump = sound("scp513_heartbeat_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dread = sound("scp513_dread");
    public static final DeferredHolder<SoundEvent, SoundEvent> DreadMoan = sound("scp513_dread_moan");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp513_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp513_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepDrag = sound("scp513_step_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grab = sound("scp513_grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrabCloth = sound("scp513_grab_cloth");
    public static final DeferredHolder<SoundEvent, SoundEvent> Choke = sound("scp513_choke");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChokeRasp = sound("scp513_choke_rasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChokeCreak = sound("scp513_choke_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Miasma = sound("scp513_miasma");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gasp = sound("scp513_gasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> GaspCatch = sound("scp513_gasp_catch");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RingLayers = List.of(Ring, RingShiver);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ThunkLayers = List.of(Thunk, ThunkRattle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TapeLayers = List.of(TapeTear, TapeSnap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HeartLayers = List.of(Heartbeat, HeartbeatThump);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DreadLayers = List.of(Dread, DreadMoan);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepLayers = List.of(Step, StepDrag);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrabLayers = List.of(Grab, GrabCloth);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChokeLayers = List.of(Choke, ChokeRasp, ChokeCreak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GaspLayers = List.of(Gasp, GaspCatch);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> Tape = ComponentRegister.registerComponentType("scp513_tape",
            Builder -> Builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> NextRing = ComponentRegister.registerComponentType("scp513_next_ring",
            Builder -> Builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredItem<Scp513BellItem> Bell = ItemRegister.register("scp513_bell",
            () -> new Scp513BellItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp513Stalker>> Stalker = EntityTypeRegister.register("scp513_stalker",
            () -> EntityType.Builder.<Scp513Stalker>of(Scp513Stalker::new, MobCategory.MISC).sized(StalkerWidth, StalkerHeight).noSummon().noSave().fireImmune()
                    .clientTrackingRange(0).updateInterval(20).build("scp513_stalker"));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp513State>> Affliction = AttachmentRegister.register("scp513_affliction",
            () -> AttachmentType.builder(() -> Scp513State.Fresh).serialize(Scp513State.StateCodec).build());

    private Scp513Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void playLayers(Level World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        GelStorageRegistry.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        StorageBehaviours.register(Scp513BellItem::is, Scp513Contained.Instance);
    }
}
