package net.rai.scp_unleashed.scp001scarlet.king.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKing;

public final class ScarletKingDeathFx {
    public static final double Reach = 96.0;
    public static final float Decay = 5.0f;
    public static final float[][] Pulses = {{ScarletKing.KneelTick, 0.9f}, {ScarletKing.SecondKneelTick, 1.3f}, {ScarletKing.CrownCrackTick, 0.45f},
            {ScarletKing.ShatterTick, 0.3f}, {ScarletKing.CollapseTick, 1.7f}};
    public static final float Rumble = 0.22f;

    private ScarletKingDeathFx() {
    }

    public static float strength(float Dying) {
        float Amount = 0.0f;
        for (float[] Pulse : Pulses) {
            if (Dying >= Pulse[0]) {
                Amount += Pulse[1] * (float) Math.exp(-(Dying - Pulse[0]) / Decay);
            }
        }
        if (Dying >= ScarletKing.CrumbleTick && Dying < ScarletKing.CollapseTick) {
            Amount += Rumble;
        }
        return Amount;
    }

    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer Viewer = Game.player;
        if (Viewer == null || Game.level == null) {
            return;
        }
        float Partial = (float) Event.getPartialTick();
        float Best = 0.0f;
        float Clock = 0.0f;
        for (ScarletKing Each : Game.level.getEntitiesOfClass(ScarletKing.class, Viewer.getBoundingBox().inflate(Reach), ScarletKing::isDeadOrDying)) {
            float Dying = Each.deathTime + Partial;
            float Fade = (float) Mth.clamp(1.0 - Each.distanceTo(Viewer) / Reach, 0.0, 1.0);
            float Amount = strength(Dying) * Fade;
            if (Amount > Best) {
                Best = Amount;
                Clock = Dying;
            }
        }
        if (Best <= 0.001f) {
            return;
        }
        Event.setRoll(Event.getRoll() + Mth.sin(Clock * 2.3f) * Best);
        Event.setPitch(Event.getPitch() + Mth.sin(Clock * 1.9f + 1.0f) * Best * 0.5f);
    }
}
