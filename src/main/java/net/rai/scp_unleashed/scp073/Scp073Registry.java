package net.rai.scp_unleashed.scp073;

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
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp073Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_073");
    public static final ResourceKey<DamageType> ReflectedDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp073_reflected"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp073Cain>> Cain = EntityTypeRegister.register("scp073_cain",
            () -> EntityType.Builder.<Scp073Cain>of(Scp073Cain::new, MobCategory.MISC)
                    .sized(Scp073Cain.Width, Scp073Cain.Height)
                    .eyeHeight(Scp073Cain.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("scp073_cain"));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp073_spawn_egg",
            () -> new DeferredSpawnEggItem(Cain, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Glint = ParticleRegister.register("scp073_glint", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Flash = ParticleRegister.register("scp073_flash", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Leaf = ParticleRegister.register("scp073_leaf", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> Reflect = sound("scp073_reflect");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReflectRing = sound("scp073_reflect_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReflectChime = sound("scp073_reflect_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Recoil = sound("scp073_recoil");
    public static final DeferredHolder<SoundEvent, SoundEvent> RecoilCrack = sound("scp073_recoil_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wither = sound("scp073_wither");
    public static final DeferredHolder<SoundEvent, SoundEvent> WitherDust = sound("scp073_wither_dust");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fade = sound("scp073_fade");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReflectLayers = List.of(Reflect, ReflectRing, ReflectChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RecoilLayers = List.of(Recoil, RecoilCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WitherLayers = List.of(Wither, WitherDust);

    private Scp073Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp073DesertCondition.TypeName, Scp073DesertCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp073Arrival::arrive);
    }
}
