package net.rai.scp_unleashed.stranger;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
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
import net.rai.scp_unleashed.archive.investigation.StartConditions;

public final class StrangerRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> Appear = sound("stranger_appear");
    public static final DeferredHolder<SoundEvent, SoundEvent> Vanish = sound("stranger_vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> VanishLow = sound("stranger_vanish_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> VanishChime = sound("stranger_vanish_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fade = sound("stranger_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> Blip = sound("stranger_blip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Greet = sound("stranger_greet");

    public static final DeferredHolder<EntityType<?>, EntityType<StrangerEntity>> Figure = EntityTypeRegister.register("stranger_figure",
            () -> EntityType.Builder.<StrangerEntity>of(StrangerEntity::new, MobCategory.MISC)
                    .sized(0.6f, 2.05f)
                    .eyeHeight(1.76f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("stranger_figure"));

    public static final DeferredItem<DeferredSpawnEggItem> FigureEgg = ItemRegister.register("stranger_figure_spawn_egg",
            () -> new DeferredSpawnEggItem(Figure, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Shadow = ParticleRegister.register("stranger_shadow", () -> new SimpleParticleType(true));

    private StrangerRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(StrangerEncounterCondition.TypeName, StrangerEncounterCondition.ConditionCodec);
    }
}
