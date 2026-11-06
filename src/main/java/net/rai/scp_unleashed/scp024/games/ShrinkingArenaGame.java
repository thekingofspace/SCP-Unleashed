package net.rai.scp_unleashed.scp024.games;

import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

public class ShrinkingArenaGame implements Scp024Game {
    public static final int Seconds = 45;
    public static final int Centre = 9;
    public static final int Outer = 8;
    public static final int Innermost = 2;
    public static final int RingEvery = 120;
    public static final int StripeEvery = 50;
    public static final int StripeStart = 200;
    public static final int Warning = 30;
    private final TileDrops Drops = new TileDrops();
    private int Radius = Outer;

    @Override
    public String id() {
        return "shrinking_arena";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.shrinkingArena(Seed);
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
    public double killDepth() {
        return -3.0;
    }

    @Override
    public void start(Scp024Session Session) {
    }

    private void ring(Scp024Session Session) {
        for (int X = Centre - Radius; X <= Centre + Radius; X++) {
            for (int Z = Centre - Radius; Z <= Centre + Radius; Z++) {
                if (Math.max(Math.abs(X - Centre), Math.abs(Z - Centre)) == Radius) {
                    Drops.schedule(Session, X, 0, Z, Warning, Scp024Mat.RedConcrete);
                }
            }
        }
        Radius--;
        Session.cue(Scp024Cue.Ooh);
    }

    private void stripe(Scp024Session Session) {
        boolean Rows = Session.Random.nextBoolean();
        int Line = Centre - Radius + Session.Random.nextInt(Radius * 2 + 1);
        if (Math.abs(Line - Centre) <= 1) {
            return;
        }
        for (int Along = Centre - Radius; Along <= Centre + Radius; Along++) {
            int X = Rows ? Along : Line;
            int Z = Rows ? Line : Along;
            Drops.schedule(Session, X, 0, Z, Warning, Scp024Mat.OrangeConcrete);
        }
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        int Elapsed = Session.ticks();
        if (Elapsed > 0 && Elapsed % RingEvery == 0 && Radius > Innermost) {
            ring(Session);
        }
        if (Elapsed >= StripeStart && Elapsed % StripeEvery == 0 && Radius > 3) {
            stripe(Session);
        }
        Drops.tick(Session);
        if (User != null && !User.onGround() && User.getDeltaMovement().y < -0.4 && Session.height(User) > -1.0) {
            Session.taunt("near");
        }
        return Scp024Outcome.Continue;
    }
}
