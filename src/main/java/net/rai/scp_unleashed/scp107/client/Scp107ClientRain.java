package net.rai.scp_unleashed.scp107.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp107.Scp107Rain;
import net.rai.scp_unleashed.scp107.Scp107RainKind;
import net.rai.scp_unleashed.scp107.network.Scp107RainPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp107ClientRain {
    public static final float FadeStep = 0.02f;
    public static final int EasingTicks = 300;
    public static final float OilGloom = 0.8f;

    private static int Kind = -1;
    private static float Elapsed;
    private static int Total;
    private static int ShownKind = -1;
    private static float Display;
    private static float ServerRain;
    private static float Written = -1.0f;
    private static float ServerThunder;
    private static float WrittenThunder = -1.0f;
    @Nullable
    private static ClientLevel Managed;

    private Scp107ClientRain() {
    }

    public static void apply(Scp107RainPayload Payload) {
        Kind = Payload.Kind();
        Elapsed = Payload.Elapsed();
        Total = Payload.Total();
    }

    public static boolean active() {
        return Kind >= 0 && Elapsed < Total;
    }

    public static Optional<Scp107RainKind> kind() {
        return active() ? Scp107RainKind.byOrdinal(Kind) : Optional.empty();
    }

    public static float strength(float Partial) {
        return active() ? Scp107Rain.strength(Elapsed + Partial, Total) : 0.0f;
    }

    public static boolean gathering() {
        return active() && Elapsed < Scp107Rain.RampTicks;
    }

    public static boolean easing() {
        return active() && Total - Elapsed < EasingTicks;
    }

    public static float display() {
        return Display;
    }

    public static Optional<Scp107RainKind> shownKind(ClientLevel World) {
        if (World.dimension() != Level.OVERWORLD || ShownKind < 0 || Display <= 0.0f) {
            return Optional.empty();
        }
        return Scp107RainKind.byOrdinal(ShownKind);
    }

    public static void tick() {
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null) {
            reset();
            return;
        }
        if (!Game.isPaused() && active()) {
            Elapsed += 1.0f;
        }
        boolean Overworld = World.dimension() == Level.OVERWORLD;
        Optional<Scp107RainKind> Sky = Overworld ? kind().filter(Scp107RainKind::drawsSky) : Optional.empty();
        if (!Overworld) {
            Display = 0.0f;
            ShownKind = -1;
        } else {
            float Target = Sky.isPresent() ? strength(0.0f) : 0.0f;
            if (Sky.isPresent()) {
                ShownKind = Sky.get().ordinal();
            }
            Display = Target > Display ? Math.min(Target, Display + FadeStep) : Math.max(Target, Display - FadeStep);
            if (Display <= 0.0f && Sky.isEmpty()) {
                ShownKind = -1;
            }
        }
        manage(World, Overworld);
    }

    private static void manage(ClientLevel World, boolean Overworld) {
        if (World != Managed) {
            Managed = World;
            Written = -1.0f;
            WrittenThunder = -1.0f;
            ServerRain = World.getRainLevel(1.0f);
            ServerThunder = World.getThunderLevel(1.0f);
        }
        float Current = World.getRainLevel(1.0f);
        if (Written < 0.0f || Math.abs(Current - Written) > 1.0e-4f) {
            ServerRain = Current;
        }
        float CurrentThunder = World.getThunderLevel(1.0f);
        if (WrittenThunder < 0.0f || Math.abs(CurrentThunder - WrittenThunder) > 1.0e-4f) {
            ServerThunder = CurrentThunder;
        }
        boolean Showing = Overworld && Display > 0.0f && ShownKind >= 0;
        if (Showing) {
            float Shown = Math.max(ServerRain, Display);
            World.setRainLevel(Shown);
            Written = Shown;
        } else if (Written >= 0.0f) {
            World.setRainLevel(ServerRain);
            Written = -1.0f;
        }
        float Gloom = Showing && ShownKind == Scp107RainKind.Oil.ordinal() ? Display * OilGloom : 0.0f;
        if (Gloom > 0.0f) {
            float Shown = Math.max(ServerThunder, Gloom);
            World.setThunderLevel(Shown);
            WrittenThunder = Shown;
        } else if (WrittenThunder >= 0.0f) {
            World.setThunderLevel(ServerThunder);
            WrittenThunder = -1.0f;
        }
    }

    public static void reset() {
        Kind = -1;
        Elapsed = 0.0f;
        Total = 0;
        ShownKind = -1;
        Display = 0.0f;
        ServerRain = 0.0f;
        Written = -1.0f;
        ServerThunder = 0.0f;
        WrittenThunder = -1.0f;
        Managed = null;
    }
}
