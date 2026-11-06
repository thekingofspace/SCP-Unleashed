package net.rai.scp_unleashed.scp105;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
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

import java.util.List;

public final class Scp105Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_105");

    public static final DeferredHolder<EntityType<?>, EntityType<Scp105Iris>> Iris = EntityTypeRegister.register("scp105_iris",
            () -> EntityType.Builder.<Scp105Iris>of(Scp105Iris::new, MobCategory.MISC)
                    .sized(Scp105Iris.Width, Scp105Iris.Height)
                    .eyeHeight(Scp105Iris.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp105_iris"));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp105Photo>> PhotoComponent = DataComponentRegister.registerComponentType("scp105_photo",
            Builder -> Builder.persistent(Scp105Photo.PhotoCodec).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Scp105Photo.PhotoCodec)));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp105_spawn_egg",
            () -> new DeferredSpawnEggItem(Iris, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Scp105PhotographItem> Photograph = ItemRegister.register("scp105_photograph",
            () -> new Scp105PhotographItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Flash = ParticleRegister.register("scp105_flash", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Scrap = ParticleRegister.register("scp105_scrap", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> Shutter = sound("scp105_shutter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShutterClick = sound("scp105_shutter_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShutterRing = sound("scp105_shutter_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eject = sound("scp105_eject");
    public static final DeferredHolder<SoundEvent, SoundEvent> EjectSlide = sound("scp105_eject_slide");
    public static final DeferredHolder<SoundEvent, SoundEvent> Give = sound("scp105_give");
    public static final DeferredHolder<SoundEvent, SoundEvent> GiveFlip = sound("scp105_give_flip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Peek = sound("scp105_peek");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tear = sound("scp105_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> TearRip = sound("scp105_tear_rip");
    public static final DeferredHolder<SoundEvent, SoundEvent> TearCrackle = sound("scp105_tear_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Appear = sound("scp105_appear");
    public static final DeferredHolder<SoundEvent, SoundEvent> AppearShimmer = sound("scp105_appear_shimmer");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShutterLayers = List.of(Shutter, ShutterClick, ShutterRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EjectLayers = List.of(Eject, EjectSlide);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GiveLayers = List.of(Give, GiveFlip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TearLayers = List.of(Tear, TearRip, TearCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AppearLayers = List.of(Appear, AppearShimmer);

    private Scp105Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp105SightingCondition.TypeName, Scp105SightingCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp105Arrival::arrive);
    }
}
