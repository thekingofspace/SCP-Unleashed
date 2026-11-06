package net.rai.scp_unleashed.scp087.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp087.network.Scp087ShiftPayload;
import net.rai.scp_unleashed.scp087.network.Scp087StatePayload;

public final class Scp087ClientHooks {
    public static final int LerpSteps = 3;

    private Scp087ClientHooks() {
    }

    public static void state(Scp087StatePayload Payload) {
        Scp087ClientState.apply(Payload);
    }

    public static void shift(Scp087ShiftPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return;
        }
        Scp087SectionRefresh.request();
        double Dx = Payload.Dx();
        double Dy = Payload.Dy();
        for (int Id : Payload.Ids()) {
            Entity Found = Client.level.getEntity(Id);
            if (Found == null || Found == Client.player) {
                continue;
            }
            double TargetX = Found.lerpTargetX();
            double TargetY = Found.lerpTargetY();
            double TargetZ = Found.lerpTargetZ();
            float TargetYaw = Found.lerpTargetYRot();
            float TargetPitch = Found.lerpTargetXRot();
            Found.setPos(Found.getX() + Dx, Found.getY() + Dy, Found.getZ());
            Found.xo += Dx;
            Found.yo += Dy;
            Found.xOld += Dx;
            Found.yOld += Dy;
            Found.lerpTo(TargetX + Dx, TargetY + Dy, TargetZ, TargetYaw, TargetPitch, LerpSteps);
        }
    }
}
