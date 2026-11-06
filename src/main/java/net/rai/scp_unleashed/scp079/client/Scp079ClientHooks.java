package net.rai.scp_unleashed.scp079.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp079.Scp079Mood;
import net.rai.scp_unleashed.scp079.network.Scp079LurchPayload;
import net.rai.scp_unleashed.scp079.network.Scp079TerminalPayload;

public final class Scp079ClientHooks {
    public static final int LurchTicks = 6;
    private static int LurchTarget = -1;
    private static int LurchLeft;

    private Scp079ClientHooks() {
    }

    public static void terminal(Scp079TerminalPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            return;
        }
        if (Client.screen instanceof Scp079TerminalScreen Open && Open.isFor(Payload.Pos())) {
            Open.receive(Payload.Lines(), Scp079Mood.byIndex(Payload.Mood()), Payload.Upgraded());
            return;
        }
        if (!Payload.Opening()) {
            return;
        }
        Scp079TerminalScreen Created = new Scp079TerminalScreen(Payload.Pos(), Payload.Upgraded(), Scp079Mood.byIndex(Payload.Mood()));
        Client.setScreen(Created);
        Created.receive(Payload.Lines(), Scp079Mood.byIndex(Payload.Mood()), Payload.Upgraded());
    }

    public static void lurch(Scp079LurchPayload Payload) {
        LurchTarget = Payload.TargetId();
        LurchLeft = LurchTicks;
    }

    public static void tickLurch() {
        Minecraft Client = Minecraft.getInstance();
        if (LurchLeft <= 0 || Client.player == null || Client.level == null) {
            return;
        }
        LurchLeft--;
        Entity Target = Client.level.getEntity(LurchTarget);
        if (Target == null) {
            LurchLeft = 0;
            return;
        }
        double Dx = Target.getX() - Client.player.getX();
        double Dy = Target.getY() + Target.getBbHeight() * 0.6 - Client.player.getEyeY();
        double Dz = Target.getZ() - Client.player.getZ();
        float WantYaw = (float) (Math.toDegrees(Math.atan2(Dz, Dx)) - 90.0);
        float WantPitch = (float) -Math.toDegrees(Math.atan2(Dy, Math.sqrt(Dx * Dx + Dz * Dz)));
        float Yaw = Client.player.getYRot();
        float Pitch = Client.player.getXRot();
        float NewYaw = Yaw + Mth.wrapDegrees(WantYaw - Yaw) * 0.45f;
        float NewPitch = Pitch + (WantPitch - Pitch) * 0.45f;
        Client.player.setYRot(NewYaw);
        Client.player.setXRot(Mth.clamp(NewPitch, -90.0f, 90.0f));
        Client.player.setYHeadRot(NewYaw);
    }
}
