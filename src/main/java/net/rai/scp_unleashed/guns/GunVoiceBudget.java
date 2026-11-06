package net.rai.scp_unleashed.guns;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.List;

public final class GunVoiceBudget<T> {
    public static final int ClientLimit = 48;
    public static final String ManagedPrefix = "guns_";
    private final int Limit;
    private final List<Voice<T>> Active = new ArrayList<>();

    public GunVoiceBudget(int Limit) {
        this.Limit = Math.max(1, Limit);
    }

    public static boolean managed(ResourceLocation Id) {
        return Scp_unleashed.ModId.equals(Id.getNamespace()) && Id.getPath().startsWith(ManagedPrefix);
    }

    public int size(long Now) {
        expire(Now);
        return Active.size();
    }

    public boolean offer(T Handle, float Gain, long Now, int Ticks, List<T> Stolen) {
        if (Active.size() >= Limit) {
            expire(Now);
        }
        Voice<T> Fresh = new Voice<>(Handle, Now + Math.max(1, Ticks), Math.max(1, Ticks), Gain);
        if (Active.size() < Limit) {
            Active.add(Fresh);
            return true;
        }
        int Quietest = -1;
        float Lowest = Float.MAX_VALUE;
        for (int Index = 0; Index < Active.size(); Index++) {
            float Left = Active.get(Index).remaining(Now);
            if (Left < Lowest) {
                Lowest = Left;
                Quietest = Index;
            }
        }
        if (Quietest < 0 || Gain <= Lowest) {
            return false;
        }
        Stolen.add(Active.get(Quietest).Handle);
        Active.set(Quietest, Fresh);
        return true;
    }

    public void clear() {
        Active.clear();
    }

    private void expire(long Now) {
        int Kept = 0;
        for (int Index = 0; Index < Active.size(); Index++) {
            Voice<T> Each = Active.get(Index);
            if (Each.End > Now) {
                Active.set(Kept++, Each);
            }
        }
        for (int Index = Active.size() - 1; Index >= Kept; Index--) {
            Active.remove(Index);
        }
    }

    private static final class Voice<T> {
        private final T Handle;
        private final long End;
        private final int Length;
        private final float Gain;

        private Voice(T Handle, long End, int Length, float Gain) {
            this.Handle = Handle;
            this.End = End;
            this.Length = Length;
            this.Gain = Gain;
        }

        private float remaining(long Now) {
            return Gain * Math.max(0L, End - Now) / Length;
        }
    }
}
