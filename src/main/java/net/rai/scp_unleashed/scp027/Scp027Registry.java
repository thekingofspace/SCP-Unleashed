package net.rai.scp_unleashed.scp027;

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

public final class Scp027Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_027");

    public static final DeferredHolder<EntityType<?>, EntityType<Scp027Entity>> Subject = EntityTypeRegister.register("scp027_vermin_god",
            () -> EntityType.Builder.<Scp027Entity>of(Scp027Entity::new, MobCategory.MISC)
                    .sized(Scp027Entity.Width, Scp027Entity.Height)
                    .eyeHeight(Scp027Entity.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp027_vermin_god"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp027Rat>> Rat = EntityTypeRegister.register("scp027_rat",
            () -> EntityType.Builder.<Scp027Rat>of(Scp027Rat::new, MobCategory.MISC)
                    .sized(Scp027Rat.Width, Scp027Rat.Height)
                    .eyeHeight(Scp027Rat.EyeHeight)
                    .clientTrackingRange(8)
                    .build("scp027_rat"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp027Silverfish>> Silverfish = EntityTypeRegister.register("scp027_silverfish",
            () -> EntityType.Builder.<Scp027Silverfish>of(Scp027Silverfish::new, MobCategory.MISC)
                    .sized(0.4f, 0.3f)
                    .eyeHeight(0.13f)
                    .passengerAttachments(0.2375f)
                    .clientTrackingRange(8)
                    .build("scp027_silverfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp027Endermite>> Endermite = EntityTypeRegister.register("scp027_endermite",
            () -> EntityType.Builder.<Scp027Endermite>of(Scp027Endermite::new, MobCategory.MISC)
                    .sized(0.4f, 0.3f)
                    .eyeHeight(0.13f)
                    .passengerAttachments(0.2375f)
                    .clientTrackingRange(8)
                    .build("scp027_endermite"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp027Bat>> Bat = EntityTypeRegister.register("scp027_bat",
            () -> EntityType.Builder.<Scp027Bat>of(Scp027Bat::new, MobCategory.MISC)
                    .sized(0.5f, 0.9f)
                    .eyeHeight(0.45f)
                    .clientTrackingRange(5)
                    .build("scp027_bat"));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp027_spawn_egg",
            () -> new DeferredSpawnEggItem(Subject, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Shuffle = sound("scp027_shuffle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShuffleGrit = sound("scp027_shuffle_grit");
    public static final DeferredHolder<SoundEvent, SoundEvent> Mumble = sound("scp027_mumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> MumbleLow = sound("scp027_mumble_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swarm = sound("scp027_swarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwarmWings = sound("scp027_swarm_wings");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flies = sound("scp027_flies");
    public static final DeferredHolder<SoundEvent, SoundEvent> FliesWings = sound("scp027_flies_wings");
    public static final DeferredHolder<SoundEvent, SoundEvent> Summon = sound("scp027_summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> SummonSkitter = sound("scp027_summon_skitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Soak = sound("scp027_soak");
    public static final DeferredHolder<SoundEvent, SoundEvent> SoakSqueal = sound("scp027_soak_squeal");
    public static final DeferredHolder<SoundEvent, SoundEvent> Scatter = sound("scp027_scatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScatterWings = sound("scp027_scatter_wings");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spoil = sound("scp027_spoil");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wilt = sound("scp027_wilt");
    public static final DeferredHolder<SoundEvent, SoundEvent> RatSqueak = sound("scp027_rat_squeak");
    public static final DeferredHolder<SoundEvent, SoundEvent> RatHurt = sound("scp027_rat_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> RatDeath = sound("scp027_rat_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> RatStep = sound("scp027_rat_step");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShuffleLayers = List.of(Shuffle, ShuffleGrit);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MumbleLayers = List.of(Mumble, MumbleLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwarmLayers = List.of(Swarm, SwarmWings);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FlyLayers = List.of(Flies, FliesWings);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SummonLayers = List.of(Summon, SummonSkitter);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SoakLayers = List.of(Soak, SoakSqueal);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ScatterLayers = List.of(Scatter, ScatterWings);

    private Scp027Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp027SightingCondition.TypeName, Scp027SightingCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp027Arrival::arrive);
    }
}
