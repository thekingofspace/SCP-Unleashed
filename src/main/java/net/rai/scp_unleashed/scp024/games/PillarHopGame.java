package net.rai.scp_unleashed.scp024.games;

import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

public class PillarHopGame implements Scp024Game {
    public static final int Seconds = 90;
    public static final double NearFall = -0.45;

    @Override
    public String id() {
        return "pillar_hop";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.pillarHop(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public double killDepth() {
        return -3.0;
    }

    @Override
    public void start(Scp024Session Session) {
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (User == null) {
            return Scp024Outcome.Continue;
        }
        if (Session.onFinish(User)) {
            return Scp024Outcome.Win;
        }
        if (!User.onGround() && User.getDeltaMovement().y < NearFall && Session.height(User) > -1.5) {
            Session.taunt("near");
        }
        return Scp024Outcome.Continue;
    }
}
