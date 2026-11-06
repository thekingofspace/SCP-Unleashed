package net.rai.scp_unleashed.forcefield.client;

import java.util.Comparator;

public final class ForcefieldDrawPlan {
    public static final boolean SheetCulls = false;
    public static final boolean GlowCulls = false;
    public static final boolean StampCulls = false;
    public static final boolean SheetWritesDepth = false;
    public static final boolean GlowWritesDepth = false;
    public static final boolean StampWritesDepth = true;
    public static final boolean StampWritesColour = false;
    public static final boolean GlowKeepsAlpha = true;
    public static final Comparator<Double> FarFirst = Comparator.reverseOrder();

    private ForcefieldDrawPlan() {
    }

    public enum Pass {
        Direct,
        AfterBlockEntities,
        AfterParticles
    }

    public static Pass pass(boolean Fallback, boolean Fabulous) {
        if (Fallback) {
            return Pass.Direct;
        }
        return Fabulous ? Pass.AfterParticles : Pass.AfterBlockEntities;
    }

    public static boolean deferred(Pass Chosen) {
        return Chosen != Pass.Direct;
    }

    public static boolean flushesSceneFirst(Pass Chosen) {
        return Chosen == Pass.AfterBlockEntities;
    }

    public static boolean stampsDepth(Pass Chosen) {
        return Chosen == Pass.AfterParticles;
    }

    public static boolean bindsParticleTarget(Pass Chosen) {
        return Chosen == Pass.AfterParticles;
    }
}
