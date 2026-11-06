package net.rai.scp_unleashed.scp017;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

public final class Scp017Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_017");
    public static final ResourceKey<DamageType> ConsumedDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp017_consumed"));
    public static final ResourceKey<DamageType> LightDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp017_light"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp017Shadow>> Shadow = EntityTypeRegister.register("scp017_shadow",
            () -> EntityType.Builder.<Scp017Shadow>of(Scp017Shadow::new, MobCategory.MONSTER)
                    .sized(Scp017Shadow.Width, Scp017Shadow.Height)
                    .eyeHeight(Scp017Shadow.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("scp017_shadow"));

    public static final DeferredItem<DeferredSpawnEggItem> ShadowEgg = ItemRegister.register("scp017_shadow_spawn_egg",
            () -> new DeferredSpawnEggItem(Shadow, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp017_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp017_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wake = sound("scp017_wake");
    public static final DeferredHolder<SoundEvent, SoundEvent> Settle = sound("scp017_settle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Recoil = sound("scp017_recoil");
    public static final DeferredHolder<SoundEvent, SoundEvent> RecoilCry = sound("scp017_recoil_cry");
    public static final DeferredHolder<SoundEvent, SoundEvent> Reach = sound("scp017_reach");
    public static final DeferredHolder<SoundEvent, SoundEvent> Consume = sound("scp017_consume");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pass = sound("scp017_pass");
    public static final DeferredHolder<SoundEvent, SoundEvent> Unravel = sound("scp017_unravel");
    public static final DeferredHolder<SoundEvent, SoundEvent> Emerge = sound("scp017_emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snuff = sound("scp017_snuff");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Shade = ParticleRegister.register("scp017_shade", () -> new SimpleParticleType(false));

    private Scp017Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp017NightfallCondition.TypeName, Scp017NightfallCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp017NightfallCondition::manifest);
    }
}
