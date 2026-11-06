package net.rai.scp_unleashed.scp007;

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

import java.util.List;

public final class Scp007Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_007");

    public static final DeferredHolder<EntityType<?>, EntityType<Scp007Entity>> Subject = EntityTypeRegister.register("scp007",
            () -> EntityType.Builder.<Scp007Entity>of(Scp007Entity::new, MobCategory.MISC)
                    .sized(Scp007Entity.Width, Scp007Entity.Height)
                    .eyeHeight(Scp007Entity.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp007"));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp007_spawn_egg",
            () -> new DeferredSpawnEggItem(Subject, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Breathe = sound("scp007_breathe");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreatheSoft = sound("scp007_breathe_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp007_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumShimmer = sound("scp007_hum_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tide = sound("scp007_tide");
    public static final DeferredHolder<SoundEvent, SoundEvent> Decline = sound("scp007_decline");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeclineChime = sound("scp007_decline_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Settle = sound("scp007_settle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SettleBreath = sound("scp007_settle_breath");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumShimmer);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WeatherLayers = List.of(Tide);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeclineLayers = List.of(Decline, DeclineChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SettleLayers = List.of(Settle, SettleBreath);

    private Scp007Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp007SightingCondition.TypeName, Scp007SightingCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp007Arrival::arrive);
    }
}
