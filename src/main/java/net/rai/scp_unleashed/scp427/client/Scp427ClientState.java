package net.rai.scp_unleashed.scp427.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp427.Scp427Transformation;
import net.rai.scp_unleashed.scp427.network.Scp427ConvulsePayload;
import net.rai.scp_unleashed.scp427.network.Scp427StatePayload;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Scp427ClientState {
    public static final float GlowRise = 0.08f;
    public static final float GlowFall = 0.05f;
    public static final float WarnRise = 0.03f;
    public static final int ConvulseTail = 30;
    public static final int VanishTicks = 60;
    private static boolean Open;
    private static int Stage;
    private static int Transform;
    private static float Glow;
    private static float GlowBefore;
    private static float Warning;
    private static float WarningBefore;
    private static int Clock;
    private static final Map<Integer, Integer> Convulsing = new HashMap<>();
    private static final Map<Integer, Integer> Vanished = new HashMap<>();

    private Scp427ClientState() {
    }

    public static void receive(Scp427StatePayload Payload) {
        Open = Payload.Open();
        Stage = Payload.Stage();
        Transform = Payload.Transform();
    }

    public static void convulse(Scp427ConvulsePayload Payload) {
        if (Payload.Elapsed() == Scp427ConvulsePayload.Overcome) {
            Convulsing.remove(Payload.EntityId());
            Vanished.put(Payload.EntityId(), VanishTicks);
        } else if (Payload.Elapsed() < 0) {
            Convulsing.remove(Payload.EntityId());
        } else {
            Convulsing.put(Payload.EntityId(), Payload.Elapsed());
        }
    }

    public static void tick() {
        Clock++;
        GlowBefore = Glow;
        WarningBefore = Warning;
        Glow = Open ? Math.min(1.0f, Glow + GlowRise) : Math.max(0.0f, Glow - GlowFall);
        float Wanted = Open ? Stage / 2.0f : 0.0f;
        Warning = Warning < Wanted ? Math.min(Wanted, Warning + WarnRise) : Math.max(Wanted, Warning - WarnRise);
        if (Transform > 0 && Transform <= Scp427Transformation.Length) {
            Transform++;
        }
        Iterator<Map.Entry<Integer, Integer>> Entries = Convulsing.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<Integer, Integer> Entry = Entries.next();
            int Next = Entry.getValue() + 1;
            if (Next > Scp427Transformation.Length + ConvulseTail || Minecraft.getInstance().level == null || Minecraft.getInstance().level.getEntity(Entry.getKey()) == null) {
                Entries.remove();
            } else {
                Entry.setValue(Next);
            }
        }
        Vanished.replaceAll((Id, Left) -> Left - 1);
        Vanished.values().removeIf(Left -> Left <= 0);
    }

    public static void reset() {
        Open = false;
        Stage = 0;
        Transform = 0;
        Glow = 0.0f;
        GlowBefore = 0.0f;
        Warning = 0.0f;
        WarningBefore = 0.0f;
        Convulsing.clear();
        Vanished.clear();
    }

    public static boolean vanished(LivingEntity Subject) {
        return Subject.isDeadOrDying() && Vanished.containsKey(Subject.getId());
    }

    public static float glow(float Partial) {
        return Mth.lerp(Partial, GlowBefore, Glow);
    }

    public static float warning(float Partial) {
        return Mth.lerp(Partial, WarningBefore, Warning);
    }

    public static int stage() {
        return Stage;
    }

    public static float transform(float Partial) {
        if (Transform <= 0) {
            return 0.0f;
        }
        return Mth.clamp((Transform + Partial) / Scp427Transformation.Length, 0.0f, 1.0f);
    }

    public static boolean transforming() {
        return Transform > 0;
    }

    public static float clock(float Partial) {
        return Clock + Partial;
    }

    public static float convulsion(int EntityId, float Partial) {
        Integer Elapsed = Convulsing.get(EntityId);
        if (Elapsed == null || Elapsed > Scp427Transformation.Length + 2) {
            return 0.0f;
        }
        return Mth.clamp((Elapsed + Partial) / Scp427Transformation.Length, 0.0f, 1.0f);
    }
}
