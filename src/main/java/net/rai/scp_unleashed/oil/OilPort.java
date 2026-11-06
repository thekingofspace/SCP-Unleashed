package net.rai.scp_unleashed.oil;

import net.rai.scp_unleashed.pressure.PortRole;

public record OilPort(PortRole Role, int Tank) {
    public static OilPort input(int Tank) {
        return new OilPort(PortRole.Input, Tank);
    }

    public static OilPort output(int Tank) {
        return new OilPort(PortRole.Output, Tank);
    }
}
