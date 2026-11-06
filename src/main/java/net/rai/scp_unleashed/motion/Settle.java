package net.rai.scp_unleashed.motion;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Settle<T> {
    @Nullable
    private T Pending;
    private int Count;

    public boolean ready(@Nullable T Current, @Nullable T Wanted, int Need) {
        if (Objects.equals(Current, Wanted)) {
            clear();
            return false;
        }
        if (Count == 0 || !Objects.equals(Pending, Wanted)) {
            Pending = Wanted;
            Count = 0;
        }
        Count++;
        return Count >= Need;
    }

    public int count() {
        return Count;
    }

    public void clear() {
        Pending = null;
        Count = 0;
    }
}
