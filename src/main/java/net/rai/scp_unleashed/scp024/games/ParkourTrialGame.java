package net.rai.scp_unleashed.scp024.games;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Quips;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

public class ParkourTrialGame implements Scp024Game {
    public static final int Seconds = 75;
    public static final double NetLine = -3.0;
    private int Falls;

    @Override
    public String id() {
        return "parkour_trial";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.parkourTrial(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public double killDepth() {
        return -12.0;
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
        if (Session.height(User) < NetLine && User.onGround()) {
            Falls++;
            Session.toStart();
            Scp024Quips.say(User, "back_to_start");
            Session.cue(Scp024Cue.Laugh);
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.falls", Falls));
        return Scp024Outcome.Continue;
    }
}
