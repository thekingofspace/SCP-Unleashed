package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScarletCourtSounds {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final List<String> Names = List.of(
            "wave", "pillar_crack", "band_snap", "sting", "breather",
            "thrall_idle", "thrall_hurt", "thrall_death", "thrall_attack",
            "hound_idle", "hound_hurt", "hound_death", "hound_lunge", "hound_tether",
            "crawler_idle", "crawler_hurt", "crawler_death", "crawler_thorns",
            "eyeling_idle", "eyeling_death", "eyeling_flash",
            "knight_idle", "knight_hurt", "knight_death", "knight_block", "knight_cleave",
            "herald_idle", "herald_hurt", "herald_death", "herald_cast", "herald_buff",
            "bell_idle", "bell_hurt", "bell_death", "bell_toll",
            "wraith_idle", "wraith_hurt", "wraith_death", "wraith_phase",
            "husk_idle", "husk_hurt", "husk_death", "husk_attack",
            "stalker_idle", "stalker_hurt", "stalker_death", "stalker_leap",
            "gnawer_idle", "gnawer_hurt", "gnawer_death",
            "golem_idle", "golem_hurt", "golem_death", "golem_slam", "golem_step",
            "thorns_idle", "thorns_hurt", "thorns_death", "thorns_wall", "thorns_roots", "thorns_cage", "thorns_bloom",
            "bride_idle", "bride_hurt", "bride_death", "bride_chain", "bride_grapple", "bride_link", "bride_scream",
            "eyes_idle", "eyes_hurt", "eyes_death", "eyes_flare", "eyes_beam", "eyes_split",
            "moon_idle", "moon_hurt", "moon_death", "moon_wane", "moon_full", "moon_pillar", "moon_tide", "moon_summon",
            "moth_idle", "moth_hurt", "moth_death");
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> Events = new LinkedHashMap<>();

    static {
        for (String Name : Names) {
            String Id = "scarlet_court_" + Name;
            Events.put(Name, SoundEventRegister.register(Id, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Id))));
        }
    }

    private ScarletCourtSounds() {
    }

    public static SoundEvent get(String Name) {
        DeferredHolder<SoundEvent, SoundEvent> Holder = Events.get(Name);
        if (Holder == null) {
            throw new IllegalArgumentException("unknown court sound " + Name);
        }
        return Holder.get();
    }

    public static boolean has(String Name) {
        return Events.containsKey(Name);
    }
}
