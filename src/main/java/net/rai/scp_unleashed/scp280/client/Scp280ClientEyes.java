package net.rai.scp_unleashed.scp280.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp280.Scp280Eyes;
import net.rai.scp_unleashed.scp280.Scp280Registry;
import net.rai.scp_unleashed.scp280.network.Scp280EyesPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp280ClientEyes {
    public static final int FirstEntityId = -280000000;
    public static final int IdSpan = 1000000;

    private static final List<Scp280Eyes> Shown = new ArrayList<>();
    private static int NextEntityId = FirstEntityId;
    @Nullable
    private static ClientLevel LastLevel;

    private Scp280ClientEyes() {
    }

    public static int alive() {
        return Shown.size();
    }

    public static void handle(Scp280EyesPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel Level = Client.level;
        if (Level == null || Client.player == null) {
            return;
        }
        if (Payload.Kind() == Scp280EyesPayload.Clear) {
            closeAll();
            return;
        }
        boolean First = true;
        for (Scp280EyesPayload.Spot Spot : Payload.Spots()) {
            Scp280Eyes Made = new Scp280Eyes(Scp280Registry.Eyes.get(), Level);
            Made.setId(nextId());
            Made.setup(Spot.Variant(), Spot.Delay(), Spot.Life(), !First);
            Made.place(new Vec3(Spot.X(), Spot.Y(), Spot.Z()));
            Level.addEntity(Made);
            Shown.add(Made);
            First = false;
        }
    }

    private static int nextId() {
        int Id = NextEntityId--;
        if (NextEntityId < FirstEntityId - IdSpan) {
            NextEntityId = FirstEntityId;
        }
        return Id;
    }

    public static void tick(Minecraft Client) {
        if (Client.level != LastLevel) {
            reset();
            LastLevel = Client.level;
        }
        Shown.removeIf(Eyes -> Eyes.isRemoved() || Eyes.level() != Client.level);
    }

    public static void closeAll() {
        for (Scp280Eyes Eyes : Shown) {
            Eyes.close(true);
        }
    }

    public static void reset() {
        for (Scp280Eyes Eyes : Shown) {
            Eyes.discard();
        }
        Shown.clear();
    }
}
