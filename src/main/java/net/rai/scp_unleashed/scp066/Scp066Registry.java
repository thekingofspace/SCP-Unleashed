package net.rai.scp_unleashed.scp066;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

public final class Scp066Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_066");
    public static final TagKey<EntityType<?>> PersonLike = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "person_like"));

    public static final int CandyNutrition = 2;
    public static final float CandySaturation = 0.1f;
    public static final int CupcakeNutrition = 5;
    public static final float CupcakeSaturation = 0.5f;
    public static final int CupcakeCheerTicks = 80;

    public static final DeferredHolder<EntityType<?>, EntityType<Scp066Entity>> Yarn = EntityTypeRegister.register("scp066",
            () -> EntityType.Builder.<Scp066Entity>of(Scp066Entity::new, MobCategory.MISC)
                    .sized(Scp066Entity.Width, Scp066Entity.Height)
                    .eyeHeight(Scp066Entity.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp066"));

    public static final DeferredItem<Scp066BallItem> BallItem = ItemRegister.register("scp066_ball",
            () -> new Scp066BallItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp066_spawn_egg",
            () -> new DeferredSpawnEggItem(Yarn, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<Scp066TreatItem> Candy = ItemRegister.register("scp066_candy",
            () -> new Scp066TreatItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(CandyNutrition).saturationModifier(CandySaturation)
                    .fast().alwaysEdible().build())));
    public static final DeferredItem<Scp066TreatItem> Cupcake = ItemRegister.register("scp066_cupcake",
            () -> new Scp066TreatItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(CupcakeNutrition).saturationModifier(CupcakeSaturation)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, CupcakeCheerTicks, 0), 1.0f).build())));

    public static final DeferredHolder<SoundEvent, SoundEvent> TuneHarp = sound("scp066_tune_harp");
    public static final DeferredHolder<SoundEvent, SoundEvent> TuneBell = sound("scp066_tune_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> TuneChime = sound("scp066_tune_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> TuneFlute = sound("scp066_tune_flute");
    public static final DeferredHolder<SoundEvent, SoundEvent> TuneXylophone = sound("scp066_tune_xylophone");
    public static final DeferredHolder<SoundEvent, SoundEvent> StabBass = sound("scp066_stab_bass");
    public static final DeferredHolder<SoundEvent, SoundEvent> StabDidgeridoo = sound("scp066_stab_didgeridoo");
    public static final DeferredHolder<SoundEvent, SoundEvent> StabHarp = sound("scp066_stab_harp");
    public static final DeferredHolder<SoundEvent, SoundEvent> StabBell = sound("scp066_stab_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> StabPling = sound("scp066_stab_pling");
    public static final DeferredHolder<SoundEvent, SoundEvent> StabXylophone = sound("scp066_stab_xylophone");
    public static final DeferredHolder<SoundEvent, SoundEvent> StabDrum = sound("scp066_stab_drum");
    public static final DeferredHolder<SoundEvent, SoundEvent> StabAnvil = sound("scp066_stab_anvil");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChordBass = sound("scp066_chord_bass");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChordDidgeridoo = sound("scp066_chord_didgeridoo");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChordHarp = sound("scp066_chord_harp");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChordBell = sound("scp066_chord_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChordPling = sound("scp066_chord_pling");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChordGuitar = sound("scp066_chord_guitar");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChordDrum = sound("scp066_chord_drum");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChordAnvil = sound("scp066_chord_anvil");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shake = sound("scp066_shake");
    public static final DeferredHolder<SoundEvent, SoundEvent> Roll = sound("scp066_roll");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bump = sound("scp066_bump");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp066_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp066_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TreatPop = sound("scp066_treat_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Scoop = sound("scp066_scoop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Place = sound("scp066_place");

    private Scp066Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
