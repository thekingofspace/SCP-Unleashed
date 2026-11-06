package net.rai.scp_unleashed.scp256;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class Scp256Typewriters {
    private static final Set<Scp256TypewriterBlockEntity> Loaded = Collections.newSetFromMap(new WeakHashMap<>());

    private Scp256Typewriters() {
    }

    public static void track(Scp256TypewriterBlockEntity Typewriter) {
        if (Typewriter.getLevel() != null && Typewriter.getLevel().isClientSide) {
            Loaded.add(Typewriter);
        }
    }

    public static void forget(Scp256TypewriterBlockEntity Typewriter) {
        Loaded.remove(Typewriter);
    }

    @Nullable
    public static Scp256TypewriterBlockEntity nearest(Vec3 Where, double Reach, long Now, long Linger) {
        Scp256TypewriterBlockEntity Best = null;
        double BestDistance = Reach * Reach;
        for (Scp256TypewriterBlockEntity Typewriter : Loaded) {
            if (Typewriter.isRemoved() || !Typewriter.hasMessage()) {
                continue;
            }
            long Elapsed = Now - Typewriter.getStarted();
            if (Elapsed < 0 || Elapsed > Typewriter.getDuration() + Linger) {
                continue;
            }
            double Distance = Vec3.atCenterOf(Typewriter.getBlockPos()).distanceToSqr(Where);
            if (Distance <= BestDistance) {
                BestDistance = Distance;
                Best = Typewriter;
            }
        }
        return Best;
    }

    public static void clear() {
        Loaded.clear();
    }
}
