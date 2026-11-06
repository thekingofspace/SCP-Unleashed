package net.rai.scp_unleashed.scp001;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;

public final class Scp001Restraint {
    private Scp001Restraint() {
    }

    public static boolean holds(Level World, Vec3 Centre) {
        return Scp1730Anchors.folded(World, Centre);
    }

    public static boolean pins(Level World, Vec3 Centre) {
        return Scp1730Anchors.anchored(World, Centre);
    }
}
