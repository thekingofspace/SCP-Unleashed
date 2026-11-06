package net.rai.scp_unleashed.scp261.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.scp261.Scp261Registry;

public final class Scp261Topsy {
    public static final float FullRoll = 180.0f;
    public static final float Ease = 0.12f;
    public static final int EndLead = 30;
    private static float Roll;
    private static float LastRoll;

    private Scp261Topsy() {
    }

    public static float target(Player User) {
        if (User == null) {
            return 0.0f;
        }
        MobEffectInstance Effect = User.getEffect(Scp261Registry.Topsy);
        if (Effect == null) {
            return 0.0f;
        }
        return Effect.isInfiniteDuration() || Effect.getDuration() > EndLead ? FullRoll : 0.0f;
    }

    public static void tick(Minecraft Client) {
        LastRoll = Roll;
        if (Client.isPaused()) {
            return;
        }
        float Goal = target(Client.player);
        Roll += (Goal - Roll) * Ease;
        if (Math.abs(Goal - Roll) < 0.05f) {
            Roll = Goal;
        }
    }

    public static void angles(ViewportEvent.ComputeCameraAngles Event) {
        float Shown = Mth.lerp((float) Event.getPartialTick(), LastRoll, Roll);
        if (Math.abs(Shown) > 1.0e-3f) {
            Event.setRoll(Event.getRoll() + Shown);
        }
    }

    public static void reset() {
        Roll = 0.0f;
        LastRoll = 0.0f;
    }
}
