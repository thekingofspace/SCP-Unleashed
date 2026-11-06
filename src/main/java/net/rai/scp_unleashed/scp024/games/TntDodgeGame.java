package net.rai.scp_unleashed.scp024.games;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Palette;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.List;

public class TntDodgeGame implements Scp024Game {
    public static final int Seconds = 45;
    public static final int Size = 15;
    public static final int SlowGap = 34;
    public static final int FastGap = 12;
    public static final int Flight = 24;
    public static final float Damage = 10.0f;
    public static final double Radius = 3.5;
    public static final float Aimed = 0.5f;
    private final Droppers Bombs = new Droppers(0.0f, Damage, Radius);
    private int Next;

    @Override
    public String id() {
        return "tnt_dodge";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.tntDodge(Seed);
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
        Next = 10;
    }

    public static void lob(Scp024Session Session, Droppers Bombs, ServerPlayer User, List<int[]> Cannons, int Size, float Aim) {
        int X;
        int Z;
        if (User != null && Session.Random.nextFloat() < Aim) {
            Vec3 Local = Session.local(User.position().add(User.getDeltaMovement().multiply(12.0, 0.0, 12.0)));
            X = Mth.clamp(Mth.floor(Local.x + Session.Random.nextInt(3) - 1), 0, Size - 1);
            Z = Mth.clamp(Mth.floor(Local.z + Session.Random.nextInt(3) - 1), 0, Size - 1);
        } else {
            X = Session.Random.nextInt(Size);
            Z = Session.Random.nextInt(Size);
        }
        Vec3 From;
        if (Cannons.isEmpty()) {
            From = Session.centre(X, 8, Z);
        } else {
            int[] Cannon = Cannons.get(Session.Random.nextInt(Cannons.size()));
            Vec3 Muzzle = Session.centre(Cannon);
            From = Muzzle.add(Scp024Palette.side(Cannon[3]).getStepX() * 0.8, 0.2, Scp024Palette.side(Cannon[3]).getStepZ() * 0.8);
        }
        Bombs.bomb(Session, From, Session.centre(X, 1, Z), Flight);
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        if (Session.ticks() >= Next) {
            lob(Session, Bombs, Session.player(), Session.marks("cannon"), Size, Aimed);
            float Progress = Math.min(1.0f, Session.ticks() / (Seconds * 20.0f));
            Next = Session.ticks() + Math.round(SlowGap + (FastGap - SlowGap) * Progress);
        }
        Bombs.tick(Session);
        return Scp024Outcome.Continue;
    }

    @Override
    public void cleanup(Scp024Session Session) {
        Bombs.clear();
    }
}
