package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class Scp1730Power {
    private Scp1730Power() {
    }

    public static final class Cache {
        private int Zone = -2;
        private int Run = -1;

        public boolean powered(Level World, BlockPos Pos) {
            if (Scp1730World.inside(World) && World.getServer() != null) {
                Scp1730State State = Scp1730State.get(World.getServer());
                if (Zone == -2 || Run != State.run()) {
                    Run = State.run();
                    Zone = Scp1730World.zoneOf(Run, Pos);
                }
                return State.powered(Zone);
            }
            return World.hasNeighborSignal(Pos);
        }

        public int zone() {
            return Zone;
        }
    }
}
