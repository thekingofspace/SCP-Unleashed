package net.rai.scp_unleashed.scp513.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.scp513.Scp513Stage;

public final class Scp513ClientState {
    public static final float AimShare = 0.3f;
    public static final int AimEase = 20;
    private static boolean Active;
    private static int Elapsed = -1;
    private static long Seed;
    private static int Started;

    private Scp513ClientState() {
    }

    public static void receive(int Ticks, long Value) {
        if (Ticks < 0) {
            reset();
            return;
        }
        if (!Active || Value != Seed) {
            Started = Ticks;
            Scp513Dread.begin();
        }
        Active = true;
        Elapsed = Ticks;
        Seed = Value;
    }

    public static void tick(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null) {
            reset();
            return;
        }
        if (!Active || Client.isPaused()) {
            return;
        }
        if (User.isDeadOrDying() || User.isSpectator() || User.isCreative()) {
            reset();
            return;
        }
        Elapsed = Math.min(Elapsed + 1, Scp513Stage.DeathAt + 40);
    }

    public static void reset() {
        if (Active) {
            Scp513Dread.end();
            Scp513Visions.reset();
        }
        Active = false;
        Elapsed = -1;
        Seed = 0L;
    }

    public static boolean isActive() {
        return Active;
    }

    public static int elapsed() {
        return Elapsed;
    }

    public static int started() {
        return Started;
    }

    public static long seed() {
        return Seed;
    }

    public static Scp513Stage stage() {
        return Active ? Scp513Stage.of(Elapsed) : Scp513Stage.None;
    }

    public static void steer(Player Mover, Input Controls) {
        if (!stage().frozen() || !(Mover instanceof LocalPlayer User)) {
            return;
        }
        Controls.leftImpulse = 0.0f;
        Controls.forwardImpulse = 0.0f;
        Controls.up = false;
        Controls.down = false;
        Controls.left = false;
        Controls.right = false;
        Controls.jumping = false;
        User.setSprinting(false);
    }

    public static float aim(float Partial) {
        if (!Active || !stage().seized()) {
            return 1.0f;
        }
        float Into = Mth.clamp((Elapsed + Partial - Scp513Stage.GrabAt) / AimEase, 0.0f, 1.0f);
        return Mth.lerp(Into, 1.0f, AimShare);
    }
}
