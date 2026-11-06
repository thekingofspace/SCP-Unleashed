package net.rai.scp_unleashed.scp3008;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class Scp3008Hours {
    public static final long Day = 24000L;
    public static final long Dusk = 13000L;
    public static final long Dawn = 23000L;
    public static final long Arrival = 1000L;

    private Scp3008Hours() {
    }

    public static boolean inStore(@Nullable Level World) {
        return World != null && World.dimension() == Scp3008World.key();
    }

    public static boolean night(@Nullable Level World) {
        return inStore(World) && nightAt(World.getDayTime());
    }

    public static boolean nightAt(long Time) {
        long Hour = Math.floorMod(Time, Day);
        return Hour >= Dusk && Hour < Dawn;
    }

    public static long hourOf(long Time) {
        return Math.floorMod(Time, Day);
    }
}
