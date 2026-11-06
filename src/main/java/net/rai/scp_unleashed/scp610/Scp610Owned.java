package net.rai.scp_unleashed.scp610;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.util.RandomSource;

public final class Scp610Owned {
    private final LongArrayList Order = new LongArrayList();
    private final Long2IntOpenHashMap Index = new Long2IntOpenHashMap();

    public Scp610Owned() {
        Index.defaultReturnValue(-1);
    }

    public int size() {
        return Order.size();
    }

    public boolean contains(long Pos) {
        return Index.containsKey(Pos);
    }

    public boolean add(long Pos) {
        if (Index.containsKey(Pos)) {
            return false;
        }
        Index.put(Pos, Order.size());
        Order.add(Pos);
        return true;
    }

    public boolean remove(long Pos) {
        int At = Index.remove(Pos);
        if (At < 0) {
            return false;
        }
        int Last = Order.size() - 1;
        long Moved = Order.getLong(Last);
        Order.removeLong(Last);
        if (At != Last) {
            Order.set(At, Moved);
            Index.put(Moved, At);
        }
        return true;
    }

    public long get(int At) {
        return Order.getLong(At);
    }

    public long random(RandomSource Random) {
        return Order.getLong(Random.nextInt(Order.size()));
    }

    public long[] toArray() {
        return Order.toLongArray();
    }

    public void clear() {
        Order.clear();
        Index.clear();
    }
}
