package net.rai.scp_unleashed.scp472.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp472.Scp472Hunger;
import net.rai.scp_unleashed.scp472.Scp472Mind;
import net.rai.scp_unleashed.scp472.Scp472Stones;
import net.rai.scp_unleashed.scp472.network.Scp472MindPayload;
import org.jetbrains.annotations.Nullable;

public final class Scp472ClientState {
    public static final int[] Intervals = {26, 23, 20, 17, 14, 12, 9};
    public static final int IdleInterval = 26;
    public static final float Quicken = 0.4f;
    public static final int FastestInterval = 6;
    public static final float PulseTicks = 4.0f;
    public static final float DubShare = 0.7f;
    private static float Sanity = Scp472Mind.Full;
    @Nullable
    private static Scp472Stones.Nearest Heard;
    private static int Age = 1000;
    private static int Interval = IdleInterval;
    private static int Clock;

    private Scp472ClientState() {
    }

    public static void apply(Scp472MindPayload Payload) {
        Sanity = Mth.clamp(Payload.Sanity(), 0.0f, Scp472Mind.Full);
    }

    public static float sanity() {
        return Sanity;
    }

    public static int stage() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.player.isCreative() || Client.player.isSpectator()) {
            return 0;
        }
        return Scp472Mind.stage(Sanity);
    }

    @Nullable
    public static Scp472Stones.Nearest heard() {
        return Heard;
    }

    public static int clock() {
        return Clock;
    }

    public static int interval() {
        return Interval;
    }

    public static int dubDelay(int Every) {
        return Math.max(3, Every / 4);
    }

    public static boolean beat(Minecraft Client) {
        Clock++;
        LocalPlayer User = Client.player;
        Heard = User == null || Client.level == null ? null : Scp472Stones.nearest(Client.level, Scp472Hunger.centre(User), Scp472Mind.HeardRange);
        Interval = Intervals[Mth.clamp(stage(), 0, Intervals.length - 1)];
        if (Heard == null) {
            Age = 1000;
            return false;
        }
        float Closeness = 1.0f - (float) Mth.clamp(Heard.Distance() / Scp472Mind.Reach, 0.0, 1.0);
        Interval = Math.max(FastestInterval, Math.round(Interval * (1.0f - Quicken * Closeness)));
        Age++;
        if (Age >= Interval) {
            Age = 0;
            return true;
        }
        return false;
    }

    public static int age() {
        return Age;
    }

    public static boolean dubNow() {
        return Heard != null && Age == dubDelay(Interval);
    }

    public static float pulse(float Partial) {
        if (Heard == null) {
            return 0.0f;
        }
        return shape(Age + Partial, dubDelay(Interval));
    }

    public static float idlePulse(long GameTime, float Partial, long Salt) {
        float Phase = (float) Math.floorMod(GameTime + Salt, (long) IdleInterval) + Partial;
        return shape(Phase, dubDelay(IdleInterval));
    }

    public static float shape(float Since, int Dub) {
        float Lub = Math.max(0.0f, 1.0f - Since / PulseTicks);
        float Second = Since >= Dub ? DubShare * Math.max(0.0f, 1.0f - (Since - Dub) / PulseTicks) : 0.0f;
        return Math.max(Lub, Second);
    }

    public static boolean throbbing() {
        if (Heard == null) {
            return false;
        }
        int Dub = dubDelay(Interval);
        return Age < 2 || (Age >= Dub && Age < Dub + 2);
    }

    public static void reset() {
        Sanity = Scp472Mind.Full;
        Heard = null;
        Age = 1000;
        Interval = IdleInterval;
        Clock = 0;
    }
}
