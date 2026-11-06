package net.rai.scp_unleashed.scp1730.pit;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.stasis.StasisPods;

public final class Scp1730PitRegistry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<EntityType<?>, EntityType<Scp1730PitForm>> PitType = EntityTypeRegister.register("scp1730_3",
            () -> EntityType.Builder.<Scp1730PitForm>of(Scp1730PitForm::new, MobCategory.MONSTER).sized(Scp1730PitForm.Width, Scp1730PitForm.Height)
                    .eyeHeight(Scp1730PitForm.EyeHeight).fireImmune().clientTrackingRange(12).build("scp1730_3"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp1730Mass>> MassType = EntityTypeRegister.register("scp1730_3_mass",
            () -> EntityType.Builder.<Scp1730Mass>of(Scp1730Mass::new, MobCategory.MONSTER).sized(Scp1730Mass.Width, Scp1730Mass.Height)
                    .eyeHeight(Scp1730Mass.EyeHeight).fireImmune().clientTrackingRange(10).build("scp1730_3_mass"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp1730PitWhip>> WhipType = EntityTypeRegister.register("scp1730_3_whip",
            () -> EntityType.Builder.<Scp1730PitWhip>of(Scp1730PitWhip::new, MobCategory.MISC).sized(Scp1730PitWhip.Width, Scp1730PitWhip.Height)
                    .fireImmune().noSummon().clientTrackingRange(10).updateInterval(1).build("scp1730_3_whip"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp1730PitTongue>> TongueType = EntityTypeRegister.register("scp1730_3_tongue",
            () -> EntityType.Builder.<Scp1730PitTongue>of(Scp1730PitTongue::new, MobCategory.MISC).sized(0.4f, 0.4f).fireImmune().noSummon()
                    .clientTrackingRange(10).updateInterval(1).build("scp1730_3_tongue"));

    public static final DeferredItem<DeferredSpawnEggItem> PitEgg = ItemRegister.register("scp1730_3_spawn_egg",
            () -> new DeferredSpawnEggItem(PitType, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> MassEgg = ItemRegister.register("scp1730_3_mass_spawn_egg",
            () -> new DeferredSpawnEggItem(MassType, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Rumble = fixed("scp1730_3_rumble", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = fixed("scp1730_3_roar", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarLow = fixed("scp1730_3_roar_low", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Idle = sound("scp1730_3_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hiss = sound("scp1730_3_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lash = sound("scp1730_3_lash");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bite = sound("scp1730_3_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spit = sound("scp1730_3_spit");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp1730_3_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ward = sound("scp1730_3_ward");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thrash = sound("scp1730_3_thrash");
    public static final DeferredHolder<SoundEvent, SoundEvent> Daze = sound("scp1730_3_daze");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = fixed("scp1730_3_death", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Inhale = fixed("scp1730_3_inhale", 40.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Sink = sound("scp1730_3_sink");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhipRise = sound("scp1730_3_whip_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhipSlam = sound("scp1730_3_whip_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassIdle = sound("scp1730_3_mass_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassRestless = sound("scp1730_3_mass_restless");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassDig = sound("scp1730_3_mass_dig");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassBurrow = fixed("scp1730_3_mass_burrow", 40.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> MassHurt = sound("scp1730_3_mass_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassDeath = sound("scp1730_3_mass_death");

    public static final ResourceKey<DamageType> PitDamage = ResourceKey.create(Registries.DAMAGE_TYPE, Scp1730Registry.location("scp1730_3"));

    private Scp1730PitRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(Scp1730Registry.location(Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixed(String Name, float Range) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(Scp1730Registry.location(Name), Range));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StasisPods.addCaptureVeto(Scp1730PitCapture::refuses);
        StasisPods.addCaptureHook(Scp1730PitCapture::captured);
        StasisPods.addReleaseHook(Scp1730PitCapture::released);
    }
}
