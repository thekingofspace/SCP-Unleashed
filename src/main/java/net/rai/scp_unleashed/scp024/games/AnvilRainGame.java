package net.rai.scp_unleashed.scp024.games;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

public class AnvilRainGame implements Scp024Game {
    public static final int Seconds = 40;
    public static final int Size = 13;
    public static final int SlowGap = 18;
    public static final int FastGap = 5;
    public static final double DropHeight = 12.0;
    public static final float Damage = 12.0f;
    public static final float Aimed = 0.45f;
    private final Droppers Anvils = new Droppers(Damage, 0.0f, 0.0);
    private int Next;

    @Override
    public String id() {
        return "anvil_rain";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.anvilRain(Seed);
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

    public static void drop(Scp024Session Session, Droppers Anvils, ServerPlayer User, int Size, float Aim, double Height) {
        int X;
        int Z;
        if (User != null && Session.Random.nextFloat() < Aim) {
            Vec3 Local = Session.local(User.position().add(User.getDeltaMovement().multiply(8.0, 0.0, 8.0)));
            X = Mth.clamp(Mth.floor(Local.x), 0, Size - 1);
            Z = Mth.clamp(Mth.floor(Local.z), 0, Size - 1);
        } else {
            X = Session.Random.nextInt(Size);
            Z = Session.Random.nextInt(Size);
        }
        Anvils.anvil(Session, Session.centre(X, 1, Z), Height);
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (Session.ticks() >= Next) {
            drop(Session, Anvils, User, Size, Aimed, DropHeight);
            float Progress = Math.min(1.0f, Session.ticks() / (Seconds * 20.0f));
            Next = Session.ticks() + Math.round(SlowGap + (FastGap - SlowGap) * Progress);
        }
        Anvils.tick(Session);
        return Scp024Outcome.Continue;
    }

    @Override
    public void cleanup(Scp024Session Session) {
        Anvils.clear();
    }
}
