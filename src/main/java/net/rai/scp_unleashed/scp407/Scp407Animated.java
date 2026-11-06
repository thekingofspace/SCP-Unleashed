package net.rai.scp_unleashed.scp407;

import org.jetbrains.annotations.Nullable;

public interface Scp407Animated {
    @Nullable
    String actionClip();

    double actionElapsed();

    @Nullable
    Scp407Gait gait();
}
