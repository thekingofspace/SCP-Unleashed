package net.rai.scp_unleashed.scp140.client;

import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;
import net.rai.scp_unleashed.scp140.network.Scp140ChroniclePayload;
import org.jetbrains.annotations.Nullable;

public final class Scp140Remains {
    public static final String Prefix = "gui.scp_unleashed.scp140.remains.";
    public static final double NearPaces = 24.0;
    private static final String[] Compass = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};

    private Scp140Remains() {
    }

    public static String subject(int Page) {
        if (Page < Scp140Pages.Awakening) {
            return "ruin";
        }
        return switch (Page - Scp140Pages.Awakening) {
            case 0 -> "temple";
            case 1 -> "banners";
            case 2 -> "knights";
            default -> "circles";
        };
    }

    public static String compass(int OffsetX, int OffsetZ) {
        double Bearing = Math.toDegrees(Math.atan2(OffsetX, -OffsetZ));
        int Index = Math.floorMod((int) Math.round(Bearing / 45.0), Compass.length);
        return Compass[Index];
    }

    public static long paces(double Distance) {
        double Step = Distance < 100.0 ? 5.0 : Distance < 1000.0 ? 10.0 : 50.0;
        return Math.max((long) Step, Math.round(Distance / Step) * (long) Step);
    }

    public static String line(int Page, @Nullable Scp140ChroniclePayload.PageEntry Entry) {
        if (Entry == null || !Entry.HasSite()) {
            return "";
        }
        Component Subject = Component.translatable(Prefix + "subject." + subject(Page));
        if (!Entry.SameRealm()) {
            return Scp140Script.clean(Component.translatable(Prefix + "elsewhere", Subject).getString());
        }
        double Distance = Math.sqrt((double) Entry.OffsetX() * Entry.OffsetX() + (double) Entry.OffsetZ() * Entry.OffsetZ());
        if (Distance < NearPaces) {
            return Scp140Script.clean(Component.translatable(Prefix + "near", Subject).getString());
        }
        Component Heading = Component.translatable("gui.scp_unleashed.scp140.direction." + compass(Entry.OffsetX(), Entry.OffsetZ()));
        return Scp140Script.clean(Component.translatable(Prefix + "far", Subject, String.valueOf(paces(Distance)), Heading).getString());
    }
}
