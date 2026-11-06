package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

public class MazeRunGame implements Scp024Game {
    public static final int Seconds = 75;

    @Override
    public String id() {
        return "maze_run";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.mazeRun(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
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
        if (Session.ticks() % 10 == 0) {
            Vec3 Exit = Session.centre(Session.mark("finish"));
            Session.World.sendParticles(ParticleTypes.HAPPY_VILLAGER, Exit.x, Exit.y + 0.4, Exit.z, 2, 0.3, 0.3, 0.3, 0.0);
        }
        return Session.onFinish(User) ? Scp024Outcome.Win : Scp024Outcome.Continue;
    }
}
