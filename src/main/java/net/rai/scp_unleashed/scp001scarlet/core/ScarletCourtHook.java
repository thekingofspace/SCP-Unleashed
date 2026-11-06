package net.rai.scp_unleashed.scp001scarlet.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public interface ScarletCourtHook {
    void begin(ServerLevel Level, BlockPos Dais, LivingEntity King, int Players);

    void tick(ServerLevel Level, LivingEntity King);

    int daughtersRemaining();

    boolean daughterActive();

    void kingPhase(int Phase);

    void reset(ServerLevel Level);

    void end(ServerLevel Level);

    final class Holder {
        private static ScarletCourtHook Current;

        private Holder() {
        }

        public static void set(ScarletCourtHook Hook) {
            Current = Hook;
        }

        public static ScarletCourtHook get() {
            return Current;
        }
    }
}
