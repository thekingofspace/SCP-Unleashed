package net.rai.scp_unleashed.scp018;

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
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp018Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceKey<DamageType> ImpactDamage = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp018_impact"));

    public static final DeferredItem<Scp018BallItem> BallItem = ItemRegister.register("scp018_ball",
            () -> new Scp018BallItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp018Ball>> BallEntity = EntityTypeRegister.register("scp018_ball",
            () -> EntityType.Builder.<Scp018Ball>of(Scp018Ball::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .eyeHeight(0.125f)
                    .fireImmune()
                    .noSummon()
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("scp018_ball"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Bounce = sound("scp018_ball_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> BounceSquish = sound("scp018_ball_bounce_squish");
    public static final DeferredHolder<SoundEvent, SoundEvent> BounceRing = sound("scp018_ball_bounce_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> BounceSnap = sound("scp018_ball_bounce_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whoosh = sound("scp018_ball_whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Throw = sound("scp018_ball_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> Catch = sound("scp018_ball_catch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hit = sound("scp018_ball_hit");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Trail = ParticleRegister.register("scp018_trail", () -> new SimpleParticleType(false));

    private Scp018Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        Scp018Stranger.register();
    }
}
