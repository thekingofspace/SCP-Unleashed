package net.rai.scp_unleashed.scp428;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Scp428Hold {
    public static final int OneHolder = 8;
    public static final int TwoHolders = 12;
    public static final int Locked = -1;
    public static final int MaxHolders = 4;

    private final UUID Victim;
    private final List<UUID> Holders = new ArrayList<>();
    private final long Started;
    private int Presses;
    private long LastPress = Long.MIN_VALUE;
    private int AutoReady;
    private int SentCount = -1;
    private int SentPresses = -1;

    public Scp428Hold(UUID Victim, long Started) {
        this.Victim = Victim;
        this.Started = Started;
    }

    public static int required(int Count) {
        if (Count <= 0) {
            return 0;
        }
        if (Count == 1) {
            return OneHolder;
        }
        return Count == 2 ? TwoHolders : Locked;
    }

    public UUID victim() {
        return Victim;
    }

    public long started() {
        return Started;
    }

    public List<UUID> holders() {
        return Holders;
    }

    public int slotOf(UUID Holder) {
        return Holders.indexOf(Holder);
    }

    public boolean has(UUID Holder) {
        return Holders.contains(Holder);
    }

    public void add(UUID Holder) {
        if (!Holders.contains(Holder)) {
            Holders.add(Holder);
        }
    }

    public boolean remove(UUID Holder) {
        return Holders.remove(Holder);
    }

    public int presses() {
        return Presses;
    }

    public boolean press(long Now, int Count) {
        if (required(Count) <= 0 || Now == LastPress) {
            return false;
        }
        LastPress = Now;
        Presses++;
        return true;
    }

    public boolean escaped(int Count) {
        int Need = required(Count);
        return Need > 0 && Presses >= Need;
    }

    public boolean autoTick(int Next) {
        if (--AutoReady > 0) {
            return false;
        }
        AutoReady = Next;
        return true;
    }

    public boolean changed(int Count) {
        if (Count == SentCount && Presses == SentPresses) {
            return false;
        }
        SentCount = Count;
        SentPresses = Presses;
        return true;
    }
}
