package net.rai.scp_unleashed.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.notice.Notice;
import net.rai.scp_unleashed.notice.Notices;

public final class RaidNotices {
    public static final String[] Compass = {"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"};

    private RaidNotices() {
    }

    public static void tell(ServerLevel World, BlockPos Near, int Range, int Colour, Component Text) {
        double Reach = (double) Range * Range;
        for (ServerPlayer User : World.players()) {
            double Dx = User.getX() - (Near.getX() + 0.5);
            double Dz = User.getZ() - (Near.getZ() + 0.5);
            if (Dx * Dx + Dz * Dz <= Reach) {
                announce(User, Text, Colour);
            }
        }
    }

    public static Component direction(double Heading) {
        int Slot = Math.floorMod(Math.round((float) (Mth.wrapDegrees(Math.toDegrees(Heading)) / 45.0)), Compass.length);
        return Component.translatable("raid.scp_unleashed.direction." + Compass[Slot]);
    }

    private static void announce(ServerPlayer User, Component Text, int Colour) {
        Notices.send(User, Notice.alert(Text).withAccent(Colour));
    }
}
