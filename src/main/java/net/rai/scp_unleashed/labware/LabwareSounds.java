package net.rai.scp_unleashed.labware;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LabwareSounds {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> VialSeal = register("labware_vial_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> VialSealHiss = register("labware_vial_seal_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> VialPlace = register("labware_vial_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> VialPlaceClink = register("labware_vial_place_clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwabTake = register("labware_swab_take");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwabTakeTap = register("labware_swab_take_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwabTransfer = register("labware_swab_transfer");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwabTransferSmear = register("labware_swab_transfer_smear");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnalyzerRun = register("labware_analyzer_run");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnalyzerRunWhir = register("labware_analyzer_run_whir");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnalyzerClean = register("labware_analyzer_clean");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnalyzerCleanChime = register("labware_analyzer_clean_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnalyzerPositive = register("labware_analyzer_positive");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnalyzerPositiveDrone = register("labware_analyzer_positive_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnalyzerPositiveDrop = register("labware_analyzer_positive_drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> StorageOpen = register("labware_storage_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> StorageOpenSeal = register("labware_storage_open_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> StorageClose = register("labware_storage_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> StorageCloseSeal = register("labware_storage_close_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> StorageAlarm = register("labware_storage_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> StorageAlarmTone = register("labware_storage_alarm_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> StorageVent = register("labware_storage_vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> StorageVentHiss = register("labware_storage_vent_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> StorageChill = register("labware_storage_chill");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refused = register("labware_refused");
    public static final DeferredHolder<SoundEvent, SoundEvent> SuitRinse = register("labware_suit_rinse");
    public static final DeferredHolder<SoundEvent, SoundEvent> SuitRinseSwish = register("labware_suit_rinse_swish");
    public static final DeferredHolder<SoundEvent, SoundEvent> SuitRinseDrip = register("labware_suit_rinse_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> WipeStroke = register("labware_wipe_stroke");
    public static final DeferredHolder<SoundEvent, SoundEvent> WipeStrokeCloth = register("labware_wipe_stroke_cloth");
    public static final DeferredHolder<SoundEvent, SoundEvent> WipeFinish = register("labware_wipe_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> WipeFinishFizz = register("labware_wipe_finish_fizz");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Unsealed = List.of(VialSeal, VialSealHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Racked = List.of(VialPlace, VialPlaceClink);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Swabbed = List.of(SwabTake, SwabTakeTap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Streaked = List.of(SwabTransfer, SwabTransferSmear);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Cycling = List.of(AnalyzerRun, AnalyzerRunWhir);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReadClean = List.of(AnalyzerClean, AnalyzerCleanChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReadPositive = List.of(AnalyzerPositive, AnalyzerPositiveDrone, AnalyzerPositiveDrop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Opened = List.of(StorageOpen, StorageOpenSeal);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Closed = List.of(StorageClose, StorageCloseSeal);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Alarmed = List.of(StorageAlarm, StorageAlarmTone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Venting = List.of(StorageVent, StorageVentHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Rinsed = List.of(SuitRinse, SuitRinseSwish, SuitRinseDrip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Wiping = List.of(WipeStroke, WipeStrokeCloth);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Wiped = List.of(WipeFinish, WipeFinishFizz);

    private LabwareSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void play(Level World, @Nullable Player Except, double X, double Y, double Z, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(Except, X, Y, Z, Layer.get(), Source, 1.0f, Pitch);
        }
    }

    public static void play(Level World, BlockPos Pos, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch) {
        play(World, null, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Layers, SoundSource.BLOCKS, Pitch);
    }

    public static float pitch(Level World) {
        return 0.95f + World.getRandom().nextFloat() * 0.1f;
    }
}
