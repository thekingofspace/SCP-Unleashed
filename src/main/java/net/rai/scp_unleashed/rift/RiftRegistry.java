package net.rai.scp_unleashed.rift;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class RiftRegistry {
    public static final String SpawningRuleName = "scpRiftSpawning";
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<EntityType<?>, EntityType<RiftEntity>> Rift = EntityTypeRegister.register("rift",
            () -> EntityType.Builder.<RiftEntity>of(RiftEntity::new, MobCategory.MISC)
                    .sized(0.5f, 1.2f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("rift"));

    public static final DeferredHolder<SoundEvent, SoundEvent> RiftHum = sound("rift_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> RiftCollapse = sound("rift_collapse");

    public static final DeferredHolder<ParticleType<?>, ParticleType<RiftParticleOptions>> RiftMote = particle("rift_mote");
    public static final DeferredHolder<ParticleType<?>, ParticleType<RiftParticleOptions>> RiftSpark = particle("rift_spark");
    public static final DeferredHolder<ParticleType<?>, ParticleType<RiftParticleOptions>> RiftShockwave = particle("rift_shockwave");

    public static GameRules.Key<GameRules.BooleanValue> SpawningRule;

    private RiftRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(RiftRegistry::registerRules));
    }

    private static void registerRules() {
        if (SpawningRule == null) {
            SpawningRule = GameRules.register(SpawningRuleName, GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean spawning(GameRules Rules) {
        if (SpawningRule == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Rules.getRule(SpawningRule);
        return Rule == null || Rule.get();
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static DeferredHolder<ParticleType<?>, ParticleType<RiftParticleOptions>> particle(String Name) {
        return ParticleRegister.register(Name, () -> new ParticleType<RiftParticleOptions>(false) {
            private final MapCodec<RiftParticleOptions> DataCodec = RiftParticleOptions.codec(this);
            private final StreamCodec<RegistryFriendlyByteBuf, RiftParticleOptions> NetworkCodec = RiftParticleOptions.streamCodec(this);

            @Override
            public MapCodec<RiftParticleOptions> codec() {
                return DataCodec;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, RiftParticleOptions> streamCodec() {
                return NetworkCodec;
            }
        });
    }
}
