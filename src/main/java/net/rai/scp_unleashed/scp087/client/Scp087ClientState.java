package net.rai.scp_unleashed.scp087.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp087.Scp087Darkness;
import net.rai.scp_unleashed.scp087.Scp087Stairwell;
import net.rai.scp_unleashed.scp087.network.Scp087StatePayload;

public final class Scp087ClientState {
    public static final float Ease = 0.08f;
    private static boolean Inside;
    private static int Flights;
    private static boolean Inert;
    private static float Fog = Scp087Darkness.TopFog;
    private static float FogO = Scp087Darkness.TopFog;
    private static float Shadow;
    private static float ShadowO;
    private static boolean Primed;

    private Scp087ClientState() {
    }

    public static boolean inStairwell(Minecraft Client) {
        return Client.level != null && Client.level.dimension() == Scp087Stairwell.Key;
    }

    public static void apply(Scp087StatePayload Payload) {
        Inside = Payload.Inside();
        Flights = Payload.Flights();
        Inert = Payload.Inert();
    }

    public static boolean inert() {
        return Inert;
    }

    public static int flights() {
        return Flights;
    }

    public static boolean inside() {
        return Inside;
    }

    public static void tick(Minecraft Client) {
        FogO = Fog;
        ShadowO = Shadow;
        if (!inStairwell(Client)) {
            Primed = false;
            Fog = Scp087Darkness.TopFog;
            Shadow = 0.0f;
            return;
        }
        float WantedFog = Scp087Darkness.fogFar(Flights, Inert);
        float WantedShadow = Scp087Darkness.shadow(Flights, Inert);
        if (!Primed) {
            Primed = true;
            Fog = WantedFog;
            FogO = WantedFog;
            Shadow = WantedShadow;
            ShadowO = WantedShadow;
            return;
        }
        Fog += (WantedFog - Fog) * Ease;
        Shadow += (WantedShadow - Shadow) * Ease;
    }

    public static float fog(float Partial) {
        return Mth.lerp(Partial, FogO, Fog);
    }

    public static float shadow(float Partial) {
        return Mth.lerp(Partial, ShadowO, Shadow);
    }

    public static void reset() {
        Inside = false;
        Flights = 0;
        Inert = false;
        Primed = false;
        Fog = Scp087Darkness.TopFog;
        FogO = Fog;
        Shadow = 0.0f;
        ShadowO = 0.0f;
    }
}
