package net.rai.scp_unleashed.wire;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public final class WireChannels {
    public static final String Everything = "*";
    public static final String GroupSuffix = "/*";
    public static final String AnyType = "*/";
    public static final String Redstone = "redstone";
    public static final String Power = "power";
    public static final String Alarm = "alarm";
    public static final String Doors = "doors";
    public static final String Lights = "lights";
    public static final String Containment = "containment";
    public static final String Security = "security";
    public static final String Data = "data";
    public static final String Entity = "entity";
    public static final int MaxLength = 64;
    public static final int MaxPort = 9999;
    public static final int PortDigits = 4;
    public static final List<String> Standard = List.of(Redstone, Power, Alarm, Doors, Lights, Containment, Security, Data, Entity);
    public static final String AnyRedstonePort = group(Redstone);

    private WireChannels() {
    }

    public static int clampPort(int Port) {
        return Mth.clamp(Port, 0, MaxPort);
    }

    public static String onPort(String Type, int Port) {
        return Type + "/" + clampPort(Port);
    }

    public static String redstonePort(int Port) {
        return onPort(Redstone, Port);
    }

    public static String anyOnPort(int Port) {
        return AnyType + clampPort(Port);
    }

    public static String group(String Channel) {
        return Channel + GroupSuffix;
    }

    public static String typeOf(String Channel) {
        int Slash = Channel.indexOf('/');
        return Slash < 0 ? Channel : Channel.substring(0, Slash);
    }

    public static int portOf(String Channel) {
        int Slash = Channel.lastIndexOf('/');
        return Slash < 0 ? -1 : parsePort(Channel.substring(Slash + 1));
    }

    private static int parsePort(String Text) {
        if (Text.isEmpty() || Text.length() > PortDigits) {
            return -1;
        }
        int Port = 0;
        for (int Index = 0; Index < Text.length(); Index++) {
            char Letter = Text.charAt(Index);
            if (Letter < '0' || Letter > '9') {
                return -1;
            }
            Port = Port * 10 + (Letter - '0');
        }
        return Port;
    }

    public static boolean isValidChannel(String Channel) {
        if (Channel == null || Channel.isEmpty() || Channel.length() > MaxLength) {
            return false;
        }
        boolean SegmentStart = true;
        for (int Index = 0; Index < Channel.length(); Index++) {
            char Letter = Channel.charAt(Index);
            if (Letter == '/') {
                if (SegmentStart) {
                    return false;
                }
                SegmentStart = true;
            } else if ((Letter >= 'a' && Letter <= 'z') || (Letter >= '0' && Letter <= '9') || Letter == '_' || Letter == '-' || Letter == '.') {
                SegmentStart = false;
            } else {
                return false;
            }
        }
        return !SegmentStart;
    }

    public static boolean isValidSubscription(String Subscription) {
        if (Subscription == null) {
            return false;
        }
        if (Everything.equals(Subscription)) {
            return true;
        }
        if (Subscription.startsWith(AnyType)) {
            return parsePort(Subscription.substring(AnyType.length())) >= 0;
        }
        if (Subscription.endsWith(GroupSuffix)) {
            return Subscription.length() <= MaxLength && isValidChannel(Subscription.substring(0, Subscription.length() - GroupSuffix.length()));
        }
        return isValidChannel(Subscription);
    }

    public static List<String> subscriptionsMatching(String Channel) {
        List<String> Keys = new ArrayList<>(5);
        Keys.add(Channel);
        int Slash = Channel.lastIndexOf('/');
        while (Slash > 0) {
            Keys.add(Channel.substring(0, Slash) + GroupSuffix);
            Slash = Channel.lastIndexOf('/', Slash - 1);
        }
        int Port = portOf(Channel);
        if (Port >= 0) {
            Keys.add(AnyType + Port);
        }
        Keys.add(Everything);
        return Keys;
    }
}
