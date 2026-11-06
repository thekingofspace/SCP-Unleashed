package net.rai.scp_unleashed.scp1730.rooms;

import org.jetbrains.annotations.Nullable;

public enum Scp1730RoomKind {
    Server("server"),
    Silent("silent"),
    Television("television"),
    Loop("loop"),
    Vault("vault"),
    VaultHall("vault_hall"),
    Lightless("lightless"),
    Slow("slow"),
    Drift("drift"),
    Echo("echo");

    private final String Id;

    Scp1730RoomKind(String Id) {
        this.Id = Id;
    }

    public String id() {
        return Id;
    }

    @Nullable
    public static Scp1730RoomKind of(String Type) {
        for (Scp1730RoomKind Each : values()) {
            if (Each.Id.equals(Type)) {
                return Each;
            }
        }
        return null;
    }
}
