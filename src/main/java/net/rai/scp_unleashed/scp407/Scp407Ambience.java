package net.rai.scp_unleashed.scp407;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

public final class Scp407Ambience {
    public static final int TickBudget = 40;
    public static final int SectionBudget = 8;
    public static final int PuffCount = 7;
    private static final Long2IntOpenHashMap Sections = new Long2IntOpenHashMap();
    private static long Tick = Long.MIN_VALUE;
    private static int Spent;

    private Scp407Ambience() {
    }

    public static synchronized boolean allow(Level World, BlockPos Pos, int Cost) {
        long Now = World.getGameTime();
        if (Now != Tick) {
            Tick = Now;
            Spent = 0;
            Sections.clear();
        }
        if (Spent + Cost > TickBudget) {
            return false;
        }
        long Key = SectionPos.asLong(Pos);
        int Used = Sections.get(Key);
        if (Used + Cost > SectionBudget) {
            return false;
        }
        Sections.put(Key, Used + Cost);
        Spent += Cost;
        return true;
    }
}
