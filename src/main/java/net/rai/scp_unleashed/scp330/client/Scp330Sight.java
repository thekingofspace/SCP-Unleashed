package net.rai.scp_unleashed.scp330.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.rai.scp_unleashed.scp330.Scp330Registry;

public final class Scp330Sight {
    public static final double Range = 48.0;
    public static final int Colour = 0xC77DFF;

    private Scp330Sight() {
    }

    public static boolean reveals(Entity Target) {
        if (!(Target instanceof Mob Body) || !Body.isAlive()) {
            return false;
        }
        LocalPlayer Viewer = Minecraft.getInstance().player;
        if (Viewer == null || Body.level() != Viewer.level() || !Viewer.hasEffect(Scp330Registry.Sight)) {
            return false;
        }
        return Body.distanceToSqr(Viewer) <= Range * Range;
    }
}
