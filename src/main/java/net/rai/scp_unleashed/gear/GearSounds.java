package net.rai.scp_unleashed.gear;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class GearSounds {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> HazmatSeal = register("gear_hazmat_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> HazmatClasp = register("gear_hazmat_clasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> HazmatValve = register("gear_hazmat_valve");
    public static final DeferredHolder<SoundEvent, SoundEvent> HazmatHiss = register("gear_hazmat_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> HazmatBreath = register("gear_hazmat_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> HazmatIntake = register("gear_hazmat_intake");
    public static final DeferredHolder<SoundEvent, SoundEvent> GocEquip = register("gear_goc_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> GocEquipLayer = register("gear_goc_equip_layer");
    public static final DeferredHolder<SoundEvent, SoundEvent> SerpentsHandEquip = register("gear_serpents_hand_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> SerpentsHandEquipLayer = register("gear_serpents_hand_equip_layer");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChaosInsurgencyEquip = register("gear_chaos_insurgency_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChaosInsurgencyEquipLayer = register("gear_chaos_insurgency_equip_layer");
    public static final DeferredHolder<SoundEvent, SoundEvent> SiteGuardTacticalEquip = register("gear_site_guard_tactical_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> SiteGuardTacticalEquipLayer = register("gear_site_guard_tactical_equip_layer");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScientistEquip = register("gear_scientist_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScientistEquipLayer = register("gear_scientist_equip_layer");
    public static final DeferredHolder<SoundEvent, SoundEvent> BrokenGodEquip = register("gear_broken_god_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> BrokenGodEquipLayer = register("gear_broken_god_equip_layer");
    public static final DeferredHolder<SoundEvent, SoundEvent> SarkicEquip = register("gear_sarkic_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> SarkicEquipLayer = register("gear_sarkic_equip_layer");

    private GearSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }
}
