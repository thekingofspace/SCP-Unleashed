package net.rai.scp_unleashed.scp539;

import net.minecraft.core.registries.Registries;
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
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp539Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_539");
    public static final ResourceKey<DamageType> HelicopterDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp539_helicopter"));

    public static final DeferredItem<Scp539StoneItem> Stone = ItemRegister.register("scp539_stone",
            () -> new Scp539StoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp539Stone>> StoneEntity = EntityTypeRegister.register("scp539_stone",
            () -> EntityType.Builder.<Scp539Stone>of(Scp539Stone::new, MobCategory.MISC)
                    .sized(0.3f, 0.2f)
                    .eyeHeight(0.1f)
                    .fireImmune()
                    .noSummon()
                    .clientTrackingRange(8)
                    .updateInterval(4)
                    .build("scp539_stone"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp539Helicopter>> HelicopterEntity = EntityTypeRegister.register("scp539_helicopter",
            () -> EntityType.Builder.<Scp539Helicopter>of(Scp539Helicopter::new, MobCategory.MISC)
                    .sized(3.0f, 2.4f)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("scp539_helicopter"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Throw = sound("scp539_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThrowFlick = sound("scp539_throw_flick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Skip = sound("scp539_skip");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkipTap = sound("scp539_skip_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Land = sound("scp539_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> LandTick = sound("scp539_land_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Notice = sound("scp539_notice");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoticeGlint = sound("scp539_notice_glint");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rotor = sound("scp539_helicopter_rotor");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thump = sound("scp539_helicopter_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whine = sound("scp539_helicopter_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bonk = sound("scp539_helicopter_bonk");
    public static final DeferredHolder<SoundEvent, SoundEvent> BonkClang = sound("scp539_helicopter_bonk_clang");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crash = sound("scp539_helicopter_crash");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrashCrunch = sound("scp539_helicopter_crash_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrashRattle = sound("scp539_helicopter_crash_rattle");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ThrowLayers = List.of(Throw, ThrowFlick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SkipLayers = List.of(Skip, SkipTap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LandLayers = List.of(Land, LandTick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> NoticeLayers = List.of(Notice, NoticeGlint);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ArrivalLayers = List.of(Rotor, Whine);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BonkLayers = List.of(Bonk, BonkClang);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrashLayers = List.of(Crash, CrashCrunch, CrashRattle);

    private Scp539Registry() {
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
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
