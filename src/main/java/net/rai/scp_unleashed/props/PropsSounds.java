package net.rai.scp_unleashed.props;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class PropsSounds {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> LockerOpen = register("props_locker_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> LockerOpenRattle = register("props_locker_open_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LockerOpenLatch = register("props_locker_open_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> LockerClose = register("props_locker_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> LockerCloseRattle = register("props_locker_close_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LockerCloseLatch = register("props_locker_close_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerOpen = register("props_drawer_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerOpenLatch = register("props_drawer_open_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerOpenStop = register("props_drawer_open_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerClose = register("props_drawer_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerCloseSlam = register("props_drawer_close_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerCloseLatch = register("props_drawer_close_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrateOpen = register("props_crate_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrateOpenCreak = register("props_crate_open_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrateClose = register("props_crate_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrateCloseSlam = register("props_crate_close_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> CabinetOpen = register("props_cabinet_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CabinetOpenLatch = register("props_cabinet_open_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> CabinetOpenRattle = register("props_cabinet_open_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CabinetClose = register("props_cabinet_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> CabinetCloseLatch = register("props_cabinet_close_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> CabinetCloseRattle = register("props_cabinet_close_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScreenOn = register("props_screen_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScreenOff = register("props_screen_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScreenClick = register("props_screen_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoolerFill = register("props_cooler_fill");
    public static final DeferredHolder<SoundEvent, SoundEvent> WorkbenchCraft = register("props_workbench_craft");

    private PropsSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }
}
