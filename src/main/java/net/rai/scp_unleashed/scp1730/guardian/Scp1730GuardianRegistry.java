package net.rai.scp_unleashed.scp1730.guardian;

import net.minecraft.core.registries.Registries;
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
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.stasis.StasisPods;

public final class Scp1730GuardianRegistry {
    public static final String ContainedName = "scp1730_2_contained";
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<EntityType<?>, EntityType<Scp1730ContainedGuardian>> ContainedType = EntityTypeRegister.register(ContainedName,
            () -> EntityType.Builder.<Scp1730ContainedGuardian>of(Scp1730ContainedGuardian::new, MobCategory.MONSTER).sized(Scp1730Guardian.Width, Scp1730Guardian.Height)
                    .eyeHeight(Scp1730Guardian.EyeHeight).fireImmune().clientTrackingRange(16).build(ContainedName));
    public static final DeferredItem<DeferredSpawnEggItem> ContainedEgg = ItemRegister.register(ContainedName + "_spawn_egg",
            () -> new DeferredSpawnEggItem(ContainedType, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<SoundEvent, SoundEvent> Phase = sound("scp1730_guardian_phase");
    public static final DeferredHolder<SoundEvent, SoundEvent> PhaseFold = sound("scp1730_guardian_phase_fold");

    private Scp1730GuardianRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(Scp1730Registry.location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StasisPods.addCaptureHook(Scp1730GuardianCapture::captured);
        StasisPods.addReleaseHook((World, Spawned) -> {
            if (Spawned instanceof Scp1730ContainedGuardian Released) {
                Released.restore();
            }
        });
    }
}
