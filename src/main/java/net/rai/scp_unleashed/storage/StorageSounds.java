package net.rai.scp_unleashed.storage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class StorageSounds {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> Open = register("storage_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> OpenLatch = register("storage_open_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> OpenStop = register("storage_open_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CloseSwing = register("storage_close_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> Close = register("storage_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> CloseBody = register("storage_close_body");
    public static final DeferredHolder<SoundEvent, SoundEvent> CloseLatch = register("storage_close_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Insert = register("storage_insert");
    public static final DeferredHolder<SoundEvent, SoundEvent> InsertSeat = register("storage_insert_seat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Remove = register("storage_remove");
    public static final DeferredHolder<SoundEvent, SoundEvent> RemoveLift = register("storage_remove_lift");
    public static final DeferredHolder<SoundEvent, SoundEvent> Glass = register("storage_glass");
    public static final DeferredHolder<SoundEvent, SoundEvent> GlassSeat = register("storage_glass_seat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Upgrade = register("storage_upgrade");
    public static final DeferredHolder<SoundEvent, SoundEvent> UpgradeClamp = register("storage_upgrade_clamp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Locked = register("storage_locked");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heater = register("storage_heater");
    public static final DeferredHolder<SoundEvent, SoundEvent> Frost = register("storage_frost");

    public static final List<StorageCue> Opening = List.of(
            new StorageCue(OpenLatch, 0),
            new StorageCue(Open, 1));
    public static final List<StorageCue> Opened = List.of(
            new StorageCue(OpenStop, 0));
    public static final List<StorageCue> Closing = List.of(
            new StorageCue(CloseSwing, 0));
    public static final List<StorageCue> Slammed = List.of(
            new StorageCue(Close, 0),
            new StorageCue(CloseBody, 0));
    public static final List<StorageCue> Settled = List.of(
            new StorageCue(CloseLatch, 0));
    public static final List<StorageCue> Stored = List.of(
            new StorageCue(Insert, 0),
            new StorageCue(InsertSeat, 1));
    public static final List<StorageCue> Taken = List.of(
            new StorageCue(Remove, 0),
            new StorageCue(RemoveLift, 0));
    public static final List<StorageCue> Glazed = List.of(
            new StorageCue(GlassSeat, 0),
            new StorageCue(Glass, 2));
    public static final List<StorageCue> Upgraded = List.of(
            new StorageCue(Upgrade, 0),
            new StorageCue(UpgradeClamp, 4));
    public static final List<StorageCue> Refused = List.of(
            new StorageCue(Locked, 0));

    private StorageSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }
}
