package net.rai.scp_unleashed.scp075;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp075Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MobEffect> MobEffectRegister = DeferredRegister.create(Registries.MOB_EFFECT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_075");
    public static final ResourceKey<DamageType> CorrosionDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp075_corrosion"));
    public static final ResourceKey<DamageType> SpitDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp075_spit"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp075Snail>> Snail = EntityTypeRegister.register("scp075_snail",
            () -> EntityType.Builder.<Scp075Snail>of(Scp075Snail::new, MobCategory.MONSTER)
                    .sized(Scp075Snail.Width, Scp075Snail.Height)
                    .eyeHeight(Scp075Snail.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp075_snail"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp075Glob>> Glob = EntityTypeRegister.register("scp075_glob",
            () -> EntityType.Builder.<Scp075Glob>of(Scp075Glob::new, MobCategory.MISC)
                    .sized(Scp075Glob.Size, Scp075Glob.Size)
                    .clientTrackingRange(6)
                    .updateInterval(10)
                    .build("scp075_glob"));

    public static final DeferredItem<DeferredSpawnEggItem> SnailEgg = ItemRegister.register("scp075_snail_spawn_egg",
            () -> new DeferredSpawnEggItem(Snail, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final DeferredHolder<MobEffect, Scp075CorrosionEffect> Corrosion = MobEffectRegister.register("scp075_corrosion", Scp075CorrosionEffect::new);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Drip = ParticleRegister.register("scp075_drip", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp075_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepClick = sound("scp075_step_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ambient = sound("scp075_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> AmbientGurgle = sound("scp075_ambient_gurgle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rear = sound("scp075_rear");
    public static final DeferredHolder<SoundEvent, SoundEvent> RearSlide = sound("scp075_rear_slide");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spit = sound("scp075_spit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpitWet = sound("scp075_spit_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Splat = sound("scp075_splat");
    public static final DeferredHolder<SoundEvent, SoundEvent> SplatMud = sound("scp075_splat_mud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sizzle = sound("scp075_sizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SizzleDrip = sound("scp075_sizzle_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rinse = sound("scp075_rinse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swipe = sound("scp075_swipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwipeClick = sound("scp075_swipe_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp075_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp075_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathSquelch = sound("scp075_death_squelch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eat = sound("scp075_eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> EatCrunch = sound("scp075_eat_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Churn = sound("scp075_churn");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChurnSlosh = sound("scp075_churn_slosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Birth = sound("scp075_birth");
    public static final DeferredHolder<SoundEvent, SoundEvent> BirthSpit = sound("scp075_birth_spit");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AmbientLayers = List.of(Ambient, AmbientGurgle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RearLayers = List.of(Rear, RearSlide);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SpitLayers = List.of(Spit, SpitWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SplatLayers = List.of(Splat, SplatMud);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SizzleLayers = List.of(Sizzle, SizzleDrip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RinseLayers = List.of(Rinse, AmbientGurgle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwipeLayers = List.of(Swipe, SwipeClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathSquelch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EatLayers = List.of(Eat, EatCrunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChurnLayers = List.of(Churn, ChurnSlosh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BirthLayers = List.of(Birth, BirthSpit);

    private Scp075Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        MobEffectRegister.register(ModEventBus);
        StartConditions.register(Scp075NightfallCondition.TypeName, Scp075NightfallCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp075NightfallCondition::manifest);
    }
}
