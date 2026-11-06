package net.rai.scp_unleashed.scp3008.staff;

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

import java.util.List;

public final class Scp3008StaffRegistry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final String StaffId = "scp3008_staff";
    public static final int TrackingRange = 10;

    public static final DeferredHolder<EntityType<?>, EntityType<Scp3008Staff>> Staff = EntityTypeRegister.register(StaffId,
            () -> EntityType.Builder.<Scp3008Staff>of(Scp3008Staff::new, MobCategory.MONSTER)
                    .sized(Scp3008StaffVariant.Lump.Width, Scp3008StaffVariant.Lump.Height)
                    .eyeHeight(Scp3008StaffVariant.Lump.Eye)
                    .clientTrackingRange(TrackingRange)
                    .build(StaffId));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp3008_staff_spawn_egg",
            () -> new DeferredSpawnEggItem(Staff, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp3008_staff_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepShuffle = sound("scp3008_staff_step_shuffle");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepSqueak = sound("scp3008_staff_step_squeak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Run = sound("scp3008_staff_run");
    public static final DeferredHolder<SoundEvent, SoundEvent> RunClatter = sound("scp3008_staff_run_clatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> RunRattle = sound("scp3008_staff_run_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp3008_staff_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumDrone = sound("scp3008_staff_hum_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Alert = sound("scp3008_staff_alert");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlertLow = sound("scp3008_staff_alert_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chime = sound("scp3008_staff_pa_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChimeShimmer = sound("scp3008_staff_pa_chime_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Voice = sound("scp3008_staff_pa_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> VoiceMurmur = sound("scp3008_staff_pa_voice_murmur");
    public static final DeferredHolder<SoundEvent, SoundEvent> VoiceHiss = sound("scp3008_staff_pa_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swing = sound("scp3008_staff_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwingCloth = sound("scp3008_staff_swing_cloth");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hit = sound("scp3008_staff_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> HitThud = sound("scp3008_staff_hit_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp3008_staff_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtCloth = sound("scp3008_staff_hurt_cloth");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp3008_staff_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathSigh = sound("scp3008_staff_death_sigh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tidy = sound("scp3008_staff_tidy");
    public static final DeferredHolder<SoundEvent, SoundEvent> TidyRustle = sound("scp3008_staff_tidy_rustle");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumDrone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AlertLayers = List.of(Alert, AlertLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChimeLayers = List.of(Chime, ChimeShimmer);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> VoiceLayers = List.of(Voice, VoiceMurmur, VoiceHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwingLayers = List.of(Swing, SwingCloth);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HitLayers = List.of(Hit, HitThud);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TidyLayers = List.of(Tidy, TidyRustle);

    private Scp3008StaffRegistry() {
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
    }
}
