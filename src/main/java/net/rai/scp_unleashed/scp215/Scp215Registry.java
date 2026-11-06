package net.rai.scp_unleashed.scp215;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp215Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_215");

    public static final DeferredHolder<SoundEvent, SoundEvent> GlassesOn = near("scp215_glasses_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = far("scp215_whisper", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperLow = far("scp215_whisper_low", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Click = far("scp215_click", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> ClickSnap = far("scp215_click_snap", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Scuttle = far("scp215_scuttle", 32.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> ScuttleTap = far("scp215_scuttle_tap", 32.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = far("scp215_rustle", 32.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Vanish = far("scp215_vanish", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> VanishAir = far("scp215_vanish_air", 24.0f);

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhisperLayers = List.of(Whisper, WhisperLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ClickLayers = List.of(Click, ClickSnap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ScuttleLayers = List.of(Scuttle, ScuttleTap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RustleLayers = List.of(Rustle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> VanishLayers = List.of(Vanish, VanishAir);

    public static final DeferredHolder<EntityType<?>, EntityType<Scp215Phantom>> Phantom = EntityTypeRegister.register("scp215_phantom",
            () -> EntityType.Builder.<Scp215Phantom>of(Scp215Phantom::new, MobCategory.MISC)
                    .sized(Scp215Variant.Lurker.Width, Scp215Variant.Lurker.Height)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(0)
                    .updateInterval(20)
                    .build("scp215_phantom"));

    public static final DeferredItem<Scp215GlassesItem> Glasses = ItemRegister.register("scp215_glasses",
            () -> new Scp215GlassesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private Scp215Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> near(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> far(String Name, float Range) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), Range));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
