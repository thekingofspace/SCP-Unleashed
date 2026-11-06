package net.rai.scp_unleashed.scp093;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class Scp093TorsoNodes extends WalkNodeEvaluator {
    public double centreShift() {
        return (entityWidth - 1) / 2.0;
    }

    @Override
    public Node getStart() {
        Node Plain = super.getStart();
        if (Plain == null) {
            return null;
        }
        double Shift = centreShift();
        return getStartNode(new BlockPos(Mth.floor(mob.getX() - Shift), Plain.y, Mth.floor(mob.getZ() - Shift)));
    }

    @Override
    public Target getTarget(double X, double Y, double Z) {
        int Shift = (entityWidth - 1) / 2;
        return super.getTarget(X - Shift, Y, Z - Shift);
    }
}
