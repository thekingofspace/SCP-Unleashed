package net.rai.scp_unleashed.guns.client;

import java.util.ArrayDeque;

public final class GunAmmoPredictor {
    public static final int Timeout = 60;
    private final ArrayDeque<long[]> Pending = new ArrayDeque<>();
    private int NextSequence = 1;
    private int Owner = Integer.MIN_VALUE;
    private int StackAmmo = Integer.MIN_VALUE;
    private int Authority;

    public int fire(long Tick, boolean Predict) {
        int Sequence = NextSequence;
        NextSequence = NextSequence == Integer.MAX_VALUE ? 1 : NextSequence + 1;
        if (Predict) {
            Pending.addLast(new long[]{Sequence, Tick});
        }
        return Sequence;
    }

    public void acknowledge(int Sequence, int Ammo) {
        boolean Known = false;
        for (long[] Entry : Pending) {
            if (Entry[0] == Sequence) {
                Known = true;
                break;
            }
        }
        if (!Known) {
            return;
        }
        while (!Pending.isEmpty()) {
            long Front = Pending.pollFirst()[0];
            if (Front == Sequence) {
                break;
            }
        }
        if (Ammo >= 0) {
            Authority = Ammo;
        }
    }

    public int display(int OwnerKey, int Ammo, long Tick) {
        if (OwnerKey != Owner) {
            Owner = OwnerKey;
            Pending.clear();
            StackAmmo = Ammo;
            Authority = Ammo;
        } else if (Ammo != StackAmmo) {
            StackAmmo = Ammo;
            Authority = Ammo;
        }
        while (!Pending.isEmpty() && Tick - Pending.peekFirst()[1] > Timeout) {
            Pending.pollFirst();
        }
        return Math.max(0, Authority - Pending.size());
    }

    public int pending() {
        return Pending.size();
    }

    public void reset() {
        Pending.clear();
        Owner = Integer.MIN_VALUE;
        StackAmmo = Integer.MIN_VALUE;
        Authority = 0;
    }
}
