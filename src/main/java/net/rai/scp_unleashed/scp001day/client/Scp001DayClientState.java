package net.rai.scp_unleashed.scp001day.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp001day.Scp001DaySunlight;
import net.rai.scp_unleashed.scp001day.Scp001DayTurning;

public final class Scp001DayClientState {
    private static final Int2ObjectOpenHashMap<float[]> Turning = new Int2ObjectOpenHashMap<>();
    private static float Local;
    private static int Lit;
    private static int Charge;
    private static boolean Day;

    private Scp001DayClientState() {
    }

    public static void turning(int EntityId, float Progress, long Now) {
        if (Progress < 0.0f) {
            Turning.remove(EntityId);
        } else {
            Turning.put(EntityId, new float[] {Progress, Now});
        }
    }

    public static float turning(Entity Body, float Partial) {
        float[] Entry = Body == null ? null : Turning.get(Body.getId());
        if (Entry == null) {
            return -1.0f;
        }
        float Since = (Body.level().getGameTime() + Partial - Entry[1]) / Scp001DayTurning.Duration;
        return Math.max(0.0f, Math.min(1.0f, Entry[0] + Math.max(0.0f, Since)));
    }

    public static void local(float Exposure, int Points, boolean Lethal) {
        Local = Exposure;
        Lit = Points;
        Day = Lethal;
        Charge = Points > 0 ? Math.min(Scp001DaySunlight.TriggerTicks, Charge + Scp001DaySunlight.CheckTicks) : Math.max(0, Charge - Scp001DaySunlight.CheckTicks);
    }

    public static float localExposure() {
        return Local;
    }

    public static int litPoints() {
        return Lit;
    }

    public static float charge() {
        return Charge / (float) Scp001DaySunlight.TriggerTicks;
    }

    public static boolean lethalDay() {
        return Day;
    }

    public static void leave() {
        Local = 0.0f;
        Lit = 0;
        Charge = 0;
        Day = false;
    }

    public static void reset() {
        leave();
        Turning.clear();
    }
}
