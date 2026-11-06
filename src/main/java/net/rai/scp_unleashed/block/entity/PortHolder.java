package net.rai.scp_unleashed.block.entity;

public interface PortHolder {
    String getKindName();

    int getSlotCount();

    int getSlot(int Slot);

    void setSlot(int Slot, int Requested);
}
