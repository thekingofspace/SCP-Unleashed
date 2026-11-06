package net.rai.scp_unleashed.intro;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.props.PropSeatEntity;

public final class IntroPrivacy {
    private IntroPrivacy() {
    }

    public static boolean cast(Entity Target) {
        return Target instanceof IntroScientist || Target instanceof IntroStaff || Target instanceof PropSeatEntity;
    }

    public static boolean hidden(Entity Target, ServerPlayer Viewer) {
        if (Target == Viewer || Target.level() != Viewer.level() || !IntroWorld.inside(Viewer)) {
            return false;
        }
        if (Viewer.isSpectator() || Viewer.isCreative() && Viewer.hasPermissions(2)) {
            return false;
        }
        if (Target instanceof Player) {
            return true;
        }
        return cast(Target) && IntroWorld.plotAt(Target.getX(), Target.getZ()) != IntroWorld.plotAt(Viewer.getX(), Viewer.getZ());
    }
}
