package net.rai.scp_unleashed.props;

import java.util.List;

public enum StorageKind {
    Locker(27,
            List.of(PropSound.now(PropsSounds.LockerOpen), PropSound.now(PropsSounds.LockerOpenRattle), PropSound.now(PropsSounds.LockerOpenLatch)),
            List.of(PropSound.now(PropsSounds.LockerClose), PropSound.now(PropsSounds.LockerCloseRattle), PropSound.now(PropsSounds.LockerCloseLatch))),
    FilingCabinet(27,
            List.of(PropSound.now(PropsSounds.DrawerOpen), PropSound.now(PropsSounds.DrawerOpenLatch), PropSound.after(PropsSounds.DrawerOpenStop, 6)),
            List.of(PropSound.now(PropsSounds.DrawerClose), PropSound.after(PropsSounds.DrawerCloseSlam, 5), PropSound.after(PropsSounds.DrawerCloseLatch, 5))),
    SupplyCrate(27,
            List.of(PropSound.now(PropsSounds.CrateOpen), PropSound.now(PropsSounds.CrateOpenCreak)),
            List.of(PropSound.now(PropsSounds.CrateClose), PropSound.now(PropsSounds.CrateCloseSlam))),
    MedicalCabinet(9,
            List.of(PropSound.now(PropsSounds.CabinetOpen), PropSound.now(PropsSounds.CabinetOpenLatch), PropSound.now(PropsSounds.CabinetOpenRattle)),
            List.of(PropSound.now(PropsSounds.CabinetClose), PropSound.now(PropsSounds.CabinetCloseLatch), PropSound.now(PropsSounds.CabinetCloseRattle)));

    private final int Slots;
    private final List<PropSound> OpenSounds;
    private final List<PropSound> CloseSounds;

    StorageKind(int Slots, List<PropSound> OpenSounds, List<PropSound> CloseSounds) {
        this.Slots = Slots;
        this.OpenSounds = OpenSounds;
        this.CloseSounds = CloseSounds;
    }

    public int getSlots() {
        return Slots;
    }

    public List<PropSound> getSounds(boolean Opened) {
        return Opened ? OpenSounds : CloseSounds;
    }
}
