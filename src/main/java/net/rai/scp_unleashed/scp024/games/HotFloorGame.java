package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

public class HotFloorGame implements Scp024Game {
    public static final int Seconds = 40;
    public static final int Size = 15;
    public static final int SlowGap = 8;
    public static final int FastGap = 3;
    public static final double MaxShare = 0.6;
    public static final float NearPlayer = 0.5f;
    private final HotTiles Floor = new HotTiles(0, 0, Size - 1, Size - 1, MaxShare);
    private int Next;

    @Override
    public String id() {
        return "hot_floor";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.hotFloor(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public boolean survival() {
        return true;
    }

    @Override
    public void start(Scp024Session Session) {
        Next = 5;
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (Session.ticks() >= Next) {
            if (User != null && Session.Random.nextFloat() < NearPlayer) {
                BlockPos Cell = Session.standing(User);
                Floor.heatNear(Session, Cell.getX(), Cell.getZ(), 2);
            } else {
                Floor.heatAnywhere(Session);
            }
            float Progress = Math.min(1.0f, Session.ticks() / (Seconds * 20.0f));
            Next = Session.ticks() + Math.round(SlowGap + (FastGap - SlowGap) * Progress);
        }
        Floor.tick(Session);
        return Scp024Outcome.Continue;
    }
}
