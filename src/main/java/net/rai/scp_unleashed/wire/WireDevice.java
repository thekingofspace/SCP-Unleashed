package net.rai.scp_unleashed.wire;

import java.util.Collection;
import java.util.List;

public interface WireDevice {
    default int getPowerOutput() {
        return 0;
    }

    default int getReserveOutput() {
        return 0;
    }

    static int bufferedReserve(WireDevice Device) {
        return Math.max(0, Device.getStoredPower() - Math.max(0, Device.getPowerOutput()));
    }

    default int getPowerDemand() {
        return 0;
    }

    default void extractPower(int Amount) {
    }

    default void insertPower(int Amount) {
    }

    default int getStoredPower() {
        return 0;
    }

    default int getPowerCapacity() {
        return 0;
    }

    default boolean isStorage() {
        return false;
    }

    default boolean isGenerator() {
        return false;
    }

    default boolean isSink() {
        return false;
    }

    default int getGenerationRate() {
        return 0;
    }

    default void observeNetwork(long Available) {
    }

    default Collection<String> getSubscriptions() {
        return List.of();
    }

    default void receiveMessage(WireMessage Message) {
    }
}
