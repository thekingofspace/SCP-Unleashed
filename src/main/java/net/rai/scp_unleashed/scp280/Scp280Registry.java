package net.rai.scp_unleashed.scp280;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
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
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.List;

public final class Scp280Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_280");
    public static final ResourceKey<DamageType> ShadeDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp280_shade"));
    public static final ResourceKey<DamageType> LightDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp280_light"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp280Shade>> Shade = EntityTypeRegister.register("scp280_shade",
            () -> EntityType.Builder.<Scp280Shade>of(Scp280Shade::new, MobCategory.MONSTER)
                    .sized(Scp280Shade.Width, Scp280Shade.Height)
                    .eyeHeight(Scp280Shade.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("scp280_shade"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp280Eyes>> Eyes = EntityTypeRegister.register("scp280_eyes",
            () -> EntityType.Builder.<Scp280Eyes>of(Scp280Eyes::new, MobCategory.MISC)
                    .sized(Scp280Eyes.Width, Scp280Eyes.Height)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .clientTrackingRange(4)
                    .build("scp280_eyes"));

    public static final DeferredItem<DeferredSpawnEggItem> ShadeEgg = ItemRegister.register("scp280_shade_spawn_egg",
            () -> new DeferredSpawnEggItem(Shade, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Wisp = ParticleRegister.register("scp280_wisp", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp280_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp280_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Blink = sound("scp280_blink");
    public static final DeferredHolder<SoundEvent, SoundEvent> BlinkWet = sound("scp280_blink_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sting = sound("scp280_sting");
    public static final DeferredHolder<SoundEvent, SoundEvent> StingLow = sound("scp280_sting_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Emerge = sound("scp280_emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmergeAir = sound("scp280_emerge_air");
    public static final DeferredHolder<SoundEvent, SoundEvent> Murmur = sound("scp280_murmur");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp280_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> Windup = sound("scp280_windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> Strike = sound("scp280_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeHiss = sound("scp280_strike_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Recoil = sound("scp280_recoil");
    public static final DeferredHolder<SoundEvent, SoundEvent> RecoilCry = sound("scp280_recoil_cry");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pass = sound("scp280_pass");
    public static final DeferredHolder<SoundEvent, SoundEvent> Huddle = sound("scp280_huddle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Contained = sound("scp280_contained");
    public static final DeferredHolder<SoundEvent, SoundEvent> ContainedLow = sound("scp280_contained_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breach = sound("scp280_breach");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dissipate = sound("scp280_dissipate");
    public static final DeferredHolder<SoundEvent, SoundEvent> DissipateAir = sound("scp280_dissipate_air");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SightingLayers = List.of(Whisper, Breath);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BlinkLayers = List.of(Blink, BlinkWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StingLayers = List.of(Sting, StingLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EmergeLayers = List.of(Emerge, EmergeAir);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StrikeLayers = List.of(Strike, StrikeHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RecoilLayers = List.of(Recoil, RecoilCry);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ContainedLayers = List.of(Contained, ContainedLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DissipateLayers = List.of(Dissipate, DissipateAir);

    private Scp280Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp280EyesCondition.TypeName, Scp280EyesCondition.ConditionCodec);
        StasisPods.addReleaseHook((World, Spawned) -> {
            if (Spawned instanceof Scp280Shade Shade) {
                Shade.leaveStasis(World);
            }
        });
    }
}
